package pathfinding;

import java.util.*;

import graph.*;
import main.Log;
import algorithm.*;

public class PathfindingMain {

    private static final String TAG = "PathfindingMain";

    /**
     * Checks if there is a line of sight between two points.
     *
     * @param x0 Start point x-coordinate
     * @param y0 Start point y-coordinate
     * @param z0 Start point z-coordinate
     * @param x1 End point x-coordinate
     * @param y1 End point y-coordinate
     * @param z1 End point z-coordinate
     * @param graph The graph used for line of sight calculation
     * @param obstacles The obstacles to consider during line of sight calculation
     * @param koz The safety distance to keep from obstacles
     * @return True if line of sight exists, false otherwise
     */
    public static boolean lineOfSight(double x0, double y0, double z0, double x1, double y1, double z1, Graph<Block, Double> graph, List<Obstacle> obstacles, double koz) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;

        for (Obstacle obstacle : obstacles) {
            for (double step = 0; step <= 1; step += 0.001) {
                double x = x0 + dx * step;
                double y = y0 + dy * step;
                double z = z0 + dz * step;
                if (x > obstacle.minX - koz && x < obstacle.maxX + koz && y > obstacle.minY - koz && y < obstacle.maxY + koz && z > obstacle.minZ - koz && z < obstacle.maxZ + koz) {
                    return false;
                }
            }
        }
        return true;
    }

    public static class PathFindingAPI {

        /**
         * Finds a path from start to end point avoiding obstacles.
         *
         * @param start The starting point
         * @param end The ending point
         * @param koz The safety distance to keep from obstacles
         * @return A list of points representing the path
         */
        public static List<Point> findPath(Point start, Point end, double koz) {
            List<Obstacle> obstacles = createObstacles();
            Graph<Block, Double> graph = buildGraph(koz, obstacles);

            Vertex source = findNearestVertex(start, graph);
            Vertex target = findNearestVertex(end, graph);

            if (isInObstacle(source, obstacles, graph) || isInObstacle(target, obstacles, graph)) {
                return Collections.emptyList();
            }

            Stack<Vertex> path = ThetaStar.run(source, target, graph, new Heuristic(), obstacles, koz);
            List<Point> result = extractPath(path, graph);
            logPoints(result, "result");

            List<Point> result2 = simplifyCollinearPoints(result);
            logPoints(result2, "result2");

            List<Point> result3 = removeUnnecessaryPoints(result2, graph, obstacles, koz);
            result3.remove(0);
            logPoints(result3, "result3");

            return result3;
        }

        private static List<Obstacle> createObstacles() {
            List<Obstacle> obstacles = new ArrayList<>();
            obstacles.add(new Obstacle(10.87, -9.5, 4.27, 11.6, -9.45, 4.97));
            obstacles.add(new Obstacle(10.25, -9.5, 4.97, 10.87, -9.45, 5.62));
            obstacles.add(new Obstacle(10.87, -8.5, 4.97, 11.6, -8.45, 5.62));
            obstacles.add(new Obstacle(10.25, -8.5, 4.27, 10.7, -8.45, 4.97));
            obstacles.add(new Obstacle(10.87, -7.40, 4.27, 11.6, -7.35, 4.97));
            obstacles.add(new Obstacle(10.25, -7.40, 4.97, 10.87, -7.35, 5.62));
            return obstacles;
        }

        private static Graph<Block, Double> buildGraph(double koz, List<Obstacle> obstacles) {
            double minX = 10.3 + koz, minY = -10.2 + koz, minZ = 4.32 + koz;
            double maxX = 11.55 - koz, maxY = -6.0 - koz, maxZ = 5.57 - koz;
            int numVertices = calculateNumVertices(minX, minY, minZ, maxX, maxY, maxZ);

            Graph<Block, Double> graph = new Graph<>(numVertices);
            Map<List<Double>, Integer> vertexLocation = new HashMap<>();
            int vertexCount = 0;

            for (double z = minZ; z <= maxZ; z += 0.05) {
                for (double y = minY; y <= maxY; y += 0.05) {
                    for (double x = minX; x <= maxX; x += 0.05) {
                        Block block = new Block(vertexCount, x, y, z);
                        graph.setVertexProperty(new Vertex(vertexCount), new VertexProperty<>(block));
                        vertexLocation.put(Arrays.asList(x, y, z), vertexCount++);
                    }
                }
            }

            buildEdges(graph, vertexLocation, obstacles, koz, numVertices);

            return graph;
        }

        private static int calculateNumVertices(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            int num = 0;
            for (double x = minX; x <= maxX; x += 0.05) {
                for (double y = minY; y <= maxY; y += 0.05) {
                    for (double z = minZ; z <= maxZ; z += 0.05) {
                        num++;
                    }
                }
            }
            return num;
        }

        private static void buildEdges(Graph<Block, Double> graph, Map<List<Double>, Integer> vertexLocation, List<Obstacle> obstacles, double koz, int numVertices) {
            for (int i = 0; i < numVertices; i++) {
                Block block = graph.getVertexProperty(i).getValue();
                double x = block.getX(), y = block.getY(), z = block.getZ();

                if (isInObstacle(x, y, z, obstacles, koz - 0.05)) continue;

                addEdges(graph, vertexLocation, x, y, z, i);
            }
        }

        private static void addEdges(Graph<Block, Double> graph, Map<List<Double>, Integer> vertexLocation, double x, double y, double z, int vertexIndex) {
            List<double[]> directions = Arrays.asList(
                new double[]{0.05, 0, 0}, new double[]{-0.05, 0, 0},
                new double[]{0, 0.05, 0}, new double[]{0, -0.05, 0},
                new double[]{0, 0, 0.05}, new double[]{0, 0, -0.05}
            );

            for (double[] dir : directions) {
                double newX = x + dir[0], newY = y + dir[1], newZ = z + dir[2];
                if (vertexLocation.containsKey(Arrays.asList(newX, newY, newZ))) {
                    graph.addDirectedEdge(vertexIndex, vertexLocation.get(Arrays.asList(newX, newY, newZ)), 0.05);
                }
            }
        }

        private static boolean isInObstacle(double x, double y, double z, List<Obstacle> obstacles, double margin) {
            for (Obstacle obstacle : obstacles) {
                if (x > obstacle.minX - margin && x < obstacle.maxX + margin && y > obstacle.minY - margin && y < obstacle.maxY + margin && z > obstacle.minZ - margin && z < obstacle.maxZ + margin) {
                    return true;
                }
            }
            return false;
        }

        private static Vertex findNearestVertex(Point point, Graph<Block, Double> graph) {
            double px = point.getX(), py = point.getY(), pz = point.getZ();
            double minDistance = Double.MAX_VALUE;
            int nearestVertexId = 0;

            for (int i = 0; i < graph.getNumVertices(); i++) {
                Block block = graph.getVertexProperty(i).getValue();
                double distance = Math.sqrt(Math.pow(px - block.getX(), 2) + Math.pow(py - block.getY(), 2) + Math.pow(pz - block.getZ(), 2));
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestVertexId = i;
                }
            }

            return new Vertex(nearestVertexId);
        }

        private static boolean isInObstacle(Vertex vertex, List<Obstacle> obstacles, Graph<Block, Double> graph) {
            Block block = graph.getVertexProperty(vertex.getId()).getValue();
            return isInObstacle(block.getX(), block.getY(), block.getZ(), obstacles, 0);
        }

        private static List<Point> extractPath(Stack<Vertex> path, Graph<Block, Double> graph) {
            List<Point> result = new ArrayList<>();
            while (!path.isEmpty()) {
                Vertex vertex = path.pop();
                Block block = graph.getVertexProperty(vertex.getId()).getValue();
                result.add(new Point(block.getX(), block.getY(), block.getZ()));
            }
            return result;
        }

        private static List<Point> simplifyCollinearPoints(List<Point> points) {
            boolean[] toDelete = new boolean[points.size()];
            for (int i = 0; i < points.size() - 2; i++) {
                Point p1 = points.get(i), p2 = points.get(i + 1), p3 = points.get(i + 2);
                if ((p1.getX() == p3.getX() && p1.getY() == p3.getY()) || (p1.getY() == p3.getY() && p1.getZ() == p3.getZ()) || (p1.getX() == p3.getX() && p1.getZ() == p3.getZ())) {
                    toDelete[i + 1] = true;
                }
            }

            List<Point> simplified = new ArrayList<>();
            for (int i = 0; i < points.size(); i++) {
                if (!toDelete[i]) {
                    simplified.add(points.get(i));
                }
            }
            return simplified;
        }

        private static List<Point> removeUnnecessaryPoints(List<Point> points, Graph<Block, Double> graph, List<Obstacle> obstacles, double koz) {
            boolean[] toDelete = new boolean[points.size()];
            List<Point> result = new ArrayList<>();

            for (int i = 0; i < points.size() - 2; i++) {
                Point p1 = points.get(i), p3 = points.get(i + 2);
                if (lineOfSight(p1.getX(), p1.getY(), p1.getZ(), p3.getX(), p3.getY(), p3.getZ(), graph, obstacles, koz)) {
                    toDelete[i + 1] = true;
                }
            }

            for (int i = 0; i < points.size(); i++) {
                if (!toDelete[i]) {
                    result.add(points.get(i));
                }
            }

            return result;
        }

        private static void logPoints(List<Point> points, String label) {
            Log.i(TAG, "-------------------------------------------" + label + "-------------------------------------------");
            for (Point p : points) {
                Log.i(TAG, "x: " + p.getX() + " y: " + p.getY() + " z: " + p.getZ());
            }
        }
    }
}


