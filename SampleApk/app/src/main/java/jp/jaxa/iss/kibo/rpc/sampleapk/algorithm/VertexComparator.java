package jp.jaxa.iss.kibo.rpc.sampleapk.algorithm;

import java.util.Comparator;
import jp.jaxa.iss.kibo.rpc.sampleapk.pathfinding.Pair;

public class VertexComparator implements Comparator<Pair<Double, Integer>> {
    @Override
    public int compare(Pair<Double, Integer> left, Pair<Double, Integer> right) {
        return left.getFirst().compareTo(right.getFirst());
    }
}
