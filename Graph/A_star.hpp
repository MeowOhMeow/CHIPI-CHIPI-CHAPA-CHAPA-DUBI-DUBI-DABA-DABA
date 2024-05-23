#ifndef A_STAR_HPP
#define A_STAR_HPP

#include <iostream>
#include <vector>
#include <map>
#include <queue>
#include <limits>
#include <stack>
#include <math.h>
#include <unordered_set>

#include "Graph.hpp"

using namespace std;

stack<Vertex> reconstructPath(const Vertex &source, const Vertex &target, const vector<int> &pred)
{
    stack<Vertex> path;
    if (pred[target.id] == -1)
        return path;

    int current = target.id;
    while (current != -1 && current != source.id)
    {
        path.push({current});
        current = pred[current];
    }
    path.push({source.id});

    return path;
}

stack<Vertex> A_star(const Vertex &source, const Vertex &target, const Graph &graph)
{
    vector<int> dist(graph.size(), numeric_limits<int>::max());
    vector<int> pred(graph.size(), -1);
    unordered_set<int> closedSet;
    dist[source.id] = 0;

    priority_queue<pair<float, int>, vector<pair<float, int>>, greater<pair<float, int>>> open;
    open.push({graph.heuristic(source, target), source.id});

    while (!open.empty())
    {
        int currentVertex = open.top().second;
        open.pop();

        if (currentVertex == target.id)
            return reconstructPath(source, target, pred);

        closedSet.insert(currentVertex);

        for (const auto &neighbor : graph.getNeighbors(currentVertex))
        {
            if (closedSet.find(neighbor) != closedSet.end())
                continue;

            int alt = dist[currentVertex] + graph.getWeight(currentVertex, neighbor);
            if (alt < dist[neighbor])
            {
                dist[neighbor] = alt;
                pred[neighbor] = currentVertex;
                open.push({dist[neighbor] + graph.heuristic(neighbor, target.id), neighbor});
            }
        }
    }

    return stack<Vertex>();
}

void printPath(const stack<Vertex> &path)
{
    stack<Vertex> temp = path;
    while (!temp.empty())
    {
        cout << temp.top().id << "->";
        temp.pop();
    }
    cout << endl;
}

#endif // A_STAR_HPP
