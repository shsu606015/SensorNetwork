import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;

// For file reading
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.util.Map;

public class WeighedDigraph {
    private HashMap<Integer, ArrayList<WeighedDigraphEdge>> adj = new HashMap(); // adjacency-list

    public WeighedDigraph(Map<String, Link> tree) {
        for (Map.Entry<String, Link> info : tree.entrySet()) {
            addEdge(new WeighedDigraphEdge(info.getValue().getEdge().getHead(), info.getValue().getEdge().getTail(), info.getValue().getTCost()));
        }
    }

    /**
     * Instantiate graph from file with data
     * @param file
     * @throws IOException
     */
    public WeighedDigraph(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("\\s");

            if (parts.length == 3) {
                int from = Integer.parseInt(parts[0]);
                int to = Integer.parseInt(parts[1]);
                double weight = Double.parseDouble(parts[2]);

                addEdge(new WeighedDigraphEdge(from, to, weight));
            }
        }
    }

    /**
     * @param vertex
     * @return list of edges vertex is connected to.
     */
    public ArrayList<WeighedDigraphEdge> edgesOf(int vertex) {
        return adj.get(vertex);
    }

    /**
     * @return list of all edges in the graph.
     */
    public ArrayList<WeighedDigraphEdge> edges() {
        ArrayList list = new ArrayList<WeighedDigraphEdge>();

        for (int from : adj.keySet()) {
            ArrayList<WeighedDigraphEdge> currentEdges = adj.get(from);
            for (WeighedDigraphEdge e : currentEdges) {
                list.add(e);
            }
        }
        return list;
    }

    /**
     * @return iterable of all vertices in the graph.
     */
    public Iterable<Integer> vertices() {
        HashSet set = new HashSet();
        for (WeighedDigraphEdge edge : edges()) {
            set.add(edge.from());
            set.add(edge.to());
        }

        return set;
    }

    /**
     * @return size of adjacency list
     */
    public int size() { return adj.size(); }

    /**
     * @return string representation of digraph
     */
    public String toString() {
        String out = "";
        for (int from : adj.keySet()) {
            ArrayList<WeighedDigraphEdge> currentEdges = adj.get(from);
            out += from + " -> ";

            if (currentEdges.size() == 0)
                out += "-,";

            for (WeighedDigraphEdge edge : currentEdges)
                out += edge.to() + " @ " + edge.weight() + ", ";

            out += "\n";
        }

        return out;
    }

    /**
     * Add new edge to the system.
     * @param newEdge
     */
    public void addEdge(WeighedDigraphEdge newEdge) {
        // create empty connection set
        if (!adj.containsKey(newEdge.from()))
            adj.put(newEdge.from(), new ArrayList<WeighedDigraphEdge>());

        ArrayList<WeighedDigraphEdge> currentEdges = adj.get(newEdge.from());
        currentEdges.add(newEdge);

        adj.put(newEdge.from(), currentEdges);
    }
}
