#include "A_star.hpp"

#include "KOZGraph.hpp"
/*
These are defined in KOZGraph.hpp:
typedef tuple<int, int, int> point;
typedef pair<point, point> KOZ;

TODO: Implement IO for KOZGraph
*/

using namespace std;

int main()
{
    KOZGraph graph2(3, 3, 3, {{{}}});
    Vertex source2{0};
    Vertex target2{26};

    stack<Vertex> path2 = A_star(source2, target2, graph2);
    printPath(path2);

    return 0;
}
