package ai.libs.jaicore.search.evaluationproblems;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.api4.java.ai.graphsearch.problem.IPathInORGraphSearch;
import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.IPathEvaluator;
import org.api4.java.algorithm.events.SolutionCandidateFoundEvent;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;
import org.api4.java.datastructure.graph.IPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.basic.sets.Pair;
import ai.libs.jaicore.search.algorithms.standard.bestfirst.BestFirstEpsilon;
import ai.libs.jaicore.search.algorithms.standard.bestfirst.StandardBestFirst;
import ai.libs.jaicore.search.algorithms.standard.dfs.DepthFirstSearch;
import ai.libs.jaicore.search.algorithms.standard.mcts.UCTPathSearch;
import ai.libs.jaicore.search.algorithms.standard.random.RandomSearch;
import ai.libs.jaicore.search.algorithms.standard.rdfs.RandomizedDepthFirstSearch;
import ai.libs.jaicore.search.model.other.SearchGraphPath;
import ai.libs.jaicore.search.probleminputs.GraphSearchInput;
import ai.libs.jaicore.search.probleminputs.GraphSearchWithPathEvaluationsInput;
import ai.libs.jaicore.search.probleminputs.GraphSearchWithSubpathEvaluationsInput;
import ai.libs.jaicore.search.testproblems.enhancedttsp.EnhancedTTSPGraphGenerator;
import ai.libs.jaicore.search.testproblems.enhancedttsp.EnhancedTTSPSolutionPredicate;
import ai.libs.jaicore.search.testproblems.enhancedttsp.EnhancedTTSPToGraphSearchReducer;
import ai.libs.jaicore.testproblems.enhancedttsp.EnhancedTTSP;
import ai.libs.jaicore.testproblems.enhancedttsp.EnhancedTTSPGenerator;
import ai.libs.jaicore.testproblems.enhancedttsp.EnhancedTTSPNode;

public class EnhancedTTSPExample {

	private static final Logger logger = LoggerFactory.getLogger(EnhancedTTSPExample.class);

	private static final int N = 6;
	private static final int MAX_DISTANCE = 12;
	private static final int TIMEOUT_IN_MS = 5 * 60 * 1000;
	private static final boolean VISUALIZE = true;

	private static EnhancedTTSP ttsp;
	private static EnhancedTTSPGraphGenerator graphGenerator;
	private static EnhancedTTSPSolutionPredicate goalTester;
	private static GraphSearchInput<EnhancedTTSPNode, String> input;
	private static EnhancedTTSPToGraphSearchReducer reducer;

	public static void main(final String... args) throws AlgorithmTimeoutedException, AlgorithmExecutionCanceledException, InterruptedException, AlgorithmException {
		EnhancedTTSPExample example = new EnhancedTTSPExample();
		ttsp = new EnhancedTTSPGenerator().generate(N, MAX_DISTANCE);
		graphGenerator = new EnhancedTTSPGraphGenerator(ttsp);
		goalTester = new EnhancedTTSPSolutionPredicate(ttsp);
		input = new GraphSearchInput<>(graphGenerator, goalTester);
		example.testRandomHillClimbing();
		example.testRandomSearch();
		example.testMCTS();
		example.testDijkstra();
		example.testDFS();
		example.testAStarEpsilon();
		example.testAStar();
	}

	public void testRandomHillClimbing() throws AlgorithmTimeoutedException, AlgorithmExecutionCanceledException, InterruptedException, AlgorithmException {
		this.runAlgorithm("Random Hill Climbing", new RandomizedDepthFirstSearch<>(input, new Random(0)), true);
	}

	public void testDFS() throws AlgorithmTimeoutedException, AlgorithmExecutionCanceledException, InterruptedException, AlgorithmException {
		this.runAlgorithm("DFS", new DepthFirstSearch<>(input), false);
	}

	public void testDijkstra() throws AlgorithmTimeoutedException, AlgorithmExecutionCanceledException, InterruptedException, AlgorithmException {
		this.runAlgorithm("Dijkstra", new StandardBestFirst<EnhancedTTSPNode, String, Double>(new GraphSearchWithSubpathEvaluationsInput<>(input, n -> n.getHead().getTime())), true);
	}

	/**
	 * Small class for heuristic based node evaluation as can be used by AStar
	 */
	private class AStarNodeEvaluator implements IPathEvaluator<EnhancedTTSPNode, String, Double> {

		@Override
		public Double evaluate(final IPath<EnhancedTTSPNode, String> node) {

			double g = node.getHead().getTime();
			double h = 0;
			return g + h;
		}

	}

	public void testAStar() throws AlgorithmTimeoutedException, AlgorithmExecutionCanceledException, InterruptedException, AlgorithmException {
		this.runAlgorithm("AStar", new StandardBestFirst<>(new GraphSearchWithSubpathEvaluationsInput<>(input, new AStarNodeEvaluator())), true);
	}

	public void testAStarEpsilon() throws AlgorithmTimeoutedException, AlgorithmExecutionCanceledException, InterruptedException, AlgorithmException {
		this.runAlgorithm("AStarEpsilon", new BestFirstEpsilon<EnhancedTTSPNode, String, Integer>(new GraphSearchWithSubpathEvaluationsInput<>(input, new AStarNodeEvaluator()), n -> ttsp.getLocations().size() - n.getHead().getCurTour().size(), 1.3, false), true);
	}

	public void testMCTS() throws AlgorithmTimeoutedException, AlgorithmExecutionCanceledException, InterruptedException, AlgorithmException {
		this.runAlgorithm("MCTS", new UCTPathSearch<EnhancedTTSPNode, String>(new GraphSearchWithPathEvaluationsInput<>(input, n -> ttsp.getSolutionEvaluator().evaluate(reducer.decodeSolution(n))), 0, 0, false), false);
	}

	public void testRandomSearch() throws AlgorithmTimeoutedException, AlgorithmExecutionCanceledException, InterruptedException, AlgorithmException {
		this.runAlgorithm("RandomSearch", new RandomSearch<>(input), false);
	}

	private void runAlgorithm(final String name, final IPathInORGraphSearch<?, ? extends SearchGraphPath<EnhancedTTSPNode, String>, EnhancedTTSPNode, String> search, final boolean stopOnFirst) throws AlgorithmTimeoutedException, AlgorithmExecutionCanceledException, InterruptedException, AlgorithmException {
		logger.info("Running {}", name);
		long start = System.currentTimeMillis();
		Pair<SearchGraphPath<EnhancedTTSPNode, String>, Double> answer = this.runSearch(search, stopOnFirst, VISUALIZE);
		long runtime = System.currentTimeMillis() - start;
		logger.info("Runtime was: {}", runtime);
		logger.info("Best tour is: {}", answer.getX().getNodes().stream().map(EnhancedTTSPNode::getCurLocation).collect(Collectors.toList()));
		logger.info("Time of best tour is: {}", answer.getY());
	}

	private Pair<SearchGraphPath<EnhancedTTSPNode, String>, Double> runSearch(final IPathInORGraphSearch<?, ? extends SearchGraphPath<EnhancedTTSPNode, String>, EnhancedTTSPNode, String> search, final boolean stopOnFirst, final boolean visualize) throws AlgorithmTimeoutedException, AlgorithmExecutionCanceledException, InterruptedException, AlgorithmException {

		if (visualize) {
			logger.warn("Currently no visualization supported.");
		}

		/* schedule timeout */
		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				logger.info("Canceling algorithm");
				search.cancel();
			}
		}, TIMEOUT_IN_MS);

		/* run search */
		SearchGraphPath<EnhancedTTSPNode, String> bestSolution = null;
		double bestValue = Double.MAX_VALUE;
		SolutionCandidateFoundEvent<? extends SearchGraphPath<EnhancedTTSPNode, String>> solution;
		while (!Thread.currentThread().isInterrupted() && (solution = search.nextSolutionCandidateEvent()) != null) {
			double value = solution.getTimestamp();
			if (value < bestValue) {
				bestSolution = solution.getSolutionCandidate();
				bestValue = value;
			}
			if (stopOnFirst) {
				break;
			}
		}
		return new Pair<>(bestSolution, bestValue);
	}
}
