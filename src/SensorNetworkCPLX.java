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

import ilog.concert.*;
import ilog.cplex.*;
import ilog.cplex.IloCplex.UnknownObjectException;

	/**
	 * This program will generate a sensor network graph
	 * and observe the graph's status (cost, data item's amount received / send / saved, dead or live).
	 *
	 * Different constraints and algorithm used to complete the task depends on users' input.
	 *
	 * @author  Shang-Lin Hsu and Yu-ning Yu
	 * @since   2018-09-09
	 */

public class SensorNetworkCPLX {

    static Random rand = new Random();
    static XSSFWorkbook energyworkbook = new XSSFWorkbook();
    static XSSFSheet energysheet = energyworkbook.createSheet("EnergyCostData");
    static Map<Integer, Axis> nodes = new LinkedHashMap<Integer, Axis>();
    static Map<Integer, Axis> nodes2 = new LinkedHashMap<Integer, Axis>();
	Map<Integer, Boolean> discovered = new HashMap<Integer, Boolean>();
	Map<Integer, Boolean> explored = new HashMap<Integer, Boolean>();
	Map<Integer, Integer> parent = new HashMap<Integer, Integer>();
	Stack<Integer> s = new Stack<Integer>();
	static Map<String, Link> links = new HashMap<String, Link>();
	static Map<String, Link> links2 = new HashMap<String, Link>();
	static Map<String, Link> linkstest = new HashMap<String, Link>();
	static HashMap<Integer, List<Integer>> close = new HashMap<>();
	static HashMap<Integer, Double> totaldataitems = new HashMap<>();
	
    static int minCapacity;
    static int biconnectcounter = 1;
    static int[] dataGens;
    static int[] storageNodes;
    static int numberOfDG;
    static int numberOfDataItemsPerDG;
    static int numberOfStoragePerSN;
    static int numberOfNodes;

	public static void main(String[] args) throws IOException, IloException {

		Scanner scan = new Scanner(System.in);
		System.out.println("The width (meters) is set to:");
		double width = scan.nextDouble();
        //double width = 1000.0;
        //System.out.println(width);
        
		System.out.println("The height (meters) is set to:");
		double height = scan.nextDouble();
        //double height = 1000.0;
        //System.out.println(height);
        
		System.out.println("Number of nodes is set to:");
		numberOfNodes = scan.nextInt();
        //numberOfNodes = 100;
        
		System.out.println("Transmission range (in meters) is set to:");
		int transmissionRange = scan.nextInt();
        //int transmissionRange = 250;
        //System.out.println(transmissionRange);
        
		System.out.println("Data Generators' amount is set to:");
		numberOfDG = scan.nextInt();
        //numberOfDG = 10;

		dataGens = new int[numberOfDG];
		System.out.println("Assuming the first " + numberOfDG + " nodes are DGs\n");
		for (int i=1; i <= dataGens.length; i++) {
            dataGens[i-1] = i;
        }

        storageNodes = new int[numberOfNodes-numberOfDG];
        for (int i=0; i < storageNodes.length; i++){
            storageNodes[i] = i + 1 + numberOfDG;
        }

		System.out.println("Data items per DG to send out is set to:");
		numberOfDataItemsPerDG = scan.nextInt();
        //numberOfDataItemsPerDG = 100;

		System.out.println("Data storage per storage node is set to: ");
		numberOfStoragePerSN = scan.nextInt();
		// CHANGE
		//numberOfStoragePerSN = 25;

		System.out.println("Please enter the initial energy capacity (milli J):");
		System.out.println("**Note: The energy will be a random amount between your input and your input + 1000.");
		minCapacity = scan.nextInt(); //max energy

		int numberOfSupDem = numberOfDataItemsPerDG * numberOfDG;
		int numberOfstorage = numberOfStoragePerSN * (numberOfNodes-numberOfDG);
        System.out.println("The total number of data items overloading: " + numberOfSupDem);
        System.out.println("The total number of data items storage: " + numberOfstorage);

        if (numberOfSupDem > numberOfstorage) {
        	System.out.println("No enough storage");
        	return;
        } else {
			System.out.println("Starting...");
		}

		SensorNetworkCPLX sensor = new SensorNetworkCPLX();

		// generate the graph from file
//		File myfile = new File("inputdata.txt");
//		readfileNodes(myfile);

		// random generate the graph
		populateNodes(numberOfNodes, width, height);
		
		System.out.println("\nNode List:");
		for(int key : sensor.nodes.keySet()) {
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

		Map<Integer, Double> sortMap = new HashMap<>();

		// -------------- set to random energy base on user input ---------------
		for (Map.Entry<Integer, Set<Integer>> node : adjacencyList1.entrySet()) {
			int temp = rand.nextInt(1000) + minCapacity;
			int i = node.getKey();
			if (i <= 10) {
				for (Link link : treeMap.values()) {
					if (i == link.getEdge().getHead()) {
						link.setEnergy(minCapacity + 1000);
						sortMap.put(link.getEdge().getHead(), (double) minCapacity + 1000);
					}
				}
			} else {
				for (Link link : treeMap.values()) {
					if (i == link.getEdge().getHead()) {
						link.setEnergy(temp);
						sortMap.put(link.getEdge().getHead(), (double) temp);
					}
				}
			}
		}

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
//		File EnergyFile = new File("Edge_cost_Original.txt");
//		BufferedWriter writer_energy = new BufferedWriter(new FileWriter(EnergyFile));
//		writer_energy.write(totalenergycost.toString());
//		writer_energy.close();

		// file for energy sorting

		PriorityQueue<Map.Entry<Integer, Double>> copyEnergy = new PriorityQueue<>((a,b) -> b.getValue().intValue() - a.getValue().intValue());
		for (Map.Entry<Integer, Double> entry : sortMap.entrySet()) {
			copyEnergy.offer(entry);
		}
		while (!copyEnergy.isEmpty()) {
			copyEnergy.poll();
		}

		// create first row
		int rowcounter = 0;
		Row energyrow = energysheet.createRow(rowcounter++);

		// name for cols
		String[] row = new String[]{"RemoveNode", "C_{V-{i}}", "C_V", "dataitems", "CPLEX_energycost", "CPLEX_Obj", "Network_base_energycost", "Network_base_data_resilience",
				"MinCostFlow_energycost", "MinCostFlow_resilience", "CPLEX_Dead", "Algo_Dead", "MinCost_Dead", "Base_energy_capacity", "Base_storage_capacity", "Gap_tolerance(%)"};
		// write
		for (int i = 0; i < row.length; i++) {
			Cell energycell = energyrow.createCell(i);
			energycell.setCellValue(row[i]);
		}

		System.out.println("Select the data you want to generate: ");
		System.out.println("0 = observe cplex gap tolerance's difference (will require user to input gap tolerance 5 times)");
		System.out.println("1 = data priority (Linear Programming): gradually increase data generator's amount to observe data priority solutions");
		System.out.println("2 = data resilience (Quadratic Programming): change storage (every run + 25), fix data item's amount");
		System.out.println("3 = data resilience (Quadratic Programming): fix storage, change data item's amount (every time + DG's amount * 5)");
		System.out.println("4 = data resilience (Quadratic Programming): change storage (user defines common difference)");
		System.out.println("5 = data resilience (Quadratic Programming): change data item's amount (user defines common difference **Difference = DG's amount * user defined value)");
		System.out.println("6 = data resilience (Quadratic Programming): run twice different storage, decrease energy gradually to observe fault tolerance");

		// select method
		int runMethod = scan.nextInt();
		if (runMethod == 0) {
			System.out.println("This program will run 5 times to generate 5 different gap tolerance.");
			for (int i = 0; i < 5; i++) {
				System.out.println("Input the gap tolerance (Percentage e.g: 0.8% = 0.008, 1% = 0.01):");
				double gapTolerance = scan.nextDouble();
				//MinCost initiate
				IloCplex cpMinFirst = new IloCplex(); // for max function to find the min off load electricity
				IloCplex cpMinObject = new IloCplex(); // for calculating optimized path
				Map<String, IloNumVar> minFirstname = varName(cpMinFirst, treeMap, dataGens, storageNodes);
				Map<String, IloNumVar> minObjtname = varName(cpMinObject, treeMap, dataGens, storageNodes);
				// Cplex initiate
				IloCplex CpFirst = new IloCplex(); // for max function to find the min off load electricity
				IloCplex CpObject = new IloCplex(); // for calculating optimized path
				Map<String, IloNumVar> Firstname = varName(CpFirst, treeMap, dataGens, storageNodes);
				Map<String, IloNumVar> Objtname = varName(CpObject, treeMap, dataGens, storageNodes);

				double[] notremove = new double[row.length];
				ArrayList<Integer> deadNodes = new ArrayList<>();
				ArrayList<Double> totalenergy = new ArrayList<>();
				ArrayList<Double> dumtotalenergy = new ArrayList<>();
				Map<String, Double> cplexresult = new HashMap<>(); // String and the cost of each edge

				double[] minnotremove = new double[row.length];
				ArrayList<Integer> mindeadNodes = new ArrayList<>();
				ArrayList<Double> mintotalenergy = new ArrayList<>();
				Map<String, Double> mincplexresult = new HashMap<>(); // String and the cost of each edge

				boolean[] sendOrNot = new boolean[numberOfNodes + 1];
				int tempData = 0;

				// sort the energy for the mincost usage
				PriorityQueue<Map.Entry<Integer, Double>> sortEnergy = new PriorityQueue<>((a, b) -> b.getValue().intValue() - a.getValue().intValue());
				for (Map.Entry<Integer, Double> entry : sortMap.entrySet()) {
					sortEnergy.offer(entry);
				}

				Arrays.fill(sendOrNot, false);
				// if the energy is high, send it
				while (!sortEnergy.isEmpty() && tempData < numberOfDG * numberOfDataItemsPerDG) {
					if (sortEnergy.peek().getKey() > numberOfDG) {
						int node = sortEnergy.peek().getKey();
						tempData += numberOfStoragePerSN;
						sendOrNot[node] = true;
					}
					sortEnergy.poll();
				}

				// CPLEX
				calculateLp(treeMap, adjacencyList1, close, Firstname, Objtname, CpFirst, CpObject, storageNodes, notremove, deadNodes, totalenergy, dumtotalenergy, "original", "original", cplexresult, gapTolerance);
				// cplex energy use for 2,4, and 8
				double Cplexenergy = cplexCalculation(treeMap, adjacencyList1, cplexresult, 0, gapTolerance);

				// MInCost
				claculateMinCost(treeMap, adjacencyList1, close, minFirstname, minObjtname, cpMinFirst, cpMinObject, storageNodes, minnotremove, mintotalenergy, "original", mincplexresult, sendOrNot);
				double minRe = resilienceMinCost(treeMap, adjacencyList1, mincplexresult, mindeadNodes);

				// Algo
				double[] AlgorithmData = new double[2]; // return the data resiliance level and the energy cost.
				PriorityQueue<Integer> deadAlgo = new PriorityQueue<>();
				// run Algorithm
				boolean algOk = Algorithm(AlgorithmData, treeMap, adjacencyList1, deadAlgo);

				// create new row to store nre data
				energyrow = energysheet.createRow(rowcounter++);

				// add the first row (not removing node)
				Cell cell = energyrow.createCell(0);
				cell.setCellValue(notremove[0]);

				// 1 and 2 have the same energy since they are the all energy before removing nodes
				// as well as 4
				Cell cell1 = energyrow.createCell(1);
				cell1.setCellValue(minnotremove[1]);

				Cell cell2 = energyrow.createCell(2);
				cell2.setCellValue(minnotremove[1]);

				Cell cell3 = energyrow.createCell(3);
				cell3.setCellValue(minnotremove[3]);

				Cell cell4 = energyrow.createCell(4);
				cell4.setCellValue(Cplexenergy);

				Cell cell5 = energyrow.createCell(5);
				cell5.setCellValue(notremove[1]);
				Cell cell6 = energyrow.createCell(6);
				cell6.setCellValue(AlgorithmData[1]);
				Cell cell7 = energyrow.createCell(7);
				cell7.setCellValue(AlgorithmData[0]);

				Cell minCostEnrgy = energyrow.createCell(8);
				minCostEnrgy.setCellValue(minnotremove[1]);

				Cell minCostResilience = energyrow.createCell(9);
				minCostResilience.setCellValue(minRe);

				Cell baseEnergy = energyrow.createCell(13);
				baseEnergy.setCellValue(minCapacity);

				Cell baseStorage = energyrow.createCell(14);
				baseStorage.setCellValue(numberOfStoragePerSN);

				Cell BaseGap = energyrow.createCell(15);
				BaseGap.setCellValue(gapTolerance);

				System.out.println();
				if (!algOk) {
					System.out.println("have problem: network base");
				}
				if (!deadAlgo.isEmpty()) {
					ArrayList<Integer> deadAlgoNodes = new ArrayList<>();
					while (!deadAlgo.isEmpty()) {
						deadAlgoNodes.add(deadAlgo.poll());
					}
					Cell AlgoDead = energyrow.createCell(11);
					AlgoDead.setCellValue(Arrays.toString(deadAlgoNodes.toArray()));
				}
				if (deadNodes.size() > 0) {
					Cell CPLEXDead = energyrow.createCell(10);
					CPLEXDead.setCellValue(Arrays.toString(deadNodes.toArray()));
				}

				if (mindeadNodes.size() > 0) {
					Cell MinCostDead = energyrow.createCell(12);
					MinCostDead.setCellValue(Arrays.toString(mindeadNodes.toArray()));
				}
			}
		}  else if (runMethod == 1) {
			System.out.println("Running priority formulation, please input the initial energy:");
			double newEnergy = scan.nextDouble();
			int tempDG = 0;
			int totoalpriority = 0;

			/* initiate random priority */
			ArrayList<Integer> DGpriority = new ArrayList<>();

			//set all energy to same level (copy the original data but change the energy)
			Map<String, Link> copyTree = new TreeMap<>();
			for (Map.Entry<String, Link> pair : treeMap.entrySet()) {
				Link link = new Link(new Edge(pair.getValue().getEdge().getTail(), pair.getValue().getEdge().getHead(), 0),
						pair.getValue().getDistance(), pair.getValue().getRCost(), pair.getValue().getTCost(), pair.getValue().getSCost(),
						newEnergy); // only energy is different
				copyTree.put(pair.getKey(), link);
			}

			/* fix the column's label*/
			// name for cols
			String[] newrow = new String[]{"Number_Of_DG", "Total_priority", "Min_DG_priority", "Preserved_priority", "Data_item_saved", "Energy_cost", "Remaining_energy", "Priority_list", "Dead_node_list",
					"Number_of_data_items", "Initial_energy", "Initial_storage", "", "", "", ""};
			// write
			for (int i = 0; i < newrow.length; i++) {
				Cell title = energyrow.createCell(i);
				title.setCellValue(newrow[i]);
			}

			for (int i = 0; i < 4; i++) {
				/* the privious priority will remain the same but add in new priority when new DGs are added*/
				for (int j = 0; j < numberOfDG - tempDG; j++) {
					int priority = rand.nextInt(100);
					if (priority == 0) {
						priority++;
					}
					DGpriority.add(priority);
					totoalpriority += priority * numberOfDataItemsPerDG;
				}

				// update the tempDG (old)
				tempDG = numberOfDG;

				// Priority initiate
				IloCplex cpMinFirst = new IloCplex(); // for max function to find the min off load electricity
				IloCplex cpMinObject = new IloCplex(); // for calculating optimized path
				Map<String, IloNumVar> minFirstname = varName(cpMinFirst, treeMap, dataGens, storageNodes);
				Map<String, IloNumVar> minObjtname = priorityName(cpMinObject, treeMap, dataGens, storageNodes);

				// method 2 Priority initiate
				IloCplex cpMinFirstMaxFlow = new IloCplex(); // for max function to find the min off load electricity
				IloCplex cpMinObjectMaxFlow = new IloCplex(); // for calculating optimized path
				Map<String, IloNumVar> minFirstnameMaxFlow = varName(cpMinFirst, treeMap, dataGens, storageNodes);
				Map<String, IloNumVar> minObjtnameMaxFlow = varName(cpMinObject, treeMap, dataGens, storageNodes);

				double[] minnotremove = new double[row.length];
				ArrayList<Integer> mindeadNodes = new ArrayList<>();
				ArrayList<Double> mintotalenergy = new ArrayList<>();
				ArrayList<Double> energycost = new ArrayList<>();
				Map<String, Double> mincplexresult = new HashMap<>(); // String and the cost of each edge

				// method 2 Priority initiate
				double[] minnotremoveMaxFlow = new double[row.length];
				ArrayList<Integer> mindeadNodesMaxFlow = new ArrayList<>();
				ArrayList<Double> mintotalenergyMaxFlow = new ArrayList<>();
				ArrayList<Double> energycostMaxFlow = new ArrayList<>();
				Map<String, Double> mincplexresultMaxFlow = new HashMap<>(); // String and the cost of each edge

				// FirstMethod
				calculatePri(copyTree, adjacencyList1, close, minFirstname, minObjtname, cpMinFirst, cpMinObject, storageNodes, minnotremove, mintotalenergy, "_" + numberOfDG, mincplexresult, DGpriority);
				double minRe = calculatePriority(copyTree, adjacencyList1, mincplexresult, mindeadNodes, energycost, false);

				// MaxFlow method
				calculatePriMaxFlow(copyTree, adjacencyList1, close, minFirstnameMaxFlow, minObjtnameMaxFlow, cpMinFirstMaxFlow, cpMinObjectMaxFlow, storageNodes, minnotremoveMaxFlow, mintotalenergyMaxFlow, "_" + numberOfDG, mincplexresultMaxFlow, DGpriority);
				double minReMaxFlow = calculatePriority(copyTree, adjacencyList1, mincplexresultMaxFlow, mindeadNodesMaxFlow, energycostMaxFlow, true);

				// create new row to store nre data
				energyrow = energysheet.createRow(rowcounter++);

				// 1 and 2 have the same energy since they are the all energy before removing nodes
				// as well as 4
				Cell numberOgGenerator = energyrow.createCell(0);
				numberOgGenerator.setCellValue(numberOfDG); // total priority

				Cell cell2 = energyrow.createCell(1);
				cell2.setCellValue(totoalpriority); // total priority

				Cell cell1 = energyrow.createCell(2);
				cell1.setCellValue(minnotremove[1]); // priority objective (in generator)

				Cell preservedPriority = energyrow.createCell(3);
				preservedPriority.setCellValue(totoalpriority - minnotremove[1]); // preserved priority

				Cell savedDate = energyrow.createCell(4);
				savedDate.setCellValue(minRe); // data item saved

				Cell consumption = energyrow.createCell(5);
				consumption.setCellValue(energycost.get(0) - energycost.get(1)); // energy

				Cell remainEnergy = energyrow.createCell(6);
				remainEnergy.setCellValue(energycost.get(1)); // remain energy

				Cell prioritylist = energyrow.createCell(7);
				prioritylist.setCellValue(DGpriority.toString()); // priority list

				Cell totalData = energyrow.createCell(9);
				totalData.setCellValue(numberOfDG * numberOfDataItemsPerDG); // initial energy level

				Cell baseEnergy = energyrow.createCell(10);
				baseEnergy.setCellValue(newEnergy); // initial energy level

				Cell baseStorage = energyrow.createCell(11);
				baseStorage.setCellValue(numberOfStoragePerSN); // initial storage level

				System.out.println();
				//dead node count
				if (mindeadNodes.size() > 0) {
					Cell MinCostDead = energyrow.createCell(8);
					MinCostDead.setCellValue(Arrays.toString(mindeadNodes.toArray()));
				}

				// Max Flow part
				// create new row to store nre data
				energyrow = energysheet.createRow(rowcounter++);

				// 1 and 2 have the same energy since they are the all energy before removing nodes
				// as well as 4
				Cell numberOgGeneratorMaxFlow = energyrow.createCell(0);
				numberOgGeneratorMaxFlow.setCellValue(numberOfDG); // total priority

				Cell cell2MaxFlow = energyrow.createCell(1);
				cell2MaxFlow.setCellValue(totoalpriority); // total priority

				Cell cell1MaxFlow = energyrow.createCell(3);
				cell1MaxFlow.setCellValue(minnotremoveMaxFlow[1]); // priority objective (in generator)

				Cell preservedPriorityMaxFlow = energyrow.createCell(2);
				preservedPriorityMaxFlow.setCellValue(totoalpriority - minnotremoveMaxFlow[1]); // preserved priority

				Cell savedDateMaxFlow = energyrow.createCell(4);
				savedDateMaxFlow.setCellValue(minReMaxFlow); // data item saved

				Cell consumptionMaxFlow = energyrow.createCell(5);
				consumptionMaxFlow.setCellValue(energycostMaxFlow.get(0) - energycostMaxFlow.get(1)); // energy

				Cell remainEnergyMaxFlow = energyrow.createCell(6);
				remainEnergyMaxFlow.setCellValue(energycostMaxFlow.get(1)); // remain energy

				Cell prioritylistMaxFlow = energyrow.createCell(7);
				prioritylistMaxFlow.setCellValue(DGpriority.toString()); // priority list

				Cell totalDataMaxFlow = energyrow.createCell(9);
				totalDataMaxFlow.setCellValue(numberOfDG * numberOfDataItemsPerDG); // initial energy level

				Cell baseEnergyMaxFlow = energyrow.createCell(10);
				baseEnergyMaxFlow.setCellValue(newEnergy); // initial energy level

				Cell baseStorageMaxFlow = energyrow.createCell(11);
				baseStorageMaxFlow.setCellValue(numberOfStoragePerSN); // initial storage level

				System.out.println();
				//dead node count
				if (mindeadNodesMaxFlow.size() > 0) {
					Cell MinCostDeadMaxFlow = energyrow.createCell(8);
					MinCostDeadMaxFlow.setCellValue(Arrays.toString(mindeadNodesMaxFlow.toArray()));
				}

				// increase the numbers of DG every run
				numberOfDG += 10;
				/* regenerate new DGs*/
				dataGens = new int[numberOfDG];
				System.out.println("Assuming the first " + numberOfDG + " nodes are DGs\n");
				for (int j = 1; j <= dataGens.length; j++) {
					dataGens[j - 1] = j;
				}

				storageNodes = new int[numberOfNodes - numberOfDG];
				for (int j = 0; j < storageNodes.length; j++) {
					storageNodes[j] = j + 1 + numberOfDG;
				}
			}
		} else if (runMethod == 2 || runMethod == 3) {
			System.out.println("Input the gap tolerance (Percentage e.g: 0.8% = 0.008, 1% = 0.01):");
			double gapTolerance = scan.nextDouble();
			for (int i = 0; i < 4; i++) {
				if (runMethod == 2) {
					System.out.println("Now running storage capacity: " + numberOfStoragePerSN);
				} else {
					System.out.println("Now running total data items: " + numberOfDataItemsPerDG);
				}
				//MinCost initiate
				IloCplex cpMinFirst = new IloCplex(); // for max function to find the min off load electricity
				IloCplex cpMinObject = new IloCplex(); // for calculating optimized path
				Map<String, IloNumVar> minFirstname = varName(cpMinFirst, treeMap, dataGens, storageNodes);
				Map<String, IloNumVar> minObjtname = varName(cpMinObject, treeMap, dataGens, storageNodes);
				// Cplex initiate
				IloCplex CpFirst = new IloCplex(); // for max function to find the min off load electricity
				IloCplex CpObject = new IloCplex(); // for calculating optimized path
				Map<String, IloNumVar> Firstname = varName(CpFirst, treeMap, dataGens, storageNodes);
				Map<String, IloNumVar> Objtname = varName(CpObject, treeMap, dataGens, storageNodes);

				double[] notremove = new double[row.length];
				ArrayList<Integer> deadNodes = new ArrayList<>();
				ArrayList<Double> totalenergy = new ArrayList<>();
				ArrayList<Double> dumtotalenergy = new ArrayList<>();
				Map<String, Double> cplexresult = new HashMap<>(); // String and the cost of each edge

				double[] minnotremove = new double[row.length];
				ArrayList<Integer> mindeadNodes = new ArrayList<>();
				ArrayList<Double> mintotalenergy = new ArrayList<>();
				Map<String, Double> mincplexresult = new HashMap<>(); // String and the cost of each edge

				boolean[] sendOrNot = new boolean[numberOfNodes + 1];
				int tempData = 0;

				// sort the energy for the mincost usage
				PriorityQueue<Map.Entry<Integer, Double>> sortEnergy = new PriorityQueue<>((a, b) -> b.getValue().intValue() - a.getValue().intValue());
				for (Map.Entry<Integer, Double> entry : sortMap.entrySet()) {
					sortEnergy.offer(entry);
				}

				Arrays.fill(sendOrNot, false);
				// if the energy is high, send it
				while (!sortEnergy.isEmpty() && tempData < numberOfDG * numberOfDataItemsPerDG) {
					if (sortEnergy.peek().getKey() > numberOfDG) {
						int node = sortEnergy.peek().getKey();
						tempData += numberOfStoragePerSN;
						sendOrNot[node] = true;
					}
					sortEnergy.poll();
				}

				// CPLEX
				calculateLp(treeMap, adjacencyList1, close, Firstname, Objtname, CpFirst, CpObject, storageNodes, notremove, deadNodes, totalenergy, dumtotalenergy, "original", "original", cplexresult, gapTolerance);
				// cplex energy use for 2,4, and 8
				double Cplexenergy = cplexCalculation(treeMap, adjacencyList1, cplexresult, 0, gapTolerance);

				// MInCost
				claculateMinCost(treeMap, adjacencyList1, close, minFirstname, minObjtname, cpMinFirst, cpMinObject, storageNodes, minnotremove, mintotalenergy, "original", mincplexresult, sendOrNot);
				double minRe = resilienceMinCost(treeMap, adjacencyList1, mincplexresult, mindeadNodes);

				// Algo
				double[] AlgorithmData = new double[2]; // return the data resiliance level and the energy cost.
				PriorityQueue<Integer> deadAlgo = new PriorityQueue<>();
				// run Algorithm
				boolean algOk = Algorithm(AlgorithmData, treeMap, adjacencyList1, deadAlgo);

				// create new row to store nre data
				energyrow = energysheet.createRow(rowcounter++);

				// add the first row (not removing node)
				Cell cell = energyrow.createCell(0);
				cell.setCellValue(notremove[0]);

				// 1 and 2 have the same energy since they are the all energy before removing nodes
				// as well as 4
				Cell cell1 = energyrow.createCell(1);
				cell1.setCellValue(minnotremove[1]);

				Cell cell2 = energyrow.createCell(2);
				cell2.setCellValue(minnotremove[1]);

				Cell cell3 = energyrow.createCell(3);
				cell3.setCellValue(minnotremove[3]);

				Cell cell4 = energyrow.createCell(4);
				cell4.setCellValue(Cplexenergy);

				Cell cell5 = energyrow.createCell(5);
				cell5.setCellValue(notremove[1]);
				Cell cell6 = energyrow.createCell(6);
				cell6.setCellValue(AlgorithmData[1]);
				Cell cell7 = energyrow.createCell(7);
				cell7.setCellValue(AlgorithmData[0]);

				Cell minCostEnrgy = energyrow.createCell(8);
				minCostEnrgy.setCellValue(minnotremove[1]);

				Cell minCostResilience = energyrow.createCell(9);
				minCostResilience.setCellValue(minRe);

				Cell baseEnergy = energyrow.createCell(13);
				baseEnergy.setCellValue(minCapacity);

				Cell baseStorage = energyrow.createCell(14);
				baseStorage.setCellValue(numberOfStoragePerSN);

				Cell BaseGap = energyrow.createCell(15);
				BaseGap.setCellValue(gapTolerance);

				System.out.println();
				if (!algOk) {
					System.out.println("have problem: network base");
				}
				if (!deadAlgo.isEmpty()) {
					ArrayList<Integer> deadAlgoNodes = new ArrayList<>();
					while (!deadAlgo.isEmpty()) {
						deadAlgoNodes.add(deadAlgo.poll());
					}
					Cell AlgoDead = energyrow.createCell(11);
					AlgoDead.setCellValue(Arrays.toString(deadAlgoNodes.toArray()));
				}
				if (deadNodes.size() > 0) {
					Cell CPLEXDead = energyrow.createCell(10);
					CPLEXDead.setCellValue(Arrays.toString(deadNodes.toArray()));
				}

				if (mindeadNodes.size() > 0) {
					Cell MinCostDead = energyrow.createCell(12);
					MinCostDead.setCellValue(Arrays.toString(mindeadNodes.toArray()));
				}

				if (runMethod == 2) {
					numberOfStoragePerSN += 25;
				} else {
					numberOfDataItemsPerDG += 5;
				}
			}
		} else if (runMethod == 4) {
			System.out.println("the program will run 4 times. Please set the common difference for each run:");
			int commonDifference = scan.nextInt();
			for (int i = 0; i < 4; i++) {
				System.out.println("Now running storage capacity: " + numberOfStoragePerSN);

				//MinCost initiate
				IloCplex cpMinFirst = new IloCplex(); // for max function to find the min off load electricity
				IloCplex cpMinObject = new IloCplex(); // for calculating optimized path
				Map<String, IloNumVar> minFirstname = varName(cpMinFirst, treeMap, dataGens, storageNodes);
				Map<String, IloNumVar> minObjtname = varName(cpMinObject, treeMap, dataGens, storageNodes);
				// Cplex initiate
				IloCplex CpFirst = new IloCplex(); // for max function to find the min off load electricity
				IloCplex CpObject = new IloCplex(); // for calculating optimized path
				Map<String, IloNumVar> Firstname = varName(CpFirst, treeMap, dataGens, storageNodes);
				Map<String, IloNumVar> Objtname = varName(CpObject, treeMap, dataGens, storageNodes);

				double[] notremove = new double[row.length];
				ArrayList<Integer> deadNodes = new ArrayList<>();
				ArrayList<Double> totalenergy = new ArrayList<>();
				ArrayList<Double> dumtotalenergy = new ArrayList<>();
				Map<String, Double> cplexresult = new HashMap<>(); // String and the cost of each edge

				double[] minnotremove = new double[row.length];
				ArrayList<Integer> mindeadNodes = new ArrayList<>();
				ArrayList<Double> mintotalenergy = new ArrayList<>();
				Map<String, Double> mincplexresult = new HashMap<>(); // String and the cost of each edge

				boolean[] sendOrNot = new boolean[numberOfNodes + 1];
				int tempData = 0;

				// sort the energy for the mincost usage
				PriorityQueue<Map.Entry<Integer, Double>> sortEnergy = new PriorityQueue<>((a, b) -> b.getValue().intValue() - a.getValue().intValue());
				for (Map.Entry<Integer, Double> entry : sortMap.entrySet()) {
					sortEnergy.offer(entry);
				}

				Arrays.fill(sendOrNot, false);
				// if the energy is high, send it
				while (!sortEnergy.isEmpty() && tempData < numberOfDG * numberOfDataItemsPerDG) {
					if (sortEnergy.peek().getKey() > numberOfDG) {
						int node = sortEnergy.peek().getKey();
						tempData += numberOfStoragePerSN;
						sendOrNot[node] = true;
					}
					sortEnergy.poll();
				}

				// CPLEX fix gap tolerance 0.03
				calculateLp(treeMap, adjacencyList1, close, Firstname, Objtname, CpFirst, CpObject, storageNodes, notremove, deadNodes, totalenergy, dumtotalenergy, "original", "original", cplexresult, 0.03);
				// cplex energy use for 2,4, and 8
				double Cplexenergy = cplexCalculation(treeMap, adjacencyList1, cplexresult, 0, 0);

				// MInCost
				claculateMinCost(treeMap, adjacencyList1, close, minFirstname, minObjtname, cpMinFirst, cpMinObject, storageNodes, minnotremove, mintotalenergy, "original", mincplexresult, sendOrNot);
				double minRe = resilienceMinCost(treeMap, adjacencyList1, mincplexresult, mindeadNodes);

				// Algo
				double[] AlgorithmData = new double[2]; // return the data resiliance level and the energy cost.
				PriorityQueue<Integer> deadAlgo = new PriorityQueue<>();
				// run Algorithm
				boolean algOk = Algorithm(AlgorithmData, treeMap, adjacencyList1, deadAlgo);

				// create new row to store nre data
				energyrow = energysheet.createRow(rowcounter++);

				// add the first row (not removing node)
				Cell cell = energyrow.createCell(0);
				cell.setCellValue(notremove[0]);

				// 1 and 2 have the same energy since they are the all energy before removing nodes
				// as well as 4
				Cell cell1 = energyrow.createCell(1);
				cell1.setCellValue(minnotremove[1]);

				Cell cell2 = energyrow.createCell(2);
				cell2.setCellValue(minnotremove[1]);

				Cell cell3 = energyrow.createCell(3);
				cell3.setCellValue(minnotremove[3]);

				Cell cell4 = energyrow.createCell(4);
				cell4.setCellValue(Cplexenergy);

				Cell cell5 = energyrow.createCell(5);
				cell5.setCellValue(notremove[1]);
				Cell cell6 = energyrow.createCell(6);
				cell6.setCellValue(AlgorithmData[1]);
				Cell cell7 = energyrow.createCell(7);
				cell7.setCellValue(AlgorithmData[0]);

				Cell minCostEnrgy = energyrow.createCell(8);
				minCostEnrgy.setCellValue(minnotremove[1]);

				Cell minCostResilience = energyrow.createCell(9);
				minCostResilience.setCellValue(minRe);

				Cell baseEnergy = energyrow.createCell(13);
				baseEnergy.setCellValue(minCapacity);

				Cell baseStorage = energyrow.createCell(14);
				baseStorage.setCellValue(numberOfStoragePerSN);

				System.out.println();
				if (!algOk) {
					System.out.println("have problem: network base");
				}
				if (!deadAlgo.isEmpty()) {
					ArrayList<Integer> deadAlgoNodes = new ArrayList<>();
					while (!deadAlgo.isEmpty()) {
						deadAlgoNodes.add(deadAlgo.poll());
					}
					Cell AlgoDead = energyrow.createCell(11);
					AlgoDead.setCellValue(Arrays.toString(deadAlgoNodes.toArray()));
				}
				if (deadNodes.size() > 0) {
					Cell CPLEXDead = energyrow.createCell(10);
					CPLEXDead.setCellValue(Arrays.toString(deadNodes.toArray()));
				}

				if (mindeadNodes.size() > 0) {
					Cell MinCostDead = energyrow.createCell(12);
					MinCostDead.setCellValue(Arrays.toString(mindeadNodes.toArray()));
				}
				// each run add the common difference
				numberOfStoragePerSN += commonDifference;
			}
		} else if (runMethod == 5) {
			System.out.println("the program will run 4 times. Please set the common difference for each run:");
			int commonDifference = scan.nextInt();
			for (int i = 0; i < 4; i++) {
				System.out.println("Now running data items per DG: " + numberOfDataItemsPerDG);

				//MinCost initiate
				IloCplex cpMinFirst = new IloCplex(); // for max function to find the min off load electricity
				IloCplex cpMinObject = new IloCplex(); // for calculating optimized path
				Map<String, IloNumVar> minFirstname = varName(cpMinFirst, treeMap, dataGens, storageNodes);
				Map<String, IloNumVar> minObjtname = varName(cpMinObject, treeMap, dataGens, storageNodes);
				// Cplex initiate
				IloCplex CpFirst = new IloCplex(); // for max function to find the min off load electricity
				IloCplex CpObject = new IloCplex(); // for calculating optimized path
				Map<String, IloNumVar> Firstname = varName(CpFirst, treeMap, dataGens, storageNodes);
				Map<String, IloNumVar> Objtname = varName(CpObject, treeMap, dataGens, storageNodes);

				double[] notremove = new double[row.length];
				ArrayList<Integer> deadNodes = new ArrayList<>();
				ArrayList<Double> totalenergy = new ArrayList<>();
				ArrayList<Double> dumtotalenergy = new ArrayList<>();
				Map<String, Double> cplexresult = new HashMap<>(); // String and the cost of each edge

				double[] minnotremove = new double[row.length];
				ArrayList<Integer> mindeadNodes = new ArrayList<>();
				ArrayList<Double> mintotalenergy = new ArrayList<>();
				Map<String, Double> mincplexresult = new HashMap<>(); // String and the cost of each edge

				boolean[] sendOrNot = new boolean[numberOfNodes + 1];
				int tempData = 0;

				// sort the energy for the mincost usage
				PriorityQueue<Map.Entry<Integer, Double>> sortEnergy = new PriorityQueue<>((a, b) -> b.getValue().intValue() - a.getValue().intValue());
				for (Map.Entry<Integer, Double> entry : sortMap.entrySet()) {
					sortEnergy.offer(entry);
				}

				Arrays.fill(sendOrNot, false);
				// if the energy is high, send it
				while (!sortEnergy.isEmpty() && tempData < numberOfDG * numberOfDataItemsPerDG) {
					if (sortEnergy.peek().getKey() > numberOfDG) {
						int node = sortEnergy.peek().getKey();
						tempData += numberOfStoragePerSN;
						sendOrNot[node] = true;
					}
					sortEnergy.poll();
				}

				// CPLEX
				calculateLp(treeMap, adjacencyList1, close, Firstname, Objtname, CpFirst, CpObject, storageNodes, notremove, deadNodes, totalenergy, dumtotalenergy, "original", "original", cplexresult, 0.03);
				// cplex energy use for 2,4, and 8
				double Cplexenergy = cplexCalculation(treeMap, adjacencyList1, cplexresult, 0, 0);

				// MInCost
				claculateMinCost(treeMap, adjacencyList1, close, minFirstname, minObjtname, cpMinFirst, cpMinObject, storageNodes, minnotremove, mintotalenergy, "original", mincplexresult, sendOrNot);
				double minRe = resilienceMinCost(treeMap, adjacencyList1, mincplexresult, mindeadNodes);

				// Algo
				double[] AlgorithmData = new double[2]; // return the data resiliance level and the energy cost.
				PriorityQueue<Integer> deadAlgo = new PriorityQueue<>();
				// run Algorithm
				boolean algOk = Algorithm(AlgorithmData, treeMap, adjacencyList1, deadAlgo);

				// create new row to store nre data
				energyrow = energysheet.createRow(rowcounter++);

				// add the first row (not removing node)
				Cell cell = energyrow.createCell(0);
				cell.setCellValue(notremove[0]);

				// 1 and 2 have the same energy since they are the all energy before removing nodes
				// as well as 4
				Cell cell1 = energyrow.createCell(1);
				cell1.setCellValue(minnotremove[1]);

				Cell cell2 = energyrow.createCell(2);
				cell2.setCellValue(minnotremove[1]);

				Cell cell3 = energyrow.createCell(3);
				cell3.setCellValue(minnotremove[3]);

				Cell cell4 = energyrow.createCell(4);
				cell4.setCellValue(Cplexenergy);

				Cell cell5 = energyrow.createCell(5);
				cell5.setCellValue(notremove[1]);
				Cell cell6 = energyrow.createCell(6);
				cell6.setCellValue(AlgorithmData[1]);
				Cell cell7 = energyrow.createCell(7);
				cell7.setCellValue(AlgorithmData[0]);

				Cell minCostEnrgy = energyrow.createCell(8);
				minCostEnrgy.setCellValue(minnotremove[1]);

				Cell minCostResilience = energyrow.createCell(9);
				minCostResilience.setCellValue(minRe);

				Cell baseEnergy = energyrow.createCell(13);
				baseEnergy.setCellValue(minCapacity);

				Cell baseStorage = energyrow.createCell(14);
				baseStorage.setCellValue(numberOfStoragePerSN);

				System.out.println();
				if (!algOk) {
					System.out.println("have problem: network base");
				}
				if (!deadAlgo.isEmpty()) {
					ArrayList<Integer> deadAlgoNodes = new ArrayList<>();
					while (!deadAlgo.isEmpty()) {
						deadAlgoNodes.add(deadAlgo.poll());
					}
					Cell AlgoDead = energyrow.createCell(11);
					AlgoDead.setCellValue(Arrays.toString(deadAlgoNodes.toArray()));
				}
				if (deadNodes.size() > 0) {
					Cell CPLEXDead = energyrow.createCell(10);
					CPLEXDead.setCellValue(Arrays.toString(deadNodes.toArray()));
				}

				if (mindeadNodes.size() > 0) {
					Cell MinCostDead = energyrow.createCell(12);
					MinCostDead.setCellValue(Arrays.toString(mindeadNodes.toArray()));
				}
				// each run add the common difference
				numberOfDataItemsPerDG += commonDifference;
			}
		} else if (runMethod == 6){
			// for a same graph, run twice
			for (int i = 0 ; i < 2; i++) {
				int tempmincapa = minCapacity;

				Map<String, Link> originalTree = new TreeMap<>();
				for (Map.Entry<String, Link> pair : treeMap.entrySet()) {
					Link link = new Link(new Edge(pair.getValue().getEdge().getTail(), pair.getValue().getEdge().getHead(), 0),
							pair.getValue().getDistance(), pair.getValue().getRCost(), pair.getValue().getTCost(), pair.getValue().getSCost(),
							pair.getValue().getEnergy());
					originalTree.put(pair.getKey(), link);
				}

				for (Link link : originalTree.values()) {
					sortMap.put(link.getEdge().getHead(), link.getEnergy());
				}

				while (minCapacity > 0) {
					System.out.println("Now running energy capacity: " + minCapacity);
					//MinCost initiate
					IloCplex cpMinFirst = new IloCplex(); // for max function to find the min off load electricity
					IloCplex cpMinObject = new IloCplex(); // for calculating optimized path
					Map<String, IloNumVar> minFirstname = varName(cpMinFirst, originalTree, dataGens, storageNodes);
					Map<String, IloNumVar> minObjtname = varName(cpMinObject, originalTree, dataGens, storageNodes);
					// Cplex initiate
					IloCplex CpFirst = new IloCplex(); // for max function to find the min off load electricity
					IloCplex CpObject = new IloCplex(); // for calculating optimized path
					Map<String, IloNumVar> Firstname = varName(CpFirst, originalTree, dataGens, storageNodes);
					Map<String, IloNumVar> Objtname = varName(CpObject, originalTree, dataGens, storageNodes);

					double[] notremove = new double[row.length];
					ArrayList<Integer> deadNodes = new ArrayList<>();
					ArrayList<Double> totalenergy = new ArrayList<>();
					ArrayList<Double> dumtotalenergy = new ArrayList<>();
					Map<String, Double> cplexresult = new HashMap<>(); // String and the cost of each edge

					double[] minnotremove = new double[row.length];
					ArrayList<Integer> mindeadNodes = new ArrayList<>();
					ArrayList<Double> mintotalenergy = new ArrayList<>();
					Map<String, Double> mincplexresult = new HashMap<>(); // String and the cost of each edge

					boolean[] sendOrNot = new boolean[numberOfNodes + 1];
					int tempData = 0;

					// sort the energy for the mincost usage
					PriorityQueue<Map.Entry<Integer, Double>> sortEnergy = new PriorityQueue<>((a, b) -> b.getValue().intValue() - a.getValue().intValue());
					for (Map.Entry<Integer, Double> entry : sortMap.entrySet()) {
						sortEnergy.offer(entry);
					}

					Arrays.fill(sendOrNot, false);
					// if the energy is high, send it
					while (!sortEnergy.isEmpty() && tempData < numberOfDG * numberOfDataItemsPerDG) {
						if (sortEnergy.peek().getKey() > numberOfDG) {
							int node = sortEnergy.peek().getKey();
							tempData += numberOfStoragePerSN;
							sendOrNot[node] = true;
						}
						sortEnergy.poll();
					}

					// CPLEX
					calculateDeadNode(originalTree, adjacencyList1, close, Firstname, Objtname, CpFirst, CpObject, storageNodes, notremove, deadNodes, totalenergy, dumtotalenergy, "original", "original", cplexresult, 0.5);
					// cplex energy use for 2,4, and 8
					double Cplexenergy = cplexCalculation(originalTree, adjacencyList1, cplexresult, 1, 0);

					// MInCost
					boolean cplexOK = claculateMinCost(originalTree, adjacencyList1, close, minFirstname, minObjtname, cpMinFirst, cpMinObject, storageNodes, minnotremove, mintotalenergy, "original", mincplexresult, sendOrNot);
					double minRe = resilienceMinCost(originalTree, adjacencyList1, mincplexresult, mindeadNodes);

					// Algo
					double[] AlgorithmData = new double[2]; // return the data resiliance level and the energy cost.
					PriorityQueue<Integer> deadAlgo = new PriorityQueue<>();
					// run Algorithm
					boolean algOk = Algorithm(AlgorithmData, originalTree, adjacencyList1, deadAlgo);

					// create new row to store nre data
					energyrow = energysheet.createRow(rowcounter++);

					// add the first row (not removing node)
					Cell cell = energyrow.createCell(0);
					cell.setCellValue(notremove[0]);

					// 1 and 2 have the same energy since they are the all energy before removing nodes
					// as well as 4
					Cell cell1 = energyrow.createCell(1);
					cell1.setCellValue(minnotremove[1]);

					Cell cell2 = energyrow.createCell(2);
					cell2.setCellValue(minnotremove[1]);

					Cell cell3 = energyrow.createCell(3);
					cell3.setCellValue(minnotremove[3]);

					Cell cell4 = energyrow.createCell(4);
					cell4.setCellValue(Cplexenergy);

					Cell cell5 = energyrow.createCell(5);
					cell5.setCellValue(notremove[1]);
					if (algOk) {
						Cell cell6 = energyrow.createCell(6);
						cell6.setCellValue(AlgorithmData[1]);
						Cell cell7 = energyrow.createCell(7);
						cell7.setCellValue(AlgorithmData[0]);
					} else {
						System.out.println("have problem: network base");
						Cell cell6 = energyrow.createCell(6);
						cell6.setCellValue("X");
						Cell cell7 = energyrow.createCell(7);
						cell7.setCellValue("X");
					}

					if (cplexOK) {
						Cell minCostEnrgy = energyrow.createCell(8);
						minCostEnrgy.setCellValue(minnotremove[1]);

						Cell minCostResilience = energyrow.createCell(9);
						minCostResilience.setCellValue(minRe);
					} else {
						Cell minCostEnrgy = energyrow.createCell(8);
						minCostEnrgy.setCellValue("X");

						Cell minCostResilience = energyrow.createCell(9);
						minCostResilience.setCellValue("X");
					}

					Cell baseEnergy = energyrow.createCell(13);
					baseEnergy.setCellValue(minCapacity);

					Cell baseStorage = energyrow.createCell(14);
					baseStorage.setCellValue(numberOfStoragePerSN);
					System.out.println();

					/* ----------------- dead node ----------------------*/
					if (algOk) {
						if (!deadAlgo.isEmpty()) {
							ArrayList<Integer> deadAlgoNodes = new ArrayList<>();
							while (!deadAlgo.isEmpty()) {
								deadAlgoNodes.add(deadAlgo.poll());
							}
							Cell AlgoDead = energyrow.createCell(11);
							AlgoDead.setCellValue(Arrays.toString(deadAlgoNodes.toArray()));
						}
					} else {
						Cell AlgoDead = energyrow.createCell(11);
						AlgoDead.setCellValue("X");
					}

					if (deadNodes.size() > 0) {
						Cell CPLEXDead = energyrow.createCell(10);
						CPLEXDead.setCellValue(Arrays.toString(deadNodes.toArray()));
					}

					if (cplexOK) {
						if (mindeadNodes.size() > 0) {
							Cell MinCostDead = energyrow.createCell(12);
							MinCostDead.setCellValue(Arrays.toString(mindeadNodes.toArray()));
						}
					} else {
						Cell MinCostDead = energyrow.createCell(12);
						MinCostDead.setCellValue("X");
					}

					// clear the sorted part (for new generation)
					sortMap.clear();
					sortEnergy.clear();

					for (Link link : originalTree.values()) {
						link.setEnergy(link.getEnergy() - 200);
						sortMap.put(link.getEdge().getHead(), link.getEnergy() - 200);
					}
					// each run, change base capacity
					minCapacity -= 200;
				}

				// generate data for another storage capacity
				rowcounter++;
				if (i < 1) {
					System.out.println("Input another capacity: ");
					numberOfStoragePerSN = scan.nextInt();
				}
				minCapacity = tempmincapa;
				originalTree.clear();
				sortMap.clear();
			}
		} else {
			System.out.println("Wrong input!");
		}

		// for .txt files data output
//		generateFiles(treeMap, adjacencyList1);
		
        // write to csv file
        FileOutputStream out = new FileOutputStream(new File("data.xlsx"));
        energyworkbook.write(out);
        out.close();

		System.out.println("Finish!");
	}

	/*-------------------------- Calculates total energy cost ------------------------------*/
    /**
     * This method calculate the cost of the resilience (minimum cost flow)
     * @param treemap: all edge cost
     * @param adjList: adjacent list
     * @param data: the result of the resilience path
     * @param deadNodes: use to record the dead nodes
     * @return : total cost of the path
     * @throws IOException :
     */
	private static double resilienceMinCost(Map<String, Link> treemap, Map<Integer, Set<Integer>> adjList, Map<String, Double> data, ArrayList<Integer> deadNodes) throws IOException {
		double result = 0;
		double[] initialEnergy = new double[numberOfNodes + 1];
		double[] storage = new double[numberOfNodes + 1];

		for (Map.Entry<String, Link> entry: treemap.entrySet()) {
			initialEnergy[entry.getValue().getEdge().getHead()] = entry.getValue().getEnergy();
		}

		StringBuilder path = new StringBuilder();

		for (Map.Entry<String, Double> entry : data.entrySet()){
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
					to = adjList.get(from).iterator().next();
					double value = treemap.get("(" + to + ", " + from + ")").getSCost() * entry.getValue();
					initialEnergy[from] -= value;
				} else {
					double fromvalue = (treemap.get("(" + from + ", " + to + ")").getTCost() - treemap.get("(" + from + ", " + to + ")").getRCost()) * entry.getValue();
					double tovalue = treemap.get("(" + from + ", " + to + ")").getRCost() * entry.getValue();
					initialEnergy[from] -= fromvalue;
					initialEnergy[to] -= tovalue;
				}
			}
		}

		for(int i = 1; i < storage.length; i++) {
			int deadcont = 0;
			for (int j : adjList.get(i)) {
				if (initialEnergy[i] < treemap.get("(" + i + ", " + j + ")").getRCost() &&
						initialEnergy[i] < treemap.get("(" + i + ", " + j + ")").getTCost() - treemap.get("(" + i + ", " + j + ")").getRCost()) {
					deadcont++;
				}
			}
			if (deadcont == adjList.get(i).size()) {
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
     * This method calculate energy cost of max data priority
     * @param treemap: all edge cost
     * @param adjList: adjacent list
     * @param data: max priority path
     * @param deadNodes: use to record the dead nodes
     * @param cost: detail cost for each node
     * @param method: true: origianl max flow method, false: adding sink for each storage (excess data will store here)
	 *                Two method will have same max priority result but different energy cost.
     * @return : cost of this data transferring path
     * @throws IOException:
     */
	private static double calculatePriority(Map<String, Link> treemap, Map<Integer, Set<Integer>> adjList, Map<String, Double> data, ArrayList<Integer> deadNodes, ArrayList<Double> cost, boolean method) throws IOException {
		double result = 0;
		double[] initialEnergy = new double[numberOfNodes + 1];
		double[] storage = new double[numberOfNodes + 1];
		double initialTotalEnergy = 0;
		double remaindEnergy = 0;

		for (Map.Entry<String, Link> entry: treemap.entrySet()) {
			initialEnergy[entry.getValue().getEdge().getHead()] = entry.getValue().getEnergy();
		}

		for (double value : initialEnergy) {
			initialTotalEnergy += value;
		}

		StringBuilder path = new StringBuilder();

		// take out the number
		for (Map.Entry<String, Double> entry : data.entrySet()){
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
						if (method) {
							// take out source sink (label 0)
							nodes.add(0);
							j++;

							// this loop will take out one number form the string
							int num = curname.charAt(j) - '0';
							while(j + 1 < curname.length() && Character.isDigit(curname.charAt(j + 1))) {
								num = num * 10 + curname.charAt(j + 1) - '0';
								j++;
							}
							nodes.add(num);
						} else {
							zeroflag = true;
						}
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

			// compare with the input map to retrieve the energy cost
			if (!zeroflag) {
				int from = nodes.get(0);
				int to = nodes.get(1);

				// destination is saving sink
				if (from == 0) {
					storage[to] = numberOfDataItemsPerDG - entry.getValue();
				} else if (to == numberOfNodes + 1) {
					storage[from] = entry.getValue();
					to = adjList.get(from).iterator().next();
					double value = treemap.get("(" + to + ", " + from + ")").getSCost() * entry.getValue();
					initialEnergy[from] -= value;
					// count the data item saved
					result += entry.getValue();
				} else if (to == numberOfNodes + 2) {
					storage[from] = entry.getValue();
				} else {
					double fromvalue = (treemap.get("(" + from + ", " + to + ")").getTCost() - treemap.get("(" + from + ", " + to + ")").getRCost()) * entry.getValue();
					double tovalue = treemap.get("(" + from + ", " + to + ")").getRCost() * entry.getValue();
					initialEnergy[from] -= fromvalue;
					initialEnergy[to] -= tovalue;
				}
			}
		}

		for(int i = 1; i < storage.length; i++) {
			int deadcont = 0;
			for (int j : adjList.get(i)) {
				if (initialEnergy[i] < treemap.get("(" + i + ", " + j + ")").getRCost() &&
						initialEnergy[i] < treemap.get("(" + i + ", " + j + ")").getTCost() - treemap.get("(" + i + ", " + j + ")").getRCost()) {
					deadcont++;
				}
			}
			if (deadcont == adjList.get(i).size()) {
				deadNodes.add(i);
			}
		}

		System.out.println(Arrays.toString(storage));
		System.out.println(Arrays.toString(initialEnergy));

		for (double value : initialEnergy) {
			remaindEnergy += value;
		}

		cost.add(initialTotalEnergy);
		cost.add(remaindEnergy);

//		File EnergyFile = new File("Cal_Priority_path_" + numberOfStoragePerSN + ".txt");
//		BufferedWriter writer_energy = new BufferedWriter(new FileWriter(EnergyFile));
//		writer_energy.write(path.toString());
//		writer_energy.close();

		return result;
	}

	/**
	 * calculate the total cost of max resilience path
	 * @param treemap: edge cost
	 * @param adjList: adjacent list
	 * @param data: path information
	 * @param method: true: without storage sink, false: with storage sink
	 * @param gapTolerance: value of gap tolerance
	 * @return : path total cost
	 * @throws IOException :
	 */
	static double cplexCalculation(Map<String, Link> treemap, Map<Integer, Set<Integer>> adjList, Map<String, Double> data, int method, double gapTolerance) throws IOException {
		double result = 0;

		if (method == 0) {
			StringBuilder path = new StringBuilder();
			//copy the map
			Map<String, Double> clonedata = new HashMap<>(data);

			// this loop we first eliminate cycles between two nodes
			for (Map.Entry<String, Double> entry : data.entrySet()) {
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
					String original = "x" + from + "''" + to + "'";
					String reverse = "x" + to + "''" + from + "'";
					if (data.containsKey(reverse)) {
						if (entry.getValue() > 0.0 && entry.getValue() <= clonedata.get(reverse)) {
							clonedata.put(reverse, clonedata.get(reverse) - entry.getValue());
							clonedata.put(original, 0.0);
						}
					}
				}
			}

			data.clear();
			data.putAll(clonedata);

			for (Map.Entry<String, Double> entry : data.entrySet()) {
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
						to = adjList.get(from).iterator().next();
						result += treemap.get("(" + from + ", " + to + ")").getSCost() * entry.getValue();
					} else {
						result += treemap.get("(" + from + ", " + to + ")").getTCost() * entry.getValue();
					}
				}
			}

//			File EnergyFile = new File("CPLEX_Data_path_" + numberOfDataItemsPerDG * numberOfDG + "_" + numberOfStoragePerSN + ".txt");
//			BufferedWriter writer_energy = new BufferedWriter(new FileWriter(EnergyFile));
//			writer_energy.write(path.toString());
//			writer_energy.close();

		} else {
			for (Map.Entry<String, Double> entry : data.entrySet()) {
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
						to = adjList.get(from).iterator().next();
						result += treemap.get("(" + from + ", " + to + ")").getSCost() * entry.getValue();
					} else {
						result += treemap.get("(" + from + ", " + to + ")").getTCost() * entry.getValue();
					}
				}
			}
		}
		return result;
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
	public static Map<String, IloNumVar> varName (IloCplex Cp, Map<String, Link> treeMap, int[] dgs, int[] sns) throws IloException {

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
			String str = "x" + sn + "''" + (numberOfNodes + 1);
			IloNumVar element = Cp.numVar(0, Double.MAX_VALUE, str);
			name.put(str, element);
		}

		return name;
	}

	/**
	 * This function create the column name for Cplex
	 * (used for priority second method with storage sink) - different name formulation
	 * @param Cp: target Cplex formulation input
	 * @param treeMap: edge cost
	 * @param dgs: data generator list
	 * @param sns: storage node list
	 * @return : column name
	 * @throws IloException:
	 */
	public static Map<String, IloNumVar> priorityName (IloCplex Cp, Map<String, Link> treeMap, int[] dgs, int[] sns) throws IloException {
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
		// nodes to nodes sink
		for (int i = 0; i < numberOfNodes; i++) {
			if (i < numberOfDG) {
				String str = "x" + (i + 1) + "''" + (numberOfNodes + 2);
				IloNumVar element = Cp.numVar(0, Double.MAX_VALUE, str);
				name.put(str, element);
			} else {
				String str = "x" + (i + 1) + "''" + (numberOfNodes + 1);
				IloNumVar element = Cp.numVar(0, Double.MAX_VALUE, str);
				name.put(str, element);
			}
		}

		return name;
	}

	/*--------------------------- Algorithm / linear programming / quadratic programming -------------------------------*/
	/**
	 * Network base algorithm for data resilience calculation - Algorithm
	 * @param dataout : store energy cumsumption and  resilient
	 * @param copytreeMap: link information distance, receive cost, transmit cost, storage cost, energy capacity (from edge perspective)
	 */
	public static boolean Algorithm (double[] dataout, Map<String, Link> copytreeMap, Map<Integer, Set<Integer>> adjlist, PriorityQueue<Integer> deadnodes) {
		double totalEnergy = 0.0;
		double dataresilient = 0.0;
		Map<String, Link> treeMap = new TreeMap<String, Link>(copytreeMap);
		HashSet<Integer> set = new HashSet<>();

		List<Map.Entry<String, Link>> list = new ArrayList<Map.Entry<String, Link>>(treeMap.entrySet());

		// descending order
		Collections.sort(list, (o1, o2) -> o1.getValue().compareTo(o2.getValue()));

		int[] storageList = new int[numberOfNodes + 1];
		double[] energyList = new double[numberOfNodes + 1];

		for (int i = 1; i < numberOfDG + 1; i++) {
			storageList[i] = numberOfDataItemsPerDG;
		}

		for (Map.Entry<String, Link> pair : treeMap.entrySet()) {
			Link link = pair.getValue();
			energyList[link.getEdge().getHead()] = link.getEnergy();
		}

//		Calling Dijkastra Algorithm
		WeighedDigraph graph = new WeighedDigraph(treeMap);
		DijkstraFind finder = new DijkstraFind(graph);

		int storageCapacity = numberOfStoragePerSN;

		// use sorted map here
		for (int numOfData = 0 ; numOfData < numberOfDG * numberOfDataItemsPerDG; numOfData++) {
			for (Map.Entry<String, Link> sortedMap : list) {
				Link link = sortedMap.getValue(); // get current link
				Map<Double, ArrayList<Integer>> map = new HashMap<>(); // list of the path, key is the result of total cost
				PriorityQueue<Double> pq = new PriorityQueue<>(); //

				if (link.getEdge().getHead() <= numberOfDG || storageList[link.getEdge().getHead()] == storageCapacity){
					continue;
				} else { // case can sent
					// if current links head > 10, can be sent
					// go over 10 nodes to find the shortest path
					for (int i = 1; i <= numberOfDG; i++) {
						ArrayList<Integer> path = finder.shortestPath(i, link.getEdge().getHead(), numberOfDG);
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
			if (i < numberOfDG + 1 && storageList[i] > 0) {
				System.out.println("Cannot deliver all data");
				System.out.println(Arrays.toString(storageList));
				System.out.println(Arrays.toString(energyList));
				return false;
			} else {
				dataresilient += storageList[i] * energyList[i];
			}
		}

		// the output data will be store in this array
		dataout[1] = totalEnergy;
		dataout[0] = dataresilient;
		System.out.println(Arrays.toString(storageList));
		System.out.println(Arrays.toString(energyList));

		return true; // calculation complete
	}

	// build the string for map's key (node 1, node 2)
	public static String buildString (ArrayList<Integer> path, int j, int k) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		sb.append(path.get(j));
		sb.append(", ");
		sb.append(path.get(k));
		sb.append(")");
		return sb.toString();
	}

    /**
     * CPLEX calculation (minimum cost flow) - linear programming
     * @param treeMap : edge cost
     * @param adjacencyList1: adjacent list
     * @param tempclose: closest neighbor for each node
     * @param nameFirst: cplex column name list for first objective (check if the network can send out all data)
     * @param nameObj: cplex column name list for true objective
     * @param cpFirst: cplex function name
     * @param cpMinCost: cplex function name
     * @param storges: storage list
     * @param exldata: record specific data
     * @param faketotalenergy: energy list when report fake cost
     * @param Lpfilename: name for generating files (only used if needed)
     * @param cplexresult: result path
     * @param snedOrNot:  list to record specific nodes we don't want to send to
     * @return : can solve or not
     * @throws IOException :
     * @throws IloException :
     */
	static boolean claculateMinCost(Map<String, Link> treeMap, Map<Integer, Set<Integer>> adjacencyList1, HashMap<Integer, List<Integer>> tempclose,
								   Map<String, IloNumVar> nameFirst, Map<String, IloNumVar> nameObj, IloCplex cpFirst, IloCplex cpMinCost, int[] storges, double[] exldata, ArrayList<Double> faketotalenergy,
								   String Lpfilename, Map<String, Double> cplexresult, boolean[] snedOrNot) throws IOException, IloException {

			List<IloRange> constraintsFirst = new ArrayList<IloRange>();
			List<IloRange> constraintsMinCost = new ArrayList<IloRange>();

			// adding first constrain
			for(int i: adjacencyList1.keySet()) {

				IloLinearNumExpr exprFirst = cpFirst.linearNumExpr();
				IloLinearNumExpr exprMincost = cpMinCost.linearNumExpr();

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
				constraintsFirst.add(cpFirst.addEq(exprFirst, 0));
				constraintsMinCost.add(cpMinCost.addEq(exprMincost, 0));
			}

			// adding second constrain
			for(int i: adjacencyList1.keySet()) {
				IloLinearNumExpr exprFirst = cpFirst.linearNumExpr();
				IloLinearNumExpr exprMinCost = cpMinCost.linearNumExpr();
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
				constraintsFirst.add(cpFirst.addLe(exprFirst, treeMap.get("("+ nei +", "+ i +")").getEnergy()));
				constraintsMinCost.add(cpMinCost.addLe(exprMinCost,  treeMap.get("("+ nei +", "+ i +")").getEnergy()));
			}

			// add constrains for single node ** only firstObj
			// objective needs to be separate since the data is possible to be discarded by the data generators (after remove nodes)
			for (Integer i : adjacencyList1.keySet()) {
				IloLinearNumExpr exprFirst = cpFirst.linearNumExpr();

				if (i <= dataGens.length) {
					String dir = "x0" + i + "'";
					exprFirst.addTerm(1, nameFirst.get(dir));
					constraintsFirst.add(cpFirst.addLe(exprFirst, numberOfDataItemsPerDG));
				} else {
					int temp = numberOfNodes + 1;
					String dir = "x" + i + "''" + temp;
					if (snedOrNot[i]) {
						exprFirst.addTerm(1, nameFirst.get(dir));
						constraintsFirst.add(cpFirst.addLe(exprFirst, numberOfStoragePerSN));
					} else {
						exprFirst.addTerm(1, nameFirst.get(dir));
						constraintsFirst.add(cpFirst.addEq(exprFirst, 0));
					}
				}
			}

			// add first obj objective function
			IloLinearNumExpr objectiveFirst = cpFirst.linearNumExpr();

			for (int i = 0; i < dataGens.length; i++) {
				int temp = i + 1;
				String dir = "x0" + temp + "'";
				objectiveFirst.addTerm(1, nameFirst.get(dir));
			}

			cpFirst.addMaximize(objectiveFirst);
			cpFirst.exportModel("MinCostFirst_" + Lpfilename + ".lp");

			// only see important messages on screen while solving
			//	CpFirst.setParam(IloCplex.Param.Simplex.Display, 0);

			// start solving
			// check if the problem is solvable
			if (!cpFirst.solve()) {
                System.out.println("Model not solved");
			}

			// initial data items to offload
			int[] dataIn = new int[numberOfDG];

			Arrays.fill(dataIn, numberOfDataItemsPerDG);

			// objective value
			System.out.println("Objective value: " + cpFirst.getObjValue());
			if (cpFirst.getObjValue() < numberOfDG * numberOfDataItemsPerDG) {
				System.out.println("Can not distribute all data!");
				// single generator constrains start at index 98
				for (int i = 1; i <= dataGens.length; i++) {
					// case the network cannot handle all data
					if (cpFirst.getValue(nameFirst.get("x0" + i + "'")) < 99.999) {
						dataIn[i - 1] = (int) cpFirst.getValue(nameFirst.get("x0" + i + "'"));
					}
				}
				return false;
			} else {
				System.out.println("Success!");
			}

			System.out.println(Arrays.toString(dataIn));

			/*------------ Obj's signal node constrains-------------------*/
			for (Integer i : adjacencyList1.keySet()) {
				IloLinearNumExpr exprMinCost = cpMinCost.linearNumExpr();

				// dataIn[i] may change if FirstObj is not solvable (DG discard data)
				if (i <= dataGens.length) {
					String dir = "x0" + i + "'";
					exprMinCost.addTerm(1, nameObj.get(dir));
					constraintsMinCost.add(cpMinCost.addEq(exprMinCost, dataIn[i - 1]));
				} else {
					int temp = numberOfNodes + 1;
					String dir = "x" + i + "''" + temp;
					if (snedOrNot[i]) {
						exprMinCost.addTerm(1, nameObj.get(dir));
						constraintsMinCost.add(cpMinCost.addLe(exprMinCost, numberOfStoragePerSN));
					} else {
						exprMinCost.addTerm(1, nameObj.get(dir));
						constraintsMinCost.add(cpMinCost.addEq(exprMinCost, 0));
					}
				}
			}

 		// original objective function
		IloLinearNumExpr minCostObj = cpMinCost.linearNumExpr();
		for (Link link : treeMap.values()){
			String dir = "x" + link.getEdge().getTail() + "''" + link.getEdge().getHead() + "'";
			minCostObj.addTerm(link.getTCost(), nameObj.get(dir));
		}
		for (int i = 0; i < storges.length; i++) {
			int temp = numberOfNodes + 1;
			String dir = "x" + storges[i] + "''" + temp;
			int n2 = adjacencyList1.get(storges[i]).iterator().next();
			minCostObj.addTerm(treeMap.get("("+ n2 + ", " + storges[i] +")").getSCost(), nameObj.get(dir));
		}

		// write objective function
		cpMinCost.addMinimize(minCostObj);

        // export model
//		cpMinCost.exportModel("MinCostObj_" + Lpfilename + ".lp");

		// start solving
		// check if the problem is solvable
		if (cpMinCost.solve()) {
			// objective value of min, x, and y
			System.out.println("obj = " + cpMinCost.getObjValue());
			for (Map.Entry<String, IloNumVar> entry : nameObj.entrySet()) {
				cplexresult.put(entry.getKey(), cpMinCost.getValue(entry.getValue()));
			}
		}
		else {
			System.out.println("Model not solved");
		}

		System.out.println("Second Objective value: " + cpMinCost.getObjValue());
		// file for verifying transferring path
		StringBuilder dataTpath = new StringBuilder();

		dataTpath.append("Second Obj Value: "+ cpMinCost.getObjValue() + "\n");

		for (Map.Entry<String, IloNumVar> entry : nameObj.entrySet()) {
			dataTpath.append(entry.getKey() + " : " + cpMinCost.getValue(entry.getValue()) + "\n");
		}

//        File PathFile = new File("G:\\downloads\\work\\Dr. Bin\\May\\sensor\\out_files\\Lpout_" + Lpfilename + ".txt");
//        BufferedWriter writer = new BufferedWriter(new FileWriter(PathFile));
//        writer.write(dataTpath.toString());
//        writer.close();

		//getMinFile
		ArrayList<Double> tempp = new ArrayList<>();

		faketotalenergy.addAll(tempp);

		exldata[1] = cpMinCost.getObjValue();
		exldata[3] = (int) cpFirst.getObjValue();

		// clean up memory used
		cpFirst.end();
		cpMinCost.end();

		return true;
	}

    /**
	 * CPLEX calculation (Priority calculation "with" storage sink) non QP - Linear programming
	 * @param treeMap : edge cost
	 * @param adjacencyList1: adjacent list
	 * @param tempclose: closest neighbor for each node
	 * @param nameFirst: cplex column name list for first objective (check if the network can send out all data)
	 * @param nameObj: cplex column name list for true objective
	 * @param cpFirst: cplex function name
	 * @param cpMinCost: cplex function name
	 * @param storges: storage list
	 * @param exldata: record specific data
	 * @param faketotalenergy: energy list when report fake cost
	 * @param Lpfilename: name for generating files (only used if needed)
	 * @param cplexresult: result path
     * @param DGpriority: each data generator's priority
     * @return : can solve or not
     * @throws IOException
     * @throws IloException
     */
    static boolean calculatePri(Map<String, Link> treeMap, Map<Integer, Set<Integer>> adjacencyList1, HashMap<Integer, List<Integer>> tempclose,
                                    Map<String, IloNumVar> nameFirst, Map<String, IloNumVar> nameObj, IloCplex cpFirst, IloCplex cpMinCost, int[] storges, double[] exldata, ArrayList<Double> faketotalenergy,
                                    String Lpfilename, Map<String, Double> cplexresult, ArrayList<Integer> DGpriority) throws IOException, IloException {
		/*
		* First: use to check if data can be offloaded from each DG
		* MinCost: use to compute the Min priority
		* */
        List<IloRange> constraintsFirst = new ArrayList<IloRange>();
        List<IloRange> constraintsMinCost = new ArrayList<IloRange>();


        // adding first constrain -> data transmit constraint (in = out)
		// First and Min Cost are the same
        for(int i: adjacencyList1.keySet()) {

            IloLinearNumExpr exprFirst = cpFirst.linearNumExpr();
            IloLinearNumExpr exprMincost = cpMinCost.linearNumExpr();

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

            // add the storage sink
            if (i > dataGens.length) {
				int temp = numberOfNodes + 1;
				String dir = "x" + i + "''" + temp;
                exprFirst.addTerm(-1, nameFirst.get(dir));
				// only for obj
				exprMincost.addTerm(-1, nameObj.get(dir));
            } else {
				int temp = numberOfNodes + 2;
				String dir = "x" + i + "''" + temp;
				// only for obj
				exprMincost.addTerm(-1, nameObj.get(dir));
			}

            // add constrain
            constraintsFirst.add(cpFirst.addEq(exprFirst, 0));
            constraintsMinCost.add(cpMinCost.addEq(exprMincost, 0));
        }

        // adding second constrain -> energy constraint (energy consumption <= the energy left on the node)
		// First and Min Cost are the same
        for(int i: adjacencyList1.keySet()) {
            IloLinearNumExpr exprFirst = cpFirst.linearNumExpr();
            IloLinearNumExpr exprMinCost = cpMinCost.linearNumExpr();
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
            constraintsFirst.add(cpFirst.addLe(exprFirst, treeMap.get("("+ nei +", "+ i +")").getEnergy()));
            constraintsMinCost.add(cpMinCost.addLe(exprMinCost,  treeMap.get("("+ nei +", "+ i +")").getEnergy()));
        }

        // add constrains for single node (for priority calculation, two formulation are the same, less than or equal to)
        for (Integer i : adjacencyList1.keySet()) {
            IloLinearNumExpr exprFirst = cpFirst.linearNumExpr();
			IloLinearNumExpr exprMinCost = cpMinCost.linearNumExpr();
			IloLinearNumExpr exprDGsink = cpMinCost.linearNumExpr();
			int temp = numberOfNodes + 1;

            if (i <= dataGens.length) {
                String dir = "x0" + i + "'";
                exprFirst.addTerm(1, nameFirst.get(dir));
                constraintsFirst.add(cpFirst.addLe(exprFirst, numberOfDataItemsPerDG));

				// data generators should add sink node only for priority objective
                // source part
				exprMinCost.addTerm(1, nameObj.get(dir));
				constraintsMinCost.add(cpMinCost.addEq(exprMinCost, numberOfDataItemsPerDG));
				// sink part (generator sink is the number of node + 2)
				dir = "x" + i + "''" + (temp + 1);
				exprDGsink.addTerm(1, nameObj.get(dir));
				constraintsMinCost.add(cpMinCost.addLe(exprDGsink, numberOfDataItemsPerDG));
            } else {
            	// both first and priority objective are the same for storage nodes
                String dir = "x" + i + "''" + temp;
                exprFirst.addTerm(1, nameFirst.get(dir));
                constraintsFirst.add(cpFirst.addLe(exprFirst, numberOfStoragePerSN));
				exprMinCost.addTerm(1, nameObj.get(dir));
				constraintsMinCost.add(cpMinCost.addLe(exprMinCost, numberOfStoragePerSN));
            }
        }

        // add first obj objective function
        IloLinearNumExpr objectiveFirst = cpFirst.linearNumExpr();
        for (int i = 0; i < dataGens.length; i++) {
            int temp = i + 1;
            String dir = "x0" + temp + "'";
            objectiveFirst.addTerm(1, nameFirst.get(dir));
        }

        cpFirst.addMaximize(objectiveFirst);
        cpFirst.exportModel("MinCostFirst_" + Lpfilename + ".lp");
        // only see important messages on screen while solving
        //	CpFirst.setParam(IloCplex.Param.Simplex.Display, 0);

        // start solving
        // check if the problem is solvable
        if (!cpFirst.solve()) {
            System.out.println("Model not solved, have some problem.");
        }

        // objective value (print and check if the data items can be offloaded)
        System.out.println("Objective value: " + cpFirst.getObjValue());
        // we want the data items "cannot" be offloaded
        if (cpFirst.getObjValue() == numberOfDG * numberOfDataItemsPerDG) {
            System.out.println("Data can be offloaded");
            return false;
        } else {
            System.out.println("Cannot distribute all data, so calculate min priority.");
			System.out.println();
        }

        /* Start second objective function (Priority) */
		IloLinearNumExpr minCostObj = cpMinCost.linearNumExpr();
        // min priority (Second) objective function
		for (int i = 0; i < dataGens.length; i++) {
			int temp = i + 1;
			String dir = "x" + temp + "''" + (numberOfNodes + 2);
			minCostObj.addTerm(DGpriority.get(i), nameObj.get(dir));
		}

        // write objective function
        cpMinCost.addMinimize(minCostObj);
//      cpMinCost.exportModel("MinPriority_" + Lpfilename + ".lp");

        // start solving
        // check if the problem is solvable
        if (cpMinCost.solve()) {
            // objective value of min, x, and y
            System.out.println("obj = " + cpMinCost.getObjValue());
            for (Map.Entry<String, IloNumVar> entry : nameObj.entrySet()) {
                cplexresult.put(entry.getKey(), cpMinCost.getValue(entry.getValue()));
            }
        }
        else {
            System.out.println("Model not solved, have some problem");
        }

        System.out.println("Second Objective value: " + cpMinCost.getObjValue());
        // file for verify transferring path
        StringBuilder dataTpath = new StringBuilder();

        dataTpath.append("Second Obj Value: "+ cpMinCost.getObjValue() + "\n");
        //      double[] tempresult1 = lpObj.getPtrVariables();

        for (Map.Entry<String, IloNumVar> entry : nameObj.entrySet()) {
            dataTpath.append(entry.getKey() + " : " + cpMinCost.getValue(entry.getValue()) + "\n");
        }

//        File PathFile = new File("G:\\downloads\\work\\Dr. Bin\\May\\sensor\\out_files\\Lpout_" + Lpfilename + ".txt");
//        BufferedWriter writer = new BufferedWriter(new FileWriter(PathFile));
//        writer.write(dataTpath.toString());
//        writer.close();

        //getMinFile
        ArrayList<Double> tempp = new ArrayList<>();

		faketotalenergy.addAll(tempp);

        exldata[1] = cpMinCost.getObjValue();
        exldata[3] = (int) cpFirst.getObjValue();

        // clean up memory used
        cpFirst.end();
        cpMinCost.end();

        return true;
    }

    /**
	 * CPLEX calculation (Priority calculation "without" storage sink) non QP - Linear programming
	 * @param treeMap : edge cost
	 * @param adjacencyList1: adjacent list
	 * @param tempclose: closest neighbor for each node
	 * @param nameFirst: cplex column name list for first objective (check if the network can send out all data)
	 * @param nameObj: cplex column name list for true objective
	 * @param cpFirst: cplex function name
	 * @param cpMinCost: cplex function name
	 * @param storges: storage list
	 * @param exldata: record specific data
	 * @param faketotalenergy: energy list when report fake cost
	 * @param Lpfilename: name for generating files (only used if needed)
	 * @param cplexresult: result path
	 * @param DGpriority: each data generator's priority
	 * @return : can solve or not
	 * @throws IOException
	 * @throws IloException
     */
	static boolean calculatePriMaxFlow(Map<String, Link> treeMap, Map<Integer, Set<Integer>> adjacencyList1, HashMap<Integer, List<Integer>> tempclose,
								Map<String, IloNumVar> nameFirst, Map<String, IloNumVar> nameObj, IloCplex cpFirst, IloCplex cpMinCost, int[] storges, double[] exldata, ArrayList<Double> faketotalenergy,
								String Lpfilename, Map<String, Double> cplexresult, ArrayList<Integer> DGpriority) throws IOException, IloException {
		/*
		 * First: use to check if data can be offloaded from each DG
		 * MinCost: use to compute the Min priority
		 * */
		List<IloRange> constraintsFirst = new ArrayList<IloRange>();
		List<IloRange> constraintsMinCost = new ArrayList<IloRange>();

		// adding first constrain -> data transmit constraint (in = out)
		// First and Min Cost are the same
		for(int i: adjacencyList1.keySet()) {

			IloLinearNumExpr exprFirst = cpFirst.linearNumExpr();
			IloLinearNumExpr exprMincost = cpMinCost.linearNumExpr();

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

			// add the storage sink
			if (i > dataGens.length) {
				int temp = numberOfNodes + 1;
				String dir = "x" + i + "''" + temp;
				exprFirst.addTerm(-1, nameFirst.get(dir));
				// only for obj
				exprMincost.addTerm(-1, nameObj.get(dir));
			}

			// add constrain
			constraintsFirst.add(cpFirst.addEq(exprFirst, 0));
			constraintsMinCost.add(cpMinCost.addEq(exprMincost, 0));
		}

		// adding second constrain -> energy constraint (energy consumption <= the energy left on the node)
		// First and Min Cost are the same
		for(int i: adjacencyList1.keySet()) {
			IloLinearNumExpr exprFirst = cpFirst.linearNumExpr();
			IloLinearNumExpr exprMinCost = cpMinCost.linearNumExpr();
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
			constraintsFirst.add(cpFirst.addLe(exprFirst, treeMap.get("("+ nei +", "+ i +")").getEnergy()));
			constraintsMinCost.add(cpMinCost.addLe(exprMinCost,  treeMap.get("("+ nei +", "+ i +")").getEnergy()));
		}

		// add constrains for single node (for priority calculation, two formulation are the same, less than or equal to)
		for (Integer i : adjacencyList1.keySet()) {
			IloLinearNumExpr exprFirst = cpFirst.linearNumExpr();
			IloLinearNumExpr exprMinCost = cpMinCost.linearNumExpr();
			int temp = numberOfNodes + 1;

			if (i <= dataGens.length) {
				String dir = "x0" + i + "'";
				exprFirst.addTerm(1, nameFirst.get(dir));
				constraintsFirst.add(cpFirst.addLe(exprFirst, numberOfDataItemsPerDG));

				// source part
				exprMinCost.addTerm(1, nameObj.get(dir));
				constraintsMinCost.add(cpMinCost.addLe(exprMinCost, numberOfDataItemsPerDG));
			} else {
				// both first and priority objective are the same for storage nodes
				String dir = "x" + i + "''" + temp;
				exprFirst.addTerm(1, nameFirst.get(dir));
				constraintsFirst.add(cpFirst.addLe(exprFirst, numberOfStoragePerSN));
				exprMinCost.addTerm(1, nameObj.get(dir));
				constraintsMinCost.add(cpMinCost.addLe(exprMinCost, numberOfStoragePerSN));
			}
		}

		// add first obj objective function
		IloLinearNumExpr objectiveFirst = cpFirst.linearNumExpr();
		for (int i = 0; i < dataGens.length; i++) {
			int temp = i + 1;
			String dir = "x0" + temp + "'";
			objectiveFirst.addTerm(1, nameFirst.get(dir));
		}

		cpFirst.addMaximize(objectiveFirst);
		cpFirst.exportModel("CheckFirst_" + Lpfilename + ".lp");
		// only see important messages on screen while solving
		//	CpFirst.setParam(IloCplex.Param.Simplex.Display, 0);

		// start solving
		// check if the problem is solvable
		if (!cpFirst.solve()) {
            System.out.println("Model not solved, have some problem.");
		}

		// objective value (print and check if the data items can be offloaded)
		System.out.println("Objective value: " + cpFirst.getObjValue());
		// we want the data items "cannot" be offloaded
		if (cpFirst.getObjValue() == numberOfDG * numberOfDataItemsPerDG) {
			System.out.println("Data can be offloaded");
			return false;
		} else {
			System.out.println("Cannot distribute all data, so calculate min priority.");
			System.out.println();
		}

		/* Start second objective function (Priority) */
		IloLinearNumExpr minCostObj = cpMinCost.linearNumExpr();
		// min priority (Second) objective function
		for (int i = 0; i < dataGens.length; i++) {
			int temp = i + 1;
			String dir = "x0" + temp + "'";
			minCostObj.addTerm(DGpriority.get(i), nameObj.get(dir));
		}

		// write objective function
		cpMinCost.addMaximize(minCostObj);

		cpMinCost.exportModel("MinPriority_" + Lpfilename + ".lp");

		// only see important messages on screen while solving

		// start solving
		// check if the problem is solvable
		if (cpMinCost.solve()) {
			// objective value of min, x, and y
			System.out.println("obj = " + cpMinCost.getObjValue());
			for (Map.Entry<String, IloNumVar> entry : nameObj.entrySet()) {
				cplexresult.put(entry.getKey(), cpMinCost.getValue(entry.getValue()));
			}
		}
		else {
			System.out.println("Model not solved, have some problem");
		}

		System.out.println("Second Objective value: " + cpMinCost.getObjValue());
		// file for verifying transfer path
		StringBuilder dataTpath = new StringBuilder();

		dataTpath.append("Second Obj Value: "+ cpMinCost.getObjValue() + "\n");

		for (Map.Entry<String, IloNumVar> entry : nameObj.entrySet()) {
			dataTpath.append(entry.getKey() + " : " + cpMinCost.getValue(entry.getValue()) + "\n");
		}

//		File PathFile = new File("G:\\downloads\\work\\Dr. Bin\\May\\sensor\\out_files\\Lpout_" + Lpfilename + ".txt");
//		BufferedWriter writer = new BufferedWriter(new FileWriter(PathFile));
//		writer.write(dataTpath.toString());
//		writer.close();

		//getMinFile
		ArrayList<Double> tempp = new ArrayList<>();

		faketotalenergy.addAll(tempp);

		exldata[1] = cpMinCost.getObjValue();
		exldata[3] = (int) cpFirst.getObjValue();

		// clean up memory used
		cpFirst.end();
		cpMinCost.end();

		return true;
	}

    /**
     * original cplex for calculating data resilience level (with gap tolerance) - Quadratic programming
     * @param treeMap: egde cost
     * @param adjacencyList1: adjacent list
     * @param tempclose: closest neighbor
	 * @param nameFirst: cplex column name list for first objective (check if the network can send out all data)
	 * @param nameObj: cplex column name list for true objective
     * @param CpFirst: cplex formulation function for first objective
     * @param CpObj: cplext formulation function for true objective
     * @param storges: storage nodes
     * @param exldata: use to record data
     * @param deadNodes: use to record dead nodes
     * @param faketotalenergy: energy list when report fake cost
     * @param truetotalenergy: energy list when report true cost
     * @param removed: label for generating files (if remove node)
     * @param Lpfilename: label for generating files
	 * @param cplexresult: result path
     * @param gapTolerance: gap tolerance value
     * @return : can solve or not
     * @throws IOException:
     * @throws IloException:
     */
	static boolean calculateLp(Map<String, Link> treeMap, Map<Integer, Set<Integer>> adjacencyList1, HashMap<Integer, List<Integer>> tempclose,
			Map<String, IloNumVar> nameFirst, Map<String, IloNumVar> nameObj, IloCplex CpFirst, IloCplex CpObj, int[] storges, double[] exldata, ArrayList<Integer> deadNodes, ArrayList<Double> faketotalenergy, 
			ArrayList<Double> truetotalenergy, String removed, String Lpfilename, Map<String, Double> cplexresult, double gapTolerance) throws IOException, IloException {
		
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
				constraintsFirst.add(CpFirst.addLe(exprFirst, numberOfDataItemsPerDG));
			} else {
				int temp = numberOfNodes + 1;
				String dir = "x" + i + "''" + temp;
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
        
        //start solving
		// check if the problem is solvable
		if (CpFirst.solve()) {
			System.out.println("CpFirst Success");
		} else {
			System.out.println("Model not solved");
		}
		
		// initial data items to offload
		int[] dataIn = new int[numberOfDG];
		
		Arrays.fill(dataIn, numberOfDataItemsPerDG);
		
        // objective value
        System.out.println("Objective value: " + CpFirst.getObjValue());
        if (CpFirst.getObjValue() < numberOfDG * numberOfDataItemsPerDG) {
        	System.out.println("Can not distribute all data!");
        	// single generator constrains start at index 98

        	for (int i = 1; i <= dataGens.length; i++) {
        		// get generator's value
        		if (CpFirst.getValue(nameFirst.get("x0" + i + "'")) < 99.999) {
        			
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
				constraintsObj.add(CpObj.addEq(exprObj, dataIn[i - 1]));
			} else {
				int temp = numberOfNodes + 1;
				String dir = "x" + i + "''" + temp;
				exprObj.addTerm(1, nameObj.get(dir));
				constraintsObj.add(CpObj.addLe(exprObj, numberOfStoragePerSN));
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
			if (i > numberOfDG) {
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

//		CpObj.exportModel("CPLEXObj_" + Lpfilename + ".lp");
        
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
		
//        File PathFile = new File("G:\\downloads\\eclipseJava-workspace\\SensorNetwork\\Lp_output\\Lpout_" + Lpfilename + ".txt");
//        BufferedWriter writer = new BufferedWriter(new FileWriter(PathFile));
//        writer.write(dataTpath.toString());
//        writer.close();
        
        ArrayList<Double> tempp = new ArrayList<>();
        
        // calculate the true cost
        if (removed.charAt(0) == 'c' && Lpfilename.charAt(0) == 'c') {
        	Map<String, Link> originaltreeMap = new TreeMap<String, Link>(linkstest);
        	tempp = getDeadNodes(originaltreeMap, tempclose, CpObj, nameObj, storges, exldata, deadNodes, "O" + removed);
			truetotalenergy.addAll(tempp);
            tempp.clear();
        }
        // calculate the fake cost or remove cost
        tempp = getDeadNodes(treeMap, tempclose, CpObj, nameObj, storges, exldata, deadNodes, removed);

		faketotalenergy.addAll(tempp);
        
        exldata[1] = CpObj.getObjValue();
        exldata[3] = (int) CpFirst.getObjValue();
        
        // clean up memory used
        CpFirst.end();
        CpObj.end();
        
        return true;
    }

    /**
	 * This method used to generate data to compare two different constraints when observing fault tolerance level
	 * (Quadratic programming)
	 * @param treeMap: egde cost
	 * @param adjacencyList1: adjacent list
	 * @param tempclose: closest neighbor
	 * @param nameFirst: cplex column name list for first objective (check if the network can send out all data)
	 * @param nameObj: cplex column name list for true objective
	 * @param CpFirst: cplex formulation function for first objective
	 * @param CpObj: cplext formulation function for true objective
	 * @param storges: storage nodes
	 * @param exldata: use to record data
	 * @param deadNodes: use to record dead nodes
	 * @param faketotalenergy: energy list when report fake cost
	 * @param truetotalenergy: energy list when report true cost
	 * @param removed: label for generating files (if remove node)
	 * @param Lpfilename: label for generating files
	 * @param cplexresult: result path
	 * @param gapTolerance: gap tolerance value
	 * @return : can solve or not
     * @throws IOException:
     */
    static boolean calculateDeadNode(Map<String, Link> treeMap, Map<Integer, Set<Integer>> adjacencyList1, HashMap<Integer, List<Integer>> tempclose,
                                   Map<String, IloNumVar> nameFirst, Map<String, IloNumVar> nameObj, IloCplex CpFirst, IloCplex CpObj, int[] storges, double[] exldata, ArrayList<Integer> deadNodes, ArrayList<Double> faketotalenergy,
                                   ArrayList<Double> truetotalenergy, String removed, String Lpfilename, Map<String, Double> cplexresult, double gapTolerance) throws IOException {
        try {
            List<IloRange> constraintsFirst = new ArrayList<IloRange>();
            List<IloRange> constraintsObj = new ArrayList<IloRange>();

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
                    constraintsFirst.add(CpFirst.addLe(exprFirst, numberOfDataItemsPerDG));
                } else {
                    int temp = numberOfNodes + 1;
                    String dir = "x" + i + "''" + temp;
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

            //start solving
            // check if the problem is solvable
            if (CpFirst.solve()) {
                System.out.println("CpFirst Success");
            } else {
                System.out.println("Model not solved");
            }

            // initial data items to offload
            int[] dataIn = new int[numberOfDG];

            Arrays.fill(dataIn, numberOfDataItemsPerDG);

            // objective value
            System.out.println("Objective value: " + CpFirst.getObjValue());
            if (CpFirst.getObjValue() < numberOfDG * numberOfDataItemsPerDG) {
                System.out.println("Can not distribute all data!");
                // single generator constrains start at index 98

                for (int i = 1; i <= dataGens.length; i++) {
                    // get generator's value
                    if (CpFirst.getValue(nameFirst.get("x0" + i + "'")) < 99.999) {

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
                    constraintsObj.add(CpObj.addEq(exprObj, dataIn[i - 1]));
                } else {
                    int temp = numberOfNodes + 1;
                    String dir = "x" + i + "''" + temp;
                    exprObj.addTerm(1, nameObj.get(dir));
                    constraintsObj.add(CpObj.addLe(exprObj, numberOfStoragePerSN));
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
                if (i > numberOfDG) {
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

            // ------------------ set parameters for the computation --------------------
            CpObj.setParam(IloCplex.Param.OptimalityTarget, IloCplex.OptimalityTarget.OptimalGlobal);
            if (gapTolerance != 0) {
                CpObj.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, gapTolerance);
            }
            CpObj.setParam(IloCplex.Param.MIP.Strategy.Branch, -1);
            CpObj.setParam(IloCplex.Param.Simplex.Tolerances.Markowitz, 0.1);
            CpObj.setParam(IloCplex.Param.Emphasis.MIP, 0); // 1: Emphasize feasibility over optimality
            CpObj.setParam(IloCplex.Param.MIP.Display, 4);
            // this part eliminate MIP issue
            CpObj.setParam(IloCplex.Param.Preprocessing.Linear, 0);
            CpObj.setParam(IloCplex.Param.Emphasis.Numerical, true); // Emphasizes precision in numerically unstable or difficult problems.
            CpObj.setParam(IloCplex.Param.Read.Scale, 1); // More aggressive scaling
//		    CpObj.setParam(IloCplex.Param.MIP.Strategy.Dive, 2);

//          CpObj.exportModel("CPLEXObj_" + Lpfilename + ".lp");

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
            // file for path
            StringBuilder dataTpath = new StringBuilder();

            dataTpath.append("Second Obj Value: " + CpObj.getObjValue() + "\n");
            //      double[] tempresult1 = lpObj.getPtrVariables();

            for (Map.Entry<String, IloNumVar> entry : nameObj.entrySet()) {
                dataTpath.append(entry.getKey() + " : " + CpObj.getValue(entry.getValue()) + "\n");
            }

//        File PathFile = new File("G:\\downloads\\eclipseJava-workspace\\SensorNetwork\\Lp_output\\Lpout_" + Lpfilename + ".txt");
//        BufferedWriter writer = new BufferedWriter(new FileWriter(PathFile));
//        writer.write(dataTpath.toString());
//        writer.close();

            ArrayList<Double> tempp = new ArrayList<>();

            // calculate the true cost
            if (removed.charAt(0) == 'c' && Lpfilename.charAt(0) == 'c') {
                Map<String, Link> originaltreeMap = new TreeMap<String, Link>(linkstest);
                tempp = getDeadNodes(originaltreeMap, tempclose, CpObj, nameObj, storges, exldata, deadNodes, "O" + removed);
				truetotalenergy.addAll(tempp);
                tempp.clear();
            }
            // calculate the fake cost or remove cost
            tempp = getDeadNodes(treeMap, tempclose, CpObj, nameObj, storges, exldata, deadNodes, removed);

			faketotalenergy.addAll(tempp);

            exldata[1] = CpObj.getObjValue();
            exldata[3] = (int) CpFirst.getObjValue();

            // clean up memory used
            CpFirst.end();
            CpObj.end();

            return true;
        } catch (IloException e) {
            System.out.println("QP Error");
            return false;
        }
	}

	/**
	 * use to calculate dead nodes and generate detail information in txt file
	 * @param treeMap: TreeMap contains the cost of each edge use to calculate total cost
	 * @param tempclose: closest neighbor of each node
	 * @param CpIn: current Cplex function which we are running
	 * @param cpresult: String = link between nodes, IloNumVar use to get value
	 * @param storages: storage node's information
	 * @param exldata: use to record data
	 * @param deadNodes: use to record dead node's list
	 * @param removed: use to label generated file
	 * @return : detail dead nodes' information (data item's received amount)
	 * @throws IOException:
	 * @throws UnknownObjectException:
	 * @throws IloException:
	 */
    static ArrayList<Double> getDeadNodes(Map<String, Link> treeMap, HashMap<Integer, List<Integer>> tempclose, IloCplex CpIn,
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
		int targetnode = removed.equals("original") ? 0 : Integer.parseInt(removed.replaceAll("[^\\d.]", ""));

        // calculate target node's energy cost
		for (List<Double> re : res) {
			int transNode = (int) Math.floor(re.get(0));
			int disNode = (int) Math.floor(re.get(1));
			double items = re.get(2);

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
				double totalTcost = (treeMap.get("(" + transNode + ", " + disNode + ")").getTCost() - treeMap.get("(" + transNode + ", " + disNode + ")").getRCost()) * items;
				double totalRcost = treeMap.get("(" + transNode + ", " + disNode + ")").getRCost() * items;

				// when in is our target node
				if (transNode == targetnode && targetnode != 0) {
					// cost for receive + out
					double inoutcost = treeMap.get("(" + transNode + ", " + disNode + ")").getTCost() * items;
					// still have energy
					if (tempenergy + inoutcost < minCapacity) {
						tempenergy += inoutcost;
						tempcapa += items;
					} else { // no energy
						double remainenergy = minCapacity - tempenergy;
						double transfercost = treeMap.get("(" + transNode + ", " + disNode + ")").getTCost();
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

        totaldataitems.clear();
        return back;
    }

	/* -------------------------------- Edge cost formulation -------------------------------- */
	// receive and save cost
    double getRSCost(double l){
        final int K = 512; // k = 512B (from paper0)
        final double E_elec = 100 * Math.pow(10,-9); // E_elec = 100nJ/bit (from paper1)
        double Erx = 8 * E_elec * K; // Receiving energy consumption assume is same as saving
        return Erx*1000; // make it milli J now for better number visualization during calculation
    }
    
    // transfer cost
    static double getTCost(double l) {
        final int K = 512; // k = 512B (from paper0)
        final double E_elec = 100 * Math.pow(10,-9); // E_elec = 100nJ/bit (from paper1)
        final double Epsilon_amp = 100 * Math.pow(10,-12); // Epsilon_amp = 100 pJ/bit/squared(m) (from paper1)
        double Etx = E_elec * K * 8 + Epsilon_amp * K * 8 * l * l; //
        return Math.round(Etx*1000*10000)/10000.0; // make it milli J now for better number visualization during calculation
    }

	/**
	 * Check if the graph is connected and generate the graph
	 * @param width:
	 * @param height:
	 * @param adjList: adjacent list of each node
	 */
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

		// Draw first sensor network graph
		SensorNetworkGraph graph = new SensorNetworkGraph(dataGens);
		graph.setGraphWidth(width);
		graph.setGraphHeight(height);
		graph.setNodes(nodes);
		graph.setAdjList(adjList);
		graph.setPreferredSize(new Dimension(960, 800));
		Thread graphThread = new Thread(graph);
		graphThread.start();
		
	}

	/**
	 * Check if the graph is bi-connect connected (remove node one by one)
	 * @param width:
	 * @param height:
	 *              - only use above parameter if we need to generate graph
	 * @param adjList: adjacent list of each node
	 */
	void executeDepthFirstSearchAlgbi(double width, double height, Map<Integer, Set<Integer>> adjList) {
		//these have to be clear since they already have elements and values after running the algorithm
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
			biconnectcounter = biconnectcounter + 1;
			System.out.println("Graph is not fully connected");
		}
	}

	/**
	 * recursiveDFS use to check connection
	 * @param u:
	 * @param connectedNode:
	 * @param adjList: adjacent list
	 */
	void recursiveDFS(int u, Set<Integer> connectedNode, Map<Integer, Set<Integer>> adjList) {
		if(!s.contains(u) && !explored.containsKey(u)) {
			s.add(u);
			discovered.put(u, true);
		}
		
		while(!s.isEmpty()) {
			if(!explored.containsKey(u)) {
				List<Integer> list = new ArrayList<>(adjList.get(u));
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

    /**
     * generate nodes
     * @param nodeCount: node's amount
     * @param width:
     * @param height:
     */
	static void populateNodes(int nodeCount, double width, double height) {
		// if user want to fix the graphic, enter a number in Random()
		Random random = new Random();
		
		for(int i = 1; i <= nodeCount; i++) {
			Axis axis = new Axis();
			int scale = (int) Math.pow(10, 1);
			double xAxis =(0 + random.nextDouble() * (width - 0));
			double yAxis = 0 + random.nextDouble() * (height - 0);
			int capa = random.nextInt(10) + 1;
			
			xAxis = Math.floor(xAxis * scale) / scale;
			yAxis = Math.floor(yAxis * scale) / scale;
			
			axis.setxAxis(xAxis);
			axis.setyAxis(yAxis);
			axis.setcapa(capa); //each nodes energy capacity
			
			nodes.put(i, axis);	
		}
	}

    /**
     * create nodes from file
     * @param file: see attached input file for formate
     * @throws IOException:
     */
	static void readfileNodes(File file) throws IOException {
		// if user want to fix the graphic, enter a number in Random()
		Random random = new Random();
		// original 1312
		Scanner scan = new Scanner(System.in);
		System.out.println("Please enter the energy capacity:");
		minCapacity = scan.nextInt(); //max energy 
		scan.close();
		
		FileReader fileReader = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line;
		
		while ((line = bufferedReader.readLine()) != null) {
			Axis axis = new Axis();
			String[] words = line.split("	");
			double xAxis = Double.parseDouble(words[1]);
			double yAxis =Double.parseDouble(words[2]);
			
			axis.setxAxis(xAxis);
			axis.setyAxis(yAxis);
			axis.setcapa(minCapacity); //each nodes energy capacity
			
			nodes.put(Integer.parseInt(words[0]) + 1, axis);
		}
		fileReader.close();
	}

    /**
     * create node's edge (link) information
     * @param nodeCount: node's amount
     * @param tr: transfer range (upper bound)
     * @param adjList: use to record adjacent node
     */
	void populateAdjacencyList(int nodeCount, int tr, Map<Integer, Set<Integer>> adjList) {
		for(int i = 1; i <= nodeCount; i++) {
			adjList.put(i, new HashSet<>());
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
					linkstest.put("(" + node2 + ", " + node1 + ")", new Link(new Edge(node2, node1, 0), distance, getRSCost(distance), getTCost(distance), getRSCost(distance), energy));
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
                        links.put("(" + node2 + ", " + node1 + ")", new Link(new Edge(node2, node1, 1), distance, getRSCost(distance), getTCost(distance), getRSCost(distance), energy));
					} else {
                    	links.put("(" + node1 + ", " + node2 + ")", new Link(new Edge(node1, node2, 1), distance, getRSCost(distance), getTCost(distance), getRSCost(distance), energy));
					}
				}
			}
		}
	}

    /**
	 * Generate connection graph when removing node
	 * similar as populateAdjacencyList but the number of nodes are different
     * @param removeconter: target removed node's label
	 * @param nodeCount: node's amount
	 * @param tr: transfer range (upper bound)
	 * @param adjList: use to record adjacent node
     */
	void checkbiconnect(int removeconter, int nodeCount, int tr, Map<Integer, Set<Integer>> adjList) {
		int j = 1;
		for(int i=1; i < nodeCount; i++) {
			if (j != removeconter) {
				adjList.put(j, new HashSet<>());
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
						links2.put("(" + node2 + ", " + node1 + ")", new Link(new Edge(node2, node1, 1), distance, getRSCost(distance), getTCost(distance), getRSCost(distance), energy));
                    } else {
                    	links2.put("(" + node1 + ", " + node2 + ")", new Link(new Edge(node1, node2, 1), distance, getRSCost(distance), getTCost(distance), getRSCost(distance), energy));
                    }
				}
			}
		}
	}
}
