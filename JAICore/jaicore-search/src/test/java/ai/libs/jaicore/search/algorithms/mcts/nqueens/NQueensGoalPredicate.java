package ai.libs.jaicore.search.algorithms.mcts.nqueens;

import org.api4.java.ai.graphsearch.problem.implicit.graphgenerator.INodeGoalTester;

import ai.libs.jaicore.search.exampleproblems.nqueens.QueenNode;

public class NQueensGoalPredicate implements INodeGoalTester<QueenNode, String> {

	private final int dimension;

	public NQueensGoalPredicate(final int dimension) {
		super();
		this.dimension = dimension;
	}

	@Override
	public boolean isGoal(final QueenNode n) {
		return n.getNumberOfQueens() == this.dimension;
	}

}
