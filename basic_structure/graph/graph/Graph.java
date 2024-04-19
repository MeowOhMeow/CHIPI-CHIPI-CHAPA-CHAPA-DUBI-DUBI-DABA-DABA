package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Graph<T, U> {
	private List<Map<Integer, Float>> adjacencyList;
	private List<Map<Integer, EdgeProperty<U>>> edgePropertiesMap;
	private EdgeProperty<U> emptyEdgeProperty;
	private VertexProperty<T> emptyVertexProperty;
	private List<VertexProperty<T>> vertexPropertiesMap;

	public Graph(int numNodes) {
		adjacencyList = new ArrayList<>();
		vertexPropertiesMap = new ArrayList<>();
		edgePropertiesMap = new ArrayList<>();
		emptyVertexProperty = new VertexProperty<>(null);
		emptyEdgeProperty = new EdgeProperty<>(null);
		for (int i = 0; i < numNodes; i++) {
			adjacencyList.add(new HashMap<>());
			vertexPropertiesMap.add(emptyVertexProperty);
			edgePropertiesMap.add(new HashMap<>());
		}
	}

	public void addBidirectedEdge(int source, int target, float weight1, float weight2) {
		addDirectedEdge(source, target, weight1);
		addDirectedEdge(target, source, weight2);
	}

	public void addBidirectedEdge(Vertex source, Vertex target, float weight1, float weight2) {
		addBidirectedEdge(source.getId(), target.getId(), weight1, weight2);
	}
	
	public void addUndirectedEdge(int source, int target, float weight) {
		addBidirectedEdge(source, target, weight, weight);
	}
	
	public void addUndirectedEdge(Vertex source, Vertex target, float weight) {
		addUndirectedEdge(source.getId(), target.getId(), weight);
	}

	public void addDirectedEdge(int source, int target, float weight) {
		adjacencyList.get(source).put(target, weight);
	}

	public void addDirectedEdge(Vertex source, Vertex target, float weight) {
		addDirectedEdge(source.getId(), target.getId(), weight);
	}

	public EdgeProperty<U> getEdgeProperty(int source, int target) {
		return edgePropertiesMap.get(source).get(target);
	}
	
	public EdgeProperty<U> getEdgeProperty(Vertex source, Vertex target) {
		return getEdgeProperty(source.getId(), target.getId());
	}

	public float getEdgeWeight(int source, int target) {
		return adjacencyList.get(source).get(target);
	}

	public float getEdgeWeight(Vertex source, Vertex target) {
		return getEdgeWeight(source.getId(), target.getId());
	}

	public List<Integer> getNeighbors(int vertex) {
		return new ArrayList<>(adjacencyList.get(vertex).keySet());
	}

	public List<Integer> getNeighbors(Vertex vertex) {
		return getNeighbors(vertex.getId());
	}

	public VertexProperty<T> getVertexProperty(int vertex) {
		return vertexPropertiesMap.get(vertex);
	}

	public VertexProperty<T> getVertexProperty(Vertex vertex) {
		return getVertexProperty(vertex.getId());
	}
	
	public Map<Integer, Map<Float, EdgeProperty<U>>> getOutEdges(int vertex) {
	    Set<Integer> s = adjacencyList.get(vertex).keySet();
	    Map<Integer, Map<Float, EdgeProperty<U>>> outEdges = new HashMap<>();
	    for (int num : s) {
	        float weight = adjacencyList.get(vertex).get(num);
	        EdgeProperty<U> edgeProperty = getEdgeProperty(vertex, num);
	        Map<Float, EdgeProperty<U>> edgeInfo = new HashMap<>();
	        edgeInfo.put(weight, edgeProperty);
	        outEdges.put(num, edgeInfo);
	    }
	    return outEdges;
	}

	
	public Map<Integer, Map<Float, EdgeProperty<U>>> getOutEdges(Vertex vertex) {
		return getOutEdges(vertex.getId());
	}
	
	public Map<Integer, Map<Float, EdgeProperty<U>>> getInEdges(int vertex) {
	    Map<Integer, Map<Float, EdgeProperty<U>>> inEdges = new HashMap<>();
	    for (int i = 0; i < adjacencyList.size(); i++) {
	        for (Map.Entry<Integer, Float> entry : adjacencyList.get(i).entrySet()) {
	            if (entry.getKey() == vertex) {
	            	float weight = adjacencyList.get(i).get(vertex);
	    	        EdgeProperty<U> edgeProperty = getEdgeProperty(i, vertex);
	    	        Map<Float, EdgeProperty<U>> edgeInfo = new HashMap<>();
	    	        edgeInfo.put(weight, edgeProperty);
	    	        inEdges.put(i, edgeInfo);
	            }
	        }
	    }
	    return inEdges;
	}

	
	public Map<Integer, Map<Float, EdgeProperty<U>>> getInEdges(Vertex vertex) {
		return getInEdges(vertex.getId());
	}
	
	public void setEdgeProperty(int source, int target, EdgeProperty<U> property) {
		if (property.notEquals(emptyEdgeProperty)) {
			edgePropertiesMap.get(source).put(target, property);
		}
	}
	
	public void setEdgeProperty(Vertex source, Vertex target, EdgeProperty<U> property) {
		setEdgeProperty(source.getId(), target.getId(), property);
	}

	public void setEdgeWeight(int source, int target, float weight) {
		adjacencyList.get(source).put(target, weight);
	}

	public void setEdgeWeight(Vertex source, Vertex target, float weight) {
		setEdgeWeight(source.getId(), target.getId(), weight);
	}
	
	public void setVertexProperty(int vertex, VertexProperty<T> property) {
		vertexPropertiesMap.set(vertex, property);
	}

	public void setVertexProperty(Vertex vertex, VertexProperty<T> property) {
		setVertexProperty(vertex.getId(), property);
	}
	
	public Map<Integer, VertexProperty<T>> getAllVertices() {
	    Map<Integer, VertexProperty<T>> verticesList = new HashMap<>();
	    for (int i = 0; i < adjacencyList.size(); i++) {
	        verticesList.put(i, getVertexProperty(i)); // 添加顶点属性
	    }
	    return verticesList;
	}
	
	public int size() {
		return adjacencyList.size();
	}


}