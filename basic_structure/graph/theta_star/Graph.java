package theta_star;
// Graph.java

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

// 定義頂點屬性結構
class VertexProperty<T> {
    T value;

    VertexProperty(T val) {
        this.value = val;
    }

    VertexProperty() {
        // 預設建構子
    }
}

// 定義邊屬性結構
class EdgeProperty<U> {
    U value;

    EdgeProperty() {
        // 預設建構子
    }

    EdgeProperty(U val) {
        this.value = val;
    }

    // 覆寫不等於運算符
    boolean notEquals(EdgeProperty<U> other) {
        return !value.equals(other.value);
    }
}

// 定義頂點結構
class Vertex {
    int id;

    Vertex(int i) {
        this.id = i;
    }

    int getId() {
        return id;
    }
}

// 定義圖類
class Graph<T, U> {
    private List<Map<Integer, Float>> adjacencyList;
    private List<Map<Integer, EdgeProperty<U>>> edgePropertiesMap;
    private EdgeProperty<U> emptyEdgeProperty = new EdgeProperty<>();
    private VertexProperty<T> emptyVertexProperty = new VertexProperty<>();
    private List<VertexProperty<T>> vertexPropertiesMap; // 設置每個頂點的屬性(寬度)

    // 建構子
    Graph() {
        adjacencyList = new ArrayList<>();
        edgePropertiesMap = new ArrayList<>();
        vertexPropertiesMap = new ArrayList<>();
    }

    Graph(int numNodes) {
        System.out.println("start init Graph");
        adjacencyList = new ArrayList<>(numNodes);
        edgePropertiesMap = new ArrayList<>(numNodes);
        vertexPropertiesMap = new ArrayList<>(numNodes);

        for (int i = 0; i < numNodes; ++i) {
            adjacencyList.add(new HashMap<>());
            edgePropertiesMap.add(new HashMap<>());
            vertexPropertiesMap.add(emptyVertexProperty);
        }
        System.out.println("finish init Graph");
    }

    // 方法:添加雙向邊
    void addBidirectedEdge(int source, int target, double weight1, double weight2) {
        addDirectedEdge(source, target, weight1);
        addDirectedEdge(target, source, weight2);
    }

    void addBidirectedEdge(Vertex source, Vertex target, double weight1, double weight2) {
        addBidirectedEdge(source.getId(), target.getId(), weight1, weight2);
    }

    // 方法:添加無向邊
    void addUndirectedEdge(int source, int target, double weight) {
        addBidirectedEdge(source, target, weight, weight);
    }

    void addUndirectedEdge(Vertex source, Vertex target, double weight) {
        addUndirectedEdge(source.getId(), target.getId(), weight);
    }

    // 方法:添加有向邊
    void addDirectedEdge(int source, int target, double weight) {
        adjacencyList.get(source).put(target, (float) weight);
    }

    void addDirectedEdge(Vertex source, Vertex target, double weight) {
        addDirectedEdge(source.getId(), target.getId(), weight);
    }

    // 方法:獲取邊屬性
    EdgeProperty<U> getEdgeProperty(int source, int target) {
        Map<Integer, EdgeProperty<U>> map = edgePropertiesMap.get(source);
        if (map.containsKey(target)) {
            return map.get(target);
        }
        return emptyEdgeProperty;
    }

    EdgeProperty<U> getEdgeProperty(Vertex source, Vertex target) {
        return getEdgeProperty(source.getId(), target.getId());
    }

    // 方法:獲取邊權重
    double getEdgeWeight(int source, int target) {
        Map<Integer, Float> map = adjacencyList.get(source);
        if (map.containsKey(target)) {
            return map.get(target);
        }
        return -1.0f;
    }

    double getEdgeWeight(Vertex source, Vertex target) {
        return getEdgeWeight(source.getId(), target.getId());
    }

    // 方法:獲取頂點鄰居
    List<Integer> getNeighbors(int vertex) {
        List<Integer> neighbors = new ArrayList<>();
        Map<Integer, Float> map = adjacencyList.get(vertex);
        for (Map.Entry<Integer, Float> entry : map.entrySet()) {
            neighbors.add(entry.getKey());
        }
        return neighbors;
    }

    List<Integer> getNeighbors(Vertex vertex) {
        return getNeighbors(vertex.getId());
    }

    // 方法:獲取頂點屬性
    VertexProperty<T> getVertexProperty(int vertex) {
        return vertexPropertiesMap.get(vertex);
    }

    VertexProperty<T> getVertexProperty(Vertex vertex) {
        return getVertexProperty(vertex.getId());
    }

    // 方法:設置邊屬性
    void setEdgeProperty(int source, int target, EdgeProperty<U> property) {
        if (property.notEquals(emptyEdgeProperty)) {
            edgePropertiesMap.get(source).put(target, property);
        }
    }

    void setEdgeProperty(Vertex source, Vertex target, EdgeProperty<U> property) {
        setEdgeProperty(source.getId(), target.getId(), property);
    }

    // 方法:設置邊權重
    void setEdgeWeight(int source, int target, double weight) {
        adjacencyList.get(source).put(target, (float) weight);
    }

    void setEdgeWeight(Vertex source, Vertex target, double weight) {
        setEdgeWeight(source.getId(), target.getId(), weight);
    }

    // 方法:設置頂點屬性
    void setVertexProperty(int vertex, VertexProperty<T> property) {
        vertexPropertiesMap.set(vertex, property);
    }

    void setVertexProperty(Vertex vertex, VertexProperty<T> property) {
        setVertexProperty(vertex.getId(), property);
    }

    // 方法:獲取圖的大小
    int size() {
        return adjacencyList.size();
    }
}
