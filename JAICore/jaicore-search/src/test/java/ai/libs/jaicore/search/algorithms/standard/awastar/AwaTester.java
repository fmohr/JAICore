package ai.libs.jaicore.search.algorithms.standard.awastar;

import org.api4.java.ai.graphsearch.problem.IPathSearch;

import ai.libs.jaicore.search.algorithms.GraphSearchWithSubPathEvaluationUninformedTester;
import ai.libs.jaicore.search.algorithms.standard.awastar.AwaStarSearch;
import ai.libs.jaicore.search.probleminputs.GraphSearchWithSubpathEvaluationsInput;

public class AwaTester extends GraphSearchWithSubPathEvaluationUninformedTester {

	@Override
	public <N, A> IPathSearch<?, ?, N, A> getSearchAlgorithm(GraphSearchWithSubpathEvaluationsInput<N, A, Double> problem) {
		 return new AwaStarSearch<>(problem);
	}

}
