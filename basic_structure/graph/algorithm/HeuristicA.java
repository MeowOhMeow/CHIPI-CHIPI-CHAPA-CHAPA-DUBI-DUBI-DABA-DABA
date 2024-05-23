package algorithm;

import graph.*;

public class HeuristicA implements HeuristicInterface {
	public float get(Graph<?, ?> graph, Vertex source, Vertex target) {
		return target.getId() - source.getId();
	}
}