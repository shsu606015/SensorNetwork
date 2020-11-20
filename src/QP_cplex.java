import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.util.*;

/**
 * Project: sensor
 * Package: PACKAGE_NAME
 * File: QP_cplex
 * Author: Shang-Lin Hsu
 * Date: Nov, 2020
 * Description: this program implements Quadratic Programming solution
 */
public class QP_cplex {
    private boolean maxDRL = false;
    private int[] dataGens;
    private int[] storageNodes;
    private double[] dataSendArray;
    private int maxDataSend = 0;
    private int dataPerDG;
    private int spacePerSt;
    private int numberOfNodes;
    private double CplexObj = 0;
    private ArrayList<Integer> deadNodes = new ArrayList<>();
    private ArrayList<Double> totalenergy;
    private Map<String, Link> treeMap;
    private Map<Integer, Set<Integer>> adjacencyList1;
    private HashMap<Integer, List<Integer>> tempclose;
    private Map<String, Double> cplexresult;


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

    /**
     * Constructor
     * @param dataGens: data generator list
     * @param storageNodes: storage list
     * @param dataPerDG: data items per generator
     * @param spacePerSt: storage space per storage nodes
     * @param treeMap: edge information
     * @param adjacencyList1: adjacent information
     * @param tempclose: the closest node
     */
    public QP_cplex(int[] dataGens, int[] storageNodes, int dataPerDG, int spacePerSt,
                    Map<String, Link> treeMap, Map<Integer, Set<Integer>> adjacencyList1, HashMap<Integer, List<Integer>> tempclose) {
        // required network inputs
        this.dataGens = dataGens; // number of data generaters
        this.storageNodes = storageNodes; // number of storages
        this.treeMap = new HashMap<>(treeMap); // the edges
        this.adjacencyList1 = new HashMap<>(adjacencyList1); // the adjacency list
        this.tempclose = new HashMap<>(tempclose); // the closest node
        this.cplexresult = new HashMap<>();
        this.totalenergy = new ArrayList<>();
        this.numberOfNodes = dataGens.length + storageNodes.length;
        this.dataPerDG = dataPerDG;
        this.spacePerSt = spacePerSt;
        this.dataSendArray = new double[dataGens.length];
    }

    /**
     * observe whether less data items sending out can reach higher DRL
     * @param maxDRL: true = enable, default = disable
     */
    public void setMethod(boolean maxDRL) {
        this.maxDRL = maxDRL;
    }

    /**
     *
     * @return return the max data items can send
     */
    public int getDataSend() {
        return maxDataSend;
    }

    /**
     *
     * @return return the max data items can send
     */
    public double[] getDataArray() {
        return dataSendArray;
    }

    /**
     *
     * @return objective function
     */
    public double getCplexObj() {
        return CplexObj;
    }

    /**
     *
     * @return list of dead nodes
     */
    public ArrayList<Integer> getDeadNodes() {
        return deadNodes;
    }

    /**
     *
     * @param method : true: eliminate cycles first. false: don't care about cycles (user for only objective check)
     * @return the energy cost of QP
     */
    public double getEnergyCost(boolean method) {
        double result = 0;

        if (method) {
            StringBuilder path = new StringBuilder();
            //copy the map
            Map<String, Double> clonedata = new HashMap<>(cplexresult);

            // this loop we first eliminate cycles between two nodes
            for (Map.Entry<String, Double> entry : cplexresult.entrySet()) {
                boolean zeroflag = false;
                List<Integer> nodes = new ArrayList<>();
                String curname = entry.getKey();
                // take out the nodes' lable form name
                // for example curname = x12''11', take out node 12 and 11
                for (int j = 0; j < curname.length(); j++) {
                    // check when char is digit
                    if (Character.isDigit(curname.charAt(j))) {
                        // any number starts at 0 is not count in the calculation
                        if (curname.charAt(j) == '0') {
                            zeroflag = true;
                            // source sink
                            nodes.add(0);

                            // take out next number
                            j++;
                            String temp = curname.substring(j);

                            int t = 0;
                            int num = temp.charAt(t) - '0';
                            while (t + 1 < temp.length() && Character.isDigit(temp.charAt(t + 1))) {
                                num = num * 10 + (temp.charAt(t + 1) - '0');
                                t++;
                            }

                            // add the number
                            nodes.add(num);
                            break;
                        } else {
                            // this loop will take out one number form the string
                            int num = curname.charAt(j) - '0';
                            while (j + 1 < curname.length() && Character.isDigit(curname.charAt(j + 1))) {
                                num = num * 10 + curname.charAt(j + 1) - '0';
                                j++;
                            }
                            nodes.add(num);
                        }
                    }
                }

                if (!zeroflag) {
                    int from = nodes.get(0);
                    int to = nodes.get(1);
                    String original = "x" + from + "''" + to + "'";
                    String reverse = "x" + to + "''" + from + "'";
                    if (cplexresult.containsKey(reverse)) {
                        if (entry.getValue() > 0.0 && entry.getValue() <= clonedata.get(reverse)) {
                            clonedata.put(reverse, clonedata.get(reverse) - entry.getValue());
                            clonedata.put(original, 0.0);
                        }
                    }
                } else {
                    int dgLabel = nodes.get(1);
                    dataSendArray[dgLabel - 1] = entry.getValue();
                }
            }

            cplexresult.clear();
            cplexresult.putAll(clonedata);

            for (Map.Entry<String, Double> entry : cplexresult.entrySet()) {
                path.append(entry.getKey() + " : " + entry.getValue()+ "\n");
                boolean zeroflag = false;
                List<Integer> nodes = new ArrayList<>();
                String curname = entry.getKey();
                // take out the nodes' lable form name
                // for example curname = x12''11', take out node 12 and 11
                for (int j = 0; j < curname.length(); j++) {
                    // check when char is digit
                    if (Character.isDigit(curname.charAt(j))) {
                        // any number starts at 0 is not count in the calculation
                        if (curname.charAt(j) == '0') {
                            zeroflag = true;
                            break;
                        } else {
                            // this loop will take out one number form the string
                            int num = curname.charAt(j) - '0';
                            while (j + 1 < curname.length() && Character.isDigit(curname.charAt(j + 1))) {
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
                        // randomly choose a neighbor of from, and get the saving cost
                        to = adjacencyList1.get(from).iterator().next();
                        result += treeMap.get("(" + from + ", " + to + ")").getSCost() * entry.getValue();
                    } else {
                        result += treeMap.get("(" + from + ", " + to + ")").getTCost() * entry.getValue();
                    }
                }
            }

//			File EnergyFile = new File("CPLEX_Data_path_" + numberOfDataItemsPerDG * numberOfDG + "_" + numberOfStoragePerSN + ".txt");
//			BufferedWriter writer_energy = new BufferedWriter(new FileWriter(EnergyFile));
//			writer_energy.write(path.toString());
//			writer_energy.close();

        } else {
            for (Map.Entry<String, Double> entry : cplexresult.entrySet()) {
                boolean zeroflag = false;
                List<Integer> nodes = new ArrayList<>();
                String curname = entry.getKey();
                // take out the nodes' lable form name
                // for example curname = x12''11', take out node 12 and 11
                for (int j = 0; j < curname.length(); j++) {
                    // check when char is digit
                    if (Character.isDigit(curname.charAt(j))) {
                        // any number starts at 0 is not count in the calculation
                        if (curname.charAt(j) == '0') {
                            zeroflag = true;
                            break;
                        } else {
                            // this loop will take out one number form the string
                            int num = curname.charAt(j) - '0';
                            while (j + 1 < curname.length() && Character.isDigit(curname.charAt(j + 1))) {
                                num = num * 10 + curname.charAt(j + 1) - '0';
                                j++;
                            }
                            nodes.add(num);
                        }
                    }
                }

                if (!zeroflag) {
                    int from = nodes.get(0);
                    int to = nodes.get(1);
                    // destination is saving sink
                    if (to == numberOfNodes + 1) {
                        // randomly choose a neighbor of from, and get the saving cost
                        to = adjacencyList1.get(from).iterator().next();
                        result += treeMap.get("(" + from + ", " + to + ")").getSCost() * entry.getValue();
                    } else {
                        result += treeMap.get("(" + from + ", " + to + ")").getTCost() * entry.getValue();
                    }
                }
            }
        }
        return result;
    }

    /**
     * start solving
     * @param gapTolerance
     * @param nameOfFile : file name creates for detail check
     * @return fail or success
     * @throws IloException
     */
    public boolean solve(double gapTolerance, String nameOfFile) throws IloException {

        List<IloRange> constraintsFirst = new ArrayList<IloRange>();
        List<IloRange> constraintsObj = new ArrayList<IloRange>();

        // construct cplex object
        IloCplex CpFirst = new IloCplex(); // for max function to find the min off load electricity
        IloCplex CpObj = new IloCplex(); // for calculating optimized path
        // initialize the name of cplex object
        Map<String, IloNumVar> nameFirst = varName(CpFirst, treeMap, dataGens, storageNodes);
        Map<String, IloNumVar> nameObj = varName(CpFirst, treeMap, dataGens, storageNodes);

        // adding first constrain
        for(int i: adjacencyList1.keySet()) {

            IloLinearNumExpr exprFirst = CpFirst.linearNumExpr();
            IloLinearNumExpr exprObj = CpObj.linearNumExpr();

            // if is generators add the source sink
            if (i <= dataGens.length) {
                String dir = "x0" + i + "'";
                // for first
                exprFirst.addTerm(1, nameFirst.get(dir));
                // for obj
                exprObj.addTerm(1, nameObj.get(dir));
            }

            // + part
            for(int j : adjacencyList1.get(i)) {
                String dir = "x" + j + "''" + i + "'";
                exprFirst.addTerm(1, nameFirst.get(dir));
                // for obj
                exprObj.addTerm(1, nameObj.get(dir));
            }

            // - part
            for(int j : adjacencyList1.get(i)) {
                String dir = "x" + i + "''" + j + "'";
                exprFirst.addTerm(-1, nameFirst.get(dir));
                // for obj
                exprObj.addTerm(-1, nameObj.get(dir));
            }

            // if is storages add the storage sink
            if (i > dataGens.length) {
                int temp = numberOfNodes + 1;
                String dir = "x" + i + "''" + temp;
                exprFirst.addTerm(-1, nameFirst.get(dir));
                // for obj
                exprObj.addTerm(-1, nameObj.get(dir));
            }

            // add constrain
            constraintsFirst.add(CpFirst.addEq(exprFirst, 0));
            constraintsObj.add(CpObj.addEq(exprObj, 0));
        }

        // adding second constrain
        for(int i: adjacencyList1.keySet()) {
            IloLinearNumExpr exprFirst = CpFirst.linearNumExpr();
            IloLinearNumExpr exprObj = CpObj.linearNumExpr();
            int nei = adjacencyList1.get(i).iterator().next();

            // in part
            for(int j : adjacencyList1.get(i)) {
                String dir = "x" + j + "''" + i + "'";
                exprFirst.addTerm(treeMap.get("("+ j + ", " + i +")").getRCost(), nameFirst.get(dir));
                exprObj.addTerm(treeMap.get("("+ j + ", " + i +")").getRCost(), nameObj.get(dir));
            }
            // out part
            for(int j : adjacencyList1.get(i)) {
                String dir = "x" + i + "''" + j + "'";
                // when calculating transfer cost, we need to take out the receive cost (from sender)
                double temp = (double)Math.round((treeMap.get("("+i+", "+j+")").getTCost() - treeMap.get("("+i+", "+j+")").getRCost()) * 10000) / 10000;
                exprFirst.addTerm(temp, nameFirst.get(dir));
                exprObj.addTerm(temp, nameObj.get(dir));
            }

            // if is storages add the storage sink
            if (i > dataGens.length) {
                int temp = numberOfNodes + 1;
                String dir = "x" + i + "''" + temp;
                exprFirst.addTerm(treeMap.get("("+ adjacencyList1.get(i).iterator().next() + ", " + i + ")").getSCost(), nameFirst.get(dir));
                exprObj.addTerm(treeMap.get("("+ adjacencyList1.get(i).iterator().next() + ", " + i + ")").getSCost(), nameObj.get(dir));
            }

            // add constrain
            constraintsFirst.add(CpFirst.addLe(exprFirst, treeMap.get("("+ nei +", "+ i +")").getEnergy()));
            constraintsObj.add(CpObj.addLe(exprObj,  treeMap.get("("+ nei +", "+ i +")").getEnergy()));
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
                exprFirst.addTerm(1, nameFirst.get(dir));
                constraintsFirst.add(CpFirst.addLe(exprFirst, spacePerSt));
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

        //start solving
        // check if the problem is solvable
        if (CpFirst.solve()) {
            System.out.println("CpFirst Success");
        } else {
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
                // get generator's value
                if (CpFirst.getValue(nameFirst.get("x0" + i + "'")) < dataPerDG - 0.1) {

                    System.out.println("x0" + i + "'" + ": " + CpFirst.getValue(nameFirst.get("x0" + i + "'")));
                    System.out.println("Flag!: ");

                    dataIn[i - 1] = (int) CpFirst.getValue(nameFirst.get("x0" + i + "'"));
                    System.out.println("change to"+ dataIn[i - 1]);
                }
            }
        }

        System.out.println(Arrays.toString(dataIn));

        /*------------ Obj's signal node constrains-------------------*/
        for (Integer i : adjacencyList1.keySet()) {
            IloLinearNumExpr exprObj = CpObj.linearNumExpr();

            // dataIn[i] may change if FirstObj is not solvable (DG discard data)
            if (i <= dataGens.length) {
                String dir = "x0" + i + "'";
                exprObj.addTerm(1, nameObj.get(dir));
                // observe whether less data items sending out can reach heigher DRL
                if (maxDRL) {
                    constraintsObj.add(CpObj.addLe(exprObj, dataPerDG));
                } else {
                    constraintsObj.add(CpObj.addEq(exprObj, dataIn[i - 1]));
                }
            } else {
                int temp = numberOfNodes + 1;
                String dir = "x" + i + "''" + temp;
                exprObj.addTerm(1, nameObj.get(dir));
                constraintsObj.add(CpObj.addLe(exprObj, spacePerSt));
            }
        }

        // add sencond objective function
        // e part
        IloLinearNumExpr enode = CpObj.linearNumExpr();

        // save part
        ArrayList<IloNumVar> qua = new ArrayList<>();
        ArrayList<Double> quaVal = new ArrayList<>();

        // the add of all objs (calculate one by one)
        IloNumExpr allObj = CpObj.numExpr();

        for (int i : adjacencyList1.keySet()) {
            if (i > dataGens.length) {
                IloLinearNumExpr receivenode = CpObj.linearNumExpr();
                IloLinearNumExpr outnode = CpObj.linearNumExpr();
                IloNumExpr tempObjre = CpObj.numExpr();
                IloNumExpr tempObjse = CpObj.numExpr();
                //save cost
                int head = adjacencyList1.get(i).iterator().next();
                double e = treeMap.get("(" + head + ", " + i + ")").getEnergy();
                int temp = numberOfNodes + 1;
                String savingNode = "x" + i + "''" + temp;
                // e * x ? 51 -> remaining energy
                enode.addTerm(e, nameObj.get(savingNode));

                qua.add(nameObj.get(savingNode));
                quaVal.add(-1 * treeMap.get("(" + head + ", " + i + ")").getSCost());

                // form the neighbor list
                for (int j : adjacencyList1.get(i)) {
                    String receiveNode = "x" + j + "''" + i + "'";
                    String sendNode = "x" + i + "''" + j + "'";

                    receivenode.addTerm(-1 * treeMap.get("(" + j + ", " + i + ")").getRCost(), nameObj.get(receiveNode));
                    outnode.addTerm(-1 * (treeMap.get("(" + i + ", " + j + ")").getTCost() - treeMap.get("(" + i + ", " + j + ")").getRCost()), nameObj.get(sendNode));
                }

                tempObjre = CpObj.prod(nameObj.get(savingNode), receivenode);
                tempObjse = CpObj.prod(nameObj.get(savingNode), outnode);

                allObj = CpObj.sum(tempObjre, allObj);
                allObj = CpObj.sum(tempObjse, allObj);
            }
        }

        for (int i = 0 ; i < quaVal.size(); i++) {
            allObj = CpObj.sum(allObj, CpObj.prod(quaVal.get(i), qua.get(i), qua.get(i)));
        }

        allObj = CpObj.sum(enode, allObj);

        // write objective function
        CpObj.addMaximize(allObj);

        // ------------- set system parameters for QP ----------------
        CpObj.setParam(IloCplex.Param.OptimalityTarget, IloCplex.OptimalityTarget.OptimalGlobal);
        if (gapTolerance != 0) {
            CpObj.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, gapTolerance);
        }

        CpObj.setParam(IloCplex.Param.MIP.Strategy.Branch, 1);
        CpObj.setParam(IloCplex.Param.Simplex.Tolerances.Markowitz, 0.1);
        CpObj.setParam(IloCplex.Param.Emphasis.MIP, 0); // Emphasize feasibility over optimality
        CpObj.setParam(IloCplex.Param.MIP.Display, 4);
        // this part eliminate MIP issue
        CpObj.setParam(IloCplex.Param.Preprocessing.Linear, 0);
        CpObj.setParam(IloCplex.Param.Emphasis.Numerical, true); // Emphasizes precision in numerically unstable or difficult problems.
        CpObj.setParam(IloCplex.Param.Read.Scale, 1); // More aggressive scaling

        CpObj.exportModel("CPLEXObj_TwoCompare" + nameOfFile + ".lp");

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
        // file for verifying transfer path
        StringBuilder dataTpath = new StringBuilder();

        dataTpath.append("Second Obj Value: "+ CpObj.getObjValue() + "\n");
        //      double[] tempresult1 = lpObj.getPtrVariables();

        for (Map.Entry<String, IloNumVar> entry : nameObj.entrySet()) {
            dataTpath.append(entry.getKey() + " : " + CpObj.getValue(entry.getValue()) + "\n");
        }

        // create file for data verification
//        File PathFile = new File("G:\\downloads\\eclipseJava-workspace\\SensorNetwork\\Lp_output\\Lpout_" + nameOfFile + ".txt");
//        BufferedWriter writer = new BufferedWriter(new FileWriter(PathFile));
//        writer.write(dataTpath.toString());
//        writer.close();

        ArrayList<Double> tempp = new ArrayList<>();

        // calculate the fake cost or remove cost
        DeadNodeCompute deadcomputeFake = new DeadNodeCompute(dataGens, storageNodes);
        tempp = deadcomputeFake.calculate(treeMap, tempclose, CpObj, nameObj);

        deadNodes = deadcomputeFake.getDeadNodes();
        totalenergy.addAll(tempp);

        CplexObj = CpObj.getObjValue();
        maxDataSend = (int) CpFirst.getObjValue();

        // clean up memory used
        CpFirst.end();
        CpObj.end();

        return true;
    }

    /**
     * start solving (allow calculation fail)
     * @param gapTolerance
     * @param nameOfFile file name creates for detail information
     * @return can solve or not
     */
    public boolean solveAllowFail(double gapTolerance, String nameOfFile) {
        try {
            List<IloRange> constraintsFirst = new ArrayList<IloRange>();
            List<IloRange> constraintsObj = new ArrayList<IloRange>();

            // construct cplex object
            IloCplex CpFirst = new IloCplex(); // for max function to find the min off load electricity
            IloCplex CpObj = new IloCplex(); // for calculating optimized path
            // initialize the name of cplex object
            Map<String, IloNumVar> nameFirst = varName(CpFirst, treeMap, dataGens, storageNodes);
            Map<String, IloNumVar> nameObj = varName(CpFirst, treeMap, dataGens, storageNodes);

            // adding first constrain
            for (int i : adjacencyList1.keySet()) {

                IloLinearNumExpr exprFirst = CpFirst.linearNumExpr();
                IloLinearNumExpr exprObj = CpObj.linearNumExpr();

                // if is generators add the source sink
                if (i <= dataGens.length) {
                    String dir = "x0" + i + "'";
                    // for first
                    exprFirst.addTerm(1, nameFirst.get(dir));
                    // for obj
                    exprObj.addTerm(1, nameObj.get(dir));
                }

                // + part
                for (int j : adjacencyList1.get(i)) {
                    String dir = "x" + j + "''" + i + "'";
                    exprFirst.addTerm(1, nameFirst.get(dir));
                    // for obj
                    exprObj.addTerm(1, nameObj.get(dir));
                }

                // - part
                for (int j : adjacencyList1.get(i)) {
                    String dir = "x" + i + "''" + j + "'";
                    exprFirst.addTerm(-1, nameFirst.get(dir));
                    // for obj
                    exprObj.addTerm(-1, nameObj.get(dir));
                }

                // if is storages add the storage sink
                if (i > dataGens.length) {
                    int temp = numberOfNodes + 1;
                    String dir = "x" + i + "''" + temp;
                    exprFirst.addTerm(-1, nameFirst.get(dir));
                    // for obj
                    exprObj.addTerm(-1, nameObj.get(dir));
                }

                // add constrain
                constraintsFirst.add(CpFirst.addEq(exprFirst, 0));
                constraintsObj.add(CpObj.addEq(exprObj, 0));
            }

            // adding second constrain
            for (int i : adjacencyList1.keySet()) {
                IloLinearNumExpr exprFirst = CpFirst.linearNumExpr();
                IloLinearNumExpr exprObj = CpObj.linearNumExpr();
                int nei = adjacencyList1.get(i).iterator().next();

                // in part
                for (int j : adjacencyList1.get(i)) {
                    String dir = "x" + j + "''" + i + "'";
                    exprFirst.addTerm(treeMap.get("(" + j + ", " + i + ")").getRCost(), nameFirst.get(dir));
                    exprObj.addTerm(treeMap.get("(" + j + ", " + i + ")").getRCost(), nameObj.get(dir));
                }
                // out part
                for (int j : adjacencyList1.get(i)) {
                    String dir = "x" + i + "''" + j + "'";
                    // when calculating transfer cost, we need to take out the receive cost (from sender)
                    double temp = (double) Math.round((treeMap.get("(" + i + ", " + j + ")").getTCost() - treeMap.get("(" + i + ", " + j + ")").getRCost()) * 10000) / 10000;
                    exprFirst.addTerm(temp, nameFirst.get(dir));
                    exprObj.addTerm(temp, nameObj.get(dir));
                }

                // if is storages add the storage sink
                if (i > dataGens.length) {
                    int temp = numberOfNodes + 1;
                    String dir = "x" + i + "''" + temp;
                    exprFirst.addTerm(treeMap.get("(" + adjacencyList1.get(i).iterator().next() + ", " + i + ")").getSCost(), nameFirst.get(dir));
                    exprObj.addTerm(treeMap.get("(" + adjacencyList1.get(i).iterator().next() + ", " + i + ")").getSCost(), nameObj.get(dir));
                }

                // add constrain
                constraintsFirst.add(CpFirst.addLe(exprFirst, treeMap.get("(" + nei + ", " + i + ")").getEnergy()));
                constraintsObj.add(CpObj.addLe(exprObj, treeMap.get("(" + nei + ", " + i + ")").getEnergy()));
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
                    exprFirst.addTerm(1, nameFirst.get(dir));
                    constraintsFirst.add(CpFirst.addLe(exprFirst, spacePerSt));
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

            //start solving
            // check if the problem is solvable
            if (CpFirst.solve()) {
                System.out.println("CpFirst Success");
            } else {
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
                    // get generator's value
                    if (CpFirst.getValue(nameFirst.get("x0" + i + "'")) < dataPerDG - 0.1) {

                        System.out.println("x0" + i + "'" + ": " + CpFirst.getValue(nameFirst.get("x0" + i + "'")));
                        System.out.println("Flag!: ");

                        dataIn[i - 1] = (int) CpFirst.getValue(nameFirst.get("x0" + i + "'"));
                        System.out.println("change to" + dataIn[i - 1]);
                    }
                }
            }

            System.out.println(Arrays.toString(dataIn));

            /*------------ Obj's signal node constrains-------------------*/
            for (Integer i : adjacencyList1.keySet()) {
                IloLinearNumExpr exprObj = CpObj.linearNumExpr();

                // dataIn[i] may change if FirstObj is not solvable (DG discard data)
                if (i <= dataGens.length) {
                    String dir = "x0" + i + "'";
                    exprObj.addTerm(1, nameObj.get(dir));
                    // observe whether less data items sending out can reach heigher DRL
                    if (maxDRL) {
                        constraintsObj.add(CpObj.addLe(exprObj, dataPerDG));
                    } else {
                        constraintsObj.add(CpObj.addEq(exprObj, dataIn[i - 1]));
                    }
                } else {
                    int temp = numberOfNodes + 1;
                    String dir = "x" + i + "''" + temp;
                    exprObj.addTerm(1, nameObj.get(dir));
                    constraintsObj.add(CpObj.addLe(exprObj, spacePerSt));
                }
            }

            // add sencond objective function
            // e part
            IloLinearNumExpr enode = CpObj.linearNumExpr();

            // save part
            ArrayList<IloNumVar> qua = new ArrayList<>();
            ArrayList<Double> quaVal = new ArrayList<>();

            // the add of all objs (calculate one by one)
            IloNumExpr allObj = CpObj.numExpr();

            for (int i : adjacencyList1.keySet()) {
                if (i > dataGens.length) {
                    IloLinearNumExpr receivenode = CpObj.linearNumExpr();
                    IloLinearNumExpr outnode = CpObj.linearNumExpr();
                    IloNumExpr tempObjre = CpObj.numExpr();
                    IloNumExpr tempObjse = CpObj.numExpr();
                    //save cost
                    int head = adjacencyList1.get(i).iterator().next();
                    double e = treeMap.get("(" + head + ", " + i + ")").getEnergy();
                    int temp = numberOfNodes + 1;
                    String savingNode = "x" + i + "''" + temp;
                    // e * x ? 51 -> remaining energy
                    enode.addTerm(e, nameObj.get(savingNode));

                    qua.add(nameObj.get(savingNode));
                    quaVal.add(-1 * treeMap.get("(" + head + ", " + i + ")").getSCost());

                    // form the neighbor list
                    for (int j : adjacencyList1.get(i)) {
                        String receiveNode = "x" + j + "''" + i + "'";
                        String sendNode = "x" + i + "''" + j + "'";

                        receivenode.addTerm(-1 * treeMap.get("(" + j + ", " + i + ")").getRCost(), nameObj.get(receiveNode));
                        outnode.addTerm(-1 * (treeMap.get("(" + i + ", " + j + ")").getTCost() - treeMap.get("(" + i + ", " + j + ")").getRCost()), nameObj.get(sendNode));
                    }

                    tempObjre = CpObj.prod(nameObj.get(savingNode), receivenode);
                    tempObjse = CpObj.prod(nameObj.get(savingNode), outnode);

                    allObj = CpObj.sum(tempObjre, allObj);
                    allObj = CpObj.sum(tempObjse, allObj);
                }
            }

            for (int i = 0; i < quaVal.size(); i++) {
                allObj = CpObj.sum(allObj, CpObj.prod(quaVal.get(i), qua.get(i), qua.get(i)));
            }

            allObj = CpObj.sum(enode, allObj);

            // write objective function
            CpObj.addMaximize(allObj);

            // ------------- set system parameters for QP ----------------
            CpObj.setParam(IloCplex.Param.OptimalityTarget, IloCplex.OptimalityTarget.OptimalGlobal);
            if (gapTolerance != 0) {
                CpObj.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, gapTolerance);
            }

            CpObj.setParam(IloCplex.Param.MIP.Strategy.Branch, -1);
            CpObj.setParam(IloCplex.Param.Simplex.Tolerances.Markowitz, 0.1);
            CpObj.setParam(IloCplex.Param.Emphasis.MIP, 0); // Emphasize feasibility over optimality
            CpObj.setParam(IloCplex.Param.MIP.Display, 4);
            // this part eliminate MIP issue
            CpObj.setParam(IloCplex.Param.Preprocessing.Linear, 0);
            CpObj.setParam(IloCplex.Param.Emphasis.Numerical, true); // Emphasizes precision in numerically unstable or difficult problems.
            CpObj.setParam(IloCplex.Param.Read.Scale, 1); // More aggressive scaling

            CpObj.exportModel("CPLEXObj_TwoCompare" + nameOfFile + ".lp");

            // start solving
            // check if the problem is solvable
            if (CpObj.solve()) {
                // objective value of min, x, and y
                System.out.println("obj = " + CpObj.getObjValue());
                for (Map.Entry<String, IloNumVar> entry : nameObj.entrySet()) {
                    cplexresult.put(entry.getKey(), CpObj.getValue(entry.getValue()));
                }
            } else {
                System.out.println("Model not solved");
            }

            System.out.println("Second Objective value: " + CpObj.getObjValue());
            // file for verifying transfer path
            StringBuilder dataTpath = new StringBuilder();

            dataTpath.append("Second Obj Value: " + CpObj.getObjValue() + "\n");
            //      double[] tempresult1 = lpObj.getPtrVariables();

            for (Map.Entry<String, IloNumVar> entry : nameObj.entrySet()) {
                dataTpath.append(entry.getKey() + " : " + CpObj.getValue(entry.getValue()) + "\n");
            }

            // create file for data verification
//        File PathFile = new File("G:\\downloads\\eclipseJava-workspace\\SensorNetwork\\Lp_output\\Lpout_" + nameOfFile + ".txt");
//        BufferedWriter writer = new BufferedWriter(new FileWriter(PathFile));
//        writer.write(dataTpath.toString());
//        writer.close();

            ArrayList<Double> tempp = new ArrayList<>();

            // calculate the fake cost or remove cost
            DeadNodeCompute deadcomputeFake = new DeadNodeCompute(dataGens, storageNodes);
            tempp = deadcomputeFake.calculate(treeMap, tempclose, CpObj, nameObj);

            deadNodes = deadcomputeFake.getDeadNodes();
            totalenergy.addAll(tempp);

            CplexObj = CpObj.getObjValue();
            maxDataSend = (int) CpFirst.getObjValue();

            // clean up memory used
            CpFirst.end();
            CpObj.end();

            return true;
        } catch (IloException e) {
            System.out.println("QP Error");
            return false;
        }
    }
}
