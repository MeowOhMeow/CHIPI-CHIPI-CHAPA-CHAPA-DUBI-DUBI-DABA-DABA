package jp.jaxa.iss.kibo.rpc.taiwan.pathfinding;

import java.util.*;

import gov.nasa.arc.astrobee.types.Point;
import android.util.Log;
import jp.jaxa.iss.kibo.rpc.taiwan.graph.*;
import jp.jaxa.iss.kibo.rpc.taiwan.algorithm.*;

/**
 * The path finding API class.
 */
public class PathFindingAPI {
    private static final String TAG = "PathFindingAPI";
    private static final double GRID_SIZE = 0.05;

    private static List<Obstacle> obstacles = createObstacles();
    private static Graph<Block, Double> graph = null;
    private static double lastExpansionVal = -1;

    /**
     * Finds a path from start to end point avoiding obstacles.
     *
     * @param start        The starting point
     * @param end          The ending point
     * @param expansionVal The safety distance to keep from obstacles
     * @return A list of points representing the path
     */
    public synchronized static List<Point> findPath(Point start, Point end, double expansionVal) {
        // lazy initialization of the graph
        if (expansionVal != lastExpansionVal || graph == null) {
            Log.i(TAG, "Building graph with expansion value: " + expansionVal);
            buildGraph(expansionVal);
            lastExpansionVal = expansionVal;
            Log.i(TAG, "Graph built");
        }

        Vertex source = findNearestVertex(start, graph);
        Vertex target = findNearestVertex(end, graph);

        Stack<Vertex> path = ThetaStar.run(source, target, graph, new Heuristic(), obstacles, expansionVal);
        List<Point> result = extractPath(path, graph);
        // remove the first point since it is the starting point
        result.remove(0);
        logPoints(result, "result");
        return result;
    }

    /**
     * Creates a list of obstacles.
     * 
     * @return A list of obstacles
     */
    private static List<Obstacle> createObstacles() {
        List<Obstacle> result = new ArrayList<>();
        // TODO: read obstacles from assets
        result.add(new Obstacle(10.87, -9.5, 4.27, 11.6, -9.45, 4.97));
        result.add(new Obstacle(10.25, -9.5, 4.97, 10.87, -9.45, 5.62));
        result.add(new Obstacle(10.87, -8.5, 4.97, 11.6, -8.45, 5.62));
        result.add(new Obstacle(10.25, -8.5, 4.27, 10.7, -8.45, 4.97));
        result.add(new Obstacle(10.87, -7.40, 4.27, 11.6, -7.35, 4.97));
        result.add(new Obstacle(10.25, -7.40, 4.97, 10.87, -7.35, 5.62));
        return result;
    }

    /**
     * Builds a graph with vertices representing points in the environment and edges
     * 
     * @param expansionVal: The safety distance to keep from obstacles
     */
    private static void buildGraph(double expansionVal) {
        // TODO: read boundaries from assets
        double minX = 10.3 + expansionVal, minY = -10.2 + expansionVal, minZ = 4.32 + expansionVal;
        double maxX = 11.55 - expansionVal, maxY = -6.0 - expansionVal, maxZ = 5.57 - expansionVal;

        int xSize = (int) Math.ceil((maxX - minX) / GRID_SIZE);
        int ySize = (int) Math.ceil((maxY - minY) / GRID_SIZE);
        int zSize = (int) Math.ceil((maxZ - minZ) / GRID_SIZE);

        int[][][] vertexLocation = new int[xSize][ySize][zSize];
        for (int[][] plane : vertexLocation) {
            for (int[] row : plane) {
                Arrays.fill(row, -1);
            }
        }

        graph = new Graph<>(xSize * ySize * zSize);
        int vertexId = 0;
        for (int z = 0; z < zSize; z++) {
            for (int y = 0; y < ySize; y++) {
                for (int x = 0; x < xSize; x++) {
                    double actualX = minX + x * GRID_SIZE;
                    double actualY = minY + y * GRID_SIZE;
                    double actualZ = minZ + z * GRID_SIZE;

                    Block block = new Block(vertexId, actualX, actualY, actualZ);
                    graph.setVertexProperty(new Vertex(vertexId), new VertexProperty<>(block));
                    vertexLocation[x][y][z] = vertexId++;
                }
            }
        }

        buildEdges(vertexLocation);
    }

    /**
     * Builds edges between vertices in the graph
     * 
     * @param vertexLocation: A 3D array representing the location of each vertex
     */
    private static void buildEdges(int[][][] vertexLocation) {
        for (int x = 0; x < vertexLocation.length; x++) {
            for (int y = 0; y < vertexLocation[0].length; y++) {
                for (int z = 0; z < vertexLocation[0][0].length; z++) {
                    if (vertexLocation[x][y][z] != -1) {
                        addEdges(vertexLocation, x, y, z, vertexLocation[x][y][z]);
                    }
                }
            }
        }
    }

    /**
     * Checks if the given indices are valid
     * 
     * @param xIndex:         The x index
     * @param yIndex:         The y index
     * @param zIndex:         The z index
     * @param vertexLocation: A 3D array representing the location of each vertex
     * @return True if the indices are valid, false otherwise
     */
    private static boolean isValidIndex(int xIndex, int yIndex, int zIndex, int[][][] vertexLocation) {
        return xIndex >= 0 && xIndex < vertexLocation.length &&
                yIndex >= 0 && yIndex < vertexLocation[0].length &&
                zIndex >= 0 && zIndex < vertexLocation[0][0].length;
    }

    /**
     * Adds edges to the graph
     * 
     * @param vertexLocation: A 3D array representing the location of each vertex
     * @param xIndex:         The x index
     * @param yIndex:         The y index
     * @param zIndex:         The z index
     * @param vertexIndex:    The index of the current vertex
     */
    private static void addEdges(int[][][] vertexLocation, int xIndex, int yIndex,
            int zIndex, int vertexIndex) {
        int[][] directions = {
                { 1, 0, 0 }, { -1, 0, 0 },
                { 0, 1, 0 }, { 0, -1, 0 },
                { 0, 0, 1 }, { 0, 0, -1 }
        };

        for (int[] dir : directions) {
            int newXIndex = xIndex + dir[0];
            int newYIndex = yIndex + dir[1];
            int newZIndex = zIndex + dir[2];

            if (isValidIndex(newXIndex, newYIndex, newZIndex, vertexLocation)
                    && vertexLocation[newXIndex][newYIndex][newZIndex] != -1) {
                graph.addDirectedEdge(vertexIndex, vertexLocation[newXIndex][newYIndex][newZIndex], GRID_SIZE);
            }
        }
    }

    /**
     * Finds the nearest vertex to the given point
     * 
     * @param point: The point
     * @param graph: The graph
     * @return The nearest vertex
     */
    private static Vertex findNearestVertex(Point point, Graph<Block, Double> graph) {
        // TODO: optimize this
        double px = point.getX(), py = point.getY(), pz = point.getZ();
        double minDistance = Double.MAX_VALUE;
        int nearestVertexId = 0;

        for (int i = 0; i < graph.size(); i++) {
            Block block = graph.getVertexProperty(i).getValue();
            double distance = Math.sqrt(Math.pow(px - block.getX(), 2) + Math.pow(py - block.getY(), 2)
                    + Math.pow(pz - block.getZ(), 2));
            if (distance < minDistance) {
                minDistance = distance;
                nearestVertexId = i;
            }
        }

        return new Vertex(nearestVertexId);
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

    private static void logPoints(List<Point> points, String label) {
        Log.i(TAG, "-------------------------------------------" + label
                + "-------------------------------------------");
        for (Point p : points) {
            Log.i(TAG, "x: " + p.getX() + " y: " + p.getY() + " z: " + p.getZ());
        }
    }
}
