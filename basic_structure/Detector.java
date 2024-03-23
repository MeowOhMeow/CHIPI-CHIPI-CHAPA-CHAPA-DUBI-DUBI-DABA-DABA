package basic_structure;

public class Detector {
    public static void main(String[] args) {
        System.out.println("Unit test for Detector.java");
        Detector detector = new Detector();
        // detect object
        CustomObject object = detector.detect();
        System.out.println("Value: " + object.getValue());
    }

    public Detector() {
        System.out.println("Detector constructor");
    }

    public CustomObject detect() {
        System.out.println("Detector detect");
        return new CustomObject(0);
    }
}
