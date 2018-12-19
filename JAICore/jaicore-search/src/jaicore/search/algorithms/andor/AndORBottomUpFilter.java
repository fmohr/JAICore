package jaicore.search.algorithms.andor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jaicore.basic.ILoggingCustomizable;
import jaicore.basic.IObjectEvaluator;
import jaicore.basic.algorithm.AAlgorithm;
import jaicore.basic.algorithm.AlgorithmEvent;
import jaicore.basic.algorithm.AlgorithmInitializedEvent;
import jaicore.basic.algorithm.AlgorithmState;
import jaicore.basic.sets.LDSBasedCartesianProductComputer;
import jaicore.graph.Graph;
import jaicore.graph.IGraphAlgorithm;
import jaicore.graphvisualizer.events.graphEvents.GraphInitializedEvent;
import jaicore.graphvisualizer.events.graphEvents.NodeReachedEvent;
import jaicore.search.core.interfaces.GraphGenerator;
import jaicore.search.model.travesaltree.NodeExpansionDescription;
import jaicore.search.model.travesaltree.NodeType;
import jaicore.search.structure.graphgenerator.SingleRootGenerator;

public class AndORBottomUpFilter<N, A, V extends Comparable<V>> extends AAlgorithm<GraphGenerator<N, A>, Graph<N>> implements IGraphAlgorithm<GraphGenerator<N, A>, Graph<N>, N, A> {

	private Logger logger = LoggerFactory.getLogger(AndORBottomUpFilter.class);
	private String loggerName;

	public class InnerNodeLabel {
		N node;
		NodeType type;
		// List<List<N>> survivedGoalPaths; // ranked list of paths from node to goals
		boolean evaluated;

		public InnerNodeLabel(final N node, final NodeType type) {
			super();
			this.node = node;
			this.type = type;
		}
	}

	class EvaluatedGraph {
		Graph<N> graph;
		V value;
	}

	private final Graph<InnerNodeLabel> graph = new Graph<>();
	private final IObjectEvaluator<Graph<N>, V> evaluator; // to evaluate (sub-)solutions
	private Graph<N> bestSolutionBase;
	private final int nodeLimit;

	public AndORBottomUpFilter(final GraphGenerator<N, A> gg, final IObjectEvaluator<Graph<N>, V> pEvaluator) {
		this(gg, pEvaluator, 1);
	}

	public AndORBottomUpFilter(final GraphGenerator<N, A> gg, final IObjectEvaluator<Graph<N>, V> pEvaluator, final int andNodeLimit) {
		super(gg);
		this.evaluator = pEvaluator;
		this.nodeLimit = andNodeLimit;
	}

	@Override
	public AlgorithmEvent nextWithException() throws Exception {
		switch (this.getState()) {
		case created: {
			/* step 1: construct the whole graph */
			Queue<InnerNodeLabel> open = new LinkedList<>();
			InnerNodeLabel root = new InnerNodeLabel(((SingleRootGenerator<N>) this.getInput().getRootGenerator()).getRoot(), NodeType.AND);
			open.add(root);
			this.post(new GraphInitializedEvent<N>(root.node));
			this.graph.addItem(root);
			while (!open.isEmpty()) {
				InnerNodeLabel n = open.poll();
				try {
					int generatedChildren = 0;
					for (NodeExpansionDescription<N, A> descr : this.getInput().getSuccessorGenerator().generateSuccessors(n.node)) {
						InnerNodeLabel newNode = new InnerNodeLabel(descr.getTo(), descr.getTypeOfToNode());
						synchronized (this.graph) {
							this.graph.addItem(newNode);
							logger.trace("Added {}-node {} as a child to {}", newNode.type, newNode, n);
							this.graph.addEdge(n, newNode);
						}
						open.add(newNode);
						generatedChildren ++;
						this.post(new NodeReachedEvent<N>(n.node, newNode.node, descr.getTypeOfToNode() == NodeType.OR ? "or" : "and"));
					}
					logger.debug("Node expansion of {}-node {} completed. Generated {} successors.", n.type, n, generatedChildren);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			this.logger.info("Size: {}", this.graph.getItems().size());
			this.setState(AlgorithmState.active);
			return new AlgorithmInitializedEvent();
		}
		case active: {
			this.logger.debug("timeout: {}", this.getTimeout());

			/* now compute best local values bottom up */
			Queue<EvaluatedGraph> bestSolutions = this.filterNodeSolution(this.graph.getRoot());
			this.logger.info("Number of solutions: {}", bestSolutions.size());
			this.bestSolutionBase = bestSolutions.poll().graph;
			return this.terminate();
		}
		default:
			throw new IllegalStateException("No handler defined for state " + this.getState());
		}

	}

	/**
	 * assumes that solution paths for children have been computed already
	 *
	 * @throws Exception
	 */
	private Queue<EvaluatedGraph> filterNodeSolution(final InnerNodeLabel node) throws Exception {

		Queue<EvaluatedGraph> filteredSolutions = new PriorityQueue<>((p1, p2) -> p2.value.compareTo(p1.value)); // a list of paths ordered by their (believed) importance in ASCENDING ORDER
																													// (unimportant first, because these are drained)
		assert !node.evaluated : "Node " + node + " is filtered for the 2nd time already!";
		node.evaluated = true;
		this.logger.debug("Filtering ({})-Node " + node.node + " with " + this.graph.getSuccessors(node).size() + " children.", node.type);

		/* if this is a leaf node, just return itself */
		if (this.graph.getSuccessors(node).isEmpty()) {
			EvaluatedGraph evaluatedGraph = new EvaluatedGraph();
			Graph<N> graph = new Graph<>();
			graph.addItem(node.node);
			evaluatedGraph.graph = graph;
			evaluatedGraph.value = this.evaluator.evaluate(graph);
			filteredSolutions.add(evaluatedGraph);
			this.logger.debug("Returning one single-node solution graph for leaf node {}", node.node);
			return filteredSolutions;
		}

		/* otherwise first compute the values for all children */
		Map<InnerNodeLabel, Queue<EvaluatedGraph>> subSolutions = new HashMap<>();
		logger.debug("Computing subsolutions of node {}", node);
		for (InnerNodeLabel child : this.graph.getSuccessors(node)) {
			Queue<EvaluatedGraph> filteredSolutionsUnderChild = this.filterNodeSolution(child);
			subSolutions.put(child, filteredSolutionsUnderChild);
			if (logger.isDebugEnabled()) {
				StringBuilder sb = new StringBuilder();
				subSolutions.get(child).forEach(l -> sb.append("\n\tGraph Evaluation: " + l.value + "\n\tGraph representation: \n" + l.graph.getLineBasedStringRepresentation(2).replaceAll("\n", "\n\t\t")));
				logger.debug("Child {} has {} solutions: {}", child, filteredSolutionsUnderChild.size(), sb.toString());
			}
		}

		/*
		 * if this is an AND node, combine all solution paths of the children and choose
		 * the best COMBINATIONs
		 */
		if (node.type == NodeType.AND) {

			/* compute cartesian product of best subsolutions of children */
			int k = (int) Math.ceil(Math.pow(this.nodeLimit, 1f / subSolutions.size())); // the number of subsolution to consider for each child
			List<List<EvaluatedGraph>> subSolutionsPerChild = new ArrayList<>();
			for (InnerNodeLabel child : subSolutions.keySet()) {
				List<EvaluatedGraph> bestSubSolutionsOfThisChild = new ArrayList<>();
				this.logger.debug("Adding " + Math.min(k, subSolutions.get(child).size()) + "/" + subSolutions.get(child).size() + " subsolutions of child into the cartesian product input.");
				for (int i = 0; i < k; i++) {
					EvaluatedGraph subsolution = subSolutions.get(child).poll();
					if (subsolution != null) {
						bestSubSolutionsOfThisChild.add(subsolution);
					}
				}
				subSolutionsPerChild.add(bestSubSolutionsOfThisChild);
			}
			/* for each such combination, build the grpah and store it */
			int i = 0;
			LDSBasedCartesianProductComputer<EvaluatedGraph> cartesianProductBuilder = new LDSBasedCartesianProductComputer<>(subSolutionsPerChild);
			List<EvaluatedGraph> subSolutionCombination;
			while ((subSolutionCombination = cartesianProductBuilder.nextTuple()) != null && i++ < this.nodeLimit) {
				EvaluatedGraph extendedSolutionBase = new EvaluatedGraph();
				extendedSolutionBase.graph = new Graph<>();
				extendedSolutionBase.graph.addItem(node.node);
				for (EvaluatedGraph subSolution : subSolutionCombination) {
					assert subSolution != null;
					extendedSolutionBase.graph.addGraph(subSolution.graph);
					extendedSolutionBase.graph.addEdge(node.node, subSolution.graph.getRoot());
				}
				extendedSolutionBase.value = this.evaluator.evaluate(extendedSolutionBase.graph);
				logger.debug("Combination {} of subsolutions with performances {} yields an aggregate performance value of {}", i, subSolutionCombination.stream().map(g -> "" + g.value).collect(Collectors.joining(", ")), extendedSolutionBase.value);
				filteredSolutions.add(extendedSolutionBase);
			}
			this.logger.debug("Determined " + filteredSolutions.size() + " sub-solutions for AND-node with " + subSolutions.size() + " children.");
		}

		/* if this is an OR node, choose the best solution paths of the children */
		else {
			logger.debug("OR-Node: Selecting best child solution out of {}", subSolutions.size());
			for (InnerNodeLabel child : subSolutions.keySet()) {
				logger.trace("Child {} has {} sub-solutions.", child, subSolutions.size());
				for (EvaluatedGraph solutionBase : subSolutions.get(child)) {
					EvaluatedGraph extendedSolutionBase = new EvaluatedGraph();
					extendedSolutionBase.graph = new Graph<>(solutionBase.graph);
					extendedSolutionBase.graph.addItem(node.node);
					extendedSolutionBase.graph.addEdge(node.node, child.node);
					extendedSolutionBase.value = solutionBase.value;
					filteredSolutions.add(extendedSolutionBase);
				}
			}
		}
		
		/* order solutions by value */
		filteredSolutions = new LinkedList<>(filteredSolutions.stream().sorted((g1,g2) -> g1.value.compareTo(g2.value)).limit(nodeLimit).collect(Collectors.toList()));
		
		// logger.debug("\treturning " + filteredSolutionsReordered.size() + "
		// graph(s):");
		filteredSolutions.forEach(g -> this.logger.debug("\tScore " + g.value + " for " + g.graph));
		return filteredSolutions;
	}

	@Override
	public Graph<N> call() throws Exception {
		while (this.hasNext()) {
			this.nextWithException();
		}
		return this.bestSolutionBase;
	}

	@Override
	public String getLoggerName() {
		return this.loggerName;
	}

	@Override
	public void setLoggerName(final String name) {
		this.logger.info("Switching logger from {} to {}", this.logger.getName(), name);
		this.logger = LoggerFactory.getLogger(name);
		this.logger.info("Activated logger {} with name {}", name, this.logger.getName());
		if (this.evaluator instanceof ILoggingCustomizable) {
			((ILoggingCustomizable) this.evaluator).setLoggerName(name + ".eval");
		}
		super.setLoggerName(this.loggerName + "._orgraphsearch");
	}
}
