package ai.libs.jaicore.ml.core.filter.sampling.inmemory.stratified.sampling;

import java.util.Random;

import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledInstance;
import org.api4.java.ai.ml.core.exception.DatasetCreationException;
import org.api4.java.algorithm.events.AlgorithmEvent;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.ml.core.filter.sampling.SampleElementAddedEvent;
import ai.libs.jaicore.ml.core.filter.sampling.inmemory.ASamplingAlgorithm;
import ai.libs.jaicore.ml.core.filter.sampling.inmemory.SimpleRandomSampling;
import ai.libs.jaicore.ml.core.filter.sampling.inmemory.WaitForSamplingStepEvent;

/**
 * Implementation of Stratified Sampling: Divide dataset into strati and sample
 * from each of these.
 *
 * @author Lukas Brandt
 */
public class StratifiedSampling<I extends ILabeledInstance, D extends ILabeledDataset<I>> extends ASamplingAlgorithm<I, D> {

	private Logger logger = LoggerFactory.getLogger(StratifiedSampling.class);
	private IStratiAmountSelector<I, D> stratiAmountSelector;
	private IStratiAssigner<I, D> stratiAssigner;
	private Random random;
	private ILabeledDataset<I>[] strati = null;
	private D datasetCopy;
	private boolean allDatapointsAssigned = false;
	private boolean simpleRandomSamplingStarted;

	/**
	 * Constructor for Stratified Sampling.
	 *
	 * @param stratiAmountSelector
	 *            The custom selector for the used amount of strati.
	 * @param stratiAssigner
	 *            Custom logic to assign datapoints into strati.
	 * @param random
	 *            Random object for sampling inside of the strati.
	 */
	public StratifiedSampling(final IStratiAmountSelector<I, D> stratiAmountSelector, final IStratiAssigner<I, D> stratiAssigner, final Random random, final D input) {
		super(input);
		this.stratiAmountSelector = stratiAmountSelector;
		this.stratiAssigner = stratiAssigner;
		this.random = random;
	}

	@SuppressWarnings("unchecked")
	@Override
	public AlgorithmEvent nextWithException() throws InterruptedException, AlgorithmException {
		switch (this.getState()) {
		case CREATED:
			try {
				this.sample = (D) this.getInput().createEmptyCopy();
				if (!this.allDatapointsAssigned) {
					this.datasetCopy = (D) this.getInput().createEmptyCopy();
					this.datasetCopy.addAll(this.getInput());
					this.stratiAmountSelector.setNumCPUs(this.getNumCPUs());
					this.stratiAssigner.setNumCPUs(this.getNumCPUs());
					this.strati = new ILabeledDataset[this.stratiAmountSelector.selectStratiAmount(this.datasetCopy)];
					for (int i = 0; i < this.strati.length; i++) {
						this.strati[i] = (D) this.getInput().createEmptyCopy();
					}
					this.stratiAssigner.init(this.datasetCopy, this.strati.length);
				}
				this.simpleRandomSamplingStarted = false;
			} catch (DatasetCreationException e) {
				throw new AlgorithmException("Could not create a copy of the dataset.", e);
			}
			return this.activate();
		case ACTIVE:
			if (this.sample.size() < this.sampleSize) {
				if (!this.allDatapointsAssigned) {
					// Stratify the datapoints one by one.
					I datapoint = this.datasetCopy.remove(0);
					int assignedStrati = this.stratiAssigner.assignToStrati(datapoint);
					if (assignedStrati < 0 || assignedStrati >= this.strati.length) {
						throw new AlgorithmException("No existing strati for index " + assignedStrati);
					} else {
						this.strati[assignedStrati].add(datapoint);
					}
					if (this.datasetCopy.isEmpty()) {
						this.allDatapointsAssigned = true;
					}
					return new SampleElementAddedEvent(this.getId());
				} else {
					if (!this.simpleRandomSamplingStarted) {
						// Simple Random Sampling has not started yet -> Initialize one sampling thread
						// per stratum.
						this.startSimpleRandomSamplingForStrati();
						this.simpleRandomSamplingStarted = true;
						return new WaitForSamplingStepEvent(this.getId());
					} else {
						// Check if all threads are finished. If yes finish Stratified Sampling, wait
						// shortly in this step otherwise.
						return this.terminate();
					}
				}
			} else {
				return this.terminate();
			}
		case INACTIVE:
			if (this.sample.size() < this.sampleSize) {
				throw new AlgorithmException("Expected sample size was not reached before termination");
			} else {
				return this.terminate();
			}
		default:
			throw new IllegalStateException("Unknown algorithm state " + this.getState());
		}
	}

	/**
	 * Calculates the necessary sample sizes and start a Simple Random Sampling
	 * Thread for each stratum.
	 */
	private void startSimpleRandomSamplingForStrati() {
		// Calculate the amount of datapoints that will be used from each strati
		int[] sampleSizeForStrati = new int[this.strati.length];
		// Calculate for each stratum the sample size by StratiSize / DatasetSize
		for (int i = 0; i < this.strati.length; i++) {
			sampleSizeForStrati[i] = Math.round((float) (this.sampleSize * (this.strati[i].size() / (double) this.getInput().size())));
		}

		// Start a Simple Random Sampling thread for each stratum
		for (int i = 0; i < this.strati.length; i++) {

			@SuppressWarnings("unchecked")
			SimpleRandomSampling<I, D> simpleRandomSampling = new SimpleRandomSampling<>(this.random, (D) this.strati[i]);
			simpleRandomSampling.setSampleSize(sampleSizeForStrati[i]);
			try {
				synchronized (this.sample) {
					this.sample.addAll(simpleRandomSampling.call());
				}
			} catch (Exception e) {
				this.logger.error("Unexpected exception during simple random sampling!", e);
			}
		}
	}

	public ILabeledDataset<I>[] getStrati() {
		return this.strati;
	}

	public void setStrati(final ILabeledDataset<I>[] strati) {
		this.strati = strati;
	}
}
