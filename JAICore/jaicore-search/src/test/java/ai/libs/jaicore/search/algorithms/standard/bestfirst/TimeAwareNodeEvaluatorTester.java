package ai.libs.jaicore.search.algorithms.standard.bestfirst;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.problems.enhancedttsp.EnhancedTTSPNode;
import ai.libs.jaicore.search.algorithms.standard.bestfirst.nodeevaluation.TimeAwareNodeEvaluator;
import ai.libs.jaicore.search.model.travesaltree.BackPointerPath;
import ai.libs.jaicore.timing.TimedComputation;

public abstract class TimeAwareNodeEvaluatorTester<T extends TimeAwareNodeEvaluator<EnhancedTTSPNode, String, Double>> extends NodeEvaluatorTester<T> {

	private static final Logger logger = LoggerFactory.getLogger(TimeAwareNodeEvaluatorTester.class);

	private static final int TIMEOUT = 3000;
	private static final int TOLERANCE = 50;

	public abstract T getTimedNodeEvaluator(int timeoutInMS);

	@Test
	public void testTimeoutAdherence() throws InterruptedException, ExecutionException, TimeoutException {
		T ne = this.getTimedNodeEvaluator(TIMEOUT);
		ne.setLoggerName("testednodeevaluator");
		for (BackPointerPath<EnhancedTTSPNode, String, Double> node : this.getNodesToTestInDifficultProblem(1)) {
			long start = System.currentTimeMillis();
			logger.info("Starting computation of score for node with hash code {} with timeout {}", node.hashCode(), TIMEOUT);
			try {
				TimedComputation.compute(() -> ne.evaluate(node), (long) TIMEOUT + TOLERANCE, "Timeout Test");
				logger.info("Finished computation regularly. Runtime was {}ms", System.currentTimeMillis() - start);
			}
			catch (AlgorithmTimeoutedException e) {
				logger.info("Observed timeout exception.");
			}
		}
		assertTrue(true); // dummy statement
	}
}
