package test;

public class Box {
	private float width, height, depth;
	private float x, y, z;

	public Box(float x, float y, float z, float width, float height, float depth) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.width = width;
		this.height = height;
		this.depth = depth;
	}

	public float getDepth() {
		return depth;
	}

	public float getHeight() {
		return height;
	}

	public float getWidth() {
		return width;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getZ() {
		return z;
	}

	public void setDepth(float depth) {
		this.depth = depth;
	}

	public void setHeight(float height) {
		this.height = height;
	}

	public void setWidth(float width) {
		this.width = width;
	}

	public void setX(float x) {
		this.x = x;
	}

	public void setY(float y) {
		this.y = y;
	}

	public void setZ(float z) {
		this.z = z;
	}

}
