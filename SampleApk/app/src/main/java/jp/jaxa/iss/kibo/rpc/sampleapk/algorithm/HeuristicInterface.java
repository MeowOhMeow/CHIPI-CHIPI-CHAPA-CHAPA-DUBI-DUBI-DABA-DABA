package jp.jaxa.iss.kibo.rpc.sampleapk.algorithm;

import jp.jaxa.iss.kibo.rpc.sampleapk.graph.*;

public interface HeuristicInterface<V, E> {
    double get(Graph<V, E> graph, Vertex source, Vertex target);
}
