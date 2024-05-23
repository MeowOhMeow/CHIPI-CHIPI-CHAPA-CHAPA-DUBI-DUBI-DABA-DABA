package theta_star;
// Main.java
import java.util.*;

// Pair表示一對值
class Pair<T, U> {
    T first;
    U second;
    Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }
}

// 障礙物
class Obstacle {
    double minX;
    double minY;
    double minZ;
    double maxX;
    double maxY;
    double maxZ;
    Obstacle(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }
}

// 啟發式函數介面
interface HeuristicInterface {
    double get(Graph<Block, Double> graph, Vertex source, Vertex target);
}

// 啟發式函數A
class HeuristicA implements HeuristicInterface {
    public double get(Graph<Block, Double> graph, Vertex source, Vertex target) {
        double dx = graph.getVertexProperty(target).value.getX() - graph.getVertexProperty(source).value.getX();
        double dy = graph.getVertexProperty(target).value.getY() - graph.getVertexProperty(source).value.getY();
        double dz = graph.getVertexProperty(target).value.getZ() - graph.getVertexProperty(source).value.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}

// 比較器類
class Comparator implements java.util.Comparator<Pair<Double, Integer>> {
    public int compare(Pair<Double, Integer> left, Pair<Double, Integer> right) {
        return left.first.compareTo(right.first);
    }
}

class ThetaStar {
    public static Stack<Vertex> reconstructPath(Vertex source, Vertex target, int[] pred) {
        Stack<Vertex> path = new Stack<>();
        int current = target.getId();
        while (current != -1 && current != source.getId()) {
            path.push(new Vertex(current));
            current = pred[current];
        }
        path.push(source);
        return path;
    }

    public static boolean lineOfSight(Vertex source, Vertex target, Graph<Block, Double> graph, ArrayList<Obstacle> obstacles) {
        double x0 = graph.getVertexProperty(source).value.getX();
        double y0 = graph.getVertexProperty(source).value.getY();
        double z0 = graph.getVertexProperty(source).value.getZ();
        double x1 = graph.getVertexProperty(target).value.getX();
        double y1 = graph.getVertexProperty(target).value.getY();
        double z1 = graph.getVertexProperty(target).value.getZ();
        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;
        System.out.println("x0: " + x0 + " y0: " + y0 + " z0: " + z0);
        System.out.println("x1: " + x1 + " y1: " + y1 + " z1: " + z1);
        System.out.println("dx: " + dx + " dy: " + dy + " dz: " + dz);
        for (Obstacle obstacle : obstacles) {
            for (double step = 0; step < 1; step += 0.001) {
                double x = x0 + dx * step;
                double y = y0 + dy * step;
                double z = z0 + dz * step;
                if (x > obstacle.minX && x < obstacle.maxX && y > obstacle.minY && y < obstacle.maxY && z > obstacle.minZ && z < obstacle.maxZ) {
                    System.out.println("source: v" + (source.getId() + 1) + " target: v" + (target.getId() + 1) + " false");
                    System.out.println("x: " + x + " y: " + y + " z: " + z);
                    return false;
                }
            }
        }
        System.out.println("source: v" + (source.getId() + 1) + " target: v" + (target.getId() + 1) + " true");
        return true;
    }

    public static Stack<Vertex> run(Vertex source, Vertex target, Graph<Block, Double> graph, HeuristicInterface heuristic, ArrayList<Obstacle> obstacles) {
        double[] dist = new double[graph.size()];
        int[] pred = new int[graph.size()];
        Arrays.fill(dist, Double.MAX_VALUE);
        Arrays.fill(pred, -1);
        Set<Integer> closedSet = new HashSet<>();
        dist[source.getId()] = 0;

        Comparator comparator = new Comparator();
        PriorityQueue<Pair<Double, Integer>> open = new PriorityQueue<>(comparator);

        open.offer(new Pair<>(heuristic.get(graph, source, target), source.getId()));

        while (!open.isEmpty()) {
            int currentVertex = open.poll().second;

            if (currentVertex == target.getId()) {
                System.out.println("distance: " + dist[target.getId()]);
                return reconstructPath(source, target, pred);
            }

            closedSet.add(currentVertex);

            for (int neighbor : graph.getNeighbors(currentVertex)) {
                if (closedSet.contains(neighbor)) continue;
                int pp = pred[currentVertex];
                System.out.println("currentVertex: v" + (currentVertex + 1) + " neighbor: v" + (neighbor + 1) + " pp: v" + (pp + 1));
                if (pp != -1) {
                    if (lineOfSight(new Vertex(pp), new Vertex(neighbor), graph, obstacles)) {
                        double edgeWeight1 = graph.getEdgeWeight(currentVertex, neighbor);
                        double edgeWeight2 = dist[currentVertex] - dist[pp];
                        double dx = graph.getVertexProperty(new Vertex(neighbor)).value.getX() - graph.getVertexProperty(new Vertex(pp)).value.getX();
                        double dy = graph.getVertexProperty(new Vertex(neighbor)).value.getY() - graph.getVertexProperty(new Vertex(pp)).value.getY();
                        double dz = graph.getVertexProperty(new Vertex(neighbor)).value.getZ() - graph.getVertexProperty(new Vertex(pp)).value.getZ();
                        double edgeWeightPP = Math.sqrt(dx * dx + dy * dy + dz * dz);

                        System.out.println("edgeWeight1: " + edgeWeight1 + " edgeWeight2: " + edgeWeight2);
                        System.out.println("edgeWeightPP: " + edgeWeightPP);

                        if (edgeWeightPP < edgeWeight1 + edgeWeight2) {
                            double alt = dist[pp] + edgeWeightPP;
                            if (alt < dist[neighbor]) {
                                dist[neighbor] = alt;
                                pred[neighbor] = pp;
                                open.offer(new Pair<>(dist[neighbor] + heuristic.get(graph, new Vertex(neighbor), target), neighbor));
                            }
                            System.out.println("PA2 100");
                        } else {
                            double alt = dist[currentVertex] + edgeWeight1;
                            if (alt < dist[neighbor]) {
                                dist[neighbor] = alt;
                                pred[neighbor] = currentVertex;
                                open.offer(new Pair<>(dist[neighbor] + heuristic.get(graph, new Vertex(neighbor), target), neighbor));
                           }
                           System.out.println("PA2 200");
                       }
                       System.out.println("distance: " + dist[neighbor]);
                   } else {
                       double edgeWeight = graph.getEdgeWeight(currentVertex, neighbor);
                       double alt = dist[currentVertex] + edgeWeight;
                       if (alt < dist[neighbor]) {
                           dist[neighbor] = alt;
                           pred[neighbor] = currentVertex;
                           open.offer(new Pair<>(dist[neighbor] + heuristic.get(graph, new Vertex(neighbor), target), neighbor));
                       }
                       System.out.println("distance: " + dist[neighbor]);
                       System.out.println("PA2 300");
                   }
               } else {
                   double edgeWeight = graph.getEdgeWeight(currentVertex, neighbor);
                   double alt = dist[currentVertex] + edgeWeight;
                   if (alt < dist[neighbor]) {
                       dist[neighbor] = alt;
                       pred[neighbor] = currentVertex;
                       open.offer(new Pair<>(dist[neighbor] + heuristic.get(graph, new Vertex(neighbor), target), neighbor));
                   }
                   System.out.println("distance: " + dist[neighbor]);
                   System.out.println("PA2 400");
               }
               System.out.println();
           }
       }
       System.out.println();
       return new Stack<>();
   }
}

public class Main {
   public static void main(String[] args) {
       // Create graph
       Graph<Block, Double> graph = new Graph<>(13);
       ArrayList<Obstacle> obstacles = new ArrayList<>();
       obstacles.add(new Obstacle(1, 2, 0, 2, 3, 1));

       // Create vertices
       Vertex v1 = new Vertex(0), v2 = new Vertex(1), v3 = new Vertex(2), v4 = new Vertex(3), v5 = new Vertex(4), v6 = new Vertex(5), v7 = new Vertex(6), v8 = new Vertex(7), v9 = new Vertex(8), v10 = new Vertex(9), v11 = new Vertex(10), v12 = new Vertex(11), v13 = new Vertex(12);

       graph.setVertexProperty(v1, new VertexProperty<>(new Block(0, 0, 0.5)));
       graph.setVertexProperty(v2, new VertexProperty<>(new Block(1, 1, 0.5)));
       graph.setVertexProperty(v3, new VertexProperty<>(new Block(2, 1, 0.5)));
       graph.setVertexProperty(v4, new VertexProperty<>(new Block(3, 1, 0.5)));
       graph.setVertexProperty(v5, new VertexProperty<>(new Block(1, 2, 0.5)));
       graph.setVertexProperty(v6, new VertexProperty<>(new Block(2, 2, 0.5)));
       graph.setVertexProperty(v7, new VertexProperty<>(new Block(3, 2, 0.5)));
       graph.setVertexProperty(v8, new VertexProperty<>(new Block(1, 3, 0.5)));
       graph.setVertexProperty(v9, new VertexProperty<>(new Block(2, 3, 0.5)));
       graph.setVertexProperty(v10, new VertexProperty<>(new Block(3, 3, 0.5)));
       graph.setVertexProperty(v11, new VertexProperty<>(new Block(1, 4, 0.5)));
       graph.setVertexProperty(v12, new VertexProperty<>(new Block(2, 4, 0.5)));
       graph.setVertexProperty(v13, new VertexProperty<>(new Block(3, 4, 0.5)));

       // Add undirected edges
       graph.addUndirectedEdge(v1, v2, 1.44);
       graph.addUndirectedEdge(v2, v3, 1);
       graph.addUndirectedEdge(v3, v4, 1);
       graph.addUndirectedEdge(v5, v6, 1);
       graph.addUndirectedEdge(v6, v7, 1);
       graph.addUndirectedEdge(v8, v9, 1);
       graph.addUndirectedEdge(v9, v10, 1);
       graph.addUndirectedEdge(v2, v5, 1);
       graph.addUndirectedEdge(v3, v6, 1);
       graph.addUndirectedEdge(v4, v7, 1);
       graph.addUndirectedEdge(v5, v8, 1);
       graph.addUndirectedEdge(v6, v9, 1);
       graph.addUndirectedEdge(v7, v10, 1);
       graph.addUndirectedEdge(v8, v11, 1);
       graph.addUndirectedEdge(v9, v12, 1);
       graph.addUndirectedEdge(v10, v13, 1);
       graph.addUndirectedEdge(v11, v12, 1);
       graph.addUndirectedEdge(v12, v13, 1);

       // Create heuristic function
       HeuristicA heuristic = new HeuristicA();

       // Create source and target vertices
       Vertex source = v13;
       Vertex target = v5;

       // Run Theta* algorithm
       Stack<Vertex> path = ThetaStar.run(source, target, graph, heuristic, obstacles);

       System.out.println("---------------");
       System.out.println("source: v" + (source.getId() + 1) + " target: v" + (target.getId() + 1));

       // Print path
       while (!path.empty()) {
           System.out.print(" v" + (path.pop().getId() + 1) + "->");
       }
       System.out.println();
   }
}

