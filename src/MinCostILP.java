import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.io.IOException;
import java.util.*;

/**
 * Project: sensor
 * Package: PACKAGE_NAME
 * File: MinCostILP
 * Author: Shang-Lin Hsu
 * Date: Nov, 2020
 * Description: this program implement Minimum Cost Base ILP
 */
public class MinCostILP {
    private int[] dataGens;
    private int[] storageNodes;
    private int dataPerDG;
    private int spacePerSt;
    private int numberOfNodes;
    private double CplexObj = 0;
    private ArrayList<Integer> deadNodes = new ArrayList<>();
    private Map<String, Link> treeMap;
    private Map<Integer, Set<Integer>> adjacencyList1;
    private HashMap<Integer, List<Integer>> tempclose;
    private Map<String, Double> cplexresult;
    private ArrayList<Double> totalenergy;

    public MinCostILP(int[] dataGens, int[] storageNodes, int minCapacity, int dataPerDG, int spacePerSt,
                    Map<String, Link> treeMap, Map<Integer, Set<Integer>> adjacencyList1, HashMap<Integer, List<Integer>> tempclose) {
        // required network inputs
        this.dataPerDG = dataPerDG;
        this.spacePerSt = spacePerSt;
        this.dataGens = dataGens; // number of data generaters
        this.storageNodes = storageNodes; // number of storages
        this.treeMap = new HashMap<>(treeMap); // the edges
        this.adjacencyList1 = new HashMap<>(adjacencyList1); // the adjacency list
        this.tempclose = new HashMap<>(tempclose); // the closest node
        this.cplexresult = new HashMap<>();
        this.totalenergy = new ArrayList<>();
        this.numberOfNodes = dataGens.length + storageNodes.length;
    }

    /**
     *
     * @return objective function
     */
    public double getILPObj() {
        return CplexObj;
    }

    /**
     *
     * @return list of dead nodes
     */
    public ArrayList<Integer> getDeadNodes() {
        return deadNodes;
    }

    /*-------------------------------- Cplex column name initialization -------------------------------*/
    /**
     * this function create the name for lp / Cplex formulation's column
     * @param treeMap: get the links
     * @param dgs: data generators
     * @param sns: storage nodes
     * @return : name for each column
     * @throws IloException :
     */
    private Map<String, IloNumVar> varName (IloCplex Cp, Map<String, Link> treeMap, int[] dgs, int[] sns) throws IloException {

        Map<String, IloNumVar> name = new TreeMap<>();

        // create column names for each index
        // source sink to data generators
        for (int i = 1; i <= dgs.length; i++) {
            String str = "x0" + i + "'";
            IloNumVar element = Cp.numVar(0, Double.MAX_VALUE, str);
            name.put(str, element);
        }
        // edges between two nodes
        for (Link link : treeMap.values()){
            String str = "x" + link.getEdge().getTail() + "''" + link.getEdge().getHead() + "'";
            IloNumVar element = Cp.numVar(0, Double.MAX_VALUE, str);
            name.put(str, element);
        }
        // storage nodes to storage sink
        for (int sn : sns) {
            String str = "x" + sn + "''" + (this.adjacencyList1.size() + 1);
            IloNumVar element = Cp.numVar(0, Double.MAX_VALUE, str);
            name.put(str, element);
        }

        return name;
    }

    /*-------------------------- Calculates total energy cost ------------------------------*/
    /**
     * This method calculate the cost of the resilience (minimum cost flow)
     * @return : total cost of the path
     */
    public double getResilience() {
        double result = 0;
        double[] initialEnergy = new double[numberOfNodes + 1];
        double[] storage = new double[numberOfNodes + 1];

        for (Map.Entry<String, Link> entry: treeMap.entrySet()) {
            initialEnergy[entry.getValue().getEdge().getHead()] = entry.getValue().getEnergy();
        }

        StringBuilder path = new StringBuilder();

        for (Map.Entry<String, Double> entry : cplexresult.entrySet()){
            boolean zeroflag = false;
            List<Integer> nodes = new ArrayList<>(); // stores the current two nodes
            path.append(entry.getKey() + " : " + entry.getValue() + "\n");
            String curname = entry.getKey();
            // take out the nodes' lable form name
            // for example curname = x12''11', take out node 12 and 11
            for(int j = 0; j < curname.length(); j++) {
                // check when char is digit
                if (Character.isDigit(curname.charAt(j))) {
                    // any number starts at 0 is not count in the calculation
                    if(curname.charAt(j) == '0'){
                        zeroflag = true;
                        break;
                    } else {
                        // this loop will take out one number form the string
                        int num = curname.charAt(j) - '0';
                        while(j + 1 < curname.length() && Character.isDigit(curname.charAt(j + 1))) {
                            num = num * 10 + curname.charAt(j + 1) - '0';
                            j++;
                        }
                        nodes.add(num);
                    }
                }
            }

            // conpare with the input map to retrieve the energy cost
            if (!zeroflag) {
                int from = nodes.get(0);
                int to = nodes.get(1);

                // destination is saving sink
                if (to == numberOfNodes + 1) {
                    storage[from] = entry.getValue();
                    to = adjacencyList1.get(from).iterator().next();
                    double value = treeMap.get("(" + to + ", " + from + ")").getSCost() * entry.getValue();
                    initialEnergy[from] -= value;
                } else {
                    double fromvalue = (treeMap.get("(" + from + ", " + to + ")").getTCost() - treeMap.get("(" + from + ", " + to + ")").getRCost()) * entry.getValue();
                    double tovalue = treeMap.get("(" + from + ", " + to + ")").getRCost() * entry.getValue();
                    initialEnergy[from] -= fromvalue;
                    initialEnergy[to] -= tovalue;
                }
            }
        }

        for(int i = 1; i < storage.length; i++) {
            int deadcont = 0;
            for (int j : adjacencyList1.get(i)) {
                if (initialEnergy[i] < treeMap.get("(" + i + ", " + j + ")").getRCost() &&
                        initialEnergy[i] < treeMap.get("(" + i + ", " + j + ")").getTCost() - treeMap.get("(" + i + ", " + j + ")").getRCost()) {
                    deadcont++;
                }
            }
            if (deadcont == adjacencyList1.get(i).size()) {
                deadNodes.add(i);
            }
            result += storage[i] * initialEnergy[i];
        }

        System.out.println(Arrays.toString(storage));
        System.out.println(Arrays.toString(initialEnergy));

//		File EnergyFile = new File("MinCost_Data_path_" + numberOfStoragePerSN + ".txt");
//		BufferedWriter writer_energy = new BufferedWriter(new FileWriter(EnergyFile));
//		writer_energy.write(path.toString());
//		writer_energy.close();

        return result;
    }

    /**
     * start solving the problem
     * @param nameOfFile: the name of the output file (if needed)
     * @param snedOrNot: high energy node's list
     * @return success or not
     * @throws IloException
     */
    public boolean solve(String nameOfFile, boolean[] snedOrNot) throws IloException {

        List<IloRange> constraintsFirst = new ArrayList<IloRange>();
        List<IloRange> constraintsMinCost = new ArrayList<IloRange>();

        // construct cplex object
        IloCplex CpFirst = new IloCplex(); // for max function to find the min off load electricity
        IloCplex CpObj = new IloCplex(); // for calculating optimized path
        // initialize the name of cplex object
        Map<String, IloNumVar> nameFirst = varName(CpFirst, treeMap, dataGens, storageNodes);
        Map<String, IloNumVar> nameObj = varName(CpFirst, treeMap, dataGens, storageNodes);


        // adding first constrain
        for(int i: adjacencyList1.keySet()) {

            IloLinearNumExpr exprFirst = CpFirst.linearNumExpr();
            IloLinearNumExpr exprMincost = CpObj.linearNumExpr();

            // if is generators add the source sink
            if (i <= dataGens.length) {
                String dir = "x0" + i + "'";
                // for first
                exprFirst.addTerm(1, nameFirst.get(dir));
                // for obj
                exprMincost.addTerm(1, nameObj.get(dir));
            }

            // + part
            for(int j : adjacencyList1.get(i)) {
                String dir = "x" + j + "''" + i + "'";
                exprFirst.addTerm(1, nameFirst.get(dir));
                // for obj
                exprMincost.addTerm(1, nameObj.get(dir));
            }

            // - part
            for(int j : adjacencyList1.get(i)) {
                String dir = "x" + i + "''" + j + "'";
                exprFirst.addTerm(-1, nameFirst.get(dir));
                // for obj
                exprMincost.addTerm(-1, nameObj.get(dir));
            }

            // if is storages add the storage sink
            if (i > dataGens.length) {
                int temp = numberOfNodes + 1;
                String dir = "x" + i + "''" + temp;
                exprFirst.addTerm(-1, nameFirst.get(dir));
                // for obj
                exprMincost.addTerm(-1, nameObj.get(dir));
            }

            // add constrain
            constraintsFirst.add(CpFirst.addEq(exprFirst, 0));
            constraintsMinCost.add(CpObj.addEq(exprMincost, 0));
        }

        // adding second constrain
        for(int i: adjacencyList1.keySet()) {
            IloLinearNumExpr exprFirst = CpFirst.linearNumExpr();
            IloLinearNumExpr exprMinCost = CpObj.linearNumExpr();
            int nei = adjacencyList1.get(i).iterator().next();

            // in part
            for(int j : adjacencyList1.get(i)) {
                String dir = "x" + j + "''" + i + "'";
                exprFirst.addTerm(treeMap.get("("+ j + ", " + i +")").getRCost(), nameFirst.get(dir));
                exprMinCost.addTerm(treeMap.get("("+ j + ", " + i +")").getRCost(), nameObj.get(dir));
            }
            // out part
            for(int j : adjacencyList1.get(i)) {
                String dir = "x" + i + "''" + j + "'";
                // when calculating transfer cost, we need to take out the receive cost (from sender)
                double temp = (double)Math.round((treeMap.get("("+i+", "+j+")").getTCost() - treeMap.get("("+i+", "+j+")").getRCost()) * 10000) / 10000;
                exprFirst.addTerm(temp, nameFirst.get(dir));
                exprMinCost.addTerm(temp, nameObj.get(dir));
            }

            // if is storages add the storage sink
            if (i > dataGens.length) {
                int temp = numberOfNodes + 1;
                String dir = "x" + i + "''" + temp;
                exprFirst.addTerm(treeMap.get("("+ adjacencyList1.get(i).iterator().next() + ", " + i + ")").getSCost(), nameFirst.get(dir));
                exprMinCost.addTerm(treeMap.get("("+ adjacencyList1.get(i).iterator().next() + ", " + i + ")").getSCost(), nameObj.get(dir));
            }

            // add constrain
            constraintsFirst.add(CpFirst.addLe(exprFirst, treeMap.get("("+ nei +", "+ i +")").getEnergy()));
            constraintsMinCost.add(CpObj.addLe(exprMinCost,  treeMap.get("("+ nei +", "+ i +")").getEnergy()));
        }

        // add constrains for single node ** only firstObj
        // objective needs to be separate since the data is possible to be discarded by the data generators (after remove nodes)
        for (Integer i : adjacencyList1.keySet()) {
            IloLinearNumExpr exprFirst = CpFirst.linearNumExpr();

            if (i <= dataGens.length) {
                String dir = "x0" + i + "'";
                exprFirst.addTerm(1, nameFirst.get(dir));
                constraintsFirst.add(CpFirst.addLe(exprFirst, dataPerDG));
            } else {
                int temp = numberOfNodes + 1;
                String dir = "x" + i + "''" + temp;
                if (snedOrNot[i]) {
                    exprFirst.addTerm(1, nameFirst.get(dir));
                    constraintsFirst.add(CpFirst.addLe(exprFirst, spacePerSt));
                } else {
                    exprFirst.addTerm(1, nameFirst.get(dir));
                    constraintsFirst.add(CpFirst.addEq(exprFirst, 0));
                }
            }
        }

        // add first obj objective function
        IloLinearNumExpr objectiveFirst = CpFirst.linearNumExpr();

        for (int i = 0; i < dataGens.length; i++) {
            int temp = i + 1;
            String dir = "x0" + temp + "'";
            objectiveFirst.addTerm(1, nameFirst.get(dir));
        }

        CpFirst.addMaximize(objectiveFirst);
        CpFirst.exportModel("MinCostFirst_" + nameOfFile + ".lp");

        // only see important messages on screen while solving
        //	CpFirst.setParam(IloCplex.Param.Simplex.Display, 0);

        // start solving
        // check if the problem is solvable
        if (!CpFirst.solve()) {
            System.out.println("Model not solved");
        }

        // initial data items to offload
        int[] dataIn = new int[dataGens.length];

        Arrays.fill(dataIn, dataPerDG);

        // objective value
        System.out.println("Objective value: " + CpFirst.getObjValue());
        if (CpFirst.getObjValue() < dataGens.length * dataPerDG) {
            System.out.println("Can not distribute all data!");
            // single generator constrains start at index 98
            for (int i = 1; i <= dataGens.length; i++) {
                // case the network cannot handle all data
                if (CpFirst.getValue(nameFirst.get("x0" + i + "'")) < 99.999) {
                    dataIn[i - 1] = (int) CpFirst.getValue(nameFirst.get("x0" + i + "'"));
                }
            }
            return false;
        } else {
            System.out.println("Success!");
        }

        System.out.println(Arrays.toString(dataIn));

        /*------------ Obj's signal node constrains-------------------*/
        for (Integer i : adjacencyList1.keySet()) {
            IloLinearNumExpr exprMinCost = CpObj.linearNumExpr();

            // dataIn[i] may change if FirstObj is not solvable (DG discard data)
            if (i <= dataGens.length) {
                String dir = "x0" + i + "'";
                exprMinCost.addTerm(1, nameObj.get(dir));
                constraintsMinCost.add(CpObj.addEq(exprMinCost, dataIn[i - 1]));
            } else {
                int temp = numberOfNodes + 1;
                String dir = "x" + i + "''" + temp;
                if (snedOrNot[i]) {
                    exprMinCost.addTerm(1, nameObj.get(dir));
                    constraintsMinCost.add(CpObj.addLe(exprMinCost, spacePerSt));
                } else {
                    exprMinCost.addTerm(1, nameObj.get(dir));
                    constraintsMinCost.add(CpObj.addEq(exprMinCost, 0));
                }
            }
        }

        // original objective function
        IloLinearNumExpr minCostObj = CpObj.linearNumExpr();
        for (Link link : treeMap.values()){
            String dir = "x" + link.getEdge().getTail() + "''" + link.getEdge().getHead() + "'";
            minCostObj.addTerm(link.getTCost(), nameObj.get(dir));
        }
        for (int i = 0; i < storageNodes.length; i++) {
            int temp = numberOfNodes + 1;
            String dir = "x" + storageNodes[i] + "''" + temp;
            int n2 = adjacencyList1.get(storageNodes[i]).iterator().next();
            minCostObj.addTerm(treeMap.get("("+ n2 + ", " + storageNodes[i] +")").getSCost(), nameObj.get(dir));
        }

        // write objective function
        CpObj.addMinimize(minCostObj);

        // export model
//		cpMinCost.exportModel("MinCostObj_" + Lpfilename + ".lp");

        // start solving
        // check if the problem is solvable
        if (CpObj.solve()) {
            // objective value of min, x, and y
            System.out.println("obj = " + CpObj.getObjValue());
            for (Map.Entry<String, IloNumVar> entry : nameObj.entrySet()) {
                cplexresult.put(entry.getKey(), CpObj.getValue(entry.getValue()));
            }
        }
        else {
            System.out.println("Model not solved");
        }

        System.out.println("Second Objective value: " + CpObj.getObjValue());
        // file for verifying transferring path
        StringBuilder dataTpath = new StringBuilder();

        dataTpath.append("Second Obj Value: "+ CpObj.getObjValue() + "\n");

        for (Map.Entry<String, IloNumVar> entry : nameObj.entrySet()) {
            dataTpath.append(entry.getKey() + " : " + CpObj.getValue(entry.getValue()) + "\n");
        }

//        File PathFile = new File("G:\\downloads\\work\\Dr. Bin\\May\\sensor\\out_files\\Lpout_" + Lpfilename + ".txt");
//        BufferedWriter writer = new BufferedWriter(new FileWriter(PathFile));
//        writer.write(dataTpath.toString());
//        writer.close();

        CplexObj = CpObj.getObjValue();

        // clean up memory used
        CpFirst.end();
        CpObj.end();

        return true;
    }
}
