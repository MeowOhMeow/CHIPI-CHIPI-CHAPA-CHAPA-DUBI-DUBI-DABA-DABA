package jp.jaxa.iss.kibo.rpc.taiwan.pathfinding;

/**
 * The block class.
 */
public class Block {
    private int id;
    private double x, y, z;

    public Block(int id, double x, double y, double z) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }
}
