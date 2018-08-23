package jaicore.search.algorithms.standard.awastar;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jaicore.search.algorithms.standard.core.INodeEvaluator;
import jaicore.search.structure.core.GraphGenerator;
import jaicore.search.structure.core.Node;
import jaicore.search.structure.core.NodeExpansionDescription;
import jaicore.search.structure.core.OpenCollection;
import jaicore.search.structure.core.PriorityQueueOpen;
import jaicore.search.structure.graphgenerator.NodeGoalTester;
import jaicore.search.structure.graphgenerator.SingleRootGenerator;
import jaicore.search.structure.graphgenerator.SuccessorGenerator;

public class AwaStarSearch<T, A, V extends Comparable<V>>{

	private static final Logger logger = LoggerFactory.getLogger(AwaStarSearch.class);

	private class Search implements Callable<T> {
		
		private SuccessorGenerator<T, A> successorGenerator;
		private NodeGoalTester<T> goalTester;
		private INodeEvaluator<T, V> nodeEvaluator;
		private OpenCollection<Node<T, V>> closedList, suspendList, openList;
		private int windowSize;
		private V bestScore;
		private T bestSolution;
		
		public Search(GraphGenerator<T, A> graphGenerator, INodeEvaluator<T, V> nodeEvaluator) throws Throwable {
			successorGenerator = graphGenerator.getSuccessorGenerator();
			goalTester = (NodeGoalTester<T>)graphGenerator.getGoalTester();
			this.nodeEvaluator = nodeEvaluator;
			closedList = new PriorityQueueOpen<>();
			suspendList = new PriorityQueueOpen<>();
			openList = new PriorityQueueOpen<>();
			windowSize = 0;
			Node<T, V> rootNode = new Node<T, V>(null, ((SingleRootGenerator<T>)graphGenerator.getRootGenerator()).getRoot());
			rootNode.setInternalLabel(this.nodeEvaluator.f(rootNode));
			rootNode.setAnnotation("level", 0);
			openList.add(rootNode);
			bestScore = null;
		}

		@Override
		public T call() throws Exception {
			do {
				closedList.addAll(openList);
				openList.addAll(suspendList);
				suspendList.clear();
				bestSolution = windowAStar();
				windowSize++;
			} while (!suspendList.isEmpty());
			return bestSolution;
		}
		
		private T windowAStar() {
			int currentLevel = -1;
			while (!openList.isEmpty()) {
				Node<T, V> n = openList.peek();
				openList.remove(n);
				closedList.add(n);
				V nScore = n.getInternalLabel();
				int nLevel = (int) n.getAnnotation("level");
				if (nScore != null && bestScore != null && nScore.compareTo(bestScore) >= 0) {
					return bestSolution;
				} else {
					if (nLevel <= (currentLevel - windowSize)) {
						closedList.remove(n);
						suspendList.add(n);
					} else {
						if (nLevel > currentLevel) {
							currentLevel = nLevel;
						}
						if (n.isGoal()) {
							n.setGoal(true);
							bestScore = n.getInternalLabel();
							bestSolution = n.getPoint();
							return bestSolution;
						}
						Collection<NodeExpansionDescription<T, A>> successors = successorGenerator.generateSuccessors(n.getPoint());
						for (NodeExpansionDescription<T, A> expansionDescription : successors) {
							Node<T, V> nPrime = new Node<>(n, expansionDescription.getTo());
							nPrime.setGoal(goalTester.isGoal(nPrime.getPoint()));
							V nPrimeScore;
							try {
								nPrimeScore = nodeEvaluator.f(nPrime);
								if (!openList.contains(nPrime) && !closedList.contains(nPrime) && !suspendList.contains(nPrime)) {
									nPrime.setParent(n);
									nPrime.setInternalLabel(nPrimeScore);
									nPrime.setAnnotation("level", ((int)n.getAnnotation("level")) + 1);
									openList.add(nPrime);
								} else if(openList.contains(nPrime) || suspendList.contains(nPrime)) {
									V oldScore = nPrime.getInternalLabel();
									if (oldScore != null) {
										if (oldScore.compareTo(nPrimeScore) > 0) {
											nPrime.setParent(n);
											nPrime.setInternalLabel(nPrimeScore);
											nPrime.setAnnotation("level", ((int)n.getAnnotation("level")) + 1);
										}
									}
								} else if(closedList.contains(nPrime)) {
									V oldScore = nPrime.getInternalLabel();
									if (oldScore != null && oldScore.compareTo(nPrimeScore) > 0) {
										nPrime.setParent(n);
										nPrime.setInternalLabel(nPrimeScore);
										nPrime.setAnnotation("level", ((int)n.getAnnotation("level")) + 1);
									}
									openList.add(nPrime);
								}
								
							} catch (Throwable e) {
								logger.error(e.getMessage());
							}
						}
					}
				}
			}
			return bestSolution;
		}

	}

	private AwaStarSearch.Search search; 
	
	public AwaStarSearch(GraphGenerator<T, A> graphGenerator, INodeEvaluator<T, V> nodeEvaluator) throws Throwable {
		this.search = new Search(graphGenerator, nodeEvaluator);
	}

	public T search(int timeout) throws Exception {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<T> future =  executor.submit(search);
		executor.shutdown();
		T bestSolution = null;
		try {
			bestSolution = future.get(timeout, TimeUnit.SECONDS);
		}
		finally {
			if (!executor.isTerminated()) {
				executor.shutdownNow();
			}
		}
		return bestSolution;
	}

}
