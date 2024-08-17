package jp.jaxa.iss.kibo.rpc.taiwan.multithreading;

import java.util.Map;

// Abstract observer that contains a list of elements and requires an update
public abstract class Observer<Key, E> {
    protected Map<Key, Element<E>> elements;

    public Observer(Map<Key, Element<E>> elements) {
        this.elements = elements;
    }

    public abstract void update();

    public abstract void addElement(Key key, Element<E> element);
    public abstract void addElement(Key key, E value);
    public abstract E removeElement(Key key);
}
