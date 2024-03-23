package basic_structure;

import java.util.ArrayList;

public class Control {
    private static Detector detector = new Detector();
    private static PathFinder pathFinder = new PathFinder();

    public static void main(String[] args) {
        System.out.println("Main method for this application");
        // find path from start to end
        ArrayList<String> path = pathFinder.findPath("start", "end");
        System.out.println("Path: " + path);
        // detect object
        CustomObject object = detector.detect();
        System.out.println("Value: " + object.getValue());
    }
}
