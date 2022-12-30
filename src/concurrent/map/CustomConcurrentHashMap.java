package concurrent.map;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import static java.lang.Math.abs;

public class CustomConcurrentHashMap<K, V> implements CustomConcurrentMap<K, V> {
    private static final int ZERO = 0;

    private static final int ONE = 1;
    private static final int ONE_NEGATIVE = -1;
    private static final int TWO = 2;

    private static final int RESIZE_RATIO = 2;

    private static final int EXT_BUCKET_RELEASES_PER_EXT_SYNC_BUCKET_IDXS_RELEASE_INITIAL_SHIFT = 100;

    private Node<K, V>[] buckets;

    private final int initialCapacity;
    private int capacity;
    private final double loadFactor;
    private int size;

    /**
     * Needed for multithreaded resize.
     */
    private volatile boolean blockedToResizeFlag;
    private AtomicIntegerArray emptyBucketsLocks;
    private AtomicInteger getPutOpsInProgressNum;

    /**
     * externalBucketLocks are not needed for ConcurrentHashMap implementation. They are needed for ensuring that in
     * case of resize get from bucket and put to the bucket operations executed by the resize initiating thread happen
     * before get and put operations for the same bucket executed by another not resize initiating thread.
     */
    private List<AtomicInteger> externalBucketsLocks;
    private List<AtomicInteger> externalBucketsCalcIdxOpsInProgressNums;

    /**
     * -1(ONE_NEGATIVE) index means that index of bucket lock which should be used to lock external operations on particular
     * bucket BUCKET1 can be calculated as the index of the BUCKET1 itself. Other indexes are indexes of bucket lock
     * which should be used to lock external operations on BUCKET1.
     */
    private List<AtomicInteger> externalBucketLocksForBucketsIdxs;

    private int externalBucketsReleasesCounter;

    /**
     * Amount of external bucket locks' releasing operations which should be performed before the next releasing
     * update(setting all not acquired external bucket locks' indexes to -1) of indexes of external bucket locks provided
     * to threads to lock a particular bucket.
     * The minimal value is equal to initialCapacity * loadFactor in case of ideal hash function. Because the releasing
     * update isn't needed before the first resize.
     */
    private final int extBucketReleasesPerExtSyncBucketIdxsRelease;

    private class Node<K, V> {
        private final int hash;
        private final K key;
        private V value;
        private Node<K, V> next;

        public Node(K key, V value) {
            this.key = key;
            this.value = value;
            this.hash = abs(key.hashCode());
        }
    }

    public CustomConcurrentHashMap(int initialCapacity, double loadFactor) {
        this.initialCapacity = initialCapacity;
        this.capacity = initialCapacity;
        this.loadFactor = loadFactor;
        this.size = 0;
        this.buckets = new Node[initialCapacity];
        this.emptyBucketsLocks = new AtomicIntegerArray(capacity);
        this.externalBucketsLocks = new ArrayList<>(capacity);
        this.externalBucketsCalcIdxOpsInProgressNums = new ArrayList<>(capacity);
        this.externalBucketLocksForBucketsIdxs = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            externalBucketsLocks.add(new AtomicInteger(ZERO));
            externalBucketsCalcIdxOpsInProgressNums.add(new AtomicInteger(ZERO));
            externalBucketLocksForBucketsIdxs.add(new AtomicInteger(ONE_NEGATIVE));
        }
        this.getPutOpsInProgressNum = new AtomicInteger(ZERO);
        this.externalBucketsReleasesCounter = 0;
        this.extBucketReleasesPerExtSyncBucketIdxsRelease = (int) (initialCapacity * loadFactor) +
                EXT_BUCKET_RELEASES_PER_EXT_SYNC_BUCKET_IDXS_RELEASE_INITIAL_SHIFT;
    }

    @Override
    public void put(K key, V value) throws InterruptedException {
        Node nodeToPut = new Node(key, value);
        put(nodeToPut);
    }

    @Override
    public void putWithExternalBucketLock(K key, V value) throws InterruptedException {
        Node nodeToPut = new Node(key, value);
        putWithExternalLock(nodeToPut);
    }

    @Override
    public V get(K key) {
        synchronized (this) {
            getPutOpsInProgressNum.incrementAndGet();
        }
        int keyHash = abs(key.hashCode());
        int keyBucketIdx = keyHash % capacity;
        if (buckets[keyBucketIdx] == null) {
            getPutOpsInProgressNum.decrementAndGet();
            return null;
        } else {
            Node<K, V> keyBucketFirstNode = buckets[keyBucketIdx];
            synchronized (keyBucketFirstNode) {
                Node<K, V> keyBucketCurNode = keyBucketFirstNode;
                while (keyBucketCurNode != null) {
                    if (keyBucketCurNode.key.equals(key)) {
                        getPutOpsInProgressNum.decrementAndGet();
                        return keyBucketCurNode.value;
                    }
                    keyBucketCurNode = keyBucketCurNode.next;
                }
                getPutOpsInProgressNum.decrementAndGet();
                return null;
            }
        }
    }

    @Override
    public V getWithoutLock(K key) {
        int keyHash = abs(key.hashCode());
        int keyBucketIdx = keyHash % capacity;
        if (buckets[keyBucketIdx] == null) {
            return null;
        } else {
            Node<K, V> keyBucketCurNode = buckets[keyBucketIdx];
            while (keyBucketCurNode != null) {
                if (keyBucketCurNode.key.equals(key)) {
                    getPutOpsInProgressNum.decrementAndGet();
                    return keyBucketCurNode.value;
                }
                keyBucketCurNode = keyBucketCurNode.next;
            }
            return null;
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public synchronized int calculateExternalBucketLockIdxForKey(K key) {
        int bucketIdx = abs(key.hashCode()) % capacity;
        int curExternalBucketLockForBucketIdx = externalBucketLocksForBucketsIdxs.get(bucketIdx).get();
        if (curExternalBucketLockForBucketIdx == ONE_NEGATIVE) {
            curExternalBucketLockForBucketIdx = bucketIdx;
        }
        int externalBucketCalcIdxOpsInProgressNum = externalBucketsCalcIdxOpsInProgressNums.get(bucketIdx).get();
        externalBucketsCalcIdxOpsInProgressNums.get(bucketIdx).set(externalBucketCalcIdxOpsInProgressNum + 1);
        return curExternalBucketLockForBucketIdx;
    }

    @Override
    public void acquireExternalBucketLock(int bucketIdx) {
        while (!externalBucketsLocks.get(bucketIdx).compareAndSet(ZERO, ONE)) ;
    }

    @Override
    public boolean releaseExternalBucketLock(int bucketIdx) {
        int curExternalBucketCalcOpsInProgressNum = externalBucketsCalcIdxOpsInProgressNums.get(bucketIdx).get();
        externalBucketsCalcIdxOpsInProgressNums.get(bucketIdx).compareAndSet(curExternalBucketCalcOpsInProgressNum,
                curExternalBucketCalcOpsInProgressNum - 1);

        //System.out.println(externalBucketLocksForBucketsIdxs.stream().map(idx -> String.valueOf(idx.get())).reduce((idxes, newIdx) -> idxes + ", " + newIdx));
        updateExternalBucketLocksIdxsForBuckets(bucketIdx);

        boolean success = externalBucketsLocks.get(bucketIdx).compareAndSet(ONE, ZERO);
        return success;
    }

    private void putWithExternalLock(Node nodeToPut) throws InterruptedException {
        int bucketIdx = 0;
        synchronized (this) {
            if (size > capacity * loadFactor) {
                while (getPutOpsInProgressNum.get() != 0) ;
                resizeWithExternalLocks();
            }
            getPutOpsInProgressNum.incrementAndGet();
        }
        bucketIdx = nodeToPut.hash % capacity;
        if (buckets[bucketIdx] == null) {
            buckets[bucketIdx] = nodeToPut;
            size++;
            getPutOpsInProgressNum.decrementAndGet();
            return;
        }
        Node bucketFirstNode = buckets[bucketIdx];
        Node curNode = bucketFirstNode;
        while (curNode.next != null) {
            if (curNode.key.equals(nodeToPut.key)) {
                curNode.value = nodeToPut.value;
                getPutOpsInProgressNum.decrementAndGet();
                return;
            }
            curNode = curNode.next;
        }
        if (curNode.key.equals(nodeToPut.key)) {
            curNode.value = nodeToPut.value;
        } else {
            curNode.next = nodeToPut;
        }
        getPutOpsInProgressNum.decrementAndGet();
    }

    private void put(Node nodeToPut) throws InterruptedException {
        synchronized (this) {
            if (size > capacity * loadFactor) {
                while (getPutOpsInProgressNum.get() != 0) ;
                resize();
            }
            getPutOpsInProgressNum.incrementAndGet();
        }
        int bucketIdx = nodeToPut.hash % capacity;
        if (buckets[bucketIdx] == null) {
            if (emptyBucketsLocks.compareAndSet(bucketIdx, ZERO, ONE)) {
                buckets[bucketIdx] = nodeToPut;
                size++;
                emptyBucketsLocks.compareAndSet(bucketIdx, ONE, TWO);
                getPutOpsInProgressNum.decrementAndGet();
                return;
            } else {
                while (!emptyBucketsLocks.compareAndSet(bucketIdx, TWO, ONE)) ;
                emptyBucketsLocks.compareAndSet(bucketIdx, ONE, TWO);
            }
        }
        Node bucketFirstNode = buckets[bucketIdx];
        synchronized (bucketFirstNode) {
            Node curNode = bucketFirstNode;
            while (curNode.next != null) {
                if (curNode.key.equals(nodeToPut.key)) {
                    curNode.value = nodeToPut.value;
                    getPutOpsInProgressNum.decrementAndGet();
                    return;
                }
                curNode = curNode.next;
            }
            if (curNode.key.equals(nodeToPut.key)) {
                curNode.value = nodeToPut.value;
            } else {
                curNode.next = nodeToPut;
            }
            getPutOpsInProgressNum.decrementAndGet();
        }
    }

    private void putForResizeInSingleThread(Node nodeToPut) {
        Node nodeToPutCopy = new Node(nodeToPut.key, nodeToPut.value);
        nodeToPutCopy.next = null;
        int bucketIdx = nodeToPutCopy.hash % capacity;
        if (buckets[bucketIdx] == null) {
            buckets[bucketIdx] = nodeToPutCopy;
            size++;
        } else {
            Node curNode = buckets[bucketIdx];
            while (curNode.next != null) {
                if (curNode.key.equals(nodeToPutCopy.key)) {
                    curNode.value = nodeToPut.value;
                    return;
                }
                curNode = curNode.next;
            }
            if (curNode.key.equals(nodeToPutCopy.key)) {
                curNode.value = nodeToPutCopy.value;
            } else {
                curNode.next = nodeToPutCopy;
            }
        }
    }

    private void resize() {
        Node[] prevBuckets;
        prevBuckets = buckets;

        int prevCapacity = capacity;
        capacity = capacity * RESIZE_RATIO;
        size = 0;
        buckets = new Node[capacity];
        emptyBucketsLocks = new AtomicIntegerArray(capacity);

        for (int i = 0; i < prevCapacity; i++) {
            externalBucketsLocks.add(new AtomicInteger(ZERO));
        }
        Node curBucketNode;
        for (Node curBucketHeadNode : prevBuckets) {
            curBucketNode = curBucketHeadNode;
            while (curBucketNode != null) {
                putForResizeInSingleThread(curBucketNode);
                curBucketNode = curBucketNode.next;
            }
        }
    }

    private void resizeWithExternalLocks() {
        Node[] prevBuckets;
        prevBuckets = buckets;

        int prevCapacity = capacity;
        capacity = capacity * RESIZE_RATIO;
        size = 0;
        buckets = new Node[capacity];
        emptyBucketsLocks = new AtomicIntegerArray(capacity);

        for (int i = 0; i < prevCapacity; i++) {
            externalBucketsLocks.add(new AtomicInteger(ZERO));
            externalBucketsCalcIdxOpsInProgressNums.add(new AtomicInteger(ZERO));
            externalBucketLocksForBucketsIdxs.add(new AtomicInteger(ONE_NEGATIVE));
        }

        AtomicInteger curBaseExternalBucketLockForBucketIdx;
        for (int i = 0; i < prevCapacity; i++) {
            if (externalBucketsCalcIdxOpsInProgressNums.get(i).get() > 0) {
                curBaseExternalBucketLockForBucketIdx = externalBucketLocksForBucketsIdxs.get(i);
                if (curBaseExternalBucketLockForBucketIdx.get() == ONE_NEGATIVE) {
                    curBaseExternalBucketLockForBucketIdx.set(i);
                }
                externalBucketLocksForBucketsIdxs.get(i + prevCapacity).set(curBaseExternalBucketLockForBucketIdx.get());
            }
        }
        Node curBucketNode;
        for (Node curBucketHeadNode : prevBuckets) {
            curBucketNode = curBucketHeadNode;
            while (curBucketNode != null) {
                putForResizeInSingleThread(curBucketNode);
                curBucketNode = curBucketNode.next;
            }
        }
    }

    private synchronized void updateExternalBucketLocksIdxsForBuckets(int bucketIdx) {
        if (externalBucketsReleasesCounter == extBucketReleasesPerExtSyncBucketIdxsRelease) {
            int bucketShift;
            int curBucketIdx;
            Queue<Integer> externalBucketsLocksIdxsToUpdate = new ArrayDeque<>();
            boolean isUpdateAllowed;
            for (int i = capacity - 1; i > capacity / RESIZE_RATIO - 1; i--) {
                bucketShift = capacity / RESIZE_RATIO;
                curBucketIdx = i;
                isUpdateAllowed = true;
                while (curBucketIdx > 0 && bucketShift != (initialCapacity / RESIZE_RATIO)) {
                    if (externalBucketsCalcIdxOpsInProgressNums.get(curBucketIdx).get() == 0) {
                        externalBucketsLocksIdxsToUpdate.add(curBucketIdx);
                    } else {
                        isUpdateAllowed = false;
                        break;
                    }
                    curBucketIdx = curBucketIdx - bucketShift;
                    bucketShift /= RESIZE_RATIO;
                }
                if (isUpdateAllowed) {
                    while (!externalBucketsLocksIdxsToUpdate.isEmpty()) {
                        externalBucketLocksForBucketsIdxs.get(externalBucketsLocksIdxsToUpdate.poll()).set(ONE_NEGATIVE);
                    }
                } else {
                    externalBucketsLocksIdxsToUpdate.clear();
                }
            }
        }
        externalBucketsReleasesCounter++;
    }

}
