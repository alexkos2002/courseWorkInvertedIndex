package concurrent.queue;

public interface CustomConcurrentPriorityQueue<T extends Comparable<T>> {

    void push(T entry) throws InterruptedException;

    T pop() throws InterruptedException;

    int size();

    boolean isEmpty();

    void setAllPushesFinished(boolean allPushesFinished);

    void notifyAllPopPerformers();
}
