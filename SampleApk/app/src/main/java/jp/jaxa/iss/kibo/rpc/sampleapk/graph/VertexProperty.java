package jp.jaxa.iss.kibo.rpc.sampleapk.graph;

/**
 * The vertex property class.
 * 
 * @param <V>: The type of the vertex property.
 */
public class VertexProperty<V> {
	private V value;

	public VertexProperty(V t) {
		value = t;
	}

	public V getValue() {
		return value;
	}

	public void setValue(V value) {
		this.value = value;
	}
}