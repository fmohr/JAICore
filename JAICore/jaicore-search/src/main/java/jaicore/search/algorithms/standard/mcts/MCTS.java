package jaicore.search.algorithms.standard.mcts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jaicore.basic.ILoggingCustomizable;
import jaicore.basic.algorithm.AlgorithmExecutionCanceledException;
import jaicore.basic.algorithm.AlgorithmState;
import jaicore.basic.algorithm.events.AlgorithmEvent;
import jaicore.basic.algorithm.exceptions.AlgorithmException;
import jaicore.basic.algorithm.exceptions.ObjectEvaluationFailedException;
import jaicore.basic.sets.SetUtil;
import jaicore.graph.LabeledGraph;
import jaicore.graphvisualizer.events.graph.GraphInitializedEvent;
import jaicore.graphvisualizer.events.graph.NodeAddedEvent;
import jaicore.graphvisualizer.events.graph.NodeTypeSwitchEvent;
import jaicore.search.core.interfaces.AOptimalPathInORGraphSearch;
import jaicore.search.core.interfaces.GraphGenerator;
import jaicore.search.core.interfaces.ISolutionEvaluator;
import jaicore.search.model.other.EvaluatedSearchGraphPath;
import jaicore.search.model.travesaltree.Node;
import jaicore.search.model.travesaltree.NodeExpansionDescription;
import jaicore.search.probleminputs.GraphSearchWithPathEvaluationsInput;
import jaicore.search.structure.graphgenerator.NodeGoalTester;
import jaicore.search.structure.graphgenerator.PathGoalTester;
import jaicore.search.structure.graphgenerator.RootGenerator;
import jaicore.search.structure.graphgenerator.SingleRootGenerator;
import jaicore.search.structure.graphgenerator.SuccessorGenerator;

/**
 * MCTS algorithm implementation.
 * 
 * This implementation follows the description in Browne, Cb AND Powley, Edward - A survey of monte carlo tree search methods (2012)
 *
 * @author Felix Mohr
 */
public class MCTS<N, A, V extends Comparable<V>> extends AOptimalPathInORGraphSearch<GraphSearchWithPathEvaluationsInput<N, A, V>, N, A, V> implements IPolicy<N, A, V> {

	private Logger logger = LoggerFactory.getLogger(MCTS.class);
	private String loggerName;

	/* communication */
	protected final Map<N, Node<N, V>> ext2int = new HashMap<>();

	protected final GraphGenerator<N, A> graphGenerator;
	protected final RootGenerator<N> rootGenerator;
	protected final SuccessorGenerator<N, A> successorGenerator;
	protected final boolean checkGoalPropertyOnEntirePath;
	protected final PathGoalTester<N> pathGoalTester;
	protected final NodeGoalTester<N> nodeGoalTester;

	protected final IPathUpdatablePolicy<N, A, V> treePolicy;
	protected final IPolicy<N, A, V> defaultPolicy;
	protected final ISolutionEvaluator<N, V> playoutSimulator;

	private final Map<List<N>, V> scoreCache = new HashMap<>();

	private final N root;
	private final Collection<N> nodesExplicitlyAdded = new HashSet<>();
	private final Collection<N> unexpandedNodes = new HashSet<>();
	protected final LabeledGraph<N, A> exploredGraph;
	private final Collection<N> fullyExploredNodes = new HashSet<>(); // set of nodes under which the tree is completely known
	private final Collection<N> deadLeafNodes = new HashSet<>();
	private final V penaltyForFailedEvaluation;

	private final boolean forbidDoublePaths;

	public MCTS(final GraphSearchWithPathEvaluationsInput<N, A, V> problem, final IPathUpdatablePolicy<N, A, V> treePolicy, final IPolicy<N, A, V> defaultPolicy, V penaltyForFailedEvaluation, boolean forbidDoublePaths) {
		super(problem);
		this.graphGenerator = problem.getGraphGenerator();
		this.rootGenerator = this.graphGenerator.getRootGenerator();
		this.successorGenerator = this.graphGenerator.getSuccessorGenerator();
		this.checkGoalPropertyOnEntirePath = !(this.graphGenerator.getGoalTester() instanceof NodeGoalTester);
		if (this.checkGoalPropertyOnEntirePath) {
			this.nodeGoalTester = null;
			this.pathGoalTester = (PathGoalTester<N>) this.graphGenerator.getGoalTester();
		} else {
			this.nodeGoalTester = (NodeGoalTester<N>) this.graphGenerator.getGoalTester();
			this.pathGoalTester = null;
		}

		this.treePolicy = treePolicy;
		this.defaultPolicy = defaultPolicy;
		this.playoutSimulator = problem.getPathEvaluator();
		this.exploredGraph = new LabeledGraph<>();
		this.root = ((SingleRootGenerator<N>) this.rootGenerator).getRoot();
		this.unexpandedNodes.add(this.root);
		this.exploredGraph.addItem(this.root);
		this.penaltyForFailedEvaluation = penaltyForFailedEvaluation;
		this.forbidDoublePaths = forbidDoublePaths;
	}

	/**
	 * This method produces a playout path in three phases. Starting with root, it always chooses one of the children of the currently considered node as the next current node.
	 * 
	 * 1. selection: if all successors of the current node have been visited, use the tree policy to choose the next node
	 * 2. expansion: one (new) child node is added to expand the tree (here, this means to mark one of the successors as visited)
	 * 3. simulation: draw a path completion from the node added in step 2
	 * 
	 * Note that we have two stages of node expansion in this algorithm.
	 * In a first step, node successors are always computed in a block, i.e. we compute all successors at once and attach them to the <code>exploredGraph</code> variable.
	 * However, the algorithm ignores such generated nodes until they become explicitly "generated" in step 2, which means that they are added to the <code>nodesExplicitlyAdded</code> variable.
	 * 
	 * If this method hits a dead-end, it will draw a new playout automatically.
	 * 
	 * @return
	 * @throws InterruptedException
	 * @throws AlgorithmExecutionCanceledException
	 * @throws TimeoutException
	 */
	private List<N> getPlayout() throws InterruptedException, AlgorithmExecutionCanceledException, TimeoutException {
		this.logger.debug("Computing a new playout ...");
		N current = this.root;
		N next;
		Collection<N> childrenOfCurrent = unexpandedNodes.contains(current) ? null : this.exploredGraph.getSuccessors(current);
		List<N> path = new ArrayList<>();
		path.add(current);

		/* Step 1 (Selection): chooses next node with tree policy until a node is reached that has at least one node that has not been part of any playout */
		this.logger.debug("Step 1: Using tree policy to identify new path to not fully expanded node.");
		int level = 0;
		while (childrenOfCurrent != null && SetUtil.differenceEmpty(childrenOfCurrent, this.nodesExplicitlyAdded)) {

			this.logger.trace("Step 1 (level {}): choose one of the {} succesors {} of current node {}", level, childrenOfCurrent.size(), childrenOfCurrent, current);

			/* determine all actions applicable in this node that are not known to lead into a dead-end */
			this.checkAndConductTermination();
			List<A> availableActions = new ArrayList<>();
			Map<A, N> successorStates = new HashMap<>();
			for (N child : childrenOfCurrent) {
				if (this.deadLeafNodes.contains(child)) {
					this.logger.trace("Ignoring child {}, which is known to be a dead end", child);
					continue;
				} else if (forbidDoublePaths && fullyExploredNodes.contains(child)) {
					this.logger.trace("Ignoring child {}, which has been fully explored.", child);
					continue;
				}
				A action = this.exploredGraph.getEdgeLabel(current, child);
				assert !successorStates.containsKey(action) : "A successor state has already been defined for action \"" + action + "\" with hashCode " + action.hashCode();
				availableActions.add(action);
				successorStates.put(action, child);
				assert successorStates.keySet().size() == availableActions.size() : "We have generated " + availableActions.size() + " available actions but the map of successor states only contains " + successorStates.keySet().size()
						+ " item(s). Actions (by hash codes): \n\t" + availableActions.stream().map(a -> a.hashCode() + ": " + a.toString()).collect(Collectors.joining("\n\t"));
			}

			/* if every applicable action is known to yield a dead-end, mark this node to be a dead-end itself and return */
			if (availableActions.isEmpty()) {
				this.logger.debug("Node {} has only dead-end successors and hence is a dead-end itself. Adding it to the list of dead ends.", current);
				this.deadLeafNodes.add(current);
				return getPlayout();
			}

			/* choose the next action and determine the subsequent node. Also compute the children of the current. If it has not been expanded yet, set the list of successors to NULL, which will interrupt the loop */
			this.logger.trace("{} available actions of expanded node {}: {}. Corresponding successor states: {}", availableActions.size(), current, availableActions, successorStates);
			A chosenAction = this.treePolicy.getAction(current, successorStates);
			assert chosenAction != null : "Chosen action must not be null!";
			next = successorStates.get(chosenAction);
			assert next != null : "Next action must not be null!";
			this.logger.trace("Tree policy decides to expand {} taking action {} to {}", current, chosenAction, next);
			current = next;
			childrenOfCurrent = unexpandedNodes.contains(current) ? null : this.exploredGraph.getSuccessors(current);
			path.add(current);

			/* if the current path is a goal path, return it */
			if (isGoal(current)) {
				logger.debug("Constructed complete solution with tree policy.");
				return path;
			}
			this.post(new NodeTypeSwitchEvent<N>(getId(), next, "or_rollout"));
			level++;
		}

		/* children of current must either not have been generated or not be empty. This assertion should already be covered by the subsequent assertion, but it is still here for robustness reasons */
		assert childrenOfCurrent == null || !childrenOfCurrent.isEmpty() : "Set of children of current node must not be empty!";

		/* if the current node is not a leaf (of the traversal tree, i.e. has no children generated yet), it must have untried successors */
		assert childrenOfCurrent == null || SetUtil.differenceNotEmpty(childrenOfCurrent, this.nodesExplicitlyAdded) : "The current node has " + childrenOfCurrent.size()
				+ " successors and all of them have been considered in at least one playout. In spite of this, the tree policy has not been used to choose a child, but it should have been used.";

		/* if the current node has at least one child, all child nodes must have been marked as dead ends */
		assert childrenOfCurrent == null || SetUtil.differenceNotEmpty(childrenOfCurrent, deadLeafNodes) : "Flag that current node is dead end is set, but there are successors that are not yet marked as dead-ends.";
		this.logger.debug("Determined non-fully-expanded node {} of traversal tree using tree policy. Untried successors are: {}. Now selecting an untried successor.", current,
				childrenOfCurrent != null ? SetUtil.difference(childrenOfCurrent, this.nodesExplicitlyAdded) : "<not generated>");

		/* Step 2 (Expansion): Use default policy to select one of the unvisited successors of the current node. If necessary, generate the successors first. */
		this.checkAndConductTermination();

		/* determine the unvisited child nodes of this node */
		Map<A, N> untriedActionsAndTheirSuccessors = new HashMap<>();
		if (this.unexpandedNodes.contains(current)) {
			this.logger.trace("This is the first time we visit this node, so compute its successors and add add them to explicit graph model.");
			untriedActionsAndTheirSuccessors.putAll(this.expandNode(current));
		} else {
			for (N child : SetUtil.difference(childrenOfCurrent, this.nodesExplicitlyAdded)) {
				A action = this.exploredGraph.getEdgeLabel(current, child);
				untriedActionsAndTheirSuccessors.put(action, child);
			}
		}

		/* choose the node expansion with the default policy */
		this.logger.debug("Step 2: Using default policy to choose one of the {} untried actions {} of current node {}", untriedActionsAndTheirSuccessors.size(), untriedActionsAndTheirSuccessors.keySet(), current);
		if (!untriedActionsAndTheirSuccessors.isEmpty()) {
			logger.trace("Asking default policy for action to take in node {}", current);
			A chosenAction = this.defaultPolicy.getAction(current, untriedActionsAndTheirSuccessors);
			current = untriedActionsAndTheirSuccessors.get(chosenAction);
			assert this.unexpandedNodes.contains(current);
			this.nodesExplicitlyAdded.add(current);
			this.post(new NodeTypeSwitchEvent<N>(getId(), current, "or_rollout"));
			path.add(current);
			this.logger.debug("Selected {} as the untried action with successor state {}. Now completing rest playout from this situation.", chosenAction, current);
		} else {
			this.deadLeafNodes.add(current);
			this.logger.debug("Found leaf node {}. Adding to dead end list.", current);
			return getPlayout();
		}

		/* Step 3 (Simulation): use default policy to proceed to a goal node. Nodes won't be added to model variable here.
		 * The invariant of the following loop is that the current node is always the last unexpanded node, and the path variable contains all nodes from root to the current one (including it).
		 */
		this.logger.debug("Step 3: Using default policy to create full path under {}.", current);
		while (!this.isGoal(current)) {
			assert this.unexpandedNodes.contains(current);
			this.checkAndConductTermination();
			Map<A, N> actionsAndTheirSuccessorStates = new HashMap<>();
			this.logger.trace("Determining possible moves for {}.", current);
			actionsAndTheirSuccessorStates.putAll(this.expandNode(current));

			/* if the default policy has led us into a state where we cannot do anything, stop playout */
			if (actionsAndTheirSuccessorStates.isEmpty()) {
				this.deadLeafNodes.add(current);
				propagateFullyKnownNodes(current);
				return path;
			}
			current = actionsAndTheirSuccessorStates.get(this.defaultPolicy.getAction(current, actionsAndTheirSuccessorStates));
			if (!isGoal(current))
				this.post(new NodeTypeSwitchEvent<>(getId(), current, "or_rollout"));
			this.nodesExplicitlyAdded.add(current);
			path.add(current);
		}
		checkThatPathIsSolution(path);
		this.logger.debug("Drawn playout path is: {}.", path);
		propagateFullyKnownNodes(current);
		return path;
	}

	private void closePath(List<N> path) {
		int n = path.size();
		for (int i = n - 2; i > 0; i--) { // don't color the root and the leaf!
			N node = path.get(i);
			this.post(new NodeTypeSwitchEvent<N>(getId(), node, fullyExploredNodes.contains(node) ? "or_exhausted" : "or_closed"));
		}
	}
	
	/**
	 * This method does NOT change the state of nodes via an event since it is assumed that closePath will be called later and assumes this task.
	 * 
	 * @param node
	 */
	private void propagateFullyKnownNodes(N node) {
		if (fullyExploredNodes.containsAll(exploredGraph.getSuccessors(node))) {
			fullyExploredNodes.add(node);
			assert exploredGraph.getPredecessors(node).size() <= 1;
			N parent = exploredGraph.getPredecessors(node).iterator().next();
			propagateFullyKnownNodes(parent);
		}
	}

	private Map<A, N> expandNode(final N node) throws InterruptedException, AlgorithmExecutionCanceledException, TimeoutException {
		this.logger.debug("Starting expansion of node {}", node);
		this.checkAndConductTermination();
		if (!this.unexpandedNodes.contains(node)) {
			throw new IllegalArgumentException();
		}
		this.logger.trace("Situation {} has never been analyzed before, expanding the graph at the respective point.", node);
		this.unexpandedNodes.remove(node);
		Collection<NodeExpansionDescription<N, A>> availableActions = null;
		try {
			availableActions = this.successorGenerator.generateSuccessors(node);
			assert availableActions.stream().map(n -> n.getAction()).collect(Collectors.toList()).size() == availableActions.stream().map(n -> n.getAction()).collect(Collectors.toSet())
					.size() : "The actions under this node don't have unique names";
		} catch (InterruptedException e) {
			this.checkAndConductTermination();
		}
		Map<A, N> successorStates = new HashMap<>();
		for (NodeExpansionDescription<N, A> d : availableActions) {
			this.checkAndConductTermination();
			successorStates.put(d.getAction(), d.getTo());
			this.logger.trace("Adding edge {} -> {} with label {}", d.getFrom(), d.getTo(), d.getAction());
			this.exploredGraph.addItem(d.getTo());
			this.unexpandedNodes.add(d.getTo());
			this.exploredGraph.addEdge(d.getFrom(), d.getTo(), d.getAction());
			this.post(new NodeAddedEvent<>(getId(), d.getFrom(), d.getTo(), this.isGoal(d.getTo()) ? "or_solution" : "or_open"));
		}
		return successorStates;
	}

	private boolean isGoal(final N node) {
		return this.nodeGoalTester.isGoal(node);
	}

	@Override
	public A getAction(final N node, final Map<A, N> actionsWithSuccessors) {

		try {
			/* compute next solution */
			this.nextSolutionCandidate();

			/* choose action in root that has best reward */
			return this.treePolicy.getAction(this.root, actionsWithSuccessors);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void checkThatPathIsSolution(List<N> path) {
		N current = exploredGraph.getRoot();
		assert current.equals(path.get(0)) : "The root of the path does not match the root of the graph!";
		for (int i = 1; i < path.size(); i++) {
			assert exploredGraph.getSuccessors(current).contains(path.get(i)) : "Invalid path. The " + i + "-th entry " + path.get(i) + " of the path " + path + " is not a successor of the " + (i - 1) + "-th node whose successors are "
					+ exploredGraph.getSuccessors(current) + "!";
			current = path.get(i);
		}
		assert !path.isEmpty() : "Solution paths cannot be empty!";
		assert isGoal(path.get(path.size() - 1)) : "The head of a solution path must be a goal node, but this is not the case for this path: \n\t" + path.stream().map(n -> n.toString()).collect(Collectors.joining("\n\t"));
	}

	@Override
	public AlgorithmEvent nextWithException() throws InterruptedException, AlgorithmExecutionCanceledException, CancellationException, AlgorithmException {
		switch (this.getState()) {
		case created:
			this.post(new GraphInitializedEvent<N>(getId(), this.root));
			logger.info("Starting MCTS with node class {} and edge (action) class {}", this.root.getClass().getName());
			return activate();

		case active:
			if (this.playoutSimulator == null) {
				throw new IllegalStateException("no simulator has been set!");
			}
			this.logger.debug("Next algorithm iteration. Number of unexpanded nodes: {}", this.unexpandedNodes.size());
			try {
				this.registerActiveThread();
				while (this.getState() == AlgorithmState.active) {
					this.checkAndConductTermination();
					if (this.unexpandedNodes.isEmpty()) {
						AlgorithmEvent finishEvent = terminate();
						this.logger.info("Finishing MCTS as all nodes have been expanded; the search graph has been exhausted.");
						return finishEvent;
					} else {

						/* compute a playout and, if the path is a solution, compute its score and update the path */
						this.logger.debug("There are {} known unexpanded nodes. Starting computation of next playout path.", this.unexpandedNodes.size());
						List<N> path = this.getPlayout();
						assert path != null : "Paths must never be null!";
						if (!this.scoreCache.containsKey(path)) {
							this.logger.debug("Obtained path {}. Now starting computation of the score for this playout.", path);
							try {
								V playoutScore = this.playoutSimulator.evaluateSolution(path);
								boolean isSolutionPlayout = this.nodeGoalTester.isGoal(path.get(path.size() - 1));
								this.logger.debug("Determined playout score {}. Is goal: {}. Now updating the path.", playoutScore, isSolutionPlayout);
								this.scoreCache.put(path, playoutScore);
								this.treePolicy.updatePath(path, playoutScore);
								if (isSolutionPlayout) {
									AlgorithmEvent solutionEvent = this.registerSolution(new EvaluatedSearchGraphPath<>(path, getActionListForPath(path), playoutScore));
									return solutionEvent;
								}
							} catch (InterruptedException e) { // don't forward this directly since this could come indirectly through a cancel. Rather invoke checkTermination
								Thread.interrupted(); // reset interrupt field
								checkAndConductTermination();
								throw e; // if we get here (no exception for timeout or cancel has been thrown in check), we really have been interrupted
							} catch (ObjectEvaluationFailedException e) {
								this.scoreCache.put(path, penaltyForFailedEvaluation);
								post(new NodeTypeSwitchEvent<>(getId(), path.get(path.size() - 1), "or_ffail"));
								this.treePolicy.updatePath(path, penaltyForFailedEvaluation);
								this.logger.warn("Could not evaluate playout " + e.toString());
							}
							finally {
								closePath(path); // visualize that path rollout has been completed
							}
						} else {
							assert !forbidDoublePaths : "Second time path " + getActionListForPath(path) + " has been generated even though double paths are forbidden!";
							logger.warn("Path {} has already been observed in the past.", getActionListForPath(path));
							V playoutScore = this.scoreCache.get(path);
							this.logger.debug("Looking up score {} for the already evaluated path {}", playoutScore, path);
							this.treePolicy.updatePath(path, playoutScore);
							closePath(path); // visualize that path rollout has been completed
						}
					}

				}
			} catch (TimeoutException e) {
				this.unregisterThreadAndShutdown();
				Thread.interrupted(); // unset interrupted flag
				this.logger.info("Finishing MCTS due to timeout.");
				return terminate();
			}

		default:
			throw new UnsupportedOperationException("Cannot do anything in state " + this.getState());
		}
	}

	private List<A> getActionListForPath(List<N> path) {
		List<A> actions = new ArrayList<>();
		int n = path.size();
		for (int i = 1; i < n; i++) {
			actions.add(exploredGraph.getEdgeLabel(path.get(i - 1), path.get(i)));
		}
		return actions;
	}

	@Override
	public String getLoggerName() {
		return this.loggerName;
	}

	@Override
	public void setLoggerName(final String name) {
		this.logger.info("Switching logger from {} to {}", this.logger.getName(), name);
		this.loggerName = name;
		this.logger = LoggerFactory.getLogger(name);
		this.logger.info("Activated logger {} with name {}", name, this.logger.getName());
		super.setLoggerName(this.loggerName + "._orgraphsearch");
		if (treePolicy instanceof ILoggingCustomizable) {
			logger.info("Setting logger of tree policy to {}", name + ".treepolicy");
			((ILoggingCustomizable) treePolicy).setLoggerName(name + ".treepolicy");
		} else
			logger.info("Not setting logger of tree policy");
	}
}