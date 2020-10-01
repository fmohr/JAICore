package ai.libs.jaicore.basic.sets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.api4.java.algorithm.events.IAlgorithmEvent;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;
import org.api4.java.common.control.ILoggingCustomizable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.basic.algorithm.AAlgorithm;
import ai.libs.jaicore.basic.algorithm.AlgorithmFinishedEvent;
import ai.libs.jaicore.basic.algorithm.AlgorithmInitializedEvent;

/**
 * This algorithms allows to compute an ordered Cartesian product. It is ordered
 * in the sense that it interprets the sets over which the product is built as
 * ORDERED sets and first generates tuples with items that appear first in the
 * sets.
 *
 * The algorithm also works for ordinary unordered sets but is a bit slower why
 * another algorithm could be favorable.
 *
 * @author fmohr
 *
 * @param <T>
 */
public class LDSRelationComputer<T> extends AAlgorithm<RelationComputationProblem<T>, List<List<T>>> {

	private Logger logger = LoggerFactory.getLogger(LDSRelationComputer.class);

	private class Node {
		private Node parent;
		private int defficiency;
		private int indexOfSet;
		private int indexOfValue;

		public Node() {
		}

		public Node(final Node parent, final int indexOfSet, final int defficiency, final int indexInSet) {
			long start = System.currentTimeMillis();
			this.parent = parent;
			this.indexOfSet = indexOfSet;
			this.defficiency = defficiency;
			this.indexOfValue = indexInSet;
			assert System.currentTimeMillis() - start <= 1 : "Constructor execution took more than 1ms: " + (System.currentTimeMillis() - start) + "ms";
		}

		public void fillTupleArrayWithValues(final List<T> tupleArray) {
			tupleArray.clear();
			this.fillTupleArrayWithValuesRec(tupleArray);
		}

		private void fillTupleArrayWithValuesRec(final List<T> tupleArray) {
			if (this.parent == null) {
				return;
			}
			tupleArray.add(0, this.getObject());
			this.parent.fillTupleArrayWithValuesRec(tupleArray);
		}

		T getObject() {
			return LDSRelationComputer.this.sets.get(this.indexOfSet).get(this.indexOfValue);
		}

		/**
		 * this method can be used to store old nodes in order to use them again later in the algorithm.
		 * This can be useful to avoid the creation of new objects all the time, which can be time consuming.
		 */
		public void recycle() {
			if (this.indexOfSet >= LDSRelationComputer.this.numSets) {
				LDSRelationComputer.this.recycledNodes.add(this);
			}
		}
	}

	private final List<List<T>> sets;
	private final int numSets;
	private final Predicate<List<T>> prefixFilter;
	private int computedTuples = 0;
	private final List<T> currentTuple;
	private Queue<Node> open = new PriorityQueue<>((n1, n2) -> n1.defficiency - n2.defficiency);
	private List<Node> recycledNodes = new ArrayList<>();
	private int numRecycledNodes;
	private int numCreatedNodes;
	private int reportRate = 1000; // number of tuples after which a message is sent to the logger

	public LDSRelationComputer(final List<Collection<T>> sets) {
		this(new RelationComputationProblem<>(sets));
	}

	public LDSRelationComputer(final RelationComputationProblem<T> problem) {
		super(problem);
		this.sets = new ArrayList<>();
		for (Collection<T> set : problem.getSets()) {
			this.sets.add(set instanceof List ? (List<T>) set : new ArrayList<>(set));
		}
		this.numSets = this.sets.size();
		this.prefixFilter = problem.getPrefixFilter();
		this.currentTuple = new ArrayList<>();
	}

	@Override
	public IAlgorithmEvent nextWithException() throws InterruptedException, AlgorithmExecutionCanceledException, AlgorithmTimeoutedException {
		this.logger.debug("Conducting next algorithm step.");
		switch (this.getState()) {
		case CREATED:
			this.open.add(new Node(null, -1, 0, 0));
			this.numCreatedNodes++;
			this.logger.info("Created algorithm for LDS Relation among {} sets with the following cardinalities: {}.", this.sets.size(), this.sets.stream().map(Collection::size).collect(Collectors.toList()));
			return this.activate();

		case ACTIVE:
			this.checkAndConductTermination();
			if (this.open.isEmpty()) {
				this.logger.info("Nothing more to compute, return AlgorithmFinishedEvent.");
				return this.terminate();
			}

			/* determine next cheapest path to a leaf */
			Node next = null;
			Node newNode;
			while (!this.open.isEmpty() && (next = this.open.poll()).indexOfSet < this.numSets - 1) {
				int i = 0;
				int setIndex = next.indexOfSet + 1;
				List<T> set = this.sets.get(setIndex);
				int n = set.size();
				next.fillTupleArrayWithValues(this.currentTuple); // get current tuple
				for (int j = 0; j < n; j++) {
					this.checkAndConductTermination();
					long innerTimePoint = System.currentTimeMillis();
					this.currentTuple.add(set.get(j));
					assert (System.currentTimeMillis() - innerTimePoint) <= 20 : "Copying the " + (next.indexOfSet) + "-tuple " + this.currentTuple + " took " + (System.currentTimeMillis() - innerTimePoint) + "ms, which is way too much!";
					innerTimePoint = System.currentTimeMillis();
					boolean adopt = this.prefixFilter.test(this.currentTuple);
					this.logger.debug("Prefix filter outputs {} tuple {}", adopt, this.currentTuple);
					assert (System.currentTimeMillis() - innerTimePoint) < 1000 : "Testing the " + (next.indexOfSet) + "-tuple " + this.currentTuple + " took " + (System.currentTimeMillis() - innerTimePoint) + "ms, which is way too much!";
					if (adopt) {
						innerTimePoint = System.currentTimeMillis();
						if (this.recycledNodes.isEmpty()) {
							newNode = new Node();
							this.numCreatedNodes++;
							assert (System.currentTimeMillis() - innerTimePoint) < 5000 : "Creating a new node took " + (System.currentTimeMillis() - innerTimePoint) + "ms, which is way too much!\n" + this.computedTuples
							+ " tuples have been computed already.\nRecycling list contains " + this.recycledNodes.size() + "\nOPEN contains " + this.open.size();
						} else {
							newNode = this.recycledNodes.remove(0);
							assert (System.currentTimeMillis() - innerTimePoint) < 10 : "Retrieving node from recycle list " + (System.currentTimeMillis() - innerTimePoint) + "ms, which is way too much!\n" + this.computedTuples
							+ " tuples have been computed already.\nRecycling list contains " + this.recycledNodes.size() + "\nOPEN contains " + this.open.size();
						}
						newNode.parent = next;
						newNode.indexOfSet = next.indexOfSet + 1;
						newNode.defficiency = next.defficiency + i++;
						newNode.indexOfValue = j;
						this.open.add(newNode);
					}
					this.currentTuple.remove(setIndex);
				}
			}

			/* at this point, next should contain a fully specified tuple. If no next element exists, or the chosen node is not a leaf, terminate */
			if (next == null || next.indexOfSet < this.sets.size() - 1) {
				this.logger.info("No next tuple found, return AlgorithmFinishedEvent.");
				return this.terminate();
			}
			this.computedTuples++;
			if (this.computedTuples % this.reportRate == 0) {
				this.logger.info("Status report: {} have been created.", this.computedTuples);
			}

			/* load tuple of selected leaf node */
			next.fillTupleArrayWithValues(this.currentTuple);

			/* recycle leaf node and possibly also inner nodes*/
			next.recycle();
			this.numRecycledNodes++;
			List<T> tuple = new ArrayList<>(this.currentTuple);
			assert this.currentTuple.size() == this.numSets : "Tuple " + this.currentTuple + " should contain " + this.numSets + " elements but has " + this.currentTuple.size();
			this.logger.debug("Computed tuple {}", tuple);
			return new TupleOfCartesianProductFoundEvent<>(this, tuple);

		default:
			throw new IllegalStateException();
		}

	}

	@SuppressWarnings("unchecked")
	public List<T> nextTuple() throws InterruptedException, AlgorithmExecutionCanceledException, AlgorithmTimeoutedException {
		while (this.hasNext()) {
			IAlgorithmEvent e = this.nextWithException();
			if (e instanceof AlgorithmFinishedEvent) {
				throw new NoSuchElementException();
			} else if (e instanceof TupleOfCartesianProductFoundEvent) {
				return ((TupleOfCartesianProductFoundEvent<T>) e).getTuple();
			} else if (!(e instanceof AlgorithmInitializedEvent)) {
				throw new IllegalStateException("Cannot handle event of type " + e.getClass());
			}
		}
		throw new IllegalStateException("No more elements but no AlgorithmFinishedEvent was generated!");
	}

	@Override
	public List<List<T>> call() throws InterruptedException, AlgorithmExecutionCanceledException, AlgorithmTimeoutedException {
		List<List<T>> product = new ArrayList<>();
		List<T> nextTuple;
		try {
			while (this.hasNext() && (nextTuple = this.nextTuple()) != null) {
				assert nextTuple.size() == this.sets.size() : "The returned tuple " + nextTuple + " does not have " + this.sets.size() + " entries.";
				product.add(nextTuple);
			}
		} catch (NoSuchElementException e) {
			this.logger.info("No more solutions exist.");
		}
		this.logger.info("Returning a set of {} tuples.", product.size());
		return product;
	}

	public int getNumRecycledNodes() {
		return this.numRecycledNodes;
	}

	public int getNumCreatedNodes() {
		return this.numCreatedNodes;
	}

	@Override
	public void setLoggerName(final String name) {
		super.setLoggerName(name + "._algorithm");
		this.logger = LoggerFactory.getLogger(name);
		this.logger.info("Switched logger to {}", name);
		if (this.prefixFilter instanceof ILoggingCustomizable) {
			((ILoggingCustomizable) this.prefixFilter).setLoggerName(name + ".filter");
		}
	}

	@Override
	public String getLoggerName() {
		return this.logger.getName();
	}
}
