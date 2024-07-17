package jp.jaxa.iss.kibo.rpc.sampleapk.pathfinding;

import jp.jaxa.iss.kibo.rpc.sampleapk.algorithm.*;
import jp.jaxa.iss.kibo.rpc.sampleapk.graph.*;

/**
 * The heuristic class.
 */
public class Heuristic implements HeuristicInterface<Block, Double> {
    @Override
    public double get(Graph<Block, Double> graph, Vertex source, Vertex target) {
        double dx = graph.getVertexProperty(target).getValue().getX() - graph.getVertexProperty(source).getValue().getX();
        double dy = graph.getVertexProperty(target).getValue().getY() - graph.getVertexProperty(source).getValue().getY();
        double dz = graph.getVertexProperty(target).getValue().getZ() - graph.getVertexProperty(source).getValue().getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
