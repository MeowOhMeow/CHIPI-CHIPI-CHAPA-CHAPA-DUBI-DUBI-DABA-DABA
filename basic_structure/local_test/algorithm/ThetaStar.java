package algorithm;

import java.util.*;
import graph.*;
import pathfinding.*;

public class ThetaStar {

    private static class VertexComparator implements Comparator<Pair<Double, Integer>> {
        @Override
        public int compare(Pair<Double, Integer> left, Pair<Double, Integer> right) {
            return left.getFirst().compareTo(right.getFirst());
        }
    }

    public static Stack<Vertex> reconstructPath(Vertex source, Vertex target, List<Integer> pred) {
        Stack<Vertex> path = new Stack<>();
        int current = target.getId();
        while (current != -1 && current != source.getId()) {
            path.push(new Vertex(current));
            current = pred.get(current);
        }
        path.push(new Vertex(source.getId()));
        return path;
    }

    public static boolean lineOfSight(Vertex source, Vertex target, Graph<Block, Double> graph,
            List<Obstacle> obstacles, double koz) {
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
                if (x > obstacle.minX - koz && x < obstacle.maxX + koz && y > obstacle.minY - koz
                        && y < obstacle.maxY + koz && z > obstacle.minZ - koz && z < obstacle.maxZ + koz) {
                    return false;
                }
            }
        }
        return true;
    }

    public static Stack<Vertex> run(Vertex source, Vertex target, Graph<Block, Double> graph,
            HeuristicInterface heuristic, List<Obstacle> obstacles, double koz) {
        int numVertices = graph.size();
        double[] dist = new double[numVertices];
        int[] pred = new int[numVertices];
        Set<Integer> closedSet = new HashSet<>();
        Arrays.fill(dist, Double.MAX_VALUE);
        Arrays.fill(pred, -1);
        dist[source.getId()] = 0;

        PriorityQueue<Pair<Double, Integer>> open = new PriorityQueue<>(new VertexComparator());
        open.add(new Pair<>(heuristic.get(graph, source, target), source.getId()));

        while (!open.isEmpty()) {
            int currentVertex = open.poll().getSecond();

            // Convert int[] pred to List<Integer> using a for loop
            List<Integer> predList = new ArrayList<>();
            for (int value : pred) {
                predList.add(value);
            }

            if (currentVertex == target.getId()) {
                return reconstructPath(source, target, predList);
            }

            closedSet.add(currentVertex);

            for (int neighbor : graph.getNeighbors(currentVertex)) {
                if (closedSet.contains(neighbor))
                    continue;

                int predecessor = pred[currentVertex];
                double alt;
                if (predecessor != -1
                        && lineOfSight(new Vertex(predecessor), new Vertex(neighbor), graph, obstacles, koz)) {
                    alt = dist[predecessor] + distance(graph.getVertexProperty(neighbor).getValue(),
                            graph.getVertexProperty(new Vertex(predecessor)).getValue());
                    if (alt < dist[neighbor]) {
                        dist[neighbor] = alt;
                        pred[neighbor] = predecessor;
                        open.add(new Pair<>(dist[neighbor] + heuristic.get(graph, new Vertex(neighbor), target),
                                neighbor));
                    }
                } else {
                    alt = dist[currentVertex] + graph.getEdgeWeight(currentVertex, neighbor);
                    if (alt < dist[neighbor]) {
                        dist[neighbor] = alt;
                        pred[neighbor] = currentVertex;
                        open.add(new Pair<>(dist[neighbor] + heuristic.get(graph, new Vertex(neighbor), target),
                                neighbor));
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
