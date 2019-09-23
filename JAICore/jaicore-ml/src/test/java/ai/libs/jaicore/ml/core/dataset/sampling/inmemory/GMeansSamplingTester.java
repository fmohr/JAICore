package ai.libs.jaicore.ml.core.dataset.sampling.inmemory;

import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.algorithm.IAlgorithm;

import ai.libs.jaicore.ml.core.filter.sampling.IClusterableInstance;
import ai.libs.jaicore.ml.core.filter.sampling.inmemory.factories.GmeansSamplingFactory;

public class GMeansSamplingTester extends GeneralSamplingTester<Number> {

	private static final long SEED = 1;
	private static final double DEFAULT_SAMPLE_FRACTION = 0.1;

	@Override
	public void testSampleSizeLargeProblem() {
		// Sample Size is not supported for GMeansSampling
		assertTrue(true);
	}

	@Override
	public void testSampleSizeSmallProblem() {
		// Sample Size is not supported for GMeansSampling
		assertTrue(true);
	}

	@Override
	public IAlgorithm<?, ?> getAlgorithm(final ILabeledDataset<IClusterableInstance> dataset) {
		GmeansSamplingFactory<IClusterableInstance, ILabeledDataset<IClusterableInstance>> factory = new GmeansSamplingFactory<>();
		if (dataset != null) {
			factory.setClusterSeed(SEED);
			int sampleSize = (int) (DEFAULT_SAMPLE_FRACTION * dataset.size());
			return factory.getAlgorithm(sampleSize, dataset, new Random(SEED));
		}
		return null;
	}
}
