# Project Description
This project is used to simulate the results for data resilience problems.
We propose a quadratic programming-based algorithm and two time-efficient heuristics based on different network metrics and compare their efficiency.
- Quadratic programming solution.
- Network-Based Algorithm.
- Minimum-Cost-Flow (MCF)-Based Algorithm.

Users can run the following methods to observe the performance between the three algorithms.
- observe Cplex's gap tolerance's influence on Quadratic programming's result.
- data resilience problem: change the storage node's storage, and fix the data item's amount.
- data resilience problem: fix the storage node's storage, and change the data item's amount.
- data resilience problem: observe the fault tolerance level for each algorithm.

# How to Run
The following external libraries are required to run this program:
- Apache XSSF: https://poi.apache.org/components/spreadsheet/
- IBM ILOG CPLEX Optimization: https://www.ibm.com/products/ilog-cplex-optimization-studio

Users will need to input basic information of the sensor network such as width, height, numbers of data generators, numbers of storage nodes, and the amount of overflow data.

1. Download the required libraries.
2. Place the libraries (.jar files) into the project.
3. Run SensorNetworkCPLX.java and follow the program steps to input the network's information.
- Input the required network parameters.
<img src="https://github.com/shsu606015/SensorNetwork/blob/master/img/paramInput.png" width="500"/>

- The program will randomly generate nodes and provide the edge information automatically. (Note: if the graph is not connected, please run the program again)
<img src="https://github.com/shsu606015/SensorNetwork/blob/master/img/graph.png" width="500"/>
<img src="https://github.com/shsu606015/SensorNetwork/blob/master/img/connection_edge.png" width="800"/>

- Select a method that you like to run. (Note: Methods may required another input parameter)
<img src="https://github.com/shsu606015/SensorNetwork/blob/master/img/select_method.png" width="900"/>

- The program will start sovling until it gets all the results. (Note: "Finish" will appear at the program log when the program finishes)
<img src="https://github.com/shsu606015/SensorNetwork/blob/master/img/solving.png" width="600"/>

- An output file "data.xlsx" that contains the results will be created at the project folder.
<img src="https://github.com/shsu606015/SensorNetwork/blob/master/img/output_file.png" width="400"/>

# Researchers
1. Shang-Lin Hsu
2. Yu-Ning Yu
3. <a href="http://csc.csudh.edu/btang/"> Dr. Bin Tang </a>
