package algorithm;

import java.util.*;
import graph.*;
import pathfinding.*;

public class ThetaStar {
    private static final double PANALTY = 0.1;

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

    public static boolean lineOfSight(Vertex source, Vertex target, Graph<Block, Double> graph,
            List<Obstacle> obstacles, double expansionVal) {
        double x0 = graph.getVertexProperty(source).getValue().getX();
        double y0 = graph.getVertexProperty(source).getValue().getY();
        double z0 = graph.getVertexProperty(source).getValue().getZ();
        double x1 = graph.getVertexProperty(target).getValue().getX();
        double y1 = graph.getVertexProperty(target).getValue().getY();
        double z1 = graph.getVertexProperty(target).getValue().getZ();
        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;

        for (Obstacle obstacle : obstacles) {
            for (double step = 0; step <= 1; step += 0.001) {
                double x = x0 + dx * step;
                double y = y0 + dy * step;
                double z = z0 + dz * step;
                if (x > obstacle.minX - expansionVal && x < obstacle.maxX + expansionVal
                        && y > obstacle.minY - expansionVal
                        && y < obstacle.maxY + expansionVal && z > obstacle.minZ - expansionVal
                        && z < obstacle.maxZ + expansionVal) {
                    return false;
                }
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
        gScore[source.getId()] = PANALTY;
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
                double tentative_gScore = PANALTY;

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
