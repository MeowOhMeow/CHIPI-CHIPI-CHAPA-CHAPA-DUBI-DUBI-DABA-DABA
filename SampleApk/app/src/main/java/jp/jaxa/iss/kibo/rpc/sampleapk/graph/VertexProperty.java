package jp.jaxa.iss.kibo.rpc.sampleapk.graph;

public class VertexProperty<T> {
	private T value;

	public VertexProperty(T t) {
		value = t;
	}

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}
}