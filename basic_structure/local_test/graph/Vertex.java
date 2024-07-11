package graph;

public class Vertex {
    private int id;

    public Vertex(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Vertex vertex = (Vertex) obj;
        return id == vertex.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
