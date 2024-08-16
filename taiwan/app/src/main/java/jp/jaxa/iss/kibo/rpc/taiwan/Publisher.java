package jp.jaxa.iss.kibo.rpc.taiwan;

import java.util.List;
import java.util.Map;

import android.util.Log;
import jp.jaxa.iss.kibo.rpc.taiwan.pathfinding.PathFindingAPI;
import gov.nasa.arc.astrobee.types.Point;

public class Publisher {
    private static final String TAG = "Publisher";

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
            Log.i(TAG, "Element update.");
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
                Point start = path.getStartPoint();
                Point end = path.getEndPoint();

                Log.i(TAG, "Recalculating path from " + start + " to " + end + " with expansion value " + expansionVal);
                List<Point> calculatedPath = PathFindingAPI.findPath(start, end, expansionVal);

                // Update the path with the calculated path
                path.setPoints(calculatedPath);
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
}

