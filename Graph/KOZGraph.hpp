#ifndef KOZGRAPH_HPP
#define KOZGRAPH_HPP

#include "CubeGraph.hpp"

typedef tuple<int, int, int> point;
typedef pair<point, point> KOZ;

class KOZGraph : public CubeGraph
{
private:
    vector<bool> isObstacle;

public:
    KOZGraph(int rows, int cols, int depth, vector<KOZ> KOZs) : CubeGraph(rows, cols, depth)
    {
        isObstacle.resize(rows * cols * depth, false);
        for (const auto &KOZ : KOZs)
        {
            point start, end;
            tie(start, end) = KOZ;

            int start_i, start_j, start_k;
            tie(start_i, start_j, start_k) = start;

            int end_i, end_j, end_k;
            tie(end_i, end_j, end_k) = end;

            for (int i = start_i; i <= end_i; ++i)
            {
                for (int j = start_j; j <= end_j; ++j)
                {
                    for (int k = start_k; k <= end_k; ++k)
                    {
                        isObstacle[i * cols * depth + j * depth + k] = true;
                    }
                }
            }
        }
    }
};

#endif // KOZGRAPH_HPP