package pathfinding;

import main.Log;

public class Utility {
	private static final String TAG = "Utility";

	public static double calEuclideanDistance(Point start, Point pt) {
		return Math.sqrt(Math.pow(start.getX() - pt.getX(), 2) + Math.pow(start.getY() - pt.getY(), 2)
				+ Math.pow(start.getZ() - pt.getZ(), 2));
	}

	public static void logSeparator() {
		Log.i(TAG, "----------------------------------------");
	}

}
