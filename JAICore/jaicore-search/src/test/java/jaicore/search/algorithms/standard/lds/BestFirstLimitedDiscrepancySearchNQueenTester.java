package jaicore.search.algorithms.standard.lds;

import jaicore.basic.algorithm.AlgorithmProblemTransformer;
import jaicore.search.core.interfaces.IGraphSearchFactory;
import jaicore.search.model.other.EvaluatedSearchGraphPath;
import jaicore.search.probleminputs.GraphSearchWithNodeRecommenderInput;
import jaicore.search.testproblems.nqueens.NQueenTester;
import jaicore.search.testproblems.nqueens.NQueensToNodeRecommendedTreeReducer;
import jaicore.search.testproblems.nqueens.QueenNode;

public class BestFirstLimitedDiscrepancySearchNQueenTester extends NQueenTester<GraphSearchWithNodeRecommenderInput<QueenNode,String>,EvaluatedSearchGraphPath<QueenNode,String,Double>> {

	@Override
	public AlgorithmProblemTransformer<Integer, GraphSearchWithNodeRecommenderInput<QueenNode, String>> getProblemReducer() {
		return new NQueensToNodeRecommendedTreeReducer();
	}

	@Override
	public IGraphSearchFactory<GraphSearchWithNodeRecommenderInput<QueenNode, String>, EvaluatedSearchGraphPath<QueenNode, String, Double>, QueenNode, String> getFactory() {
		return new BestFirstLimitedDiscrepancySearchFactory<>();
	}
	
	
}
