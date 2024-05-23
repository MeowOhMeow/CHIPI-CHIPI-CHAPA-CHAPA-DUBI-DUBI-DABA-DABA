#ifndef CUBEGRAPH_HPP
#define CUBEGRAPH_HPP

#include "Graph.hpp"

class CubeGraph : public Graph
{
protected:
    int rows, cols, depth;

    int neighbors[26][3] = {
        {-1, -1, -1},
        {-1, -1, 0},
        {-1, -1, 1},
        {-1, 0, -1},
        {-1, 0, 0},
        {-1, 0, 1},
        {-1, 1, -1},
        {-1, 1, 0},
        {-1, 1, 1},
        {0, -1, -1},
        {0, -1, 0},
        {0, -1, 1},
        {0, 0, -1},
        {0, 0, 1},
        {0, 1, -1},
        {0, 1, 0},
        {0, 1, 1},
        {1, -1, -1},
        {1, -1, 0},
        {1, -1, 1},
        {1, 0, -1},
        {1, 0, 0},
        {1, 0, 1},
        {1, 1, -1},
        {1, 1, 0},
        {1, 1, 1}};

    inline void
    build26Neighbors(int i, int j, int k, int rows, int cols, int depth)
    {
        for (int n = 0; n < 26; ++n)
        {
            int new_i = i + neighbors[n][0];
            int new_j = j + neighbors[n][1];
            int new_k = k + neighbors[n][2];

            if (new_i >= 0 && new_i < rows && new_j >= 0 && new_j < cols && new_k >= 0 && new_k < depth)
            {
                float weight = sqrt(pow(neighbors[n][0], 2) + pow(neighbors[n][1], 2) + pow(neighbors[n][2], 2));
                addBidirectedEdge(i * cols * depth + j * depth + k, new_i * cols * depth + new_j * depth + new_k, weight, weight);
            }
        }
    }

public:
    CubeGraph(int rows, int cols, int depth) : Graph(rows * cols * depth)
    {
        this->rows = rows;
        this->cols = cols;
        this->depth = depth;

        for (int i = 0; i < rows; ++i)
        {
            for (int j = 0; j < cols; ++j)
            {
                for (int k = 0; k < depth; ++k)
                {
                    build26Neighbors(i, j, k, rows, cols, depth);
                }
            }
        }
    }

    inline float heuristic(int u, int v) const override
    {
        int u_i = u / (cols * depth), u_j = (u / depth) % cols, u_k = u % depth;
        int v_i = v / (cols * depth), v_j = (v / depth) % cols, v_k = v % depth;

        // Euclidean distance
        return sqrt(pow(u_i - v_i, 2) + pow(u_j - v_j, 2) + pow(u_k - v_k, 2));
    }

    inline float heuristic(const Vertex &u, const Vertex &v) const override
    {
        return heuristic(u.id, v.id);
    }
};

#endif // CUBEGRAPH_HPP