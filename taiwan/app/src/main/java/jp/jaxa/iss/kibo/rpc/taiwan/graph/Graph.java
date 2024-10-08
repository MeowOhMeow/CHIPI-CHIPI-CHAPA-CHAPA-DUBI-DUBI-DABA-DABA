package jp.jaxa.iss.kibo.rpc.taiwan.graph;

import java.util.*;

/**
 * The graph class.
 * 
 * @implNote This graph does not support adding vertices after initialization.
 *           TODO: Add support for adding vertices after initialization.
 * 
 * @param <V>: The type of the vertex property.
 * @param <E>: The type of the edge property.
 */
public class Graph<V, E> {
    private List<Map<Integer, Double>> adjacencyList;
    private List<Map<Integer, Double>> edgePropertiesMap;
    private List<VertexProperty<V>> vertexPropertiesMap;
    private final VertexProperty<V> emptyVertexProperty = new VertexProperty<>(null);
    private final EdgeProperty<E> emptyEdgeProperty = new EdgeProperty<>(null);

    public Graph() {
    }

    public Graph(int numNodes) {
        adjacencyList = new ArrayList<>(numNodes);
        edgePropertiesMap = new ArrayList<>(numNodes);
        vertexPropertiesMap = new ArrayList<>(numNodes);

        for (int i = 0; i < numNodes; ++i) {
            adjacencyList.add(new HashMap<Integer, Double>());
            edgePropertiesMap.add(new HashMap<Integer, Double>());
            vertexPropertiesMap.add(emptyVertexProperty);
        }
    }

    public void addBidirectedEdge(int source, int target, double weight1, double weight2) {
        addDirectedEdge(source, target, weight1);
        addDirectedEdge(target, source, weight2);
    }

    public void addUndirectedEdge(int source, int target, double weight) {
        addBidirectedEdge(source, target, weight, weight);
    }

    public void addUndirectedEdge(Vertex source, Vertex target, double weight) {
        addUndirectedEdge(source.getId(), target.getId(), weight);
    }

    public void addDirectedEdge(int source, int target, double weight) {
        adjacencyList.get(source).put(target, weight);
    }

    public void addDirectedEdge(Vertex source, Vertex target, double weight) {
        addDirectedEdge(source.getId(), target.getId(), weight);
    }

    public double getEdgeWeight(int source, int target) {
        Double weight = adjacencyList.get(source).get(target);
        return (weight != null) ? weight : -1.0;
    }

    public double getEdgeWeight(Vertex source, Vertex target) {
        return getEdgeWeight(source.getId(), target.getId());
    }

    public List<Integer> getNeighbors(int vertex) {
        return new ArrayList<>(adjacencyList.get(vertex).keySet());
    }

    public List<Integer> getNeighbors(Vertex vertex) {
        return getNeighbors(vertex.getId());
    }

    public VertexProperty<V> getVertexProperty(int vertex) {
        return vertexPropertiesMap.get(vertex);
    }

    public VertexProperty<V> getVertexProperty(Vertex vertex) {
        return getVertexProperty(vertex.getId());
    }

    public void setEdgeProperty(int source, int target, Double property) {
        if (!property.equals(emptyEdgeProperty)) {
            edgePropertiesMap.get(source).put(target, property);
        }
    }

    public void setEdgeProperty(Vertex source, Vertex target, Double property) {
        setEdgeProperty(source.getId(), target.getId(), property);
    }

    public void setEdgeWeight(int source, int target, double weight) {
        adjacencyList.get(source).put(target, weight);
    }

    public void setEdgeWeight(Vertex source, Vertex target, double weight) {
        setEdgeWeight(source.getId(), target.getId(), weight);
    }

    public void setVertexProperty(int vertex, VertexProperty<V> property) {
        vertexPropertiesMap.set(vertex, property);
    }

    public void setVertexProperty(Vertex vertex, VertexProperty<V> property) {
        setVertexProperty(vertex.getId(), property);
    }

    /**
     * Get the number of vertices in the graph.
     * 
     * @return The number of vertices in the graph.
     */
    public int size() {
        return adjacencyList.size();
    }
}
