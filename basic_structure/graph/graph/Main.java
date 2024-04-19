package graph;

import java.util.List;
import java.util.Map;

public class Main {

	public static void main(String[] args) {
		Graph<Integer, Integer> graph = new Graph<>(5);

		Vertex v1 = new Vertex(0);
		Vertex v2 = new Vertex(1);
		Vertex v3 = new Vertex(2);
		Vertex v4 = new Vertex(3);
		Vertex v5 = new Vertex(4);

		graph.addUndirectedEdge(v1, v2, 1);
		graph.addUndirectedEdge(v1, v3, 2);
		graph.addUndirectedEdge(v2, v3, 3);
		graph.addUndirectedEdge(v3, v4, 4);
		graph.addUndirectedEdge(v4, v5, 3);
		graph.addUndirectedEdge(v2, v5, 8);
		
		graph.addDirectedEdge(v1, v4, 5);

		graph.addBidirectedEdge(v1, v5, 5, 1);
		
		graph.addDirectedEdge(v3, v5, 7);

		//Set vertex property
		graph.setVertexProperty(0, new VertexProperty<>(10));
		graph.setVertexProperty(1, new VertexProperty<>(20));
		graph.setVertexProperty(2, new VertexProperty<>(30));
		graph.setVertexProperty(3, new VertexProperty<>(40));
		graph.setVertexProperty(4, new VertexProperty<>(50));
	
		//Set edge property
		graph.setEdgeProperty(0, 1, new EdgeProperty<>(100));
		graph.setEdgeProperty(0, 2, new EdgeProperty<>(200));
		graph.setEdgeProperty(1, 2, new EdgeProperty<>(300));
		graph.setEdgeProperty(2, 3, new EdgeProperty<>(400));
		graph.setEdgeProperty(3, 4, new EdgeProperty<>(500));
		
		graph.setEdgeProperty(v5, v1, new EdgeProperty<>(700));
		
		graph.setEdgeProperty(v4, v5, new EdgeProperty<>(600));
		graph.setEdgeProperty(v5, v2, new EdgeProperty<>(800));
		graph.setEdgeProperty(v5, v4, new EdgeProperty<>(800));

		//Get vertex property
		VertexProperty<Integer> vp = graph.getVertexProperty(0);
		System.out.println("Vertex property of vertex 0: " + vp.getValue());

		//Get edge property
		EdgeProperty<Integer> ep = graph.getEdgeProperty(v4, v5);
		System.out.println("Edge property of edge (v4, v5): " + ep.getValue());

		//Get neighbors of a vertex
		List<Integer> neighbors = graph.getNeighbors(3);
		System.out.print("Neighbors of vertex 0: ");
		for (int neighbor : neighbors) {
			System.out.print(neighbor + " ");
		}
		System.out.println();

		// Get weight of an edge
		float weight = graph.getEdgeWeight(0, 1);
		System.out.println("Weight of edge (0, 1): " + weight);
		
		graph.setEdgeWeight(0, 1, 6);
		weight = graph.getEdgeWeight(0, 1);
		System.out.println("Weight of edge (0, 1): " + weight);
		
		Map<Integer, Map<Float, EdgeProperty<Integer>>> mo = graph.getOutEdges(v5);
		System.out.println("v5 output edges: ");
		for (Map.Entry<Integer, Map<Float, EdgeProperty<Integer>>> entry : mo.entrySet()) {
		    Integer target = entry.getKey();
		    Map<Float, EdgeProperty<Integer>> edgeInfo = entry.getValue();
		    for (Map.Entry<Float, EdgeProperty<Integer>> edgeEntry : edgeInfo.entrySet()) {
		        Float edgeWeight = edgeEntry.getKey();
		        Integer edgeProperty = edgeEntry.getValue().getValue();
		        System.out.println("(v5, v" + (target+1) + "), Weight: " + edgeWeight + ", Property: " + edgeProperty);
		    }
		}
		
		Map<Integer, Map<Float, EdgeProperty<Integer>>> mi = graph.getOutEdges(v5);
		System.out.println("v5 input edges: ");
		for (Map.Entry<Integer, Map<Float, EdgeProperty<Integer>>> entry : mi.entrySet()) {
		    Integer target = entry.getKey();
		    Map<Float, EdgeProperty<Integer>> edgeInfo = entry.getValue();
		    for (Map.Entry<Float, EdgeProperty<Integer>> edgeEntry : edgeInfo.entrySet()) {
		        Float edgeWeight = edgeEntry.getKey();
		        Integer edgeProperty = edgeEntry.getValue().getValue();
		        System.out.println("(v" + (target+1) + ", v5)" + ", Weight: " + edgeWeight + ", Property: " + edgeProperty);
		    }
		}
		
		Map<Integer, VertexProperty<Integer>> mv = graph.getAllVertices();
		System.out.println("all vertics: ");
		for (int i : mv.keySet()) {
		    System.out.println("Vertex: " + i + ", Verticx property: " + mv.get(i).getValue());
		}
		
		// Get size of graph
		System.out.println("Size of graph: " + graph.size());
	}
	
}
