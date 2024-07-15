package main;

import java.util.List;

import pathfinding.*;
import pathfinding.PathfindingMain.PathFindingAPI;

public class Main {

	public static void main(String[] args) {
		List<Point> path = PathFindingAPI.findPath(new Point(10.9078d, -10.0293d, 5.1124d),
				new Point(11.1852d, -6.7607d, 4.8828d), 0.2);

		Log.close();

		System.out.println("Path: ");
		for (Point point : path) {
			System.out.print(point + " ");
		}
	}
}
