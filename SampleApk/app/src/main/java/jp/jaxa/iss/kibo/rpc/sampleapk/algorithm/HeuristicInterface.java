package jp.jaxa.iss.kibo.rpc.sampleapk.algorithm;

import jp.jaxa.iss.kibo.rpc.sampleapk.graph.*;

public interface HeuristicInterface {
    double get(Graph<Block, Double> graph, Vertex source, Vertex target);
}
