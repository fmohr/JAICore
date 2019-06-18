package ai.libs.jaicore.ml.core.dataset.sampling.inmemory.factories;

import java.util.Random;

import ai.libs.jaicore.ml.core.dataset.IDataset;
import ai.libs.jaicore.ml.core.dataset.IInstance;
import ai.libs.jaicore.ml.core.dataset.sampling.inmemory.casecontrol.OSMAC;
import ai.libs.jaicore.ml.core.dataset.sampling.inmemory.factories.interfaces.IRerunnableSamplingAlgorithmFactory;

public class OSMACSamplingFactory<I extends IInstance> implements IRerunnableSamplingAlgorithmFactory<I, OSMAC<I>> {

	private OSMAC<I> previousRun;
	private int preSampleSize = -1;

	/**
	 * Set the size of the sample the pilot estimator will be trained with. Default
	 * is half the dataset.
	 * 
	 * @param preSampleSize
	 */
	public void setPreSampleSize(int preSampleSize) {
		this.preSampleSize = preSampleSize;
	}

	@Override
	public void setPreviousRun(OSMAC<I> previousRun) {
		this.previousRun = previousRun;
	}

	@Override
	public OSMAC<I> getAlgorithm(int sampleSize, IDataset<I> inputDataset, Random random) {
		OSMAC<I> osmac = new OSMAC<>(random, this.preSampleSize, inputDataset);
		if (this.previousRun != null && this.previousRun.getProbabilityBoundaries() != null) {
			osmac.setProbabilityBoundaries(this.previousRun.getProbabilityBoundaries());
			osmac.setChosenInstance(previousRun.getChosenInstance());
		}
		osmac.setSampleSize(sampleSize);
		return osmac;
	}

}
