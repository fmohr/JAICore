package ai.libs.jaicore.ml.core.filter.sampling.inmemory;

import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.algorithm.IAlgorithm;

import ai.libs.jaicore.basic.algorithm.IAlgorithmTestProblemSet;
import ai.libs.jaicore.ml.core.filter.sampling.IClusterableInstance;
import ai.libs.jaicore.ml.core.filter.sampling.inmemory.factories.KmeansSamplingFactory;

public class KMeansSamplingTester extends GeneralSamplingTester<Number> {

	private static final long SEED = 1;
	private static final double DEFAULT_SAMPLE_FRACTION = 0.1;
	private static final int K = -1;
	private static final int MAX_ITERATIONS = 100;

	@Override
	public void testSampleSizeSimpleProblem(final MemoryBasedSamplingAlgorithmTestProblemSet set) {
		// Sample Size is not supported for KMeansSampling
		assertTrue(true);
	}

	@Override
	public void testSampleSizeMediumProblem(final MemoryBasedSamplingAlgorithmTestProblemSet set) {
		// Sample Size is not supported for KMeansSampling
		assertTrue(true);
	}

	@Override
	public void testSampleSizeLargeProblem(final MemoryBasedSamplingAlgorithmTestProblemSet set) {
		// Sample Size is not supported for KMeansSampling
		assertTrue(true);
	}

	@Override
	public void testNoDuplicatesLargeProblem(final MemoryBasedSamplingAlgorithmTestProblemSet set) {
		// KMeansSampling cannot be used for too large problems, because it is too slow
		assertTrue(true);
	}

	@Override
	public IAlgorithm<?, ?> getAlgorithm(final ILabeledDataset<?> dataset) {
		KmeansSamplingFactory<IClusterableInstance, ILabeledDataset<IClusterableInstance>> factory = new KmeansSamplingFactory<>();
		if (dataset != null) {
			factory.setClusterSeed(SEED);
			factory.setK(K);
			int sampleSize = (int) (DEFAULT_SAMPLE_FRACTION * dataset.size());
			factory.setMaxIterations(MAX_ITERATIONS);
			return factory.getAlgorithm(sampleSize, (ILabeledDataset<IClusterableInstance>)dataset, new Random(SEED));
		}
		return null;
	}

	@Override
	public void testTimeout(final IAlgorithmTestProblemSet<?> problemSet) {
		/* skip this test, because the used G-Means implementation is not interruptible (and hence not timeoutable and not cancelable) */
		assertTrue(true);
	}

	@Override
	public void testTimeoutWhenParallelized(final IAlgorithmTestProblemSet<?> problemSet) {
		/* skip this test, because the used G-Means implementation is not interruptible (and hence not timeoutable and not cancelable) */
		assertTrue(true);
	}

	@Override
	public void testInterrupt(final IAlgorithmTestProblemSet<?> problemSet) {
		/* skip this test, because the used G-Means implementation is not interruptible (and hence not timeoutable and not cancelable) */
		assertTrue(true);
	}

	@Override
	public void testInterruptWhenParallelized(final IAlgorithmTestProblemSet<?> problemSet) {
		/* skip this test, because the used G-Means implementation is not interruptible (and hence not timeoutable and not cancelable) */
		assertTrue(true);
	}

	@Override
	public void testCancel(final IAlgorithmTestProblemSet<?> problemSet) {
		/* skip this test, because the used G-Means implementation is not interruptible (and hence not timeoutable and not cancelable) */
		assertTrue(true);
	}

	@Override
	public void testCancelWhenParallelized(final IAlgorithmTestProblemSet<?> problemSet) {
		/* skip this test, because the used G-Means implementation is not interruptible (and hence not timeoutable and not cancelable) */
		assertTrue(true);
	}
}
