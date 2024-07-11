package pathfinding;

import java.text.DecimalFormat;

public class Vec3d {
    protected final double m_vec[];

    public Vec3d() {
        this(0.0, 0.0, 0.0);
    }

    public Vec3d(double x, double y, double z) {
        m_vec = new double[]{ x, y, z };
    }

    public Vec3d(final double vec[]) {
        if (vec == null)
            throw new NullPointerException("vec must not be null");
        if (vec.length != 3)
            throw new IllegalArgumentException("array must be of size 3");
        m_vec = vec.clone();
    }

    public double[] toArray() {
        return m_vec.clone();
    }

    private static final DecimalFormat s_decimalFormatter = new DecimalFormat("#.###");

    @Override
    public String toString() {
        final String clzName = this.getClass().getName();
        return clzName + "[" + s_decimalFormatter.format(m_vec[0]) +
                        ", " + s_decimalFormatter.format(m_vec[1]) +
                        ", " + s_decimalFormatter.format(m_vec[2]) + "]";
    }
}
