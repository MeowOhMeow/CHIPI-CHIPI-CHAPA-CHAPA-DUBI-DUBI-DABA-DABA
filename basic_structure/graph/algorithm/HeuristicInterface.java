package algorithm;

import graph.*;

// base class for heuristic functions
public interface HeuristicInterface {
	// virtual function to be implemented by derived classes
	public float get(Graph<?, ?> graph, Vertex source, Vertex target);
}
