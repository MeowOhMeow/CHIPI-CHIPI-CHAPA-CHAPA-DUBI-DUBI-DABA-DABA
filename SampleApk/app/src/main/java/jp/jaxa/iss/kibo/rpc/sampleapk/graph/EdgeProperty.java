package jp.jaxa.iss.kibo.rpc.sampleapk.graph;

public class EdgeProperty<T> {
	private T value;

	public EdgeProperty(T t) {
		value = t;
	}

	public boolean notEquals(EdgeProperty<T> other) {
		return !value.equals(other.value);
	}

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}
}
