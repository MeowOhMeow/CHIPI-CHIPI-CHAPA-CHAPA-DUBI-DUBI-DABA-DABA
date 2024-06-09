package jp.jaxa.iss.kibo.rpc.sampleapk.pathfinding;

import java.util.*;

import java.lang.Math;

import gov.nasa.arc.astrobee.types.Point;

public class PathfindingMain {
	
	public static double calculateDistance(List<Vertex> path, Graph<Block, Double> graph) {
	    double distance = 0;
	    for (int i = 0; i < path.size() - 1; i++) {
	        int currentVertexId = path.get(i).getId();
	        int nextVertexId = path.get(i + 1).getId();
	        
	        double dx = graph.getVertexProperty(currentVertexId).getValue().getX() - graph.getVertexProperty(nextVertexId).getValue().getX();
	        double dy = graph.getVertexProperty(currentVertexId).getValue().getY() - graph.getVertexProperty(nextVertexId).getValue().getY();
	        double dz = graph.getVertexProperty(currentVertexId).getValue().getZ() - graph.getVertexProperty(nextVertexId).getValue().getZ();
	        
	        distance += Math.sqrt(dx * dx + dy * dy + dz * dz);;
	    }
	    return distance;
	}

	public static int calculateTurningCount(List<Vertex> path, Graph<Block, Double> graph) {
	    int turningCount = 0;
	    turningCount = path.size() - 2;
	    return turningCount;
	}

    static class Obstacle {
        double minX, minY, minZ, maxX, maxY, maxZ;

        Obstacle(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
    }

    static class Block {
        int id;
        double x, y, z;

        Block(int id, double x, double y, double z) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        double getX() { return x; }
        double getY() { return y; }
        double getZ() { return z; }
    }

    static class Vertex {
        private int id;

        Vertex(int id) {
            this.id = id;
        }

        int getId() { return id; }
    }

    static class VertexProperty<T> {
        private T value;

        VertexProperty(T value) {
            this.value = value;
        }

        T getValue() { return value; }
    }

    static class Graph<V, E> {
        private List<Vertex> vertices;
        private Map<Integer, VertexProperty<V>> vertexProperties;
        private Map<Integer, ArrayList<Object>> adjacencyList;

        Graph(int numVertices) {
            vertices = new ArrayList<>(numVertices);
            vertexProperties = new HashMap<>();
            adjacencyList = new HashMap<Integer, ArrayList<Object>>();
        }

        void setVertexProperty(Vertex vertex, VertexProperty<V> property) {
            vertexProperties.put(vertex.getId(), property);
            vertices.add(vertex);
            adjacencyList.put(vertex.getId(), new ArrayList<>());
        }

        VertexProperty<V> getVertexProperty(int id) {
            return vertexProperties.get(id);
        }

        void addDirectedEdge(int from, int to, E weight) {
            adjacencyList.get(from).add(new Edge<>(to, weight));
        }
    }

    static class Edge<E> {
        int to;
        E weight;

        Edge(int to, E weight) {
            this.to = to;
            this.weight = weight;
        }

        public int getTo() { return to; }
        public E getWeight() { return weight; }
    }

    static class HeuristicA {
        // Define heuristic implementation
    }

    static class ThetaStar {
        static Stack<Vertex> run(Vertex source, Vertex target, Graph<Block, Double> graph, HeuristicA heuristic, List<Obstacle> obstacles) {
            // Implement Theta* algorithm
            return new Stack<>();
        }
    }

    public static class PathFindingAPI {

        public static List<Point> findPath(Point start, Point end) {
            List<Obstacle> obstacles = new ArrayList<>();
            obstacles.add(new Obstacle(10.87, -9.5, 4.27, 11.6, -9.45, 4.97));
            obstacles.add(new Obstacle(10.25, -9.5, 4.97, 10.87, -9.45, 5.62));
            obstacles.add(new Obstacle(10.87, -8.5, 4.97, 11.6, -8.45, 5.62));
            obstacles.add(new Obstacle(10.25, -8.5, 4.27, 10.7, -8.45, 4.97));
            obstacles.add(new Obstacle(10.87, -7.40, 4.27, 11.6, -7.35, 4.97));
            obstacles.add(new Obstacle(10.25, -7.40, 4.97, 10.87, -7.35, 5.62));

            double minX1 = 10.3 + 0.2, minY1 = -10.2 + 0.2, minZ1 = 4.32 + 0.2, maxX1 = 11.55 - 0.2, maxY1 = -6.0 - 0.2, maxZ1 = 5.57 - 0.2;

            int num = 0;

            for (double x = minX1; x <= maxX1; x += 0.05) {
                for (double y = minY1; y <= maxY1; y += 0.05) {
                    for (double z = minZ1; z <= maxZ1; z += 0.05) {
                        num++;
                    }
                }
            }

            Graph<Block, Double> graph = new Graph<>(num);
            Map<List<Double>, Integer> vertexLocation = new HashMap<>();

            int vertexCount = 0;
            for (double z = minZ1; z <= maxZ1; z += 0.05) {
                for (double y = minY1; y <= maxY1; y += 0.05) {
                    for (double x = minX1; x <= maxX1; x += 0.05) {
                        Block block = new Block(vertexCount, x, y, z);
                        VertexProperty<Block> vertexProperty = new VertexProperty<>(block);
                        graph.setVertexProperty(new Vertex(vertexCount), vertexProperty);
                        vertexLocation.put(Arrays.asList(x, y, z), vertexCount);
                        vertexCount++;
                    }
                }
            }

            for (int i = 0; i < num; i++) {
                double x = graph.getVertexProperty(i).getValue().getX();
                double y = graph.getVertexProperty(i).getValue().getY();
                double z = graph.getVertexProperty(i).getValue().getZ();

                boolean inObstacleFlag = false;

                for (Obstacle obstacle : obstacles) {
                    if (x > obstacle.minX - 0.2 && x < obstacle.maxX + 0.2 && y > obstacle.minY - 0.2 && y < obstacle.maxY + 0.2 && z > obstacle.minZ - 0.2 && z < obstacle.maxZ + 0.2) {
                        inObstacleFlag = true;
                        break;
                    }
                }

                if (inObstacleFlag) {
                    continue;
                } else {
                    if (!vertexLocation.containsKey(Arrays.asList(x, y, z))) {
                        continue;
                    } else {
                        if (vertexLocation.containsKey(Arrays.asList(x + 0.05, y, z))) {
                            graph.addDirectedEdge(i, vertexLocation.get(Arrays.asList(x + 0.05, y, z)), 0.05);
                        }
                        if (vertexLocation.containsKey(Arrays.asList(x, y + 0.05, z))) {
                            graph.addDirectedEdge(i, vertexLocation.get(Arrays.asList(x, y + 0.05, z)), 0.05);
                        }
                        if (vertexLocation.containsKey(Arrays.asList(x, y, z + 0.05))) {
                            graph.addDirectedEdge(i, vertexLocation.get(Arrays.asList(x, y, z + 0.05)), 0.05);
                        }
                        if (vertexLocation.containsKey(Arrays.asList(x - 0.05, y, z))) {
                            graph.addDirectedEdge(i, vertexLocation.get(Arrays.asList(x - 0.05, y, z)), 0.05);
                        }
                        if (vertexLocation.containsKey(Arrays.asList(x, y - 0.05, z))) {
                            graph.addDirectedEdge(i, vertexLocation.get(Arrays.asList(x, y - 0.05, z)), 0.05);
                        }
                        if (vertexLocation.containsKey(Arrays.asList(x, y, z - 0.05))) {
                            graph.addDirectedEdge(i, vertexLocation.get(Arrays.asList(x, y, z - 0.05)), 0.05);
                        }
                    }
                }
            }

            HeuristicA heuristic = new HeuristicA();

            double sx = start.getX();
            double sy = start.getY();
            double sz = start.getZ();
            double tx = end.getX();
            double ty = end.getY();
            double tz = end.getZ();

            double minDistance1 = 1000000;
            double minDistance2 = 1000000;
            int s = 0, t = 0;

            for (int i = 0; i < num; i++) {
                double x = graph.getVertexProperty(i).getValue().getX();
                double y = graph.getVertexProperty(i).getValue().getY();
                double z = graph.getVertexProperty(i).getValue().getZ();

                double distance = Math.sqrt((sx - x) * (sx - x) + (sy - y) * (sy - y) + (sz - z) * (sz - z));
                if (distance < minDistance1) {
                    minDistance1 = distance;
                    s = i;
                }

                distance = Math.sqrt((tx - x) * (tx - x) + (ty - y) * (ty - y) + (tz - z) * (tz - z));
                if (distance < minDistance2) {
                    minDistance2 = distance;
                    t = i;
                }
            }

            Vertex source = new Vertex(s);
            Vertex target = new Vertex(t);

            for (Obstacle obstacle : obstacles) {
                if (graph.getVertexProperty(source.getId()).getValue().getX() > obstacle.minX && graph.getVertexProperty(source.getId()).getValue().getX() < obstacle.maxX && graph.getVertexProperty(source.getId()).getValue().getY() > obstacle.minY && graph.getVertexProperty(source.getId()).getValue().getY() < obstacle.maxY && graph.getVertexProperty(source.getId()).getValue().getZ() > obstacle.minZ && graph.getVertexProperty(source.getId()).getValue().getZ() < obstacle.maxZ) {
                    return Collections.emptyList();
                }
                if (graph.getVertexProperty(target.getId()).getValue().getX() > obstacle.minX && graph.getVertexProperty(target.getId()).getValue().getX() < obstacle.maxX && graph.getVertexProperty(target.getId()).getValue().getY() > obstacle.minY && graph.getVertexProperty(target.getId()).getValue().getY() < obstacle.maxY && graph.getVertexProperty(target.getId()).getValue().getZ() > obstacle.minZ && graph.getVertexProperty(target.getId()).getValue().getZ() < obstacle.maxZ) {
                    return Collections.emptyList();
                }
            }

            Stack<Vertex> path = ThetaStar.run(source, target, graph, heuristic, obstacles);

            List<Point> result = new ArrayList<>();

            while (!path.isEmpty()) {
                Vertex vertex = path.pop();
                Block block = graph.getVertexProperty(vertex.getId()).getValue();
                result.add(new Point(block.getX(), block.getY(), block.getZ()));
            }

            return result;
        }

        public static void main(String[] args) {
            Point start = new Point(10.35, -10.0, 4.52);
            Point end = new Point(11.35, -7.0, 5.32);

            List<Point> path = findPath(start, end);

            for (Point p : path) {
                System.out.println("x: " + p.getX() + ", y: " + p.getY() + ", z: " + p.getZ());
            }
        }
    }
}

