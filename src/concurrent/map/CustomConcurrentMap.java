package concurrent.map;

public interface CustomConcurrentMap<K, V> {

    void put(K key, V value) throws InterruptedException;

    void putWithExternalBucketLock(K key, V value) throws InterruptedException;

    V get(K key);

    V getWithoutLock(K key);

    void acquireExternalBucketLock(int bucketIdx);

    int size();

    boolean releaseExternalBucketLock(int bucketIdx);

    int calculateExternalBucketLockIdxForKey(K key);

}
