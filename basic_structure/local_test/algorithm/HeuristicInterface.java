package algorithm;

import graph.*;

public interface HeuristicInterface<V, E> {
    double get(Graph<V, E> graph, Vertex source, Vertex target);
}
