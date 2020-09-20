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
3. Run SensorNetworkCPLX.java and follow the program to input the network's information.

# Researchers
1. Shang-Lin Hsu
2. Yu-Ning Yu
3.<a href="http://csc.csudh.edu/btang/"> Dr. Bin Tang </a>
