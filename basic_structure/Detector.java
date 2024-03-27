package basic_structure;

public class Detector {

    public Detector() {
        System.out.println("Detector constructor");
    }

    public CustomObject detect() {
        System.out.println("Detector detect");
        return new CustomObject(0);
    }
}
