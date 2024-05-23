package algorithm;

import java.util.Stack;

import graph.*;

public class Main {

	public static void main(String[] args) {
		// create graph
		Graph<Integer, Integer> graph = new Graph<>(9);

		// 3 * 3 grid
		Vertex v1 = new Vertex(0);
		Vertex v2 = new Vertex(1);
		Vertex v3 = new Vertex(2);
		Vertex v4 = new Vertex(3);
		Vertex v5 = new Vertex(4);
		Vertex v6 = new Vertex(5);
		Vertex v7 = new Vertex(6);
		Vertex v8 = new Vertex(7);
		Vertex v9 = new Vertex(8);

		graph.addUndirectedEdge(v1, v2, 1);
		graph.addUndirectedEdge(v1, v4, 1);
		graph.addUndirectedEdge(v2, v3, 1);
		graph.addUndirectedEdge(v2, v5, 1);
		graph.addUndirectedEdge(v3, v6, 1);
		graph.addUndirectedEdge(v4, v5, 1);
		graph.addUndirectedEdge(v4, v7, 1);
		graph.addUndirectedEdge(v5, v6, 1);
		graph.addUndirectedEdge(v5, v8, 1);
		graph.addUndirectedEdge(v6, v9, 1);
		graph.addUndirectedEdge(v7, v8, 1);
		graph.addUndirectedEdge(v8, v9, 1);

		// create heuristic function
		HeuristicInterface heuristic = new HeuristicA();

		// create source and target vertices
		Vertex source = v1;
		Vertex target = v9;

		// run AStar algorithm
		Stack<Vertex> path = AStar.run(source, target, graph, heuristic);

		// print path
		AStar.printPath(path);
	}

}
