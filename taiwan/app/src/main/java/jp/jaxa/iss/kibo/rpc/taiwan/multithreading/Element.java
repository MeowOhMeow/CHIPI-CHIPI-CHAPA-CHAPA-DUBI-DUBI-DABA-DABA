package jp.jaxa.iss.kibo.rpc.taiwan.multithreading;

import android.util.Log;

// Represents a generic element holding data
public class Element<D> {
    private static final String TAG = "";
    private D data;

    public Element(D data) {
        this.data = data;
    }

    public D getData() {
        return data;
    }

    public void setData(D data) {
        this.data = data;
    }

    public void update() {
        Log.i(TAG, "Element update.");
    }
}
