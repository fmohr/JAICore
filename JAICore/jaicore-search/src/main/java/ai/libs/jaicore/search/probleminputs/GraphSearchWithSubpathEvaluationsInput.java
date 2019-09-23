package ai.libs.jaicore.search.probleminputs;

import org.api4.java.ai.graphsearch.problem.IGraphSearchInput;
import org.api4.java.ai.graphsearch.problem.implicit.graphgenerator.PathGoalTester;
import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.IPathEvaluator;
import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.PathEvaluationException;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;
import org.api4.java.datastructure.graph.implicit.IGraphGenerator;

import ai.libs.jaicore.search.model.travesaltree.BackPointerPath;

/**
 * Many algorithms such as best first and A* use a traversal tree to browse the underlying
 * graph. Each node in this tree corresponds to a node in the original graph but has only
 * one predecessor, which may be updated over time.
 *
 * The underlying class Node<T,V> implicitly defines a back pointer PATH from the node to
 * the root. Therefore, evaluating a node of this class equals evaluating a path in the
 * original graph.
 *
 * @author fmohr
 *
 * @param <N>
 * @param <A>
 * @param <V>
 */
public class GraphSearchWithSubpathEvaluationsInput<N, A, V extends Comparable<V>> extends GraphSearchWithPathEvaluationsInput<N, A, V> {
	private final IPathEvaluator<N, A, V> nodeEvaluator;

	public GraphSearchWithSubpathEvaluationsInput(final IGraphSearchInput<N, A> graphSearchInput, final IPathEvaluator<N, A, V> nodeEvaluator) {
		this(graphSearchInput.getGraphGenerator(), graphSearchInput.getGoalTester(), nodeEvaluator);
	}

	public GraphSearchWithSubpathEvaluationsInput(final IGraphGenerator<N, A> graphGenerator, final PathGoalTester<N, A> goalTester, final IPathEvaluator<N, A, V> nodeEvaluator) {
		super(graphGenerator, goalTester, p -> {
			try {
				return nodeEvaluator.evaluate(new BackPointerPath<>(null, p.getHead(), null));
			} catch (PathEvaluationException e) {
				throw new ObjectEvaluationFailedException("Could not evaluate path", e);
			}
		});
		this.nodeEvaluator = nodeEvaluator;
	}

	public IPathEvaluator<N, A, V> getNodeEvaluator() {
		return this.nodeEvaluator;
	}
}
