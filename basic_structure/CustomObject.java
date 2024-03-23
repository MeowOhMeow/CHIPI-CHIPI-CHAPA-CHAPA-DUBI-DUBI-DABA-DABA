package basic_structure;

public class CustomObject {
    private int value;

    public static void main(String[] args) {
        System.out.println("Unit test for CustomObject.java");
    }

    public CustomObject(int value) {
        this.value = value;
        System.out.println("CustomObject constructor");
    }

    public int getValue() {
        System.out.println("CustomObject getValue");
        return value;
    }
}
