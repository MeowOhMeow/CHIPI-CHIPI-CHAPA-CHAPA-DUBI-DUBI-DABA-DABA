#ifndef GRIDGRAPH_HPP
#define GRIDGRAPH_HPP

#include "Graph.hpp"

class GridGraph : public Graph
{
public:
    GridGraph(int rows, int cols) : Graph(rows * cols)
    {
        this->rows = rows;
        this->cols = cols;

        for (int i = 0; i < rows; ++i)
        {
            for (int j = 0; j < cols; ++j)
            {
                buildOctileNeighbors(i, j, rows, cols);
            }
        }
    }

    inline float heuristic(int u, int v) const override
    {
        int u_i = u / cols, u_j = u % cols;
        int v_i = v / cols, v_j = v % cols;

        // Euclidean distance
        return sqrt(pow(u_i - v_i, 2) + pow(u_j - v_j, 2));
    }

    inline float heuristic(const Vertex &u, const Vertex &v) const override
    {
        return heuristic(u.id, v.id);
    }

private:
    int rows, cols;

    inline void buildOctileNeighbors(int i, int j, int rows, int cols)
    {
        if (i - 1 >= 0)
        {
            if (j - 1 >= 0)
                addBidirectedEdge(i * cols + j, (i - 1) * cols + (j - 1), 1.41, 1.41);
            addBidirectedEdge(i * cols + j, (i - 1) * cols + j, 1, 1);
            if (j + 1 < cols)
                addBidirectedEdge(i * cols + j, (i - 1) * cols + (j + 1), 1.41, 1.41);
        }
        if (j - 1 >= 0)
            addBidirectedEdge(i * cols + j, i * cols + (j - 1), 1, 1);
        if (j + 1 < cols)
            addBidirectedEdge(i * cols + j, i * cols + (j + 1), 1, 1);
        if (i + 1 < rows)
        {
            if (j - 1 >= 0)
                addBidirectedEdge(i * cols + j, (i + 1) * cols + (j - 1), 1.41, 1.41);
            addBidirectedEdge(i * cols + j, (i + 1) * cols + j, 1, 1);
            if (j + 1 < cols)
                addBidirectedEdge(i * cols + j, (i + 1) * cols + (j + 1), 1.41, 1.41);
        }
    }
};

#endif // GRIDGRAPH_HPP