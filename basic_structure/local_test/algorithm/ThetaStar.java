package algorithm;

import java.util.*;
import graph.*;
import pathfinding.*;

public class ThetaStar {
    private static final double PENALTY = 4.15;

    private static class VertexComparator implements Comparator<Pair<Double, Integer>> {
        @Override
        public int compare(Pair<Double, Integer> left, Pair<Double, Integer> right) {
            return left.getFirst().compareTo(right.getFirst());
        }
    }

    public static Stack<Vertex> reconstructPath(Vertex source, Vertex target, int[] pred) {
        Stack<Vertex> path = new Stack<>();
        int current = target.getId();
        while (current != -1 && current != source.getId()) {
            path.push(new Vertex(current));
            current = pred[current];
        }
        path.push(new Vertex(source.getId()));
        return path;
    }

    private static double[] getCoordinates(Graph<Block, Double> graph, Vertex vertex) {
        Block block = graph.getVertexProperty(vertex).getValue();
        return new double[] { block.getX(), block.getY(), block.getZ() };
    }

    private static boolean isWithinBoundingBox(double[] boxMin, double[] boxMax, double[] point1, double[] point2) {
        return !(boxMin[0] > Math.max(point1[0], point2[0]) || boxMax[0] < Math.min(point1[0], point2[0]) ||
                boxMin[1] > Math.max(point1[1], point2[1]) || boxMax[1] < Math.min(point1[1], point2[1]) ||
                boxMin[2] > Math.max(point1[2], point2[2]) || boxMax[2] < Math.min(point1[2], point2[2]));
    }

    public static boolean lineIntersectsBox(double[] linePoint, double[] lineDir, double[] boxMin, double[] boxMax) {
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

    public static boolean lineOfSight(Vertex source, Vertex target, Graph<Block, Double> graph,
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
        openSet.add(new Pair<>(fScore[source.getId()], source.getId()));

        while (!openSet.isEmpty()) {
            int currentVertex = openSet.poll().getSecond();
            int predVertex = pred[currentVertex];

            if (currentVertex == target.getId()) {
                return reconstructPath(source, target, pred);
            }

            for (int neighbor : graph.getNeighbors(currentVertex)) {
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

    private static double distance(Block a, Block b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
