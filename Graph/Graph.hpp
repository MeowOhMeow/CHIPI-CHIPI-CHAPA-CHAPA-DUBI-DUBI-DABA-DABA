#ifndef GRAPH_HPP
#define GRAPH_HPP

#include <map>
#include <vector>

using namespace std;

struct Vertex
{
    int id;
};

class Graph
{
private:
    vector<map<int, float>> adjacencyList;

public:
    Graph(int n) : adjacencyList(n) {}

    void addDirectedEdge(int source, int target, float weight)
    {
        adjacencyList[source][target] = weight;
    }

    void addDirectedEdge(Vertex &source, Vertex &target, float weight)
    {
        addDirectedEdge(source.id, target.id, weight);
    }

    void addUndirectedEdge(int source, int target, float weight)
    {
        addDirectedEdge(source, target, weight);
        addDirectedEdge(target, source, weight);
    }

    void addUndirectedEdge(Vertex &source, Vertex &target, float weight)
    {
        addUndirectedEdge(source.id, target.id, weight);
    }

    void addBidirectedEdge(int source, int target, float weight1, float weight2)
    {
        addDirectedEdge(source, target, weight1);
        addDirectedEdge(target, source, weight2);
    }

    void addBidirectedEdge(Vertex &source, Vertex &target, float weight1, float weight2)
    {
        addBidirectedEdge(source.id, target.id, weight1, weight2);
    }

    vector<int> getNeighbors(int vertex) const
    {
        vector<int> neighbors;
        for (const auto &neighbor : adjacencyList[vertex])
        {
            neighbors.push_back(neighbor.first);
        }
        return neighbors;
    }

    vector<int> getNeighbors(Vertex &vertex) const
    {
        return getNeighbors(vertex.id);
    }

    float getWeight(int source, int target) const
    {
        return adjacencyList[source].at(target);
    }

    float getWeight(Vertex &source, Vertex &target) const
    {
        return getWeight(source.id, target.id);
    }

    int size() const
    {
        return adjacencyList.size();
    }

    virtual float heuristic(int u, int v) const
    {
        return 0;
    }

    virtual float heuristic(const Vertex &u, const Vertex &v) const
    {
        return heuristic(u.id, v.id);
    }
};

#endif // GRAPH_HPP