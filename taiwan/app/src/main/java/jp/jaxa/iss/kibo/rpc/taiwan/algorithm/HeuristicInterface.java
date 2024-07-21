package jp.jaxa.iss.kibo.rpc.taiwan.algorithm;

import jp.jaxa.iss.kibo.rpc.taiwan.graph.*;

/**
 * The heuristic interface.
 * 
 * @param <V>: The type of the vertex property.
 * @param <E>: The type of the edge property.
 */
public interface HeuristicInterface<V, E> {
    /**
     * Get the heuristic value for the given source and target vertices.
     * 
     * @param graph:  The graph.
     * @param source: The source vertex.
     * @param target: The target vertex.
     * @return The heuristic value.
     */
    double get(Graph<V, E> graph, Vertex source, Vertex target);
}
