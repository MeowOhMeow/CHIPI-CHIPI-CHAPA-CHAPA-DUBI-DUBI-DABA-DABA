package theta_star;
// Block.java

class Block {
    private int id;
    private double x;
    private double y;
    private double z;
    private double weight;

    Block(int id) {
        this.id = id;
    }

    Block() {
        this.id = -1;
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    Block(double x, double y, double z) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    int getId() {
        return id;
    }

    int getWeight() {
        return (int) weight;
    }

    double getX() {
        return x;
    }

    double getY() {
        return y;
    }

    double getZ() {
        return z;
    }

    void setWeight(double weight) {
        this.weight = weight;
    }

    void setX(double x) {
        this.x = x;
    }

    void setY(double y) {
        this.y = y;
    }

    void setZ(double z) {
        this.z = z;
    }
}
