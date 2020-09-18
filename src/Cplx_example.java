import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ilog.concert.*;
import ilog.cplex.*;


public class Cplx_example {

	public static void main(String[] args) throws IloException {
		// TODO Auto-generated method stub
		IloCplex cplex = new IloCplex();
		Map<String, IloNumVar> map = mymap(cplex);
		example(cplex, map);
	}
	
	public static void example2() throws IloException {
		IloCplex cplex = new IloCplex();
		
		// define variables (x and y) also their non-binding constraints
		// cplex.numVar(min, max , name);
		IloNumVar x1 = cplex.numVar(0, Double.MAX_VALUE, "x1");
		IloNumVar x2 = cplex.numVar(0, Double.MAX_VALUE, "x2");
		IloNumVar x3 = cplex.numVar(0, Double.MAX_VALUE, "x3");
		
		// objective 
		cplex.addMinimize(
				// ***max 8 parameters for each sum argument
				cplex.sum(
						x1,
						cplex.prod(2, x2),
						cplex.prod(3, x3),
						cplex.prod(1, x1, x1), // x1^2
						cplex.prod(-16.5, x1, x1),
						cplex.prod(-16.5, x1, x1),
						cplex.prod(-16.5, x1, x1),
						cplex.sum(
								cplex.prod(-16.5, x1, x1),
								cplex.prod(-16.5, x1, x1),
								cplex.prod(-16.5, x1, x1),
								cplex.prod(-16.5, x1, x1)
								)
						)
				
		);
		
	}
	
	public static Map<String, IloNumVar> mymap(IloCplex cplex ) throws IloException {
		Map<String, IloNumVar> map = new HashMap<>();
		IloNumVar x = cplex.numVar(0, Double.MAX_VALUE, "x");
		IloNumVar y = cplex.numVar(0, Double.MAX_VALUE, "y");
		map.put("x", x);
		map.put("y", y);
		return map;
	}
	
	public static void example(IloCplex cplex , Map<String, IloNumVar> map) throws IloException {
		
		// define variables (x and y) also their non-binding constraints
		// cplex.numVar(min, max , name);

		/************************ basic LP **********************/
		// expressions
		// 0.12x + 0.15y
		IloLinearNumExpr objective = cplex.linearNumExpr();
		objective.addTerm(0.12, map.get("x"));
		objective.addTerm(0.15, map.get("y"));
		// define objective
		// objective: min 0.12x + 0.15y
		cplex.addMaximize(cplex.prod(60, cplex.sum(map.get("x"), map.get("y"))));

		// define constraints
		// 1. 60x + 60y >= 300
		// 2. 12x + 6y >= 36
		// 3. 10x + 30y >= 90
		List<IloRange> constraints = new ArrayList<IloRange>();
		IloLinearNumExpr num_expr = cplex.linearNumExpr();
		num_expr.addTerm(60, map.get("x"));
		num_expr.addTerm(60, map.get("y"));

//		cplex.addMaximize(cplex.addGe(map.get("x"), map.get("y")));

		constraints.add(cplex.addGe(num_expr, 300));
		
		num_expr = cplex.linearNumExpr();
		num_expr.addTerm(10, map.get("x"));
		num_expr.addTerm(30, map.get("y"));
		constraints.add(cplex.addGe(num_expr, 90));
		
		num_expr = cplex.linearNumExpr();
		num_expr.addTerm(12, map.get("x"));
		num_expr.addTerm(6, map.get("y"));
		constraints.add(cplex.addGe(num_expr, 36));
		
		//constraints.add(cplex.addGe(cplex.sum(cplex.prod(12, map.get("x")),cplex.prod(6, map.get("y"))), 36));
		//constraints.add(cplex.addGe(cplex.sum(cplex.prod(10, map.get("x")),cplex.prod(30, map.get("y"))), 90));
		IloNumExpr test = cplex.numExpr();
		num_expr = cplex.linearNumExpr();
		num_expr.addTerm(10, map.get("x"));
		num_expr.addTerm(30, map.get("y"));
		test = cplex.prod(-2, map.get("x"), map.get("x"));
		constraints.add(cplex.addGe(test, 50));
		// constraints
		IloNumVar[] receivelist = new IloNumVar[5];
		receivelist[0] = cplex.numVar(0, Double.MAX_VALUE, "A");
		receivelist[1] = cplex.numVar(0, Double.MAX_VALUE, "B");
		receivelist[2] = cplex.numVar(0, Double.MAX_VALUE, "C");
		receivelist[3] = cplex.numVar(0, Double.MAX_VALUE, "D");
		receivelist[4] = cplex.numVar(0, Double.MAX_VALUE, "E");
		double[] du = new double[receivelist.length];
		du[0] = 1;
		du[1] = 2;
		du[2] = 3;
		du[3] = 4;
		du[4] = 5;
		num_expr = cplex.linearNumExpr();
		num_expr.addTerms(du, receivelist);
		constraints.add(cplex.addEq(num_expr, 0));
		// 2x - y = 0
		num_expr = cplex.linearNumExpr();
		IloNumVar element = map.get("x");
		IloNumVar element2 = map.get("y");
		num_expr = cplex.linearNumExpr();
		num_expr.addTerm(2, element);
		num_expr.addTerm(-1, element2);
		constraints.add(cplex.addEq(num_expr, 0));
		// clear
		// y - x <= 8
		num_expr = cplex.linearNumExpr();
		num_expr.addTerm(1,element2);
		num_expr.addTerm(-1,element);
		constraints.add(cplex.addLe(num_expr,8));

		for (int i = 0; i < constraints.size(); i++) {
			System.out.println(constraints.get(i).toString());
		}
		// display option
		//cplex.setParam(IloCplex.Param.Simplex.Display, 0);
		
		// start solving
		// return true if solvable
		if (cplex.solve()) {
			// objective value of min, x, and y
			System.out.println("obj = " + cplex.getObjValue());
			System.out.println("x   = " + cplex.getValue(map.get("x")));
			System.out.println("y   = " + cplex.getValue(map.get("y")));
//			for (int i = 0; i < constraints.size(); i++) {
//				System.out.println("dual constraint (shadow price)" + (i + 1)+ "  = " + cplex.getDual(constraints.get(i)));
//				System.out.println("slack constraint (surplus)" + (i + 1)+ " = " + cplex.getSlack(constraints.get(i)));
//			}
			
			for (int i = 0; i < constraints.size(); i++) {
				System.out.println(constraints.get(i).toString());
			}
		}
		else {
			System.out.println("Model not solved");
		}
		
		cplex.end();
	}
}
