package acmi.l2.clientmod.unreal.util;

import lombok.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ObservableSetWrapper<E> implements ObservableSet<E> {
    private final Set<E> backingSet;
    private final Collection<InvalidationListener> listeners = new CopyOnWriteArraySet<>();

    public ObservableSetWrapper(@NonNull Set<E> backingSet) {
        this.backingSet = backingSet;
    }

    @Override
    public void addListener(InvalidationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        listeners.remove(listener);
    }

    private void callObservers() {
        for (InvalidationListener listener : listeners) {
            listener.invalidated(this);
        }
    }

    @Override
    public int size() {
        return backingSet.size();
    }

    @Override
    public boolean isEmpty() {
        return backingSet.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return backingSet.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {

            private final Iterator<E> backingIt = backingSet.iterator();

            @Override
            public boolean hasNext() {
                return backingIt.hasNext();
            }

            @Override
            public E next() {
                return backingIt.next();
            }

            @Override
            public void remove() {
                backingIt.remove();
                callObservers();
            }
        };
    }

    @Override
    public Object[] toArray() {
        return backingSet.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return backingSet.toArray(a);
    }

    @Override
    public boolean add(E e) {
        boolean ret = backingSet.add(e);
        if (ret) {
            callObservers();
        }
        return ret;
    }

    @Override
    public boolean remove(Object o) {
        boolean ret = backingSet.remove(o);
        if (ret) {
            callObservers();
        }
        return ret;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return backingSet.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean ret = backingSet.addAll(c);
        if (ret) {
            callObservers();
        }
        return ret;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean ret = backingSet.retainAll(c);
        if (ret) {
            callObservers();
        }
        return ret;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean ret = backingSet.removeAll(c);
        if (ret) {
            callObservers();
        }
        return ret;
    }

    @Override
    public void clear() {
        if (isEmpty()) {
            return;
        }
        backingSet.clear();
        callObservers();
    }

    @Override
    public boolean equals(Object o) {
        return backingSet.equals(o);
    }

    @Override
    public int hashCode() {
        return backingSet.hashCode();
    }

    @Override
    public String toString() {
        return backingSet.toString();
    }
}
