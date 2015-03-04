/*******************************************************************************
 * Copyright (c) 2013-2014 Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza - initial API and implementation
 ******************************************************************************/

package com.tejas.workspace.examples.netDesignAlgorithm.fa;

import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.CandidatePathList;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.Triple;
import com.tejas.engine.utils.Constants.OrderingType;
import com.tejas.engine.utils.Constants.SearchType;

/**
 * Given a set of nodes and links, with their capacities, an offered traffic, and a set of admissible paths for each demand (given by the k-shortest paths
 * in km for each demand, being k a user-defined parameter), this algorithm uses an evolutionary algorithm metaheuristic to find for each demand two
 * link-disjoint routes: a primary route, and a backup route used for restoration. That is, the backup route is pre-computed, but it carries not traffic
 * unless the primary route fails. The optimization target is minimizing the average between the network congestion in the non-failure state, and the
 * worse network congestion among the single link failures. For each demand, the candidate link-disjoint candidate pairs are computed as all the
 * link disjoint pairs p1,p2, such that p1 and p2 are admissible paths. Two solutions are considered neighbors if they differ in the routing of one demand
 * (either primary path, backup path or both). A solution is codified as in int vector, with one coordinate per demand, containing the identifier of the
 * link-disjoint route pair for that demand. The population size, the number of generations and the offspring size are used-defined parameters.
 * In each generation, a user-defined fraction of the parents are selected randomly, while the rest are the top ones. Cross-over is uniform: for each demand
 * the routing of a randomly chosen parent passes to the child. Children are mutated in one demand randomly chosen. The top solutions among parents and
 * offspring make it to the next generation. The evolutionary core is implemented in a separated internal class, for didactic purposes.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, May 2013
 */
public class FA_EA_minCongestionPrecomputedRestoration implements IAlgorithm
{
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
	/* Initialize some variables */
	final int N = netPlan.getNumberOfNodes();
	final int D = netPlan.getNumberOfDemands();
	final int E = netPlan.getNumberOfLinks();

	/* Basic checks */
	if (N == 0 || E == 0 || D == 0) throw new Net2PlanException("This algorithm requires a topology with links and a demand set");

	final double [] h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
	final double [] u_e = netPlan.getLinkCapacityInErlangsVector();

	final int k = Integer.parseInt(algorithmParameters.get("k")); // number of loopless candidate paths per demand
	final int ea_numberIterations = Integer.parseInt(algorithmParameters.get("ea_numberIterations"));
	final int ea_populationSize = Integer.parseInt(algorithmParameters.get("ea_populationSize"));
	final int ea_offSpringSize = Integer.parseInt(algorithmParameters.get("ea_offSpringSize"));
	final double ea_fractionParentsChosenRandomly = Double.parseDouble(algorithmParameters.get("ea_fractionParentsChosenRandomly"));
	if (ea_offSpringSize > ea_populationSize) throw new Net2PlanException("The offspring size cannot exceed the population size divided by two");

	/* Construct the candidate path list */
	CandidatePathList cpl = new CandidatePathList(netPlan , netPlan.getLinkLengthInKmVector() , "K" , Integer.toString(k));
	/* For each path, compute those of the same demand which are link disjoint */
	List<List<int[]>> candidate11PairsList_d = computeCandidate11PairList(netPlan, cpl);

	EvolutionaryAlgorithmCore gac = new EvolutionaryAlgorithmCore(u_e, h_d, candidate11PairsList_d, cpl, ea_numberIterations, ea_populationSize, ea_fractionParentsChosenRandomly, ea_offSpringSize);
	gac.evolve();

	/* Save the best solution found into the netPlan object */
	netPlan.removeAllRoutes();
	netPlan.removeAllProtectionSegments();
	for (int d = 0 ; d < D ; d ++)
	{
	    final int bestPairThisDemand = gac.best_solution[d];
	    final int [] primaryAndBackupPath = candidate11PairsList_d.get(d).get(bestPairThisDemand);
	    final int [] primarySeqLinks = cpl.getSequenceOfLinks(primaryAndBackupPath [0]);
	    final int [] backupSeqLinks = cpl.getSequenceOfLinks(primaryAndBackupPath [1]);

	    final int newProtectionSegment = netPlan.addProtectionSegment(backupSeqLinks, 0 , null);
	    netPlan.addRoute(d, h_d [d], primarySeqLinks , new int [] { newProtectionSegment } , null);
	}

	return "Ok! Cost : " + gac.best_cost;
    }

    @Override
    public String getDescription()
    {
	return "Given a set of nodes and links, with their capacities, an offered traffic, and a set of admissible paths for each demand (given by" +
	    " the k-shortest paths in km for each demand, being k a user-defined parameter), this algorithm uses an evolutionary algorithm " +
	    "metaheuristic to find for each demand two link-disjoint routes: a primary route, and a backup route used for restoration. That is, " +
	    "the backup route is pre-computed, but it carries not traffic unless the primary route fails. The optimization target is minimizing the average " +
	    "between the network congestion in the non-failure state, and the worse network congestion among the single link failures. For each demand, " +
	    "the candidate link-disjoint candidate pairs are computed as all the link disjoint pairs p1,p2, such that p1 and p2 are admissible paths. " +
	    "Two solutions are considered neighbors if they differ in the routing of one demand (either primary path, backup path or both). A solution is " +
	    "codified as in int vector, with one coordinate per demand, containing the identifier of the link-disjoint route pair for that demand. " +
	    "The population size, the number of generations and the offspring size are used-defined parameters. In each generation, a user-defined fraction" +
	    "of the parents are selected randomly, while the rest are the top ones. Cross-over is uniform: for each demand the routing of a randomly " +
	    "chosen parent passes to the child. Children are mutated in one demand randomly chosen. The top solutions among parents and offspring make it " +
	    "to the next generation. The evolutionary core is implemented in a separated internal class, for didactic purposes.";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
	List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
	algorithmParameters.add(Triple.of("k", "3", "Number of candidate paths per demand, used as a source to compute the 1:1 pairs"));
	algorithmParameters.add(Triple.of("ea_numberIterations", "100", "Number of iterations (one per generation)"));
	algorithmParameters.add(Triple.of("ea_populationSize", "1000", "Number of elements in the population"));
	algorithmParameters.add(Triple.of("ea_offSpringSize", "500", "Number of childs in the offspring every generation"));
	algorithmParameters.add(Triple.of("ea_fractionParentsChosenRandomly", "0.5", "Fraction of the parents that are selected randomly for creating the offspring"));
	return algorithmParameters;
    }

    /* From a candidate path list, extract for each demand, the list of link-disjoint path pairs. There pairs are candidates for becoming primary-backup
     * path pairs for the demand.  */
    private List<List<int []>> computeCandidate11PairList (NetPlan netPlan , CandidatePathList cpl)
    {
	final int D = netPlan.getNumberOfDemands();
	List<List<int[]>> cpl11 = new ArrayList<List<int[]>>(D);

	for (int d = 0 ; d < D ; d ++)
	{
	    cpl11.add(new ArrayList<int []> ());
	    int [] pIds = cpl.getPathsPerDemand(d);
	    for (int contP_1 = 0 ; contP_1 < pIds.length - 1 ; contP_1 ++)
	    {
		final int p1 = pIds [contP_1];
		final int [] seqLinks_1 = cpl.getSequenceOfLinks(p1);
		for (int contP_2 = 0 ; contP_2 < pIds.length ; contP_2 ++)
		{
		    if (contP_1 == contP_2) continue;

		    final int p2 = pIds [contP_2];
		    final int [] seqLinks_2 = cpl.getSequenceOfLinks(p2);
		    if (IntUtils.intersect(seqLinks_1 , seqLinks_2).length == 0) cpl11.get(d).add (new int [] { p1 , p2 } );
		    //System.out.println("Candidate link disjoint pair is " + cpl11.toString());
		}
	    }

	    if (cpl11.get(d).isEmpty()) throw new Net2PlanException("Demand " + d + " has no two link-disjoint paths");
	}

	return cpl11;
    }

    /**
     * This class implements the core operations of the genetic algorithm to solve the network design problem. The problem consists of determining, for each
     * network demand, one primary and backup path that are link disjoint. The backup path is assumed to carry no traffic, but when the primary fails.
     * The optimization target is minimizing the average of two components. First, is the network congestion when no failure occurs in the network. The second is
     * the worst network congestion when ONE link fails. With network congestion we mean the utilization in the bottleneck link.
     * For each demand, we receive the set of admissible link-disjoint pairs to the problem. The problem solution is coded as an array with as many
     * elements as demands. The d-th element, corresponds to the identifier of the 1:1 pair used as primary-backup for demand d.
     *
     * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
     * @version 1.0, May 2013
     */
    public static class EvolutionaryAlgorithmCore
    {
	private final double [] u_e;
	private final double [] h_d;
	private final List<List<int []>> potential11Pairs_d;
	private final CandidatePathList cpl;
	private final int E;
	private final int D;
	private final int ea_numberIterations;
	private final int ea_populationSize;
	private final double ea_fractionChosenRandomly;
	private final int ea_offspringSize;
	private Random rnd;

	private List<int []> population;
	private double [] costs;

        private double best_cost;
        private int [] best_solution;

	/**
         * Initialize the object with the problem data and the genetic algorithm specific parameters.
         *
         * @param u_e
         * @param h_d
         * @param potential11Pairs_d
         * @param cpl
         * @param ea_numberIterations
         * @param ea_populationSize
         * @param ea_fractionParentsChosenRandomly
         * @param ea_offSpringSize
         */
        public EvolutionaryAlgorithmCore (double[] u_e , double [] h_d , List<List<int []>> potential11Pairs_d , CandidatePathList cpl , int ea_numberIterations , int ea_populationSize , double ea_fractionParentsChosenRandomly , int ea_offSpringSize)
	{
	    this.u_e = DoubleUtils.copy(u_e);
	    this.h_d = DoubleUtils.copy(h_d);
	    this.E = u_e.length;
	    this.D = h_d.length;
	    this.potential11Pairs_d = potential11Pairs_d;
	    this.cpl = cpl;
	    this.ea_numberIterations = ea_numberIterations;
	    this.ea_populationSize = ea_populationSize;
	    this.ea_fractionChosenRandomly = ea_fractionParentsChosenRandomly;
	    this.ea_offspringSize = ea_offSpringSize;
	    this.rnd = new Random();
	}

	/* Computes the cost of a given solution */
	private double computeCostSolution (int [] solution_d)
	{
	    /* Compute the congestion when there are no failures */
	    double [] y_e = new double [E];
	    for (int d = 0 ; d < D ; d ++)
	    {
		    final int [] pathPair = potential11Pairs_d.get(d).get(solution_d [d]);
		    for (int e : cpl.getSequenceOfLinks(pathPair [0]))
			    y_e [e] += h_d [d];
	    }

	    double congestion_noFailure = DoubleUtils.maxValue(DoubleUtils.divide(y_e, u_e));
	    double [] congestion_s = new double [E + 1]; // congestion when fails link s

	    /* Compute the congestion for the single link failures */
	    for (int s = 0 ; s < E ; s ++)
	    {
		y_e = new double [E];
		for (int d = 0 ; d < D ; d ++)
		{
		    final int [] pathPair = potential11Pairs_d.get(d).get(solution_d [d]);
		    final int primaryPath = pathPair [0];
		    final int backupPath = pathPair [1];
		    final int usedPath = (cpl.linkBelongsToPath(s, primaryPath))? backupPath : primaryPath;
		    for (int e : cpl.getSequenceOfLinks(usedPath))
			    y_e [e] += h_d [d];
		}

		congestion_s [s] = DoubleUtils.maxValue(DoubleUtils.divide(y_e, u_e));
	    }

	    return 0.5 * congestion_noFailure + 0.5 * DoubleUtils.maxValue(congestion_s);
	}

	/*
	 * Generates an initial population of solutions.
	 * Each solution is generated choosing randomly for each demand, one of the admissible 1:1 pairs
	 */
	private void generateInitialSolutions()
	{
		this.population = new ArrayList<int []> (ea_populationSize);
		this.costs = new double [ea_populationSize];

		for (int cont = 0 ; cont < ea_populationSize ; cont ++)
		{
			int [] solution_d = new int [D];
			for (int d = 0 ; d < D ; d ++)
				solution_d [d] = rnd.nextInt(this.potential11Pairs_d.get(d).size());
			final double solutionCost =  computeCostSolution (solution_d);
			population.add (solution_d);
			costs [cont] = solutionCost;
		}
	}

	/* Selects from the population ea_offspringSize*2 elements that will become parents, generating the offspring.
	 * A fraction of the parents (1-ea_fractionChosenRandomly) is chosen from the best in the population.
	 * A fraction ea_fractionChosenRandomly is chosen randomly from all the population, so that repetitions can occur */
	private LinkedList<Integer> operator_parentSelection()
	{
	    final int numParentsToChoose = 2 * ea_offspringSize;
	    final int numParentsChosenRandomly = (int) Math.floor(numParentsToChoose * ea_fractionChosenRandomly);
	    final int numParentsChosenFromCost = numParentsToChoose - numParentsChosenRandomly;

	    /* Initialize the list to be returned */
	    LinkedList<Integer> chosenParents = new LinkedList <Integer> ();

	    /* Sorts the existing population: first the best cost */
	    sortAscending(this.population, this.costs);

	    /* Choose the best solutions as parents: as many as numParentsChosenFromCost */
	    for (int cont = 0 ; cont < numParentsChosenFromCost ; cont ++)
		    chosenParents.add (cont);

	    /* The rest of the parents (numParentsChosenRandomly) are chosen randomly among the non-selected. Parents can be repeated */
	    for (int cont = 0 ; cont < numParentsChosenRandomly ; cont ++)
		    chosenParents.add (rnd.nextInt(this.population.size()));

	    return chosenParents;
	}

	/* Receive a set of parents of size ea_offspringSize*2. From them, generates an offspring of size ea_offspringSize. Couples are generated randomly,
	 * each parent appears in only one couple. Given two parents, the descendant is created by, for each demand, picking from any parent (chosen randomly),
	 * the 1:1 pair to carry the traffic. */
	private ArrayList<int []> operator_crossover (LinkedList<Integer> parents)
	{
		/* The offspring to be returned */
		ArrayList<int []> offspring = new ArrayList<int []> (parents.size() / 2);

		/* Shuffle randomly the parent selection. Then, couples are formed randomly */
		Collections.shuffle(parents);

		/* Two consecutive parents are a couple */
		while (parents.size() >= 2)
		{
			final int firstParentId = parents.poll ();
			final int secondParentId = parents.poll ();

			final int [] firstParent = population.get(firstParentId);
			final int [] secondParent = population.get(secondParentId);

			/* The descendant chooses randomly for each demand, the paths from any of the two parents */
			int [] descendant = new int [D];
			for (int d = 0 ; d < D ; d ++)
				descendant [d] = (rnd.nextBoolean())? firstParent [d] : secondParent [d];

			offspring.add(descendant);
		}
		return offspring;
	}

	/*
	 * Receives a population (typically an offspring). For each element in
	 * the population, mutates the solution. A mutation consists of choosing
	 * a demand randomly, and changing randomly the 1:1 pair carrying the
	 * traffic for this demand (the same 1:1 pair as before could be chosen)
	 */
	private void operator_mutate (ArrayList<int[]> offspring)
	{
	    for (int cont = 0 ; cont < offspring.size() ; cont ++)
	    {
		final int d = rnd.nextInt(D);
		final int number11PathPairs = this.potential11Pairs_d.get(d).size();
		int[] solution = offspring.get(cont);
		solution[d] = rnd.nextInt(number11PathPairs);
	    }
	}

	/* Given the population from previous generation, and a new offspring, selects the elements that will pass to the next generation.
	 * These will be the best ea_populationSize among all the aggregated population */
	private void operator_select (ArrayList<int []> offspring)
	{
	    /* Compute the costs of the offspring */
	    double[] offspringCosts = new double[offspring.size()];
	    for (int cont = 0; cont < offspring.size(); cont ++)
		offspringCosts[cont] = computeCostSolution(offspring.get(cont));

	    /* Append the offspring to the population */
	    population.addAll(offspring);
	    costs = DoubleUtils.concatenate(costs, offspringCosts);

	    /* Reorder the new population */
	    sortAscending(population, costs);

	    /* Take the best ones, keeping the population size equal to ea_populationSize */
	    while(population.size() > ea_populationSize)
		population.remove(population.size()-1);

	    costs = Arrays.copyOf(costs, ea_populationSize);
	}

	/**
         * This method is called after creating the object, to launch the genetic algorithm. The method will first create an initial population. Then,
	 * will complete a number of iterations given by ea_numberIterations. In each iteration, a population is transformed using the evolutionary operators:
	 * parent selection, crossover, mutation, selection. The method keeps track of the best solution found, which is kept in the internal variables
	 * best_solution, best_cost. Then, they are publicly accessible when the method returns.
         */
        public void evolve()
	{
	    /* Generate the initial population */
	    generateInitialSolutions();

	    /* Update the best solution found so far */
	    final int bestSolutionId = DoubleUtils.maxIndexes(this.costs, SearchType.FIRST)[0];
	    best_cost = costs [bestSolutionId];
	    best_solution = Arrays.copyOf(this.population.get(bestSolutionId) , D);
	    System.out.println("Initial population. Best solution cost: " + this.best_cost);

	    /* Evolve: one iteration per generation */
	    for (int it = 0 ; it < this.ea_numberIterations ; it ++)
	    {
		LinkedList<Integer> parents = this.operator_parentSelection ();
		ArrayList<int []> offspring =  this.operator_crossover(parents);
		operator_mutate(offspring);
		operator_select(offspring);
		if (this.costs [0] < this.best_cost)
		{
		    best_cost = this.costs[0];
		    best_solution = Arrays.copyOf(this.population.get(0) , D);
		}

		System.out.println("Iteration: " + it + ". Best solution cost: " + this.best_cost);
	    }
	}

	/* Auxiliary function: Sorts a population in ascending order according to its cost (best cost first) */
	private void sortAscending (List<int[]> population , double[] costs)
	{
	    double[] copyCosts = DoubleUtils.copy(costs);
	    List<int[]> copyPopulation = new ArrayList<int[]>(population);

	    int [] orderedIndexes = DoubleUtils.sortIndexes(costs, OrderingType.ASCENDING);
	    for (int newPosition = 0; newPosition < orderedIndexes.length; newPosition ++)
	    {
		final int oldIndex = orderedIndexes [newPosition];
		costs[newPosition] = copyCosts [oldIndex];
		population.set(newPosition, copyPopulation.get(oldIndex));
	    }
	}
    }
}