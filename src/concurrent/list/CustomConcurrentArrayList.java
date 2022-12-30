package concurrent.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CustomConcurrentArrayList<E> implements CustomConcurrentList<E> {
    private final Lock readLock;
    private final Lock writeLock;
    private final List<E> list = new ArrayList();

    {
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        readLock = rwLock.readLock();
        writeLock = rwLock.writeLock();
    }

    @Override
    public boolean add(E element) {
        writeLock.lock();
        list.add(element);
        writeLock.unlock();
        return true;
    }

    @Override
    public E get(int index) {
        readLock.lock();
        E element = list.get(index);
        readLock.unlock();
        return element;
    }

    @Override
    public int size() {
        readLock.lock();
        int size = list.size();
        readLock.unlock();
        return size;
    }

    // Implementation of methods below isn't needed

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator<E> iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }
}
