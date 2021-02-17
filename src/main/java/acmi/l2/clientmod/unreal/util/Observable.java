package acmi.l2.clientmod.unreal.util;

public interface Observable {
    void addListener(InvalidationListener listener);

    void removeListener(InvalidationListener listener);
}
