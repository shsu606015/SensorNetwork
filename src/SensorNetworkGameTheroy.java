import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.text.DecimalFormat;
import ilog.concert.*;
import ilog.cplex.*;
import ilog.cplex.IloCplex.UnknownObjectException;

	/**
	 * This program will generate a sensor network graphic
	 * and use Dijkstra algorithm to find the shortest data
	 * preservation path. It will calculate the cost, payment
	 * utility, and total energy cost for the node.
	 *
	 * @author  Shang-Lin Hsu and Yu-ning Yu
	 * @since   2018-09-09
	 */

public class SensorNetworkGameTheroy {

    private static long seed = 995;
    static Random rand = new Random(995);
    static XSSFWorkbook energyworkbook = new XSSFWorkbook();
    static XSSFWorkbook dataworkbook = new XSSFWorkbook();
    static XSSFWorkbook deadnodeworkbook = new XSSFWorkbook();
    static XSSFSheet energysheet = energyworkbook.createSheet("EnergyCostData");
    static XSSFSheet datasheet = dataworkbook.createSheet("DataItems");
    static XSSFSheet deadnodesheet = deadnodeworkbook.createSheet("DeadNode");
    static Map<Integer, Axis> nodes = new LinkedHashMap<Integer, Axis>();
    static Map<Integer, Axis> nodes2 = new LinkedHashMap<Integer, Axis>();
    static Map<Integer, Axis> nodes5 = new LinkedHashMap<Integer, Axis>();
	Map<Integer, Boolean> discovered = new HashMap<Integer, Boolean>();
	Map<Integer, Boolean> explored = new HashMap<Integer, Boolean>();
	Map<Integer, Integer> parent = new HashMap<Integer, Integer>();
	Map<Integer, Integer> connectedNodes = new HashMap<Integer, Integer>();
	Stack<Integer> s = new Stack<Integer>();
	static Map<String, Link> links = new HashMap<String, Link>();
	static Map<String, Link> links2 = new HashMap<String, Link>();
	static Map<String, Link> links3 = new HashMap<String, Link>();
	static Map<String, Link> linkstest = new HashMap<String, Link>();
	static Map<String, Link> linksamp1 = new HashMap<String, Link>();
	static Map<String, Link> linksamp10 = new HashMap<String, Link>();
	static Map<String, Link> linksamp1000 = new HashMap<String, Link>();
	static Map<String, Link> linksamp10000 = new HashMap<String, Link>();
	static Map<String, Link> linksamp1re = new HashMap<String, Link>();
	static Map<String, Link> linksamp10re = new HashMap<String, Link>();
	static Map<String, Link> linksamp1000re = new HashMap<String, Link>();
	static Map<String, Link> linksamp10000re = new HashMap<String, Link>();
	static HashMap<Integer, List<Integer>> close = new HashMap<>();
	static HashMap<Integer, Double> totaldataitems = new HashMap<>();
	
    static int minCapacity;
    static int capacityRandomRange;
    static int biconnectcounter = 1;
    static int[] dataGens;
    static int[] storageNodes;
    static int[] dataGens2;
    static int[] storageNodes2;
    static int numberOfDG;
    static int numberOfDataItemsPerDG;
    static int numberOfStoragePerSN;
    static int numberOfNodes;
    static DecimalFormat fix = new DecimalFormat("##.########");

	public static void main(String[] args) throws IOException, IloException {

		Scanner scan = new Scanner(System.in);
		System.out.print("The width is set to: ");
		//double width = scan.nextDouble();
        double width = 1000.0;
        System.out.println(width);
        
		System.out.print("The height is set to: ");
		//double height = scan.nextDouble();
        double height = 1000.0;
        System.out.println(height);
        
		System.out.print("Number of nodes is set to: ");
		//numberOfNodes = scan.nextInt();
        numberOfNodes = 50;
        System.out.println(numberOfNodes);
        
		System.out.print("Transmission range in meters is set to: ");
		//int transmissionRange = scan.nextInt();
        int transmissionRange = 250;
        System.out.println(transmissionRange);
        
		System.out.print("Data Generators amount is set to: ");
		//numberOfDG = scan.nextInt();
        numberOfDG = 10;
        System.out.println(numberOfDG);
        
		dataGens = new int[numberOfDG];
		System.out.println("Assuming the first " + numberOfDG + " nodes are DGs\n");
		for (int i=1; i<=dataGens.length; i++) {
            dataGens[i-1] = i;
        }

        storageNodes = new int[numberOfNodes-numberOfDG];
        for (int i=0; i<storageNodes.length; i++){
            storageNodes[i] = i + 1 + numberOfDG;
        }

		System.out.print("Data items per DG is set to: ");
		//numberOfDataItemsPerDG = scan.nextInt();
        numberOfDataItemsPerDG = 100;
        System.out.println(numberOfDataItemsPerDG);
        
		System.out.print("Data storage per node is set to:");
		numberOfStoragePerSN = scan.nextInt();
		// CHANGE
//		numberOfStoragePerSN = 26;
//		System.out.println(numberOfStoragePerSN);
		
        capacityRandomRange= 0;
        
		int numberOfSupDem = numberOfDataItemsPerDG * numberOfDG;
		int numberOfstorage = numberOfStoragePerSN * (numberOfNodes-numberOfDG);
        System.out.println("The total number of data items overloading: " + numberOfSupDem);
        System.out.println("The total number of data items storage: " + numberOfstorage);
        
        if (numberOfSupDem > numberOfstorage) {
        	System.out.println("No enough storage");
        	return;
        }
        
		int numberOfStorageNodes = numberOfNodes - numberOfDG;
		int totalNumberOfData = numberOfDG * numberOfDataItemsPerDG;

		SensorNetworkGameTheroy sensor = new SensorNetworkGameTheroy();
		//sensor.populateNodes(numberOfNodes, width, height);
		
		File myfile = new File("inputdata.txt");
		readfileNodes(myfile);
		
		System.out.println("\nNode List:");
		for(int key :sensor.nodes.keySet()) {
			Axis ax = sensor.nodes.get(key);
			System.out.println("Node:" + key + ", xAxis:" + ax.getxAxis() + ", yAxis:" + ax.getyAxis() + ", energycapacity:" + ax.getcapa());
		}

		Map<Integer, Set<Integer>> adjacencyList1 = new LinkedHashMap<Integer, Set<Integer>> ();

		sensor.populateAdjacencyList(numberOfNodes, transmissionRange, adjacencyList1);
		System.out.println("\nAdjacency List: ");

		for(int i: adjacencyList1.keySet()) {
			System.out.print(i);
			System.out.print(": {");
			int adjSize = adjacencyList1.get(i).size();

			if(!adjacencyList1.isEmpty()){
                int adjCount = 0;
				for(int j: adjacencyList1.get(i)) {
                    adjCount+=1;
				    if(adjCount==adjSize){
                        System.out.print(j);
                    } else {
                        System.out.print(j + ", ");
                    }
				}
			}
			System.out.println("}");
		}
		
		System.out.println("\nOriginal Graph:");
		sensor.executeDepthFirstSearchAlg(width, height, adjacencyList1);
        System.out.println();
		
		//test if the graphic is bi-connect
		for (int i = 1; i <= numberOfNodes; i++) {
			for (Map.Entry<Integer, Axis> entry : nodes.entrySet()) {
            	int k = entry.getKey();
            	Axis v = entry.getValue();
            	nodes2.put(k, v);
        	}
			nodes2.remove(i);
			Map<Integer, Set<Integer>> adjacencyList2 = new LinkedHashMap<Integer, Set<Integer>> ();
			sensor.checkbiconnect(i ,numberOfNodes, transmissionRange, adjacencyList2);
			sensor.executeDepthFirstSearchAlgbi(width, height, adjacencyList2);
		}
		
		if(biconnectcounter == 1) {
			System.out.println("\nAll of the Graph is fully connected!");
		} else {
			System.out.println("\nSome Graph is not fully connected!!");
			return;
		}

		//sorting
		Map<String, Link> treeMap = new TreeMap<String, Link>(linkstest);
		
		StringBuilder totalenergycost = new StringBuilder();
		totalenergycost.append("Sensor Network Edges with Distance, Cost and Capacity:\n");
        System.out.println("\nSensor Network Edges with Distance, Cost and Capacity:");
		for (Link link : treeMap.values()){
		    for (Link innerlink : treeMap.values()) {
		    	if ((innerlink.getEdge().getHead() == link.getEdge().getHead()) &&(innerlink.getEdge().getTail() == link.getEdge().getTail())) {
		    		System.out.println(innerlink.toString());
		    		totalenergycost.append(innerlink.toString() + "\n");
		    	}
		    }
		}
		
		// write original energy to file
//        File EnergyFile = new File("G:\\downloads\\eclipseJava-workspace\\SensorNetwork\\Edgecost\\edge_cost_Original.txt");
//        BufferedWriter writer = new BufferedWriter(new FileWriter(EnergyFile));
//        writer.write(totalenergycost.toString());
//        writer.close();
		
		//System.out.println(treeMap.get("(1, 2)"));
		
		//setting capacity for the nodes (amp1)
		
        System.out.println();
        
        // run Algorithm
//        Algorithm(nodes, adjacencyList1, treeMap);
        
		// initiate Lp solver
        IloCplex CpFirst = new IloCplex(); // for max function to find the min off load electricity
        IloCplex CpObject = new IloCplex(); // for calculating optimized path
		
        Map<String, IloNumVar> Firstname = varName(CpFirst, treeMap, dataGens, storageNodes);
        Map<String, IloNumVar> Objtname = varName(CpObject, treeMap, dataGens, storageNodes);
        
		System.out.println("Original Objective:");
		// create first row
		int rowcounter = 0;
		Row energyrow = energysheet.createRow(rowcounter);
		Row datarow = datasheet.createRow(rowcounter);
		Row deadnoderow = deadnodesheet.createRow(rowcounter++);
		// name for cols
		String[] row = new String[]{"RemoveNode", "C_{V-{i}}", "C_V", "dataitems", "dead(Cvi)", "dead(Cv)", "deadNodeLabel",
				"C_V_fake", "Utility_i_Truth_Telling", "C*_i", "C_i", "Utility_i_Lying", "Die_if_lie", "true_dataoffload", "fake_dataoffload", 
				"Utility_Difference", "Discarded_amount"};
		// write
		for (int i = 0; i < row.length; i++) {
			Cell energycell = energyrow.createCell(i);
			energycell.setCellValue((String) row[i]);
		}

		energyrow = energysheet.createRow(rowcounter++);
        double[] notremove = new double[row.length];
        ArrayList<Integer> deadNodes = new ArrayList<>();
        ArrayList<Double> totalenergy = new ArrayList<>();
        ArrayList<Double> dumtotalenergy = new ArrayList<>();
        claculateLp(treeMap, adjacencyList1, close, Firstname, Objtname, CpFirst, CpObject, storageNodes, notremove, deadNodes, totalenergy, dumtotalenergy, "original", "original");
        // add the first row (not removing node)
        
		Cell cell = energyrow.createCell(0);
		cell.setCellValue((Double) notremove[0]);
		Cell cell1 = energyrow.createCell(1);
		cell1.setCellValue((Double) notremove[1]);
		Cell cell2 = energyrow.createCell(2);
		cell2.setCellValue((Double) notremove[1]);
		Cell cell3 = energyrow.createCell(3);
		cell3.setCellValue((Double) notremove[3]);
		Cell cell4 = energyrow.createCell(4);
		cell4.setCellValue((Double) notremove[4]);
		Cell cell5 = energyrow.createCell(5);
		cell5.setCellValue((Double) notremove[4]);
		
		Cell cell6 = energyrow.createCell(6);
		cell6.setCellValue(deadNodes.toString());
		
		Cell cell7 = energyrow.createCell(7);
		cell7.setCellValue((Double) notremove[1]);
		
		// for remove nodes also generates .xlxs
        removeLp(treeMap, adjacencyList1, rowcounter);
		
        // write to csv file
        FileOutputStream out = new FileOutputStream(new File("data.xlsx"));
        energyworkbook.write(out);
        out.close();

		System.out.println();
		System.out.println("Finish");
        //for generating input file for min cost program
//
//        StringBuilder output = new StringBuilder();
//
//        output.append("p min ").append(numberOfNodes + 2).append(" ").append(paths.size()).append("\n");
//        output.append("c min-cost flow problem with ").append(numberOfNodes+2).append(" nodes and ").
//                append(paths.size()).append(" arcs\n");
//        output.append("n 0 ").append(numberOfSupDem).append("\n");
//        output.append("c supply of ").append(numberOfSupDem).append(" at node 0").append("\n");
//        output.append("n ").append(numberOfNodes+1).append(" -").append(numberOfSupDem).append("\n");
//        output.append("c demand of ").append(numberOfSupDem).append(" at node ").append(numberOfNodes+1).append("\n");
//        output.append("c arc list follows\n");
//        output.append("c arc has <tail> <head> <capacity l.b.> <capacity u.b> <cost>\n");
//
//        for (Path path : paths){
//            output.append("a ").append(path.getPath().get(0)).append(" ").
//                    append(path.getPath().get(path.getPath().size()-1)).append(" ").append("0 50").
//                    append(" ").append(path.getCost()).append("\n");
//        }
//        System.out.println();
//        System.out.println("Generated Input file for cs2-4.6 program:(pls refer to ourinput.inp in the folder");
//        System.out.println(output);
//
//        String fileName = "outinput.inp";
//        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
//        writer.write(output.toString());
//
//        writer.close();

/*      //for generating input file for min cost program

        StringBuilder output = new StringBuilder();

        output.append("p min ").append(numberOfNodes + 2).append(" ").append(paths.size()).append("\n");
        output.append("c min-cost flow problem with ").append(numberOfNodes+2).append(" nodes and ").
                append(paths.size()).append(" arcs\n");
        output.append("n 0 ").append(numberOfSupDem).append("\n");
        output.append("c supply of ").append(numberOfSupDem).append(" at node 0").append("\n");
        output.append("n ").append(numberOfNodes+1).append(" -").append(numberOfSupDem).append("\n");
        output.append("c demand of ").append(numberOfSupDem).append(" at node ").append(numberOfNodes+1).append("\n");
        output.append("c arc list follows\n");
        output.append("c arc has <tail> <head> <capacity l.b.> <capacity u.b> <cost>\n");

        for (Path path : paths){
            output.append("a ").append(path.getPath().get(0)).append(" ").
                    append(path.getPath().get(path.getPath().size()-1)).append(" ").append("0 ").
                    append((int) path.getCapacity()).append(" ").append(path.getCost()).append("\n");
        }
        System.out.println();
        System.out.println("Generated Input file for cs2-4.6 program:(pls refer to ourinput.inp in the folder");
        System.out.println(output);

        fileName = "outinput.inp";
        writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(output.toString());

        writer.close();
/*        
*/
	}
	
	/**
	 * this function create the name for lp solver's col
	 * @param treeMap: get the links
	 * @param dgs: data generators
	 * @param sns: storage nodes
	 * @return
	 * @throws IloException 
	 */
	public static Map<String, IloNumVar> varName (IloCplex Cp, Map<String, Link> treeMap, int[] dgs, int[] sns) throws IloException {
        //generateFiles(treeMap, adjacencyList1);
		Map<String, IloNumVar> name = new TreeMap<>();
		
		// create column names for each index
		for (int i = 1; i <= dgs.length; i++) {
			String str = "x0" + i + "'";
			IloNumVar element = Cp.numVar(0, Double.MAX_VALUE, str);
			name.put(str, element);
		}
		for (Link link : treeMap.values()){
			String str = "x" + link.getEdge().getTail() + "''" + link.getEdge().getHead() + "'";
			IloNumVar element = Cp.numVar(0, Double.MAX_VALUE, str);
			name.put(str, element);
		}
		for (int i = 0; i < sns.length; i++) {
			String str = "x" + sns[i] + "''" + (dataGens.length + storageNodes.length + 1);
			IloNumVar element = Cp.numVar(0, Double.MAX_VALUE, str);
			name.put(str, element);
		}
		return name;
	}
	
	public static void removeLp(Map<String, Link> treeMap, Map<Integer, Set<Integer>> adjacencyList1, int rowcounter) throws IOException, IloException {
		/**
		 * fix remove first to get the dataitem to offload
		 * 
		 * 
		 * 
		 */
		Scanner scan = new Scanner(System.in);
		System.out.println("Select which amplifier to modify: (1: transfer, 2: receive, 3: save)");
		int method = scan.nextInt();
		System.out.println("Please enter the amplifier:");
		double amp = scan.nextDouble();

		for (int i = dataGens.length + 1; i <= (dataGens.length + storageNodes.length); i++) {
			Map<String, Link> temptreeMap = new TreeMap<>(treeMap);
			Map<String, Link> newtreeMap = new TreeMap<>();
			Map<Integer, Set<Integer>> tempadj = new LinkedHashMap<>();
			HashMap<Integer, List<Integer>> tempclose = new HashMap<>();
			double[] tempvalues = new double[8];
			double[] values = new double[8];
			
			values[0] = i;
			Row energyrow = energysheet.createRow(rowcounter);
			
			Cell node = energyrow.createCell(0);
			node.setCellValue((int) values[0]);
			
//			Cell originalEnergy = energyrow.createCell(10);
//			originalEnergy.setCellValue((double) totalenergy.get(i - 1));
			
			for(int k: adjacencyList1.keySet()) {
				if(!adjacencyList1.isEmpty()){
					tempadj.put(k, new HashSet<Integer>());
					for(Integer j : adjacencyList1.get(k)) {
						tempadj.get(k).add(j);
					}
				}
			}
			
			for(int k: close.keySet()) {
				if(!close.isEmpty()){
					tempclose.put(k, new ArrayList<Integer>());
					for(Integer j : close.get(k)) {
						tempclose.get(k).add(j);
					}
				}
			}
			
			// change current edge cost (lying) -> only change Tcost
			if (method == 1) {
				for (Link link : treeMap.values()) {
					// calculating new cost for amp = 1
					double dis = link.getDistance();
					double newTcost = getTCostOther(dis, amp);
					// only change the cost of i (if edge form )
					// Tail = sender, Head = receiver
					if (link.getEdge().getTail() == i) {
						Link templink = new Link(link.getEdge(), link.getDistance(), link.getRCost(), newTcost, link.getSCost(), link.getEnergy());
						newtreeMap.put("(" + link.getEdge().getTail() + ", " + link.getEdge().getHead() + ")", templink);
					} else {
						Link templink = new Link(link.getEdge(), link.getDistance(), link.getRCost(), link.getTCost(), link.getSCost(), link.getEnergy());
						newtreeMap.put("(" + link.getEdge().getTail() + ", " + link.getEdge().getHead() + ")", templink);
					}
				}
			} else if (method == 2){
				for (Link link : treeMap.values()) {
					// calculating new cost for amp = 1
					double dis = link.getDistance();
					double newRcost = getRCostOther(dis, amp);
					// only change the cost of i (if edge form )
					// Tail = sender, Head = receiver
					if (link.getEdge().getHead() == i) {
						// when change receive cost, we also needs to change the transfer cost
						double newTcost = link.getTCost() - link.getRCost() + newRcost;
						Link templink = new Link(link.getEdge(), link.getDistance(), newRcost, newTcost, link.getSCost(), link.getEnergy());
						newtreeMap.put("(" + link.getEdge().getTail() + ", " + link.getEdge().getHead() + ")", templink);
					} else {
						Link templink = new Link(link.getEdge(), link.getDistance(), link.getRCost(), link.getTCost(), link.getSCost(), link.getEnergy());
						newtreeMap.put("(" + link.getEdge().getTail() + ", " + link.getEdge().getHead() + ")", templink);
					}
				}
			} else if (method == 3) {
				for (Link link : treeMap.values()) {
					// calculating new cost for amp = 1
					double dis = link.getDistance();
					double newScost = getSCostOther(dis, amp);
					// only change the cost of i (if edge form )
					// Tail = sender, Head = receiver
					if (link.getEdge().getHead() == i) {
						Link templink = new Link(link.getEdge(), link.getDistance(), link.getRCost(), link.getTCost(), newScost, link.getEnergy());
						newtreeMap.put("(" + link.getEdge().getTail() + ", " + link.getEdge().getHead() + ")", templink);
					} else {
						Link templink = new Link(link.getEdge(), link.getDistance(), link.getRCost(), link.getTCost(), link.getSCost(), link.getEnergy());
						newtreeMap.put("(" + link.getEdge().getTail() + ", " + link.getEdge().getHead() + ")", templink);
					}
				}
			}

			// save the edge cost as a file
			StringBuilder totalenergycost = new StringBuilder();
			totalenergycost.append("Sensor Network Edges with Distance, Cost and Capacity:\n");
			for (Link link : newtreeMap.values()){
			    for (Link innerlink : newtreeMap.values()) {
			    	if ((link.getEdge().getHead() == innerlink.getEdge().getHead()) && (link.getEdge().getTail() == innerlink.getEdge().getTail())) {
			    		totalenergycost.append(innerlink.toString() + "\n");
			    	}
			    }
			}
			
//	        File EnergyFile = new File("G:\\downloads\\eclipseJava-workspace\\SensorNetwork\\Edgecost\\edge_cost_" + "c" + String.valueOf(i) + ".txt");
//	        BufferedWriter writer = new BufferedWriter(new FileWriter(EnergyFile));
//	        writer.write(totalenergycost.toString());
//	        writer.close();
			
			
			// calculate not yet remove part
			ArrayList<Integer> tempdeadNodes = new ArrayList<>();
			
	        IloCplex tempCpFirst = new IloCplex(); // for max function to find the min off load electricity
	        IloCplex tempCpObject = new IloCplex(); // for calculating optimized path
			
	        // use newtreeMap when remove nodes
	        Map<String, IloNumVar> tempFirstname = varName(tempCpFirst, newtreeMap, dataGens, storageNodes);
	        Map<String, IloNumVar> tempObjtname = varName(tempCpObject, newtreeMap, dataGens, storageNodes);
			
			ArrayList<Double> newstorage = new ArrayList<>();
			ArrayList<Double> originalstorage = new ArrayList<>();
			//
			claculateLp(newtreeMap, adjacencyList1, close, tempFirstname, tempObjtname, tempCpFirst, tempCpObject, storageNodes, tempvalues, tempdeadNodes, newstorage, originalstorage, "c" + String.valueOf(i), "c" + String.valueOf(i));
			
			// fake obj
			Cell fakeObjvalue = energyrow.createCell(7);
			fakeObjvalue.setCellValue((double) tempvalues[1]);
			
			// fake energy cost
			Cell fakecost = energyrow.createCell(9);
			fakecost.setCellValue((double) newstorage.get(i - 1));
			
			Cell originalEnergy = energyrow.createCell(10);
			originalEnergy.setCellValue((double) originalstorage.get(i - 1));
			
			Cell truedataoffload = energyrow.createCell(13);
			truedataoffload.setCellValue((double) tempvalues[6]);
			
			Cell fakedataoffload = energyrow.createCell(14);
			fakedataoffload.setCellValue((double) tempvalues[7]);
			
			// check die 
    		// current energy + energy cost of saving one data item > minCapacity , or 
    		// current energy + energy cost to rely (transfer + receive) data to closest node  >  the minCapacity
    		if (originalstorage.get(i - 1) + treeMap.get("(" + close.get(i).get(0) + ", " + i + ")").getSCost() > minCapacity ||
    				originalstorage.get(i - 1) + treeMap.get("(" + i + ", " + close.get(i).get(0) + ")").getTCost() + 
    				treeMap.get("(" + i + ", " + close.get(i).get(0) + ")").getRCost() > minCapacity) {
    			
    			Cell deadornot = energyrow.createCell(12);
    			deadornot.setCellValue("Dead");

    		} else {
    			Cell deadornot = energyrow.createCell(12);
    			deadornot.setCellValue("OK");
    		}
			
			System.out.println("Removing " + i + " :");
			// remove treeMap which contains the target node
			for (Link link : treeMap.values()){
				if (link.getEdge().getTail() == i) {
					temptreeMap.remove("(" + link.getEdge().getHead() + ", " + link.getEdge().getTail() + ")");
				} else if (link.getEdge().getHead() == i) {
					temptreeMap.remove("(" + link.getEdge().getHead() + ", " + link.getEdge().getTail() + ")");
				}
			}
			
			// remove adjacent list which contains the target node
			Set<Integer> set = tempadj.remove(i);
			for (Integer target : set) {
				Integer obj = i;
				// test the removing lists
				// System.out.println(target + " " + obj);
				tempadj.get(target).remove(obj);
			}
			
			// removing node in closest list
			tempclose.remove(i);
			for (Integer j : tempclose.keySet()) {
				// if node j's closest node in tempclose is i (the removed node) change it 
				if (tempclose.get(j).get(0) == i) {
					tempclose.get(j).set(1, Integer.MAX_VALUE); // find a new closest node, so change the distance to max
					Set<Integer> tempset = tempadj.get(j); // get the new adjacent list form tempadj
					for (int k : tempset) {
						if (temptreeMap.get("(" + j + ", " + k + ")").distance < tempclose.get(j).get(1)) {
							tempclose.get(j).set(0, k); // change node
							tempclose.get(j).set(1, (int) temptreeMap.get("(" + j + ", " + k + ")").distance); //change distance
						}
					}
				}
			}

			// DG will not change
			int[] tempDG = dataGens;
			// storage will be take out 1 every time
			int[] tempSN = new int[storageNodes.length - 1];
			int incounter = 0;
			int outcounter = 0;
			
			while (incounter < storageNodes.length) {
				if (storageNodes[incounter] == i) {
					incounter++;
				} else {
					tempSN[outcounter++] = storageNodes[incounter++];
				}
			}
			
			// use to test the current storageNodes
//			System.out.println(Arrays.toString(storageNodes));
//			System.out.println(Arrays.toString(tempSN));
			
	        IloCplex CpFirst = new IloCplex(); // for max function to find the min off load electricity
	        IloCplex CpObject = new IloCplex(); // for calculating optimized path
			
	        // use newtreeMap when remove nodes
	        Map<String, IloNumVar> Firstname = varName(CpFirst, temptreeMap, tempDG, tempSN);
	        Map<String, IloNumVar> Objtname = varName(CpObject, temptreeMap, tempDG, tempSN);
			
			ArrayList<Integer> deadNodes = new ArrayList<>();
			ArrayList<Double> newtotalenergy = new ArrayList<>();
			ArrayList<Double> dumtotalenergy = new ArrayList<>();
			// remove
	        boolean flag = claculateLp(temptreeMap, tempadj, tempclose, Firstname, Objtname, CpFirst, CpObject, tempSN, values, deadNodes, newtotalenergy, dumtotalenergy, "r" + String.valueOf(i),  "r" + String.valueOf(i));
	        
	        // new Obj
			Cell Objvalue = energyrow.createCell(1);
			Objvalue.setCellValue((double) values[1]);
	        
			// old Obj is the same as non removed result
			double original = energysheet.getRow(1).getCell(2).getNumericCellValue();
			Cell Originalvalue = energyrow.createCell(2);
			Originalvalue.setCellValue(original);
			
			//new data items
			Cell dataitems = energyrow.createCell(3);
			dataitems.setCellValue(values[3]);
			
			// new dead nodes
			Cell deadnodes = energyrow.createCell(4);
			deadnodes.setCellValue(values[4]);
			
			// old dead node is same as non removed result
			double originaldead = energysheet.getRow(1).getCell(4).getNumericCellValue();
			Cell originaldeadnodes = energyrow.createCell(5);
			originaldeadnodes.setCellValue(originaldead);
			
			Cell deadNodeList = energyrow.createCell(6);
			deadNodeList.setCellValue(deadNodes.toString());
			
			Cell utiliyTure = energyrow.createCell(8);
			double util = (double) values[1] - original;
			if (util < 0.01) {
				utiliyTure.setCellValue((double) 0);
			} else {
				utiliyTure.setCellValue((double) util);
			}

			// calculate the fake utility
			double fakeUtility = energysheet.getRow(rowcounter).getCell(1).getNumericCellValue() - energysheet.getRow(rowcounter).getCell(7).getNumericCellValue()
					+ energysheet.getRow(rowcounter).getCell(9).getNumericCellValue() - energysheet.getRow(rowcounter).getCell(10).getNumericCellValue();
			
			Cell fakeUcell = energyrow.createCell(11);
			fakeUcell.setCellValue((double) fakeUtility);
			
			double diff = energysheet.getRow(rowcounter).getCell(11).getNumericCellValue() - energysheet.getRow(rowcounter).getCell(8).getNumericCellValue();
			Cell originaldata = energyrow.createCell(15);
			originaldata.setCellValue((double) diff);

			double discardData = 0;
			if (energysheet.getRow(rowcounter).getCell(10).getNumericCellValue() > minCapacity) {
				discardData = energysheet.getRow(rowcounter).getCell(14).getNumericCellValue() - energysheet.getRow(rowcounter).getCell(13).getNumericCellValue();
			}
			Cell discard = energyrow.createCell(16);
			discard.setCellValue((double) discardData);

			// after writing one row, row++;
			rowcounter++;
			
	        if(!flag) {
				System.out.println("having some problem!");
				return;
	        }
	        
	        newtreeMap.clear();
	        temptreeMap.clear();
	        tempadj.clear();
	        tempclose.clear();
		}
		
	}
	
	static boolean claculateLp(Map<String, Link> treeMap, Map<Integer, Set<Integer>> adjacencyList1, HashMap<Integer, List<Integer>> tempclose, 
			Map<String, IloNumVar> nameFirst, Map<String, IloNumVar> nameObj, IloCplex CpFirst, IloCplex CpObj, int[] storges, double[] exldata, ArrayList<Integer> deadNodes, ArrayList<Double> faketotalenergy, 
			ArrayList<Double> truetotalenergy, String removed, String Lpfilename) throws IOException, IloException {
		
		List<IloRange> constraintsFirst = new ArrayList<IloRange>();
		List<IloRange> constraintsObj = new ArrayList<IloRange>();
		
		
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
				String dir = "x" + i + "''51";
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
				String dir = "x" + i + "''51";
				exprFirst.addTerm(treeMap.get("("+ adjacencyList1.get(i).iterator().next() + ", " + i + ")").getSCost(), nameFirst.get(dir));
				exprObj.addTerm(treeMap.get("("+ adjacencyList1.get(i).iterator().next() + ", " + i + ")").getSCost(), nameObj.get(dir));
			}
			
			// add constrain
			constraintsFirst.add(CpFirst.addLe(exprFirst, minCapacity));
			constraintsObj.add(CpObj.addLe(exprObj, minCapacity));
		}
		
		// add constrains for single node ** only firstObj
		// objective needs to be separate since the data is possible to be discarded by the data generators (after remove nodes)
		for (Integer i : adjacencyList1.keySet()) {
			IloLinearNumExpr exprFirst = CpFirst.linearNumExpr();
			
			if (i <= dataGens.length) {
				String dir = "x0" + i + "'";
				exprFirst.addTerm(1, nameFirst.get(dir));
				constraintsFirst.add(CpFirst.addLe(exprFirst, numberOfDataItemsPerDG));
			} else {
				String dir = "x" + i + "''51";
				exprFirst.addTerm(1, nameFirst.get(dir));
				constraintsFirst.add(CpFirst.addLe(exprFirst, numberOfStoragePerSN));
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
		
        // only see important messages on screen while solving
	//	CpFirst.setParam(IloCplex.Param.Simplex.Display, 0);
        
        //start solving
		// check if the problem is solvable
		if (CpFirst.solve()) {
			// objective value of min, x, and y
//			System.out.println("obj = " + CpFirst.getObjValue());
//			for (Map.Entry<String, IloNumVar> entry : nameFirst.entrySet()) {
//				System.out.println(entry.getKey() + "  = " + CpFirst.getValue(entry.getValue()));
//			}
//			
//			System.out.println(objectiveFirst.toString());
//			for (int i = 0; i < constraintsFirst.size(); i++) {
//				System.out.println(constraintsFirst.get(i).toString());
//			}

		} else {
			System.out.println("Model not solved");
		}
		
		// initial data items to offload
		int[] dataIn = new int[numberOfDG];
		
		Arrays.fill(dataIn, 100);
		
        // objective value
        System.out.println("Objective value: " + CpFirst.getObjValue());
        if (CpFirst.getObjValue() < 999.999) {
        	System.out.println("Can not distribute all data!");
        	// single generator constrains start at index 98

        	for (int i = 1; i <= dataGens.length; i++) {
        		// get generator's value
        		if (CpFirst.getValue(nameFirst.get("x0" + i + "'")) < 99.999) {
        			
            		System.out.println("x0" + i + "'" + ": " + CpFirst.getValue(nameFirst.get("x0" + i + "'"))); 
        			System.out.println("Flag!: ");
        			
        			dataIn[i - 1] = (int) CpFirst.getValue(nameFirst.get("x0" + i + "'"));
        			System.out.println("change to"+ dataIn[i - 1]);
//    				CpFirst.exportModel("FirstObj_Problem" + Lpfilename + ".lp");
        		}
        	}
        } else {
        	System.out.println("Success!");
        }
		
        System.out.println(Arrays.toString(dataIn));
        
        /*------------ Obj's signal node constrains-------------------*/
		for (Integer i : adjacencyList1.keySet()) {
			IloLinearNumExpr exprObj = CpObj.linearNumExpr();
			
			// dataIn[i] may change if FirstObj is not solvable (DG discard data)
			if (i <= dataGens.length) {
				String dir = "x0" + i + "'";
				exprObj.addTerm(1, nameObj.get(dir));
				constraintsObj.add(CpObj.addEq(exprObj, dataIn[i - 1]));
			} else {
				String dir = "x" + i + "''51";
				exprObj.addTerm(1, nameObj.get(dir));
				constraintsObj.add(CpObj.addLe(exprObj, numberOfStoragePerSN));
			}
		}
        
		// add sencond objective function
		IloLinearNumExpr objectiveObj = CpObj.linearNumExpr();
		
		for (Link link : treeMap.values()){
			String dir = "x" + link.getEdge().getTail() + "''" + link.getEdge().getHead() + "'";
			objectiveObj.addTerm(link.getTCost(), nameObj.get(dir));
		}
		for (int i = 0; i < storges.length; i++) {
			String dir = "x" + storges[i] + "''51";
			int n2 = adjacencyList1.get(storges[i]).iterator().next();
			objectiveObj.addTerm(treeMap.get("("+ n2 + ", " + storges[i] +")").getSCost(), nameObj.get(dir));
		}
		
		// write objective function
		CpObj.addMinimize(objectiveObj);
		
//		CpObj.exportModel("SecondObj_" + Lpfilename + ".lp");
		
        // only see important messages on screen while solving
        
        // start solving
		// check if the problem is solvable
		if (CpObj.solve()) {
//			// objective value of min, x, and y
//			System.out.println("obj = " + CpObj.getObjValue());
//			for (Map.Entry<String, IloNumVar> entry : nameObj.entrySet()) {
//				System.out.println(entry.getKey() + "  = " + CpObj.getValue(entry.getValue()));
//			}
//			
//			System.out.println(objectiveObj.toString());
//			for (int i = 0; i < constraintsObj.size(); i++) {
//				System.out.println(constraintsObj.get(i).toString());
//			}
			
		}
		else {
			System.out.println("Model not solved");
		}
		
		
        /* use to test lpFirst output*/
        // objective value
//        double[] result1 = lpFirst.getPtrVariables();
//        for (int i = 0; i < result1.length; i++) {
//        	System.out.println(name.get(i)+ ": " + result1[i]); 
//        }
        
        
        System.out.println("Second Objective value: " + CpObj.getObjValue());
        // file for path
		StringBuilder dataTpath = new StringBuilder();
		
		dataTpath.append("Second Obj Value: "+ CpObj.getObjValue() + "\n");
  //      double[] tempresult1 = lpObj.getPtrVariables();
		
		for (Map.Entry<String, IloNumVar> entry : nameObj.entrySet()) {
			dataTpath.append(entry.getKey() + " : " + CpObj.getValue(entry.getValue()) + "\n");
		}
		
//        File PathFile = new File("G:\\downloads\\eclipseJava-workspace\\SensorNetwork\\Lp_output\\Lpout_" + Lpfilename + ".txt");
//        BufferedWriter writer = new BufferedWriter(new FileWriter(PathFile));
//        writer.write(dataTpath.toString());
//        writer.close();
        
        
        
        //getMinFile
        
        ArrayList<Double> tempp = new ArrayList<>();
        
        // calculate the true cost
        if (removed.charAt(0) == 'c' && Lpfilename.charAt(0) == 'c') {
        	Map<String, Link> originaltreeMap = new TreeMap<String, Link>(linkstest);
        	tempp = getMinFile(originaltreeMap, tempclose, CpObj, nameObj, storges, exldata, deadNodes, "O" + removed);
            for (Double D : tempp) {
            	truetotalenergy.add(D);
            }
            tempp.clear();
        }
        // calculate the fake cost or remove cost
        tempp = getMinFile(treeMap, tempclose, CpObj, nameObj, storges, exldata, deadNodes, removed);
        
        for (Double D : tempp) {
        	faketotalenergy.add(D);
        }
        
        exldata[1] = CpObj.getObjValue();
        exldata[3] = (int) CpFirst.getObjValue();
        /* use to test the result*/
//        for (int i = 0; i < result.length; i++) {
//        	System.out.println(name.get(i)+ ": " + result[i]); 
//        }
        
        // clean up memory used
        CpFirst.end();
        CpObj.end();
        
        return true;
    }
	
	/**
	 * treemap contains the cost of each edge use to calculate total cost
	 * @param treeMap : contains the edge cost
	 * @throws IOException
	 * @throws IloException 
	 * @throws UnknownObjectException 
	 */
	static ArrayList<Double> getMinFile(Map<String, Link> treeMap, HashMap<Integer, List<Integer>> tempclose, IloCplex CpIn,
										Map<String, IloNumVar> cpresult, int[] storages, double[] exldata, ArrayList<Integer> deadNodes, String removed) throws IOException, UnknownObjectException, IloException {
		//treeMap.get("("+j+", "+i+")").getRCost() <- format to get cost
		/*
		 * getRcost() = receive cost
		 * getTcost() = transmit cost
		 * getScost() = save cost
		 */
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

		// use to check if data items transfered exceed energy capacity
		double tempcapa = 0.0;
		double tempenergy = 0.0;
		double expect = 0.0;
		String tempremoved = removed;
		int targetnode = tempremoved.equals("original") ? 0 : Integer.parseInt(tempremoved.replaceAll("[^\\d.]", ""));

		// calculate target node's energy cost
		for (int i = 0; i < res.size(); i++){
			int transNode = (int) Math.floor(res.get(i).get(0));
			int disNode = (int) Math.floor(res.get(i).get(1));
			double items =  res.get(i).get(2);

			if (removed.equals("original")) {
				totaldataitems.put(transNode, totaldataitems.getOrDefault(transNode, 0.0) + items);
			}

			if (transNode == 0) {
				continue;
			}
			// System.out.println(transNode + " " + disNode); debug
			// calculate non storage node
			if (disNode != numberOfNodes + 1) {
				// case transfer / receive data
				// T cost = sender's transmit cost - receiver's receive cost
				double totalTcost = (treeMap.get("("+transNode+", "+disNode+")").getTCost() - treeMap.get("("+transNode+", "+disNode+")").getRCost()) * items;
				double totalRcost = treeMap.get("("+transNode+", "+disNode+")").getRCost() * items;

				// when in is our target node
				if (transNode == targetnode && targetnode != 0) {
					// cost for receive + out
					double inoutcost = treeMap.get("("+transNode+", "+disNode+")").getTCost() * items;
					// still have energy
					if (tempenergy + inoutcost < minCapacity) {
						tempenergy += inoutcost;
						tempcapa += items;
					} else { // no energy
						double remainenergy = minCapacity - tempenergy;
						double transfercost = treeMap.get("("+transNode+", "+disNode+")").getTCost();
						double cansend = remainenergy / transfercost;
						tempcapa += cansend;
						if (exldata[6] == 0.0) {
							exldata[6] = tempcapa;
						}
						tempenergy = minCapacity;
					}
					expect += items;
				}

				map.get(transNode).set(0, map.get(transNode).get(0) + totalTcost); // tansferNode's Tcost
				map.get(disNode).set(1, map.get(disNode).get(1) + totalRcost); // receiveNode's Rcost

			} else {
				// case save data
				map.get(transNode).set(2, nodeScost.getOrDefault(transNode, 0.0) * items);

				// when in is our target node
				if (transNode == targetnode && targetnode != 0) {
					// cost for receive + out
					double tempcost = nodeScost.getOrDefault(transNode, 0.0) * items;
					// still have energy
					if (tempenergy + tempcost < minCapacity) {
						tempenergy += tempcost;
						tempcapa += items;
					} else { // no energy
						double remainenergy = minCapacity - tempenergy;
						double transfercost = nodeScost.getOrDefault(transNode, 0.0);
						double cansend = remainenergy / transfercost;
						tempcapa += cansend;
						if (exldata[6] == 0.0) {
							exldata[6] = tempcapa;
						}
						tempenergy = minCapacity;
					}
					expect += items;
				}
			}

		}

		if (exldata[6] == 0.0) {
			exldata[6] = tempcapa;
		}
		exldata[7] = expect;

		// output file
		StringBuilder energy_mincostoutput = new StringBuilder();
		energy_mincostoutput.append("The order of the cost: Transfer cost, Receive cost, Save cost, total cost, node status").append("\r\n");

		//combine DG and storages
		int[] combine = new int[dataGens.length + storages.length];
		for (int i = 0; i < combine.length; i++) {
			if (i < dataGens.length) {
				combine[i] = dataGens[i];
			} else {
				combine[i] = storages[i - dataGens.length];
			}
		}

		// calculate total cost (0 + 1 + 2)
		int deadcounter = 0;
		for (int i : combine) {
			double totalcost = map.get(i).get(0) + map.get(i).get(1) + map.get(i).get(2);
			map.get(i).set(3, totalcost);
			energy_mincostoutput.append("Node "+ i + ": ["+ map.get(i).get(0) + ", " + map.get(i).get(1) + ", " + map.get(i).get(2) + ", " +
					map.get(i).get(3)).append("], closest node: ").append(tempclose.get(i).get(0));
			//System.out.println("Node "+ i + ": "+ map.get(i).get(0) + " " + map.get(i).get(1) + " " + map.get(i).get(2) + " " + map.get(i).get(3));
			// add the energy cost result to send back
			back.add(map.get(i).get(3));
			// calculate weather the node is dead
			if (i <= numberOfDG) { // source nodes
				//  current energy    +   energy cost to rely (transfer + receive) data to closest node  >   the minCapacity user identified
				if (map.get(i).get(3) + treeMap.get("(" + i + ", " + tempclose.get(i).get(0) + ")").getTCost() +
						treeMap.get("(" + i + ", " + tempclose.get(i).get(0) + ")").getRCost() >= treeMap.get("(" + tempclose.get(i).get(0) + ", " + i + ")").getEnergy()) {
					energy_mincostoutput.append(", status: DEAD!").append(";\r\n");
					deadcounter++;
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
					deadcounter++;
					deadNodes.add(i);
				} else {
					energy_mincostoutput.append(", status: Good").append(";\r\n");
				}
			}
		}
		exldata[4] = deadcounter;
		//System.out.println(sb.toString());

//		File fileNameout = new File("G:\\downloads\\work\\Dr. Bin\\May\\sensor\\out_files\\energy_mincostoutput_" + removed + ".txt");
//		BufferedWriter writer = new BufferedWriter(new FileWriter(fileNameout));
//		writer.write(energy_mincostoutput.toString());
//		writer.close();

		totaldataitems.clear();
		return back;
	}
	
	/* for different cost analysis */
	// receive and save cost
    double getRSCost(double l){
        final int K = 512; // k = 512B (from paper0)
        final double E_elec = 100 * Math.pow(10,-9); // E_elec = 100nJ/bit (from paper1)
        double Erx = 8 * E_elec * K; // Receiving energy consumption assume is same as saving
        //return Math.round(Erx*100)/100.0; // return the sum of sending and receiving energy
        return Erx*1000; // make it milli J now for better number visualization during calculation
    }
    
    // transfer cost -> ORIGINAL
    static double getTCost(double l) {
        final int K = 512; // k = 512B (from paper0)
        final double E_elec = 100 * Math.pow(10,-9); // E_elec = 100nJ/bit (from paper1)
        final double Epsilon_amp = 100 * Math.pow(10,-12); // Epsilon_amp = 100 pJ/bit/squared(m) (from paper1)
//      double Etx = E_elec * K + Epsilon_amp * K * l * l; // Transfer energy consumption
        double Etx = E_elec * K * 8 + Epsilon_amp * K * 8 * l * l; //
        //return Math.round(Etx*100)/100.0; // return the sum of sending and receiving energy
        return Math.round(Etx*1000*10000)/10000.0; // make it milli J now for better number visualization during calculation
    }
    
    static double getTCostOther(double l, double amp){
        final int K = 512; // k = 512B (from paper0)
        final double E_elec = 100 * Math.pow(10,-9); // E_elec = 100nJ/bit (from paper1)
        final double Epsilon_amp = 100 * amp * Math.pow(10,-12); // Epsilon_amp = 100 pJ/bit/squared(m) (from paper1)
        double Etx = E_elec * K * 8 + Epsilon_amp * K * 8 * l * l; //
        //return Math.round(Etx*100)/100.0; // return the sum of sending and receiving energy
        return Math.round(Etx*1000*10000)/10000.0; // make it milli J now for better number visualization during calculation
    }

    static double getRCostOther(double l, double amp){
        final int K = 512; // k = 512B (from paper0)
        final double E_elec = 100 * amp * Math.pow(10,-9); // E_elec = 100nJ/bit (from paper1)
        double Erx = 8 * E_elec * K; // Receiving energy consumption assume is same as saving
        //return Math.round(Erx*100)/100.0; // return the sum of sending and receiving energy
        return Erx*1000; // make it milli J now for better number visualization during calculation
    }

    static double getSCostOther(double l, double amp){
        final int K = 512; // k = 512B (from paper0)
        final double E_elec = 100 * amp * Math.pow(10,-9); // E_elec = 100nJ/bit (from paper1)
        double Erx = 8 * E_elec * K; // Receiving energy consumption assume is same as saving
        //return Math.round(Erx*100)/100.0; // return the sum of sending and receiving energy
        return Erx*1000; // make it milli J now for better number visualization during calculation
    }
    
    //for the original graphic
	void executeDepthFirstSearchAlg(double width, double height, Map<Integer, Set<Integer>> adjList) {
		s.clear();
		explored.clear();
		discovered.clear();
		parent.clear();
		List<Set<Integer>> connectedNodes = new ArrayList<Set<Integer>>();
		for(int node: adjList.keySet()) {
			Set<Integer> connectedNode = new LinkedHashSet<Integer>();
			recursiveDFS(node, connectedNode, adjList);
			
			if(!connectedNode.isEmpty()) {
				connectedNodes.add(connectedNode);
			}
		}
		
		if(connectedNodes.size() == 1) {
			//System.out.println("Graph is fully connected with one connected component.");
		} else {
			System.out.println("Graph is not fully connected");
		}


		//Draw first sensor network graph
		SensorNetworkGraph graph = new SensorNetworkGraph(dataGens);
		graph.setGraphWidth(width);
		graph.setGraphHeight(height);
		graph.setNodes(nodes);
		graph.setAdjList(adjList);
		graph.setPreferredSize(new Dimension(960, 800));
		Thread graphThread = new Thread(graph);
		graphThread.start();
		
	}
	
	//for the new graphic (delete nodes to test)
	void executeDepthFirstSearchAlgbi(double width, double height, Map<Integer, Set<Integer>> adjList) {
		//System.out.println("\nExecuting DFS Algorithm");
		//these have to be clear since they already have elements and values after running the algorithm
		s.clear();
		explored.clear();
		discovered.clear();
		parent.clear();

		//
		List<Set<Integer>> connectedNodes = new ArrayList<Set<Integer>>();
		for(int node: adjList.keySet()) {
			Set<Integer> connectedNode = new LinkedHashSet<Integer>();
			recursiveDFS(node, connectedNode, adjList);
			
			if(!connectedNode.isEmpty()) {
				connectedNodes.add(connectedNode);
			}
		}

		if(connectedNodes.size() == 1) {
			//ystem.out.println("Graph is fully connected with one connected component.");
		} else {
			biconnectcounter = biconnectcounter + 1;
			System.out.println("Graph is not fully connected");
		}
	}

	void recursiveDFS(int u, Set<Integer> connectedNode, Map<Integer, Set<Integer>> adjList) {
		
		if(!s.contains(u) && !explored.containsKey(u)) {
			s.add(u);
			discovered.put(u, true);
		}
			while(!s.isEmpty()) {
				if(!explored.containsKey(u)) {
					List<Integer> list = new ArrayList<Integer>(adjList.get(u));
					for(int v: list) {
						
						if(!discovered.containsKey(v)) {
							s.add(v);
							discovered.put(v, true);
							
							if(parent.get(v) == null) {
								parent.put(v, u);
							}
							recursiveDFS(v, connectedNode, adjList);
						} else if(list.get(list.size()-1) == v) {
							if( parent.containsKey(u)) {
								explored.put(u, true);
								s.removeElement(u);
								
								connectedNode.add(u);
								recursiveDFS(parent.get(u), connectedNode, adjList);
							}
						}
					}
				if(!explored.containsKey(u))
					explored.put(u, true);
					s.removeElement(u);
					connectedNode.add(u);
				}
			}
	}
	
	void populateNodes(int nodeCount, double width, double height) {
		// if user want to fix the graphic, enter a number in Random()
		Random random = new Random();
		
		for(int i = 1; i <= nodeCount; i++) {
			Axis axis = new Axis();
			int scale = (int) Math.pow(10, 1);
			double xAxis =(0 + random.nextDouble() * (width - 0));
			double yAxis = 0 + random.nextDouble() * (height - 0);
			int capa = random.nextInt(10) + 1;
			
			xAxis = (double)Math.floor(xAxis * scale) / scale;
			yAxis = (double)Math.floor(yAxis * scale) / scale;
			
			
			axis.setxAxis(xAxis);
			axis.setyAxis(yAxis);
			axis.setcapa(capa); //each nodes energy capacity
			
			nodes.put(i, axis);	
		}
	}
	
	static void readfileNodes(File file) throws IOException {
		// if user want to fix the graphic, enter a number in Random()
		Random random = new Random();
		// original 1312
		Scanner scan = new Scanner(System.in);
		System.out.println("Please enter the energy capacity:");
		minCapacity = scan.nextInt(); //max energy
		
		FileReader fileReader = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line;
		
		while ((line = bufferedReader.readLine()) != null) {
			Axis axis = new Axis();
			String[] words = line.split("	");
			int scale = (int) Math.pow(10, 1);
			double xAxis = Double.parseDouble(words[1]);
			double yAxis =Double.parseDouble(words[2]);

			//xAxis = (double)Math.floor(xAxis * scale) / scale;
			//yAxis = (double)Math.floor(yAxis * scale) / scale;
			
			axis.setxAxis(xAxis);
			axis.setyAxis(yAxis);
			axis.setcapa(minCapacity); //each nodes energy capacity
			
			nodes.put(Integer.parseInt(words[0]) + 1, axis);
		}
		
		fileReader.close();

	}
	
	void populateAdjacencyList(int nodeCount, int tr, Map<Integer, Set<Integer>> adjList) {
		for(int i = 1; i <= nodeCount; i++) {
			adjList.put(i, new HashSet<Integer>());
		}
		
		for(int node1: nodes.keySet()) {
			Axis axis1 = nodes.get(node1);
			for(int node2: nodes.keySet()) {
				Axis axis2 = nodes.get(node2);
				
				if(node1 == node2) {
					continue;
				}
				double xAxis1 = axis1.getxAxis();
				double yAxis1 = axis1.getyAxis();
					
				double xAxis2 = axis2.getxAxis();
				double yAxis2 = axis2.getyAxis();
				
				double distance =  Math.sqrt(((xAxis1-xAxis2)*(xAxis1-xAxis2)) + ((yAxis1-yAxis2)*(yAxis1-yAxis2)));
				
				double energy = minCapacity;
				
				if(distance <= tr) {
					linkstest.put(new String("(" + node2 + ", " + node1 + ")"), new Link(new Edge(node2, node1, 0), distance, getRSCost(distance), getTCost(distance), getRSCost(distance), energy));
					if (!close.containsKey(node2)) {
						List<Integer> list = new ArrayList<>();
						list.add(node1);
						list.add((int) distance);
						close.put(node2, list);
					} else {
						if (close.get(node2).get(1) > distance) {
							close.get(node2).set(0, node1);
							close.get(node2).set(1, (int) distance);
						}
					}
					Set<Integer> tempList = adjList.get(node1);
					tempList.add(node2);
					adjList.put(node1, tempList);
						
					tempList = adjList.get(node2);
					tempList.add(node1);
					adjList.put(node2, tempList);
					if (node1 > node2){
                        links.put(new String("(" + node2 + ", " + node1 + ")"), new Link(new Edge(node2, node1, 1), distance, getRSCost(distance), getTCost(distance), getRSCost(distance), energy));
					} else {
                    	links.put(new String("(" + node1 + ", " + node2 + ")"), new Link(new Edge(node1, node2, 1), distance, getRSCost(distance), getTCost(distance), getRSCost(distance), energy));
					}
					
		
				}
			}
		}
	}
	
	//similar as populateAdjacencyList but the number of source nodes are different
	void checkbiconnect(int removeconter,int nodeCount, int tr, Map<Integer, Set<Integer>> adjList) {
		int j = 1;
		for(int i=1; i < nodeCount; i++) {
			if (j != removeconter) {
				adjList.put(j, new HashSet<Integer>());
				j++;
			} else {
				j++;
				i=i-1;
			}
		}
		
		for(int node1: nodes2.keySet()) {
			Axis axis1 = nodes2.get(node1);
			for(int node2: nodes2.keySet()) {
				
				Axis axis2 = nodes2.get(node2);
				
				if(node1 == node2) {
					continue;
				}
				double xAxis1 = axis1.getxAxis();
				double yAxis1 = axis1.getyAxis();
					
				double xAxis2 = axis2.getxAxis();
				double yAxis2 = axis2.getyAxis();
				
				double distance =  Math.sqrt(((xAxis1-xAxis2)*(xAxis1-xAxis2)) + ((yAxis1-yAxis2)*(yAxis1-yAxis2)));
				
				double energy = minCapacity;
				
				if(distance <= tr) {
					Set<Integer> tempList = adjList.get(node1);
					tempList.add(node2);
					adjList.put(node1, tempList);
						
					tempList = adjList.get(node2);
					tempList.add(node1);
					adjList.put(node2, tempList);
					if (node1 > node2){
						links2.put(new String("(" + node2 + ", " + node1 + ")"), new Link(new Edge(node2, node1, 1), distance, getRSCost(distance), getTCost(distance), getRSCost(distance), energy));
                    } else {
                    	links2.put(new String("(" + node1 + ", " + node2 + ")"), new Link(new Edge(node1, node2, 1), distance, getRSCost(distance), getTCost(distance), getRSCost(distance), energy));
                    }
				}
			}
		}
	}
}
