package jaicore.search.algorithms.standard.mcts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jaicore.graph.LabeledGraph;
import jaicore.graphvisualizer.events.graphEvents.GraphInitializedEvent;
import jaicore.graphvisualizer.events.graphEvents.NodeReachedEvent;
import jaicore.graphvisualizer.events.graphEvents.NodeTypeSwitchEvent;
import jaicore.search.algorithms.interfaces.IObservableORGraphSearch;
import jaicore.search.algorithms.standard.core.IGraphDependentNodeEvaluator;
import jaicore.search.algorithms.standard.core.INodeEvaluator;
import jaicore.search.structure.core.GraphEventBus;
import jaicore.search.structure.core.GraphGenerator;
import jaicore.search.structure.core.Node;
import jaicore.search.structure.core.NodeExpansionDescription;
import jaicore.search.structure.graphgenerator.NodeGoalTester;
import jaicore.search.structure.graphgenerator.PathGoalTester;
import jaicore.search.structure.graphgenerator.RootGenerator;
import jaicore.search.structure.graphgenerator.SingleRootGenerator;
import jaicore.search.structure.graphgenerator.SuccessorGenerator;

/**
 * MCTS algorithm implementation.
 *
 * @author Felix Mohr
 */
public class MCTS<T, A, V extends Comparable<V>> implements IObservableORGraphSearch<T, A, V>, IPolicy<T, A, V> {

	private static final Logger logger = LoggerFactory.getLogger(MCTS.class);

	/* communication */
	protected final GraphEventBus<Node<T, V>> graphEventBus = new GraphEventBus<>();
	protected final Map<T, Node<T, V>> ext2int = new HashMap<>();

	protected final GraphGenerator<T, A> graphGenerator;
	protected final RootGenerator<T> rootGenerator;
	protected final SuccessorGenerator<T, A> successorGenerator;
	protected final boolean checkGoalPropertyOnEntirePath;
	protected final PathGoalTester<T> pathGoalTester;
	protected final NodeGoalTester<T> nodeGoalTester;

	protected final IPathUpdatablePolicy<T, A, V> treePolicy;
	protected final IPolicy<T, A, V> defaultPolicy;
	protected final INodeEvaluator<T, V> playoutSimulator;

	protected final Map<List<T>, V> playouts = new HashMap<>();

	private boolean initialized = false;
	private final T root;
	protected final LabeledGraph<T, A> exploredGraph;
	private int timeoutInS = -1;

	@SuppressWarnings("unchecked")
	public MCTS(GraphGenerator<T, A> graphGenerator, IPathUpdatablePolicy<T, A, V> treePolicy, IPolicy<T, A, V> defaultPolicy, INodeEvaluator<T, V> playoutSimulator) {
		super();
		this.graphGenerator = graphGenerator;
		this.rootGenerator = graphGenerator.getRootGenerator();
		this.successorGenerator = graphGenerator.getSuccessorGenerator();
		checkGoalPropertyOnEntirePath = !(graphGenerator.getGoalTester() instanceof NodeGoalTester);
		if (checkGoalPropertyOnEntirePath) {
			this.nodeGoalTester = null;
			this.pathGoalTester = (PathGoalTester<T>) graphGenerator.getGoalTester();
			;
		} else {
			this.nodeGoalTester = (NodeGoalTester<T>) graphGenerator.getGoalTester();
			this.pathGoalTester = null;
		}

		this.treePolicy = treePolicy;
		this.defaultPolicy = defaultPolicy;
		this.playoutSimulator = playoutSimulator;
		this.exploredGraph = new LabeledGraph<>();
		this.root = ((SingleRootGenerator<T>) rootGenerator).getRoot();
		this.exploredGraph.addItem(root);

		/* if the node evaluator is graph dependent, communicate the generator to it */
		if (playoutSimulator instanceof IGraphDependentNodeEvaluator<?, ?, ?>) {
			logger.info("{} is a graph dependent node evaluator. Setting its graph generator now ...", playoutSimulator);
			((IGraphDependentNodeEvaluator<T, A, V>) playoutSimulator).setGenerator(graphGenerator);
		}
	}

	@Override
	public void bootstrap(Collection<Node<T, V>> nodes) {
		// TODO Auto-generated method stub
	}

	@Override
	public List<T> nextSolution() {

		if (!initialized) {
			initialized = true;
			graphEventBus.post(new GraphInitializedEvent<T>(root));
		}

		/* iterate over playouts */
		try {
			while (!Thread.interrupted()) {
				logger.info("Starting computation of next playout path.");
				List<T> path = getPlayout();
				logger.info("Obtained path {}. Now starting computation of next playout.", path);
				V playout = playoutSimulator.f(getFakeInternalNode(path));
				logger.info("Determined playout score {}. Now updating the path.", playout);
				treePolicy.updatePath(path, playout);
				// if (isGoal(path.get(path.size() - 1))) {
				// playouts.put(path, playout);
				return path;
				// }
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	private List<T> getPlayout() throws Exception {
		logger.info("Computing a new playout ...");
		T current = root;
		T next;
		Collection<T> childrenOfCurrent;
		List<T> path = new ArrayList<>();
		path.add(current);

		/* use tree policy to select a leaf node of the explored graph */
		while (!(childrenOfCurrent = exploredGraph.getSuccessors(current)).isEmpty()) {
			List<A> availableActions = new ArrayList<>();
			Map<A, T> successorStates = new HashMap<>();
			for (T child : childrenOfCurrent) {
				A action = exploredGraph.getEdgeLabel(current, child);
				availableActions.add(action);
				successorStates.put(action, child);
			}
			logger.debug("Available actions of expanded node: {}", availableActions);
			A chosenAction = treePolicy.getAction(current, successorStates);
			if (chosenAction == null)
				throw new IllegalStateException("Chosen action is null!");
			next = successorStates.get(chosenAction);
			if (next == null)
				throw new IllegalStateException("Next action is null!");

			logger.debug("Tree policy decides to expand {} taking action {} to {}", current, chosenAction, next);
			current = next;
			graphEventBus.post(new NodeTypeSwitchEvent<T>(next, "expanding"));
			path.add(current);
			logger.debug("Chosen action: {}. Successor: {}", chosenAction, current);
		}
		logger.info("Determined leaf node {} of traversal tree using tree policy. Now completing the path.", current);

		/* use default policy to proceed to a goal node */
		while (!isGoal(current)) {
			Collection<NodeExpansionDescription<T, A>> availableActions = successorGenerator.generateSuccessors(current);
			Map<A, T> successorStates = new HashMap<>();
			if (availableActions.isEmpty()) {
				return path;
			}
			List<A> actions = new ArrayList<>();
			for (NodeExpansionDescription<T, A> d : availableActions) {
				successorStates.put(d.getAction(), d.getTo());
				logger.debug("Adding edge {} -> {} with label {}", d.getFrom(), d.getTo(), d.getAction());
				exploredGraph.addItem(d.getTo());
				exploredGraph.addEdge(d.getFrom(), d.getTo(), d.getAction());
				graphEventBus.post(new NodeReachedEvent<>(d.getFrom(), d.getTo(), isGoal(d.getTo()) ? "or_solution" : "or_open"));
				actions.add(d.getAction());
			}
			current = successorStates.get(defaultPolicy.getAction(current, successorStates));
			path.add(current);
		}
		logger.info("Draw playout path {}.", path);

		/* change all node types on path to closed again */
		while (true) {
			if (exploredGraph.getPredecessors(current).isEmpty())
				break;
			current = exploredGraph.getPredecessors(current).iterator().next();
			graphEventBus.post(new NodeTypeSwitchEvent<T>(current, "or_closed"));
		}
		return path;
	}

	private boolean isGoal(T node) {
		return nodeGoalTester.isGoal(node);
	}

	@Override
	public V getFValue(T node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public V getFValue(Node<T, V> node) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getAnnotationOfReturnedSolution(List<T> solution, String annotation) {
		if (!playouts.containsKey(solution))
			return null;
		else {
			return playouts.get(solution);
		}
	}

	@Override
	public V getFOfReturnedSolution(List<T> solution) {
		@SuppressWarnings("unchecked")
		V annotation = (V) getAnnotationOfReturnedSolution(solution, "<not used anyway>");
		if (annotation == null) {
			throw new IllegalArgumentException(
					"There is no solution annotation for the given solution. Please check whether the solution was really produced by the algorithm. If so, please check that its annotation was added into the list of annotations before the solution itself was added to the solution set");
		}
		return annotation;
	}

	@Override
	public void cancel() {

	}

	@Override
	public Node<T, V> getInternalRepresentationOf(T node) {
		return null;
	}

	@Override
	public List<Node<T, V>> getOpenSnapshot() {
		return null;
	}

	@Override
	public INodeEvaluator<T, V> getNodeEvaluator() {
		return null;
	}

	private Node<T, V> getFakeInternalNode(List<T> externalPath) {
		Iterator<T> i = externalPath.iterator();
		Node<T, V> current = new Node<>(null, i.next());
		while (i.hasNext()) {
			current = new Node<>(current, i.next());
		}
		return current;
	}

	@Override
	public Map<String, Object> getAnnotationsOfReturnedSolution(List<T> solution) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void registerListener(Object listener) {
		this.graphEventBus.register(listener);
	}

	@Override
	public A getAction(T node, Map<A, T> actionsWithSuccessors) {

		/* compute next solution */
		nextSolution();

		/* choose action in root that has best reward */
		return treePolicy.getAction(root, actionsWithSuccessors);
	}

	@Override
	public GraphGenerator<T, A> getGraphGenerator() {
		return graphGenerator;
	}
}