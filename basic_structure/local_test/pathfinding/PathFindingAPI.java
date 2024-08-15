package pathfinding;

import java.util.*;

import graph.*;
import main.Log;
import algorithm.*;
import pathfinding.Point;

/**
 * The path finding API class.
 */
public class PathFindingAPI {
    private static final String TAG = "PathFindingAPI";
    private static final double GRID_SIZE = 0.05;

    private static List<Obstacle> obstacles = createObstacles();
    private static Graph<Point, NoProperty> graph = null;
    private static double lastExpansionVal = -1;
    private static int xSize, ySize, zSize;
    private static double minX, minY, minZ;
    private static double maxX, maxY, maxZ;

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
        Log.i(TAG, "x:" + start.getX());

        Vertex source = findNearestVertex(start, graph);
        Vertex target = findNearestVertex(end, graph);

        // set temporary points to avoid obstacles
        Point originalSourcePoint = graph.getVertexProperty(source.getId()).getValue();
        Point originalTargetPoint = graph.getVertexProperty(target.getId()).getValue();
        graph.setVertexProperty(source, new VertexProperty<>(new Point(start.getX(), start.getY(), start.getZ())));
        graph.setVertexProperty(target, new VertexProperty<>(new Point(end.getX(), end.getY(), end.getZ())));

        Stack<Vertex> path = ThetaStar.run(source, target, graph, new Heuristic(), obstacles, expansionVal);
        List<Point> result = extractPath(path, graph);

        // restore the original points
        graph.setVertexProperty(source, new VertexProperty<>(originalSourcePoint));
        graph.setVertexProperty(target, new VertexProperty<>(originalTargetPoint));

        // remove the first and last points
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
        minX = 10.3 + expansionVal;
        minY = -10.2 + expansionVal;
        minZ = 4.32 + expansionVal;
        maxX = 11.55 - expansionVal;
        maxY = -6.0 - expansionVal;
        maxZ = 5.57 - expansionVal;

        xSize = (int) Math.ceil((maxX - minX) / GRID_SIZE);
        ySize = (int) Math.ceil((maxY - minY) / GRID_SIZE);
        zSize = (int) Math.ceil((maxZ - minZ) / GRID_SIZE);

        graph = new Graph<>(xSize * ySize * zSize);
        int vertexId = 0;
        for (int z = 0; z < zSize; z++) {
            for (int y = 0; y < ySize; y++) {
                for (int x = 0; x < xSize; x++) {
                    double actualX = minX + x * GRID_SIZE;
                    double actualY = minY + y * GRID_SIZE;
                    double actualZ = minZ + z * GRID_SIZE;

                    graph.setVertexProperty(new Vertex(vertexId++),
                            new VertexProperty<>(new Point(actualX, actualY, actualZ)));
                }
            }
        }

        buildEdges();
    }

    /**
     * Gets the vertex id from the x, y, and z coordinates
     * 
     * @param x: The x coordinate
     * @param y: The y coordinate
     * @param z: The z coordinate
     * @return The vertex id
     */
    private static int getVertexId(int x, int y, int z) {
        return x + y * xSize + z * xSize * ySize;
    }

    /**
     * Builds the edges of the graph
     */
    private static void buildEdges() {
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < zSize; z++) {
                    int vertexId = getVertexId(x, y, z);
                    addEdges(x, y, z, vertexId);
                }
            }
        }
    }

    /**
     * Checks if the given indices are valid
     * 
     * @param x: The x coordinate
     * @param y: The y coordinate
     * @param z: The z coordinate
     * @return True if the indices are valid
     */
    private static boolean isValidIndex(int x, int y, int z) {
        return x >= 0 && x < xSize && y >= 0 && y < ySize && z >= 0 && z < zSize;
    }

    /**
     * Adds edges to the graph
     * 
     * @param x:        The x coordinate
     * @param y:        The y coordinate
     * @param z:        The z coordinate
     * @param vertexId: The vertex id
     */
    private static void addEdges(int x, int y, int z, int vertexId) {
        final int[][] DIRS = {
                { 1, 0, 0 }, { -1, 0, 0 },
                { 0, 1, 0 }, { 0, -1, 0 },
                { 0, 0, 1 }, { 0, 0, -1 }
        };

        for (int[] dir : DIRS) {
            int newX = x + dir[0];
            int newY = y + dir[1];
            int newZ = z + dir[2];

            if (isValidIndex(newX, newY, newZ)) {
                int neighborVertexId = getVertexId(newX, newY, newZ);
                graph.addDirectedEdge(vertexId, neighborVertexId, GRID_SIZE);
            }
        }
    }

    /**
     * Finds the nearest vertex
     * 
     * @param point: The point
     * @param graph: The graph
     * @return The nearest vertex
     */
    private static Vertex findNearestVertex(Point point, Graph<Point, NoProperty> graph) {
        int xIndex = (int) Math.round((point.getX() - minX) / GRID_SIZE);
        int yIndex = (int) Math.round((point.getY() - minY) / GRID_SIZE);
        int zIndex = (int) Math.round((point.getZ() - minZ) / GRID_SIZE);

        xIndex = Math.max(0, Math.min(xIndex, xSize - 1));
        yIndex = Math.max(0, Math.min(yIndex, ySize - 1));
        zIndex = Math.max(0, Math.min(zIndex, zSize - 1));

        int vertexId = getVertexId(xIndex, yIndex, zIndex);
        return new Vertex(vertexId);
    }

    /**
     * Extracts the path from the stack
     * 
     * @param path:  The path
     * @param graph: The graph
     * @return The list of points representing the path
     */
    private static List<Point> extractPath(Stack<Vertex> path, Graph<Point, NoProperty> graph) {
        List<Point> result = new ArrayList<>();
        while (!path.isEmpty()) {
            Vertex vertex = path.pop();
            Point point = graph.getVertexProperty(vertex.getId()).getValue();
            result.add(new Point(point.getX(), point.getY(), point.getZ()));
        }
        return result;
    }

    /**
     * Logs the points
     * 
     * @param points: The points
     * @param label:  The label
     */
    public static void logPoints(List<Point> points, String label) {
        Utility.logSeparator();
        // Log.i(TAG, label + ":");
        // for (Point p : points) {
        // Log.i(TAG, "x: " + p.getX() + " y: " + p.getY() + " z: " + p.getZ());
        // }
        System.out.println(TAG + label + ":");
        for (Point p : points) {
            System.out.println("x: " + p.getX() + " y: " + p.getY() + " z: " + p.getZ());
        }
        Utility.logSeparator();
    }
}
