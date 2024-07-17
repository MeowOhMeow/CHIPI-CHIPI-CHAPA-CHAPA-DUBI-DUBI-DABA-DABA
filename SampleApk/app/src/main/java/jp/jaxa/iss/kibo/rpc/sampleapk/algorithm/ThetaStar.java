package jp.jaxa.iss.kibo.rpc.sampleapk.algorithm;

import java.util.*;

import jp.jaxa.iss.kibo.rpc.sampleapk.graph.*;
import jp.jaxa.iss.kibo.rpc.sampleapk.pathfinding.*;

/**
 * The Theta* algorithm.
 * 
 * * This algorithm only alows Graph<Block, Double> as input graph.
 * 
 * @see ThetaStar#run(Vertex, Vertex, Graph, HeuristicInterface, List, double)
 */
public class ThetaStar {
    /**
     * The penalty value of adding a vertex to the path.
     * This value is calculated based on the max speed and acceleration of the astrobee.
     */
    private static final double PENALTY = 4.15;

    private static class VertexComparator implements Comparator<Pair<Double, Integer>> {
        @Override
        public int compare(Pair<Double, Integer> left, Pair<Double, Integer> right) {
            return left.getFirst().compareTo(right.getFirst());
        }
    }

    /**
     * Reconstruct the path from the source to the target vertex.
     * 
     * @param source: The source vertex.
     * @param target: The target vertex.
     * @param pred:   The predecessor array.
     * @return The path from the source to the target vertex.
     */
    private static Stack<Vertex> reconstructPath(Vertex source, Vertex target, int[] pred) {
        Stack<Vertex> path = new Stack<>();
        int current = target.getId();
        while (current != -1 && current != source.getId()) {
            path.push(new Vertex(current));
            current = pred[current];
        }
        path.push(new Vertex(source.getId()));
        return path;
    }

    /**
     * Get the coordinates of the given vertex.
     * 
     * @param graph:  The graph.
     * @param vertex: The vertex.
     * @return The coordinates of the vertex.
     */
    private static double[] getCoordinates(Graph<Block, Double> graph, Vertex vertex) {
        Block block = graph.getVertexProperty(vertex).getValue();
        return new double[] { block.getX(), block.getY(), block.getZ() };
    }

    /**
     * Check if the line between the two points has potential to intersect the
     * bounding box.
     * 
     * @param boxMin: The minimum coordinates of the bounding box.
     * @param boxMax: The maximum coordinates of the bounding box.
     * @param point1: The first point.
     * @param point2: The second point.
     * @return True if the line has potential to intersect the bounding box.
     */
    private static boolean isWithinBoundingBox(double[] boxMin, double[] boxMax, double[] point1, double[] point2) {
        return !(boxMin[0] > Math.max(point1[0], point2[0]) || boxMax[0] < Math.min(point1[0], point2[0]) ||
                boxMin[1] > Math.max(point1[1], point2[1]) || boxMax[1] < Math.min(point1[1], point2[1]) ||
                boxMin[2] > Math.max(point1[2], point2[2]) || boxMax[2] < Math.min(point1[2], point2[2]));
    }

    /**
     * Check if the line intersects the bounding box.
     * 
     * @param linePoint: The point on the line.
     * @param lineDir:   The direction of the line.
     * @param boxMin:    The minimum coordinates of the bounding box.
     * @param boxMax:    The maximum coordinates of the bounding box.
     * @return True if the line intersects the bounding box.
     */
    private static boolean lineIntersectsBox(double[] linePoint, double[] lineDir, double[] boxMin, double[] boxMax) {
        double tMin = (boxMin[0] - linePoint[0]) / lineDir[0];
        double tMax = (boxMax[0] - linePoint[0]) / lineDir[0];

        if (tMin > tMax) {
            double temp = tMin;
            tMin = tMax;
            tMax = temp;
        }

        for (int i = 1; i < 3; i++) {
            double t1 = (boxMin[i] - linePoint[i]) / lineDir[i];
            double t2 = (boxMax[i] - linePoint[i]) / lineDir[i];

            if (t1 > t2) {
                double temp = t1;
                t1 = t2;
                t2 = temp;
            }

            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);

            if (tMin > tMax) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if there is a line of sight between the source and target vertices.
     * 
     * @param source:       The source vertex.
     * @param target:       The target vertex.
     * @param graph:        The graph.
     * @param obstacles:    The list of obstacles.
     * @param expansionVal: The expansion value.
     * @return True if there is a line of sight between the source and target
     *         vertices.
     */
    private static boolean lineOfSight(Vertex source, Vertex target, Graph<Block, Double> graph,
            List<Obstacle> obstacles, double expansionVal) {
        double[] startPoint = getCoordinates(graph, source);
        double[] endPoint = getCoordinates(graph, target);
        double[] direction = { endPoint[0] - startPoint[0], endPoint[1] - startPoint[1], endPoint[2] - startPoint[2] };

        for (Obstacle obstacle : obstacles) {
            double[] boxMin = { obstacle.minX - expansionVal, obstacle.minY - expansionVal,
                    obstacle.minZ - expansionVal };
            double[] boxMax = { obstacle.maxX + expansionVal, obstacle.maxY + expansionVal,
                    obstacle.maxZ + expansionVal };

            if (!isWithinBoundingBox(boxMin, boxMax, startPoint, endPoint)) {
                continue;
            }

            if (lineIntersectsBox(startPoint, direction, boxMin, boxMax)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the Euclidean distance between two blocks.
     * 
     * @param a: The first block.
     * @param b: The second block.
     * @return The Euclidean distance between the two blocks.
     */
    private static double distance(Block a, Block b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Run the Theta* algorithm.
     * 
     * @param source:       The source vertex.
     * @param target:       The target vertex.
     * @param graph:        The graph.
     * @param heuristic:    The heuristic.
     * @param obstacles:    The list of obstacles.
     * @param expansionVal: The expansion value.
     * @return The path from the source to the target vertex.
     */
    public static Stack<Vertex> run(Vertex source, Vertex target, Graph<Block, Double> graph,
            HeuristicInterface<Block, Double> heuristic, List<Obstacle> obstacles, double expansionVal) {
        int numVertices = graph.size();
        double[] gScore = new double[numVertices];
        double[] fScore = new double[numVertices];
        int[] pred = new int[numVertices];
        Arrays.fill(gScore, Double.MAX_VALUE);
        Arrays.fill(fScore, Double.MAX_VALUE);
        Arrays.fill(pred, -1);
        gScore[source.getId()] = PENALTY;
        fScore[source.getId()] = heuristic.get(graph, source, target);

        PriorityQueue<Pair<Double, Integer>> openSet = new PriorityQueue<>(new VertexComparator());
        Set<Integer> closedSet = new HashSet<>();
        openSet.add(new Pair<>(fScore[source.getId()], source.getId()));

        while (!openSet.isEmpty()) {
            int currentVertex = openSet.poll().getSecond();
            closedSet.add(currentVertex);
            int predVertex = pred[currentVertex];

            // Check if the target has been reached
            if (currentVertex == target.getId()) {
                return reconstructPath(source, target, pred);
            }

            // Iterate over the neighbors of the current vertex
            for (int neighbor : graph.getNeighbors(currentVertex)) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                double tentative_gScore = PENALTY;

                if (predVertex != -1
                        && lineOfSight(new Vertex(predVertex), new Vertex(neighbor), graph, obstacles, expansionVal)) {
                    tentative_gScore += gScore[predVertex] + distance(graph.getVertexProperty(neighbor).getValue(),
                            graph.getVertexProperty(new Vertex(predVertex)).getValue());

                    if (tentative_gScore < gScore[neighbor]) {
                        pred[neighbor] = predVertex;
                        gScore[neighbor] = tentative_gScore;
                        fScore[neighbor] = gScore[neighbor] + heuristic.get(graph, new Vertex(neighbor), target);
                        openSet.add(new Pair<>(fScore[neighbor], neighbor));
                    }
                } else {
                    tentative_gScore += gScore[currentVertex] + graph.getEdgeWeight(currentVertex, neighbor);

                    if (tentative_gScore < gScore[neighbor]) {
                        pred[neighbor] = currentVertex;
                        gScore[neighbor] = tentative_gScore;
                        fScore[neighbor] = gScore[neighbor] + heuristic.get(graph, new Vertex(neighbor), target);
                        openSet.add(new Pair<>(fScore[neighbor], neighbor));
                    }
                }

            }
        }
        return new Stack<>();
    }
}
