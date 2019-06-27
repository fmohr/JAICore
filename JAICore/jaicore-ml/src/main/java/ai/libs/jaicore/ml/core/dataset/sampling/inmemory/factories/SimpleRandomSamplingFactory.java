package ai.libs.jaicore.ml.core.dataset.sampling.inmemory.factories;

import java.util.Random;

import ai.libs.jaicore.ml.core.dataset.IOrderedDataset;
import ai.libs.jaicore.ml.core.dataset.sampling.inmemory.SimpleRandomSampling;
import ai.libs.jaicore.ml.core.dataset.sampling.inmemory.factories.interfaces.ISamplingAlgorithmFactory;

public class SimpleRandomSamplingFactory<I, D extends IOrderedDataset<I>> implements ISamplingAlgorithmFactory<I, D, SimpleRandomSampling<I, D>> {

	@Override
	public SimpleRandomSampling<I, D> getAlgorithm(final int sampleSize, final D inputDataset, final Random random) {
		SimpleRandomSampling<I, D> simpleRandomSampling = new SimpleRandomSampling<>(random, inputDataset);
		simpleRandomSampling.setSampleSize(sampleSize);
		return simpleRandomSampling;
	}

}
