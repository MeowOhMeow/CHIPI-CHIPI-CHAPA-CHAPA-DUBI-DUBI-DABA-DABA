package jp.jaxa.iss.kibo.rpc.sampleapk.pathfinding;

import java.util.*;
import jp.jaxa.iss.kibo.rpc.sampleapk.graph.*;
import jp.jaxa.iss.kibo.rpc.sampleapk.algorithm.*;

import java.io.*;
import java.util.*;
import java.lang.Math;

public class PathfindingMain {
	
	public static double calculateDistance(List<Vertex> path, Graph<Block, Double> graph) {
	    double distance = 0;
	    for (int i = 0; i < path.size() - 1; i++) {
	        int currentVertexId = path.get(i).getId();
	        int nextVertexId = path.get(i + 1).getId();
	        
	        double dx = graph.getVertexProperty(currentVertexId).getValue().getX() - graph.getVertexProperty(nextVertexId).getValue().getX();
	        double dy = graph.getVertexProperty(currentVertexId).getValue().getY() - graph.getVertexProperty(nextVertexId).getValue().getY();
	        double dz = graph.getVertexProperty(currentVertexId).getValue().getZ() - graph.getVertexProperty(nextVertexId).getValue().getZ();
	        
	        distance += Math.sqrt(dx * dx + dy * dy + dz * dz);;
	    }
	    return distance;
	}

	public static int calculateTurningCount(List<Vertex> path, Graph<Block, Double> graph) {
	    int turningCount = 0;
	    turningCount = path.size() - 2;
	    return turningCount;
	}
	
    public static void main(String[] args) {
        List<Obstacle> obstacles = new ArrayList<>();
        obstacles.add(new Obstacle(10.87, -9.5, 4.27, 11.6, -9.45, 4.97));
        obstacles.add(new Obstacle(10.25, -9.5, 4.97, 10.87, -9.45, 5.62));
        obstacles.add(new Obstacle(10.87, -8.5, 4.97, 11.6, -8.45, 5.62));
        obstacles.add(new Obstacle(10.25, -8.5, 4.27, 10.7, -8.45, 4.97));
        obstacles.add(new Obstacle(10.87, -7.40, 4.27, 11.6, -7.35, 4.97));
        obstacles.add(new Obstacle(10.25, -7.40, 4.97, 10.87, -7.35, 5.62));

        double minX1 = 10.3 + 0.2, minY1 = -10.2 + 0.2, minZ1 = 4.32 + 0.2, maxX1 = 11.55 - 0.2, maxY1 = -6.0 - 0.2, maxZ1 = 5.57 - 0.2;

        int num = 0;

        for (double x = minX1; x <= maxX1; x += 0.05) {
            for (double y = minY1; y <= maxY1; y += 0.05) {
                for (double z = minZ1; z <= maxZ1; z += 0.05) {
                    num++;
                }
            }
        }

        Graph<Block, Double> graph = new Graph<>(num);
        Map<List<Double>, Integer> vertexLocation = new HashMap<>();

        int vertexCount = 0;
        for (double z = minZ1; z <= maxZ1; z += 0.05) {
            for (double y = minY1; y <= maxY1; y += 0.05) {
                for (double x = minX1; x <= maxX1; x += 0.05) {
                	Block block = new Block(vertexCount, x, y, z);
                	VertexProperty<Block> vertexProperty = new VertexProperty<>(block); // 假設 VertexProperty 有相應的構造函數
                	graph.setVertexProperty(new Vertex(vertexCount), vertexProperty);
                    vertexLocation.put(Arrays.asList(x, y, z), vertexCount);
                    vertexCount++;
                }
            }
        }
        
        System.out.println("vertex number: "+ vertexCount);

        for (int i = 0; i < num; i++) {
            double x = graph.getVertexProperty(i).getValue().getX();
            double y = graph.getVertexProperty(i).getValue().getY();
            double z = graph.getVertexProperty(i).getValue().getZ();

            boolean inObstacleFlag = false;

            for (Obstacle obstacle : obstacles) {
                if (x > obstacle.minX - 0.2 && x < obstacle.maxX + 0.2 && y > obstacle.minY - 0.2 && y < obstacle.maxY + 0.2 && z > obstacle.minZ - 0.2 && z < obstacle.maxZ + 0.2) {
                    inObstacleFlag = true;
                    break;
                }
            }

            if (inObstacleFlag) {
                continue;
            } else {
                if (!vertexLocation.containsKey(Arrays.asList(x, y, z))) {
                    continue;
                } else {
                	if (vertexLocation.containsKey(Arrays.asList(x + 0.05, y, z)))
                	{
                		graph.addDirectedEdge(i, vertexLocation.get(Arrays.asList(x + 0.05, y, z)), 0.05);
                	}
                	if (vertexLocation.containsKey(Arrays.asList(x, y + 0.05, z)))
                	{
                		graph.addDirectedEdge(i, vertexLocation.get(Arrays.asList(x, y + 0.05, z)), 0.05);
                	}
                	if (vertexLocation.containsKey(Arrays.asList(x, y, z + 0.05)))
                	{
                		graph.addDirectedEdge(i, vertexLocation.get(Arrays.asList(x, y, z + 0.05)), 0.05);
                	}
                	if (vertexLocation.containsKey(Arrays.asList(x - 0.05, y, z)))
                	{
                		graph.addDirectedEdge(i, vertexLocation.get(Arrays.asList(x - 0.05, y, z)), 0.05);
                	}
                	if (vertexLocation.containsKey(Arrays.asList(x, y - 0.05, z)))
                	{
                		graph.addDirectedEdge(i, vertexLocation.get(Arrays.asList(x, y - 0.05, z)), 0.05);
                	}
                	if (vertexLocation.containsKey(Arrays.asList(x, y, z - 0.05)))
                	{
                		graph.addDirectedEdge(i, vertexLocation.get(Arrays.asList(x, y, z - 0.05)), 0.05);
                	}
                	
                	//System.out.println("build edge");
                }
            }
        }

        HeuristicA heuristic = new HeuristicA();

        try {
        	File inputFile = new File("resources/input.txt");
            Scanner inputScanner = new Scanner(inputFile);
            PrintWriter outputWriter = new PrintWriter(new File("resources/Paths.csv"));

            outputWriter.println("xmin,ymin,zmin,xmax,ymax,zmax");
            
            double distance = 0;
            Integer turningCount = 0;

            List<List<Double>> locationData = new ArrayList<>();

            while (inputScanner.hasNextLine()) {
                String line = inputScanner.nextLine();
                String[] parts = line.split(" ");
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                double z = Double.parseDouble(parts[2]);
                locationData.add(Arrays.asList(x, y, z));
            }

            for (int k = 0; k < locationData.size() - 1; k++) {
                double sx = locationData.get(k).get(0);
                double sy = locationData.get(k).get(1);
                double sz = locationData.get(k).get(2);

                double tx = locationData.get(k + 1).get(0);
                double ty = locationData.get(k + 1).get(1);
                double tz = locationData.get(k + 1).get(2);

                double minDistance1 = 1000000;
                double minDistance2 = 1000000;
                int s = 0, t = 0;

                for (int i = 0; i < num; i++) {
                    double x = graph.getVertexProperty(i).getValue().getX();
                    double y = graph.getVertexProperty(i).getValue().getY();
                    double z = graph.getVertexProperty(i).getValue().getZ();

                    distance = Math.sqrt((sx - x) * (sx - x) + (sy - y) * (sy - y) + (sz - z) * (sz - z));
                    if (distance < minDistance1) {
                        minDistance1 = distance;
                        s = i;
                    }

                    distance = Math.sqrt((tx - x) * (tx - x) + (ty - y) * (ty - y) + (tz - z) * (tz - z));
                    if (distance < minDistance2) {
                        minDistance2 = distance;
                        t = i;
                    }
                }

                Vertex source = new Vertex(s);
                Vertex target = new Vertex(t);

                for (Obstacle obstacle : obstacles) {
                    if (graph.getVertexProperty(source.getId()).getValue().getX() > obstacle.minX && graph.getVertexProperty(source.getId()).getValue().getX() < obstacle.maxX && graph.getVertexProperty(source.getId()).getValue().getY() > obstacle.minY && graph.getVertexProperty(source.getId()).getValue().getY() < obstacle.maxY && graph.getVertexProperty(source.getId()).getValue().getZ() > obstacle.minZ && graph.getVertexProperty(source.getId()).getValue().getZ() < obstacle.maxZ) {
                        System.out.println("source in obstacle");
                        return;
                    }
                    if (graph.getVertexProperty(target.getId()).getValue().getX() > obstacle.minX && graph.getVertexProperty(target.getId()).getValue().getX() < obstacle.maxX && graph.getVertexProperty(target.getId()).getValue().getY() > obstacle.minY && graph.getVertexProperty(target.getId()).getValue().getY() < obstacle.maxY && graph.getVertexProperty(target.getId()).getValue().getZ() > obstacle.minZ && graph.getVertexProperty(target.getId()).getValue().getZ() < obstacle.maxZ) {
                        System.out.println("target in obstacle");
                        return;
                    }
                }

                Stack<Vertex> path = ThetaStar.run(source, target, graph, heuristic, obstacles);
                
                for (Vertex vertex : path) {
                    System.out.println("Vertex ID: " + vertex.getId());
                }
                
                // Calculate distance
                distance += calculateDistance(new ArrayList<>(path), graph);

                // Calculate turning count
                turningCount += calculateTurningCount(new ArrayList<>(path), graph);

                List<List<Double>> passingVertex = new ArrayList<>();

                System.out.println("---------------");
                System.out.println("source: v" + source.getId() + " target: v" + target.getId());
                while (!path.isEmpty()) {
                    System.out.println(" v" + path.peek().getId() + "(x = " + graph.getVertexProperty(path.peek().getId()).getValue().getX() + ", y = " + graph.getVertexProperty(path.peek().getId()).getValue().getY() + ", z = " + graph.getVertexProperty(path.peek().getId()).getValue().getZ() + ")");
                    passingVertex.add(Arrays.asList(graph.getVertexProperty(path.peek().getId()).getValue().getX(), graph.getVertexProperty(path.peek().getId()).getValue().getY(), graph.getVertexProperty(path.peek().getId()).getValue().getZ()));
                    path.pop();
                }

                for (int i = 0; i < passingVertex.size() - 1; i++) {
                    outputWriter.println(passingVertex.get(i).get(0) + "," + passingVertex.get(i).get(1) + "," + passingVertex.get(i).get(2) + "," + passingVertex.get(i + 1).get(0) + "," + passingVertex.get(i + 1).get(1) + "," + passingVertex.get(i + 1).get(2));
                    //System.out.println(passingVertex.get(i).get(0) + "," + passingVertex.get(i).get(1) + "," + passingVertex.get(i).get(2) + "," + passingVertex.get(i + 1).get(0) + "," + passingVertex.get(i + 1).get(1) + "," + passingVertex.get(i + 1).get(2));
                }
                // Print distance and turning count
                
            }     
            
            System.out.println("Distance: " + distance);
            System.out.println("Turning count: " + turningCount);
            inputScanner.close();
            outputWriter.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        }
    }
}

