package ai.libs.jaicore.ml.core.evaluation.evaluator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.api4.java.ai.ml.classification.IClassifier;
import org.api4.java.ai.ml.core.dataset.splitter.IDatasetSplitter;
import org.api4.java.ai.ml.core.dataset.splitter.SplitFailedException;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledInstance;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;
import org.api4.java.common.control.ILoggingCustomizable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import ai.libs.jaicore.basic.events.IEvent;
import ai.libs.jaicore.basic.events.IEventEmitter;
import ai.libs.jaicore.ml.core.evaluation.evaluator.events.MCCVSplitEvaluationEvent;
import ai.libs.jaicore.ml.core.evaluation.evaluator.splitevaluation.AbstractSplitBasedClassifierEvaluator;
import ai.libs.jaicore.ml.core.evaluation.evaluator.splitevaluation.ISplitBasedClassifierEvaluator;
import ai.libs.jaicore.ml.core.timeseries.util.WekaUtil;
import weka.classifiers.Classifier;
import weka.core.Instances;

/**
 * A classifier evaluator that can perform a (monte-carlo)cross-validation on
 * the given dataset. Thereby, it uses the
 * {@link AbstractSplitBasedClassifierEvaluator} to evaluate the classifier on a random
 * split of the dataset.
 *
 * @author fmohr, joshua
 *
 */
public class MonteCarloCrossValidationEvaluator<I extends ILabeledInstance, D extends ILabeledDataset<I>> implements IClassifierEvaluator, ILoggingCustomizable, IEventEmitter {

	private final EventBus eventBus = new EventBus();
	private boolean hasListeners;
	private Logger logger = LoggerFactory.getLogger(MonteCarloCrossValidationEvaluator.class);
	private boolean canceled = false;
	private final IDatasetSplitter<I, D> datasetSplitter;
	private final int repeats;
	private final Instances data;
	private final double trainingPortion;
	private final long seed;

	/* Can either compute the loss or cache it */
	private final ISplitBasedClassifierEvaluator<Double, I, D> splitBasedEvaluator;

	private final Map<Long, List<Instances>> splitCache = new HashMap<>();

	public MonteCarloCrossValidationEvaluator(final ISplitBasedClassifierEvaluator<Double, I, D> splitBasedEvaluator, final IDatasetSplitter<I, D> datasetSplitter, final int repeats, final Instances data, final double trainingPortion,
			final long seed) {
		super();
		if (data == null) {
			throw new IllegalArgumentException("Cannot work with NULL data");
		}
		if (splitBasedEvaluator == null) {
			throw new IllegalArgumentException("Cannot work with NULL split based evaluator");
		}
		this.datasetSplitter = datasetSplitter;
		this.repeats = repeats;
		this.splitBasedEvaluator = splitBasedEvaluator;
		if (this.splitBasedEvaluator instanceof IEventEmitter) {
			((IEventEmitter) splitBasedEvaluator).registerListener(this);
		}
		this.data = data;
		this.trainingPortion = trainingPortion;
		this.seed = seed;
	}

	public MonteCarloCrossValidationEvaluator(final ISplitBasedClassifierEvaluator<Double, I, D> splitBasedEvaluator, final int repeats, final Instances data, final double trainingPortion, final long seed) {
		this(splitBasedEvaluator, new MulticlassClassStratifiedSplitter(), repeats, data, trainingPortion, seed);
	}

	public void cancel() {
		this.logger.info("Received cancel");
		this.canceled = true;
	}

	@Override
	public Double f(final IClassifier pl) throws ObjectEvaluationFailedException, InterruptedException {
		return this.evaluate(pl, new DescriptiveStatistics());
	}

	public Double f(final Classifier pl, final DescriptiveStatistics stats) throws ObjectEvaluationFailedException, InterruptedException {
		if (pl == null) {
			throw new IllegalArgumentException("Cannot compute score for null pipeline!");
		}

		long startTimestamp = System.currentTimeMillis();
		/* perform random stratified split */
		this.logger.info("Starting MMCV evaluation of {} (Description: {})", pl.getClass().getName(), WekaUtil.getClassifierDescriptor(pl));
		for (int i = 0; i < this.repeats && !this.canceled; i++) {
			this.logger.debug("Obtaining predictions of {} for split #{}/{}", pl, i + 1, this.repeats);
			if (Thread.interrupted()) { // clear the interrupted field. This is Java a general convention when an InterruptedException is thrown (see Java documentation for details)
				this.logger.info("MCCV has been interrupted, leaving MCCV.");
				throw new InterruptedException("MCCV has been interrupted.");
			}

			if (!this.splitCache.containsKey(this.seed + i)) {
				try {
					this.splitCache.put(this.seed + i, this.datasetSplitter.split(this.data, this.seed + i, this.trainingPortion));
				} catch (SplitFailedException e) {
					throw new ObjectEvaluationFailedException("Could not evaluate classifier!", e);
				}
			}
			List<Instances> split = this.splitCache.get(this.seed + i);

			try {
				long startTimeForSplitEvaluation = System.currentTimeMillis();
				double score = this.splitBasedEvaluator.evaluateSplit(pl, split.get(0), split.get(1));
				if (this.hasListeners) {
					this.eventBus.post(new MCCVSplitEvaluationEvent(pl, split.get(0).size(), split.get(1).size(), (int) (System.currentTimeMillis() - startTimeForSplitEvaluation), score));
				}
				this.logger.info("Score for evaluation of {} with split #{}/{}: {} after {}ms", pl.getClass().getName(), i + 1, this.repeats, score, (System.currentTimeMillis() - startTimestamp));
				stats.addValue(score);
			} catch (InterruptedException e) {
				throw e;
			} catch (Exception e) {
				throw new ObjectEvaluationFailedException("Could not evaluate classifier!", e);
			}
		}
		Double score = stats.getMean();
		this.logger.info("Obtained score of {} for classifier {} in {}ms.", score, pl.getClass().getName(), (System.currentTimeMillis() - startTimestamp));
		return score;
	}

	public ISplitBasedClassifierEvaluator<Double, I, D> getBridge() {
		return this.splitBasedEvaluator;
	}

	@Override
	public String getLoggerName() {
		return this.logger.getName();
	}

	@Override
	public void setLoggerName(final String name) {
		this.logger.info("Switching logger of {} from {} to {}", this, this.logger.getName(), name);
		this.logger = LoggerFactory.getLogger(name);
		this.logger.info("Switched logger of {} to {}", this, name);
	}

	@Override
	public void registerListener(final Object listener) {
		this.hasListeners = true;
		this.eventBus.register(listener);
	}

	/* forward all events potentially coming in from the split evaluator */
	@Subscribe
	public void receiveEvent(final IEvent event) {
		this.eventBus.post(event);
	}
}
