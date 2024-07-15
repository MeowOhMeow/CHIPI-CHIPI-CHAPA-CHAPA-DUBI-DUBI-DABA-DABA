package pathfinding;


public class Point extends Vec3d {
    public Point() {
        this(0.0, 0.0, 0.0);
    }

    public Point(final double x, final double y, final double z) {
        super(x, y, z);
    }

    public double getX() {
        return m_vec[0];
    }

    public double getY() {
        return m_vec[1];
    }

    public double getZ() {
        return m_vec[2];
    }
}
