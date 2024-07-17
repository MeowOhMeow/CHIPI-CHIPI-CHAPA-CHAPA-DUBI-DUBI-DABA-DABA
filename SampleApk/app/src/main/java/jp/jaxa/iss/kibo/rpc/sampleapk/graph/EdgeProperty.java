package jp.jaxa.iss.kibo.rpc.sampleapk.graph;

/**
 * The edge property class.
 * 
 * @param <E>: The type of the edge property.
 */
public class EdgeProperty<E> {
	private E value;

	public EdgeProperty(E t) {
		value = t;
	}

	public boolean notEquals(EdgeProperty<E> other) {
		return !value.equals(other.value);
	}

	public E getValue() {
		return value;
	}

	public void setValue(E value) {
		this.value = value;
	}
}
