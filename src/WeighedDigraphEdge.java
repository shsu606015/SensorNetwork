public class WeighedDigraphEdge {
    private int from, to;
    private double weight;

    /**
     * Construct graph edge
     * @param from
     * @param to
     * @param weight
     */
    public WeighedDigraphEdge(int from, int to, double weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }

    /**
     * @return from vertex
     */
    public int from() { return from; }

    /**
     * @return to vertex
     */
    public int to() { return to; }

    /**
     * @return weight of edge between from() and to()
     */
    public double weight() { return weight; }
}
