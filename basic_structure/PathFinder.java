package basic_structure;

import java.util.ArrayList;

public class PathFinder {
    public static void main(String[] args) {
        System.out.println("Unit test for PathFinder.java");
        PathFinder pathFinder = new PathFinder();
        // find path from start to end
        ArrayList<String> path = pathFinder.findPath("start", "end");
        System.out.println("Path: " + path);
    }

    public PathFinder() {
        System.out.println("PathFinder constructor");
    }

    public ArrayList<String> findPath(String start, String end) {
        System.out.println("PathFinder findPath");
        return new ArrayList<String>();
    }
}
