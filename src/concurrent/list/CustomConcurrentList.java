package concurrent.list;

import java.util.Collection;

public interface CustomConcurrentList<E> extends Collection<E> {

    E get(int index);

    int size();

}
