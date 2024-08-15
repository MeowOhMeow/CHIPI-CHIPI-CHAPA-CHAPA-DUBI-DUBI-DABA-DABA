package main;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import graph.Vertex;
import main.Log;
import pathfinding.PathFindingAPI;
import pathfinding.Point;

public class Publisher {
    private static final String TAG = "Test";

    // Represents a path consisting of points with an associated expansion value
    static class Path {
        private List<Point> points;
        private double expansionVal;
        private Point startPoint;
        private Point endPoint;

        public Path(List<Point> points, double expansionVal, Point startPoint, Point endPoint) {
            this.points = points;
            this.expansionVal = expansionVal;
            this.startPoint = startPoint;
            this.endPoint = endPoint;
        }

        public double getExpansionVal() {
            return expansionVal;
        }

        public void setExpansionVal(double expansionVal) {
            this.expansionVal = expansionVal;
        }

        public List<Point> getPoints() {
            return points;
        }

        public void setPoints(List<Point> points) {
            this.points = points;
        }

        public Point getStartPoint() {
            return startPoint;
        }

        public Point getEndPoint() {
            return endPoint;
        }
    }

    // Represents a generic element holding data
    static class Element<D> {
        private D data;

        public Element(D data) {
            this.data = data;
        }

        public D getData() {
            return data;
        }

        public void setData(D data) {
            this.data = data;
        }

        public void update() {
            System.out.println("Element update");
        }
    }

    // Abstract observer that contains a list of elements and requires an update
    // method
    static abstract class Observer<Key, E> {
        protected Map<Key, Element<E>> elements;

        public Observer(Map<Key, Element<E>> elements) {
            this.elements = elements;
        }

        public abstract void update();
    }

    // Concrete implementation of Observer for elements of type Path
    static class Implementation extends Observer<String, Path> {
        private double expansionVal;

        public Implementation(Map<String, Element<Path>> elements, double expansionVal) {
            super(elements);
            this.expansionVal = expansionVal;
        }

        @Override
        public void update() {
            for (Element<Path> element : elements.values()) { // look over all Element<Path> type object in element
                element.update();
                Path path = element.getData();
                // List<Point> points = path.getPoints();
                // Point start = points.get(0); // Initialize with actual start vertex
                // Point end = points.get(points.size() - 1); // Initialize with actual end
                // vertex
                Point start = path.getStartPoint();
                Point end = path.getEndPoint();
                List<Point> calculatedPath = methodToCalPath(start, end, expansionVal);
                // Update the path with the calculated path
                path.setPoints(calculatedPath);

                // PathFindingAPI.logPoints(calculatedPath, "Print path.");
            }
        }

        // Method to add a new path
        public void addPath(String key, Path path) {
            elements.put(key, new Element<>(path));
        }

        // Method to remove a path by key
        public Path removePath(String key) {
            Element<Path> removedElement = elements.remove(key);
            return removedElement != null ? removedElement.getData() : null;
        }

        // Method to get the expansion value
        public double getExpansionVal() {
            return expansionVal;
        }

        // Method to update the expansion value
        public void setExpansionVal(double expansionVal) {
            this.expansionVal = expansionVal;
        }
    }

    // Stub method for calculating a path, to be implemented
    static List<Point> methodToCalPath(Point start, Point end, double expansionVal) {
        System.out.println("Calculating path from " + start + " to " + end + " with expansion value " + expansionVal);
        // Implement path calculation logic
        return PathFindingAPI.findPath(start, end, expansionVal);
    }

    public static void main(String[] args) {
        // Initialize elements map
        Map<String, Element<Path>> elements = new HashMap<>();
        Implementation implementation = new Implementation(elements, 0.08);

        // generate an original path
        Point pointAtAstronaut = new Point(11.1852d, -6.7607d, 4.8828d);
        Point area_1 = new Point(10.9922d, -9.8876d, 5.2776d);
        System.out.println("x:" + pointAtAstronaut.getX()); // 使用 System.out.println 來打印信息
        List<Point> points = methodToCalPath(pointAtAstronaut, area_1, 0.08);

        // Example usage
        Path path = new Path(points, 0.08, pointAtAstronaut, area_1); // Initialize with actual points and expansion
                                                                      // value
        implementation.addPath("path1", path);

        implementation.setExpansionVal(implementation.getExpansionVal() + 0.01);
        implementation.update();
        Path removedPath = implementation.removePath("path1");
    }
}
