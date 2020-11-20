import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Project: sensor
 * Package: PACKAGE_NAME
 * File: DeadNodeCompute
 * Author: Shang-Lin Hsu
 * Date: Nov, 2020
 * Description: This program is used to calculate the dead nodes' information and energy cost
 */
public class DeadNodeCompute {

    private int[] dataGens;
    private int[] storageNodes;
    private int numberOfNodes;
    private HashMap<Integer, Double> totaldataitems;
    private ArrayList<Integer> deadNodes;

    /**
     *
     * @param dataGens: data generator list
     * @param storageNodes: stoge node's list
     */
    public DeadNodeCompute(int[] dataGens, int[] storageNodes) {
        this.deadNodes = new ArrayList<>();
        this.dataGens = dataGens;
        this.storageNodes = storageNodes;
        this.numberOfNodes = dataGens.length + storageNodes.length;
        this.totaldataitems = new HashMap<>();
    }

    /**
     *
     * @return dead node's list
     */
    public ArrayList<Integer> getDeadNodes() {
        return deadNodes;
    }

    /**
     *
     * @param treeMap: edge information
     * @param tempclose: closest node
     * @param CpIn: cplex variable data
     * @param cpresult: result of the data transmission path
     * @return each node's cost
     * @throws IloException
     */
    public ArrayList<Double> calculate(Map<String, Link> treeMap, HashMap<Integer, List<Integer>> tempclose, IloCplex CpIn,
                                       Map<String, IloNumVar> cpresult) throws IloException {

        // this map contains cost for each node
        ArrayList<Double> back = new ArrayList<>();
        HashMap<Integer,List<Double>> map = new HashMap<>();

        // this map contains Scost for each non-generator node (since save cost cannot be retrieve from itself)
        HashMap<Integer,Double> nodeScost = new HashMap<>();
        for (Map.Entry<Integer, List<Integer>> pair : tempclose.entrySet()) {
            nodeScost.put(pair.getKey(), treeMap.get("("+pair.getValue().get(0)+", "+pair.getKey()+")").getSCost());
        }

        // output for each node List: receive cost, transmit cost, store cost;

        // get information form the file (file contains: [transfer node, direction node, how many data items])
        List<List<Double>> res = new ArrayList<>();
        List<Double> tempres = new ArrayList<>(); // temp is for numbers at a line from input file

        for (Map.Entry<String, IloNumVar> entry : cpresult.entrySet()){
            String curname = entry.getKey();
            // take out the nodes' lable form name
            // for example curname = x12''11', take out node 12 and 11
            for(int j = 0; j < curname.length(); j++) {
                // check when char is digit
                if (Character.isDigit(curname.charAt(j))) {
                    if(curname.charAt(j) == '0'){
                        tempres.add((double) 0);
                    } else {
                        int num = curname.charAt(j) - '0';
                        while(j + 1 < curname.length() && Character.isDigit(curname.charAt(j + 1))) {
                            num = num * 10 + curname.charAt(j + 1) - '0';
                            j++;
                        }
                        tempres.add((double) num);
                    }
                }
            }
            // add result value to the last element in tempres and add to res
            tempres.add(CpIn.getValue(entry.getValue()));
            res.add(new ArrayList<>(tempres));
            tempres.clear();
        }

        //initial map
        for (int i = 1; i <= numberOfNodes; i++) {
            List<Double> initial = new ArrayList<>();
            initial.add(0.0); // 0 Tcost
            initial.add(0.0); // 1 Rcost
            initial.add(0.0); // 2 Scost
            initial.add(0.0); // 3 total
            map.put(i, initial);
        }

        // calculate target node's energy cost
        for (List<Double> re : res) {
            int transNode = (int) Math.floor(re.get(0));
            int disNode = (int) Math.floor(re.get(1));
            double items = re.get(2);

            totaldataitems.put(transNode, totaldataitems.getOrDefault(transNode, 0.0) + items);

            if (transNode == 0) {
                continue;
            }

            // System.out.println(transNode + " " + disNode); debug
            // calculate non storage node
            if (disNode != numberOfNodes + 1) {
                // case transfer / receive data
                // T cost = sender's transmit cost - receiver's receive cost
                double totalTcost = (treeMap.get("(" + transNode + ", " + disNode + ")").getTCost() - treeMap.get("(" + transNode + ", " + disNode + ")").getRCost()) * items;
                double totalRcost = treeMap.get("(" + transNode + ", " + disNode + ")").getRCost() * items;

                map.get(transNode).set(0, map.get(transNode).get(0) + totalTcost); // tansferNode's Tcost
                map.get(disNode).set(1, map.get(disNode).get(1) + totalRcost); // receiveNode's Rcost

            } else {
                // case save data
                map.get(transNode).set(2, nodeScost.getOrDefault(transNode, 0.0) * items);

            }
        }

        // for text output
        // output file
        StringBuilder energy_mincostoutput = new StringBuilder();
        energy_mincostoutput.append("The order of the cost: Transfer cost, Receive cost, Save cost, total cost, node status").append("\r\n");

        //combine DG and storages
        int[] combine = new int[dataGens.length + storageNodes.length];
        for (int i = 0; i < combine.length; i++) {
            if (i < dataGens.length) {
                combine[i] = dataGens[i];
            } else {
                combine[i] = storageNodes[i - dataGens.length];
            }
        }

        // calculate total cost (0 + 1 + 2)
        for (int i : combine) {
            double totalcost = map.get(i).get(0) + map.get(i).get(1) + map.get(i).get(2);
            map.get(i).set(3, totalcost);
            energy_mincostoutput.append("Node "+ i + ": ["+ map.get(i).get(0) + ", " + map.get(i).get(1) + ", " + map.get(i).get(2) + ", " +
                    map.get(i).get(3)).append("], closest node: ").append(tempclose.get(i).get(0));
            // System.out.println("Node "+ i + ": "+ map.get(i).get(0) + " " + map.get(i).get(1) + " " + map.get(i).get(2) + " " + map.get(i).get(3));
            // add the energy cost result to send back
            back.add(map.get(i).get(3));
            // calculate weather the node is dead
            if (i <= dataGens.length) { // source nodes
                //  current energy    +   energy cost to rely (transfer + receive) data to closest node  >   the minCapacity user identified
                if (map.get(i).get(3) + treeMap.get("(" + i + ", " + tempclose.get(i).get(0) + ")").getTCost() +
                        treeMap.get("(" + i + ", " + tempclose.get(i).get(0) + ")").getRCost() >= treeMap.get("(" + tempclose.get(i).get(0) + ", " + i + ")").getEnergy()) {
                    energy_mincostoutput.append(", status: DEAD!").append(";\r\n");
                    deadNodes.add(i);
                } else {
                    energy_mincostoutput.append(", status: Good").append(";\r\n");
                }
            } else { // storage nodes
                // current energy + energy cost of saving one data item > minCapacity , or
                // current energy + energy cost to rely (transfer + receive) data to closest node  >  the minCapacity
                if (map.get(i).get(3) + treeMap.get("(" + tempclose.get(i).get(0) + ", " + i + ")").getSCost() >= treeMap.get("(" + tempclose.get(i).get(0) + ", " + i + ")").getEnergy() ||
                        map.get(i).get(3) + treeMap.get("(" + i + ", " + tempclose.get(i).get(0) + ")").getTCost() +
                                treeMap.get("(" + i + ", " + tempclose.get(i).get(0) + ")").getRCost() >= treeMap.get("(" + tempclose.get(i).get(0) + ", " + i + ")").getEnergy()) {
                    energy_mincostoutput.append(", status: DEAD!").append(";\r\n");
                    deadNodes.add(i);
                } else {
                    energy_mincostoutput.append(", status: Good").append(";\r\n");
                }
            }
        }
        totaldataitems.clear();
        return back;
    }
}
