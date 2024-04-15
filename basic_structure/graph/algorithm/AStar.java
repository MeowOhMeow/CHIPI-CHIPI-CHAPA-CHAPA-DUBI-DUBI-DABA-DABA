package algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;

import graph.*;

public class AStar {
	public static void printPath(Stack<Vertex> path) {
		Stack<Vertex> temp = new Stack<>();
		temp.addAll(path);
		while (!temp.isEmpty()) {
			System.out.print(temp.pop().getId() + "->");
		}
		System.out.println();
	}

	public static Stack<Vertex> reconstructPath(Vertex source, Vertex target, List<Integer> pred) {
		Stack<Vertex> path = new Stack<>();
		if (pred.get(target.getId()) == -1)
			return path;

		int current = target.getId();
		while (current != -1 && current != source.getId()) {
			path.push(new Vertex(current));
			current = pred.get(current);
		}
		path.push(new Vertex(source.getId()));

		return path;
	}

	public static <T, U> Stack<Vertex> run(Vertex source, Vertex target, Graph<T, U> graph,
			HeuristicInterface heuristic) {
		List<Integer> dist = new ArrayList<>(Collections.nCopies(graph.size(), Integer.MAX_VALUE));
		List<Integer> pred = new ArrayList<>(Collections.nCopies(graph.size(), -1));
		Set<Integer> closedSet = new HashSet<>();
		dist.set(source.getId(), 0);

		PriorityQueue<Pair<Float, Integer>> open = new PriorityQueue<>(Comparator.comparing(Pair::getFirst));
		open.add(new Pair<>(heuristic.get(graph, source, target), source.getId()));

		while (!open.isEmpty()) {
			int currentVertex = open.poll().getSecond();

			if (currentVertex == target.getId())
				return reconstructPath(source, target, pred);

			closedSet.add(currentVertex);

			for (int neighbor : graph.getNeighbors(currentVertex)) {
				if (closedSet.contains(neighbor))
					continue;

				int alt = (int) (dist.get(currentVertex) + graph.getEdgeWeight(currentVertex, neighbor));
				if (alt < dist.get(neighbor)) {
					dist.set(neighbor, alt);
					pred.set(neighbor, currentVertex);
					open.add(new Pair<>(dist.get(neighbor) + heuristic.get(graph, source, target), neighbor));
				}
			}
		}

		return new Stack<>();
	}

}
