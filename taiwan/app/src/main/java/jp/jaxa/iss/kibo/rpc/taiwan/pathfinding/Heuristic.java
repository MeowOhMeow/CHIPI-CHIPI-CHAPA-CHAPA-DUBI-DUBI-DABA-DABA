package jp.jaxa.iss.kibo.rpc.taiwan.pathfinding;

import gov.nasa.arc.astrobee.types.Point;
import jp.jaxa.iss.kibo.rpc.taiwan.algorithm.*;
import jp.jaxa.iss.kibo.rpc.taiwan.graph.*;

/**
 * The heuristic class.
 */
public class Heuristic implements HeuristicInterface<Point, NoProperty> {
    @Override
    public double get(Graph<Point, NoProperty> graph, Vertex source, Vertex target) {
        double dx = graph.getVertexProperty(target).getValue().getX() - graph.getVertexProperty(source).getValue().getX();
        double dy = graph.getVertexProperty(target).getValue().getY() - graph.getVertexProperty(source).getValue().getY();
        double dz = graph.getVertexProperty(target).getValue().getZ() - graph.getVertexProperty(source).getValue().getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
