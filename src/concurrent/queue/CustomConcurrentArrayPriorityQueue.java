package concurrent.queue;

import java.util.ArrayList;

public class CustomConcurrentArrayPriorityQueue<E extends Comparable<E>> implements CustomConcurrentPriorityQueue<E> {

    private BinaryHeap binaryHeap;

    private volatile boolean isAllPushesFinished;

    private QueueMonitor monitor;

    public CustomConcurrentArrayPriorityQueue(int capacity) {
        this.binaryHeap = new BinaryHeap(capacity);
        this.isAllPushesFinished = false;
    }

    @Override
    public synchronized void push(E entry) throws InterruptedException {
        while (binaryHeap.size == binaryHeap.capacity) {
            System.out.println("Queue is FULL, waiting..");
            System.out.println(binaryHeap.capacity);
            wait();
        }
        binaryHeap.insert(entry);
        notifyAll();
    }

    @Override
    public synchronized E pop() throws InterruptedException {
        while (binaryHeap.size == 0 && !isAllPushesFinished) {
            System.out.println("Queue is EMPTY, waiting... " + isAllPushesFinished);
            wait();
        }
        if (isAllPushesFinished) {
            return null;
        }
        E maxEntry = binaryHeap.popMax();
        notifyAll();
        return maxEntry;
    }

    @Override
    public int size() {
        return binaryHeap.size;
    }

    @Override
    public synchronized boolean isEmpty() {
        return binaryHeap.size == 0;
    }

    @Override
    public String toString() {
        return binaryHeap.toString();
    }

    class BinaryHeap {
        private final int capacity;

        private ArrayList<E> heap;
        private int size;

        public BinaryHeap(int capacity) {
            this.capacity = capacity;
            heap = new ArrayList<>(capacity);
            size = 0;
        }

        private int parent(int i) {
            return (i - 1) / 2;
        }

        private int leftChild(int i) {
            return 2 * i + 1;
        }

        private int rightChild(int i) {
            return 2 * i + 2;
        }

        public void insert(E entry) {
            if (size >= capacity) {
                System.out.println("The heap is full. Cannot insert");
                return;
            }

            heap.add(size, entry);
            size++;

            int i = size - 1;
            while (i != 0 && (heap.get(parent(i))).compareTo(heap.get(i)) < 0) {
                E temp = heap.get(i);
                heap.set(i, heap.get(parent(i)));
                heap.set(parent(i), temp);
                i = parent(i);
            }
        }

        public void maxHeapify(int i) {
            int left = leftChild(i);

            int right = rightChild(i);

            int largest = i;

            if (left <= size && (heap.get(left)).compareTo(heap.get(largest)) > 0) {
                largest = left;
            }

            if (right <= size && (heap.get(right)).compareTo(heap.get(largest)) > 0) {
                largest = right;
            }

            if (largest != i) {
                E temp = heap.get(i);
                heap.set(i, heap.get(largest));
                heap.set(largest, temp);
                maxHeapify(largest);
            }

        }
        public E popMax() {
            E maxItem = heap.get(0);
            heap.set(0, heap.get(size - 1));
            size--;

            maxHeapify(0);
            heap.remove(size);
            return maxItem;
        }

        @Override
        public String toString() {
            return String.format("Priority Queue: %s",
                    heap.size() > 0 ? (heap.stream().map(entry -> entry.toString())
                            .reduce((accumulator, entry) -> accumulator + ", " + entry)).get() : "") ;
        }

    }

    public synchronized boolean isAllPushesFinished() {
        return isAllPushesFinished;
    }

    @Override
    public synchronized void notifyAllPopPerformers() {
        System.out.println("XYZ " + isAllPushesFinished);
        notifyAll();
    }

    @Override
    public void setAllPushesFinished(boolean allPushesFinished) {
        isAllPushesFinished = allPushesFinished;
    }
    class QueueMonitor {

    }
}
