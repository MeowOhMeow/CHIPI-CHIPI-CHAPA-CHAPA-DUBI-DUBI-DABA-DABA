package jp.jaxa.iss.kibo.rpc.sampleapk.algorithm;

import java.util.*;
import android.util.Log;
import java.util.stream.Collectors;

import jp.jaxa.iss.kibo.rpc.sampleapk.graph.*;
import jp.jaxa.iss.kibo.rpc.sampleapk.pathfinding.*;



public class ThetaStar {
    private static class VertexComparator implements Comparator<Pair<Double, Integer>> {
        @Override
        public int compare(Pair<Double, Integer> left, Pair<Double, Integer> right) {
            return left.getFirst().compareTo(right.getFirst());
        }
    }

    public static double totalLength = 0;
    public static int turningCount = 0;

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

    public static boolean lineOfSight(Vertex source, Vertex target, Graph<Block, Double> graph, List<Obstacle> obstacles, double koz) {
        final String TAG = "lineOfSight";
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
            for (double step = 0; step <= 1; step += 0.001) { // Adjust step size if necessary
                double x = x0 + dx * step;
                double y = y0 + dy * step;
                double z = z0 + dz * step;
                if (x > obstacle.minX - koz && x < obstacle.maxX + koz && y > obstacle.minY - koz && y < obstacle.maxY + koz && z > obstacle.minZ - koz && z < obstacle.maxZ + koz) {
                    //Log.i(TAG, "--------------------collision location: " + x + ", " + y + ", " + z + "---------------------");
                    return false;
                }
            }
        }
        //System.out.println("x " + x0 + " y " + y0 + " z " + z0);
        //System.out.println("theta success");
        return true;
    }


    public static Stack<Vertex> run(Vertex source, Vertex target, Graph<Block, Double> graph,
                                    HeuristicInterface heuristic, List<Obstacle> obstacles, double koz) {
        //System.out.println("theta");
        final String TAG = "ThetaStar";
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

            // Correctly convert int[] pred to List<Integer>
            List<Integer> predList = new ArrayList<>();
            for (int i : pred) {
                predList.add(i);
            }

            if (currentVertex == target.getId()) {
                totalLength += dist[target.getId()];
                return reconstructPath(source, target, predList);
            }

            closedSet.add(currentVertex);

            for (int neighbor : graph.getNeighbors(currentVertex)) {
                if (closedSet.contains(neighbor)) continue;
                int pp = pred[currentVertex];

                if (pp != -1 && lineOfSight(new Vertex(pp), new Vertex(neighbor), graph, obstacles, koz)) {
                    double dx = graph.getVertexProperty(neighbor).getValue().getX() - graph.getVertexProperty(new Vertex(pp)).getValue().getX();
                    double dy = graph.getVertexProperty(neighbor).getValue().getY() - graph.getVertexProperty(new Vertex(pp)).getValue().getY();
                    double dz = graph.getVertexProperty(neighbor).getValue().getZ() - graph.getVertexProperty(new Vertex(pp)).getValue().getZ();
                    double edgeWeightPP = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    double alt = dist[pp] + edgeWeightPP;
                    //Log.i(TAG, "alt: " + alt);
                    //Log.i(TAG, "dist[neighbor]: " + dist[neighbor]);
                    
                    //Log.i(TAG, "--------------line of sight success-----------------");
                    if (alt < dist[neighbor]) {
                        //Log.i(TAG, "-----------------theta success--------------");
                        dist[neighbor] = alt;
                        pred[neighbor] = pp;
                        open.add(new Pair<>(dist[neighbor] + heuristic.get(graph, new Vertex(neighbor), target), neighbor));
                        //System.out.println("actually success");
                    }else{
                        //Log.i(TAG, "-----------------success nothing------------");
                    }
                } else { 
                    double edgeWeight = graph.getEdgeWeight(currentVertex, neighbor);
                    double alt = dist[currentVertex] + edgeWeight;
                    if(pp == -1)
                    {
                        //Log.i(TAG, "pp is -1");
                    }else{
                        //Log.i(TAG, "Line of sight fail");
                    }
                    if (alt < dist[neighbor]) {
                        //Log.i(TAG, "-----------------theta fail-----------------");
                        dist[neighbor] = alt;
                        pred[neighbor] = currentVertex;
                        open.add(new Pair<>(dist[neighbor] + heuristic.get(graph, new Vertex(neighbor), target), neighbor));
                        //System.out.println("actually fail");
                    }else{
                        //Log.i(TAG, "-----------------fail nothing---------------");
                    }
                }
            }
        }
        return new Stack<>();
    }
}
