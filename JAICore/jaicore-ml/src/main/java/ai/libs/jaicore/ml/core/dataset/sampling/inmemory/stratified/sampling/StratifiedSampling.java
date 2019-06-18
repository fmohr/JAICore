package ai.libs.jaicore.ml.core.dataset.sampling.inmemory.stratified.sampling;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.basic.algorithm.events.AlgorithmEvent;
import ai.libs.jaicore.basic.algorithm.exceptions.AlgorithmException;
import ai.libs.jaicore.ml.core.dataset.IDataset;
import ai.libs.jaicore.ml.core.dataset.IInstance;
import ai.libs.jaicore.ml.core.dataset.sampling.SampleElementAddedEvent;
import ai.libs.jaicore.ml.core.dataset.sampling.inmemory.ASamplingAlgorithm;
import ai.libs.jaicore.ml.core.dataset.sampling.inmemory.SimpleRandomSampling;
import ai.libs.jaicore.ml.core.dataset.sampling.inmemory.WaitForSamplingStepEvent;

/**
 * Implementation of Stratified Sampling: Divide dataset into strati and sample
 * from each of these.
 * 
 * @author Lukas Brandt
 */
public class StratifiedSampling<I extends IInstance> extends ASamplingAlgorithm<I> {

	private Logger logger = LoggerFactory.getLogger(StratifiedSampling.class);
	private IStratiAmountSelector<I> stratiAmountSelector;
	private IStratiAssigner<I> stratiAssigner;
	private Random random;
	private IDataset<I>[] strati = null;
	private IDataset<I> datasetCopy;
	private ExecutorService executorService;
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
	public StratifiedSampling(IStratiAmountSelector<I> stratiAmountSelector, IStratiAssigner<I> stratiAssigner,
			Random random, IDataset<I> input) {
		super(input);
		this.stratiAmountSelector = stratiAmountSelector;
		this.stratiAssigner = stratiAssigner;
		this.random = random;
	}

	@Override
	public AlgorithmEvent nextWithException() throws InterruptedException, AlgorithmException {
		switch (this.getState()) {
		case created:
			this.sample = getInput().createEmpty();
			if (!allDatapointsAssigned) {
				this.datasetCopy = getInput().createEmpty();
				this.datasetCopy.addAll(this.getInput());
				this.stratiAmountSelector.setNumCPUs(this.getNumCPUs());
				this.stratiAssigner.setNumCPUs(this.getNumCPUs());
				this.strati = new IDataset[this.stratiAmountSelector.selectStratiAmount(this.datasetCopy)];
				for (int i = 0; i < this.strati.length; i++) {
					this.strati[i] = getInput().createEmpty();
				}
				this.stratiAssigner.init(this.datasetCopy, this.strati.length);
			}
			this.simpleRandomSamplingStarted = false;
			this.executorService = Executors.newCachedThreadPool();
			return this.activate();
		case active:
			if (this.sample.size() < this.sampleSize) {
				if (!allDatapointsAssigned) {
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
					return new SampleElementAddedEvent(getId());
				} else {
					if (!simpleRandomSamplingStarted) {
						// Simple Random Sampling has not started yet -> Initialize one sampling thread
						// per stratum.
						this.startSimpleRandomSamplingForStrati();
						this.simpleRandomSamplingStarted = true;
						return new WaitForSamplingStepEvent(getId());
					} else {
						// Check if all threads are finished. If yes finish Stratified Sampling, wait
						// shortly in this step otherwise.
						if (this.executorService.isTerminated()) {
							return this.terminate();
						} else {
							Thread.sleep(100);
							return new WaitForSamplingStepEvent(getId());

						}
					}
				}
			} else {
				return this.terminate();
			}
		case inactive:
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
			sampleSizeForStrati[i] = Math.round(
					(float) (this.sampleSize * ((double) this.strati[i].size() / (double) this.getInput().size())));
		}

		// Start a Simple Random Sampling thread for each stratum
		for (int i = 0; i < this.strati.length; i++) {
			int index = i;
			this.executorService.execute(() -> {
				SimpleRandomSampling<I> simpleRandomSampling = new SimpleRandomSampling<>(random, strati[index]);
				simpleRandomSampling.setSampleSize(sampleSizeForStrati[index]);
				try {
					synchronized (sample) {
						sample.addAll(simpleRandomSampling.call());
					}
				} catch (Exception e) {
					logger.error("Unexpected exception during simple random sampling!", e);
				}

			});
		}
		// Prevent executor service from more threads being added.
		this.executorService.shutdown();
	}

	public IDataset<I>[] getStrati() {
		return strati;
	}

	public void setStrati(IDataset<I>[] strati) {
		this.strati = strati;
		this.allDatapointsAssigned = true;
	}

}
