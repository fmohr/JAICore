package jaicore.search.algorithms.standard.random;

import org.junit.Test;

import jaicore.basic.algorithm.AlgorithmProblemTransformer;
import jaicore.search.core.interfaces.IGraphSearchFactory;
import jaicore.search.model.other.SearchGraphPath;
import jaicore.search.probleminputs.GraphSearchInput;
import jaicore.search.testproblems.enhancedttsp.EnhancedTTSP;
import jaicore.search.testproblems.enhancedttsp.EnhancedTTSPNode;
import jaicore.search.testproblems.enhancedttsp.EnhancedTTSPTester;

public class RandomSearchEnhancedTTSPTester extends EnhancedTTSPTester<GraphSearchInput<EnhancedTTSPNode, String>, SearchGraphPath<EnhancedTTSPNode, String>> {



	@Override
	public IGraphSearchFactory<GraphSearchInput<EnhancedTTSPNode, String>, SearchGraphPath<EnhancedTTSPNode, String>, EnhancedTTSPNode, String> getFactory() {
		return new RandomSearchFactory<>();
	}

	@Override
	public AlgorithmProblemTransformer<EnhancedTTSP, GraphSearchInput<EnhancedTTSPNode, String>> getProblemReducer() {
		return new AlgorithmProblemTransformer<EnhancedTTSP, GraphSearchInput<EnhancedTTSPNode,String>>() {

			@Override
			public GraphSearchInput<EnhancedTTSPNode, String> transform(final EnhancedTTSP problem) {
				return new GraphSearchInput<>(problem.getGraphGenerator());
			}
		};
	}

	@Override
	@Test
	public void testQuickTimeout() throws Exception {

		/* we cannot create sufficiently difficult instances of this problem in general for this algorithm to require at least 5 seconds */
	}

	@Override
	@Test
	public void testCancel() throws Exception {

		/* we cannot create sufficiently difficult instances of this problem in general for this algorithm to require at least 5 seconds */
	}

	@Override
	@Test
	public void testInterrupt() throws Exception {

		/* we cannot create sufficiently difficult instances of this problem in general for this algorithm to require at least 5 seconds */
	}
}
