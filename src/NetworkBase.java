import java.util.*;

/**
 * Project: sensor
 * Package: PACKAGE_NAME
 * File: NetworkBase
 * Author: Shang-Lin Hsu
 * Date: Nov, 2020
 * Description: this program implements Network Base Algorithm
 */
public class NetworkBase {
    private int[] dataGens;
    private int numberOfNodes;
    private int dataPerDG;
    private int spacePerSt;
    private double totalEnergyCost;
    private double dataResilience;
    private PriorityQueue<Integer> deadnodes = new PriorityQueue<>();
    private Map<String, Link> copytreeMap;

    /**
     *
     * @return totalEnergyCost
     */
    public double getTotalEnergyCost() {
        return totalEnergyCost;
    }

    /**
     *
     * @return dataResilience
     */
    public double getDataResilience() {
        return  dataResilience;
    }

    /**
     *
     * @return deadnodes list
     */
    public PriorityQueue<Integer> getDeadnodes() {
        return deadnodes;
    }

    public NetworkBase(int[] dataGens, int[] storageNodes, int dataPerDG, int spacePerSt, Map<String, Link> copytreeMap) {
        this.dataGens = dataGens;
        this.numberOfNodes = dataGens.length + storageNodes.length;
        this.dataPerDG = dataPerDG;
        this.spacePerSt = spacePerSt;
        this.copytreeMap = new HashMap<>(copytreeMap);
    }

    public boolean solve() {
        double totalEnergy = 0.0;
        double dataresilient = 0.0;
        Map<String, Link> treeMap = new TreeMap<String, Link>(copytreeMap);
        HashSet<Integer> set = new HashSet<>();

        List<Map.Entry<String, Link>> list = new ArrayList<Map.Entry<String, Link>>(treeMap.entrySet());

        // descending order
        Collections.sort(list, (o1, o2) -> o1.getValue().compareTo(o2.getValue()));

        int[] storageList = new int[numberOfNodes + 1];
        double[] energyList = new double[numberOfNodes + 1];

        for (int i = 1; i < dataGens.length + 1; i++) {
            storageList[i] = dataPerDG;
        }

        for (Map.Entry<String, Link> pair : treeMap.entrySet()) {
            Link link = pair.getValue();
            energyList[link.getEdge().getHead()] = link.getEnergy();
        }

//		Calling Dijkastra Algorithm
        WeighedDigraph graph = new WeighedDigraph(treeMap);
        DijkstraFind finder = new DijkstraFind(graph);

        int storageCapacity = spacePerSt;

        // use sorted map here
        for (int numOfData = 0 ; numOfData < dataGens.length * dataPerDG; numOfData++) {
            for (Map.Entry<String, Link> sortedMap : list) {
                Link link = sortedMap.getValue(); // get current link
                Map<Double, ArrayList<Integer>> map = new HashMap<>(); // list of the path, key is the result of total cost
                PriorityQueue<Double> pq = new PriorityQueue<>(); //

                if (link.getEdge().getHead() <= dataGens.length || storageList[link.getEdge().getHead()] == storageCapacity){
                    continue;
                } else { // case can sent
                    // if current links head > 10, can be sent
                    // go over 10 nodes to find the shortest path
                    for (int i = 1; i <= dataGens.length; i++) {
                        ArrayList<Integer> path = finder.shortestPath(i, link.getEdge().getHead(), dataGens.length);
                        double min = 0;

                        int size = path.size();
                        if (size < 2) continue; // data generator cannot send data
                        for (int j = 0; j < size; j++) {
                            if (j == 0) {
                                String str = buildString(path, j + 1, j);

                                if (treeMap.containsKey(str)) {
                                    double headCost = treeMap.get(str).getTCost() - treeMap.get(str).getRCost();
                                    min += headCost;
                                } else {
                                    System.out.println("have prob can't find" + str);
                                    System.out.println(path.toString());
                                }

                            } else if (j == size - 1) {
                                String str = buildString(path, j - 1, j);
                                if (treeMap.containsKey(str)) {

                                    double tailRcost = treeMap.get(str).getRCost();
                                    double tailScost = treeMap.get(str).getSCost();
                                    min += tailRcost + tailScost;
                                } else {
                                    System.out.println("have prob can't find" + str);
                                    System.out.println(path.toString());
                                }

                            } else {
                                String str = buildString(path, j, j + 1);
                                if (treeMap.containsKey(str)) {
                                    double middleRcost = treeMap.get(str).getTCost();
                                    min += middleRcost;
                                } else {
                                    System.out.println("have prob can't find" + str);
                                    System.out.println(path.toString());
                                }
                            }
                        }
                        // put the path to map
                        map.put(min, new ArrayList<>(path));
                        pq.add(min);
                    }

                    // check if the node can send data (still have storage)
                    while (!pq.isEmpty() && storageList[map.get(pq.peek()).get(0)] == 0) {
                        pq.poll();
                    }
                    if (pq.isEmpty()) {
                        System.out.println("energy is not enough to send all data out");
                        System.out.println(Arrays.toString(storageList));
                        System.out.println(Arrays.toString(energyList));
                        return false;
                    }

                    // send the data so add to total cost
                    totalEnergy += pq.peek();
                    storageList[map.get(pq.peek()).get(0)]--; // data generator send out one data
                    storageList[map.get(pq.peek()).get(map.get(pq.peek()).size() - 1)]++; // storage receive one data

                    // if we find min node, calculate map's value here
                    ArrayList<Integer> target = new ArrayList<>(map.get(pq.peek()));

                    // this time take out the cost
                    int size = target.size();
                    for (int j = 0; j < target.size(); j++) {
                        if (j == 0) {
                            String str = buildString(target, j + 1, j);
                            if (treeMap.containsKey(str)) {
                                double headCost = treeMap.get(str).getTCost() - treeMap.get(str).getRCost();
                                energyList[target.get(j)] -= headCost;
                            } else {
                                System.out.println("Oppps" + str);
                            }

                        } else if (j == size - 1) {
                            String str = buildString(target, j - 1, j);
                            if (treeMap.containsKey(str)) {
                                double tailRcost = treeMap.get(str).getRCost();
                                double tailScost = treeMap.get(str).getSCost();
                                energyList[target.get(j)] -= (tailRcost + tailScost);
                            } else {
                                System.out.println("Oppps" + str);
                            }

                        } else {
                            String str = buildString(target, j, j + 1);
                            if (treeMap.containsKey(str)) {
                                double middleTcost = treeMap.get(str).getTCost();
                                energyList[target.get(j)] -= middleTcost;
                            } else {
                                System.out.println("Oppps" + str);
                            }
                        }
                    }
                }

                // every loop finish will break, the data will be resorted
                break;
            }

            // copy the node, resort the energy
            TreeMap<String, Link> cloneTreeMap = new TreeMap<>(treeMap);

            // clone the map
            for (Map.Entry<String, Link> entry : treeMap.entrySet()) {
                if (energyList[entry.getValue().getEdge().getHead()] < entry.getValue().getTCost()) {
                    int target = entry.getValue().getEdge().getHead();
                    String reverse = "(" + entry.getValue().getEdge().getHead() + ", " + entry.getValue().getEdge().getTail() + ")";
                    cloneTreeMap.remove(entry.getKey());
                    cloneTreeMap.remove(reverse);
                    set.add(target);
                }
            }

            treeMap.clear();
            treeMap.putAll(cloneTreeMap);

            list = new ArrayList<>(treeMap.entrySet());

            // descending
            Collections.sort(list, (o1, o2) -> o1.getValue().compareTo(o2.getValue()));

            // after sort, regenerate the graph
            graph = new WeighedDigraph(treeMap);
            finder = new DijkstraFind(graph);
        }

        for (int i : set) {
            deadnodes.offer(i);
        }

        // after all calculation the resault will be dataresilient
        for (int i = 0; i < storageList.length; i++) {
            if (i < dataGens.length + 1 && storageList[i] > 0) {
                System.out.println("Cannot deliver all data");
                System.out.println(Arrays.toString(storageList));
                System.out.println(Arrays.toString(energyList));
                return false;
            } else {
                dataresilient += storageList[i] * energyList[i];
            }
        }

        // the output data will be store in this array
        totalEnergyCost = totalEnergy;
        dataResilience = dataresilient;
        System.out.println(Arrays.toString(storageList));
        System.out.println(Arrays.toString(energyList));

        return true; // calculation complete
    }

    /**
     * build the string for map's key (node 1, node 2)
     * @param path: the edge
     * @param j: start
     * @param k: end
     * @return return key for the Map
     */
    public static String buildString (ArrayList<Integer> path, int j, int k) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(path.get(j));
        sb.append(", ");
        sb.append(path.get(k));
        sb.append(")");
        return sb.toString();
    }
}
