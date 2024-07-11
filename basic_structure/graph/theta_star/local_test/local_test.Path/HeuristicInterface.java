package local_test.Path;

public interface HeuristicInterface {
    double get(Graph<Block, Double> graph, Vertex source, Vertex target);
}