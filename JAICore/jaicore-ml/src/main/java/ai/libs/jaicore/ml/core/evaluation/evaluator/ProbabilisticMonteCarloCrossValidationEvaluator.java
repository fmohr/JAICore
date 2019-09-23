package ai.libs.jaicore.ml.core.evaluation.evaluator;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TTest;
import org.api4.java.ai.ml.classification.IClassifier;
import org.api4.java.ai.ml.core.dataset.splitter.IDatasetSplitter;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledInstance;
import org.api4.java.common.attributedobjects.IInformedObjectEvaluatorExtension;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;
import org.api4.java.common.control.ILoggingCustomizable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.ml.core.evaluation.evaluator.splitevaluation.AbstractSplitBasedClassifierEvaluator;
import ai.libs.jaicore.ml.core.evaluation.evaluator.splitevaluation.ISplitBasedClassifierEvaluator;

/**
 * A classifier evaluator that can perform a (monte-carlo)cross-validation on
 * the given dataset. Thereby, it uses the
 * {@link AbstractSplitBasedClassifierEvaluator} to evaluate the classifier on a random
 * split of the dataset.
 * This probabilistic version can be used to speed up the process by early termination based on
 * a threshold value that has to be beaten by the evaluation. If it is unlikely after the first
 * repeats that this is possible the unfinished, not as precise result will be returned.
 *
 * @author fmohr, jnowack
 *
 */
public class ProbabilisticMonteCarloCrossValidationEvaluator<I extends ILabeledInstance, D extends ILabeledDataset<I>> implements IClassifierEvaluator, IInformedObjectEvaluatorExtension<Double>, ILoggingCustomizable {

	private Logger logger = LoggerFactory.getLogger(ProbabilisticMonteCarloCrossValidationEvaluator.class);
	private boolean canceled = false;
	private final int repeats;
	private final D data;
	private final double trainingPortion;
	private final long seed;

	private double bestScore = 1.0;

	/* Can either compute the loss or cache it */
	private final ISplitBasedClassifierEvaluator<Double, I, D> bridge;
	private final IDatasetSplitter<I, D> datasetSplitter;

	@Override
	public void updateBestScore(final Double bestScore) {
		this.bestScore = bestScore;
	}

	public ProbabilisticMonteCarloCrossValidationEvaluator(final ISplitBasedClassifierEvaluator<Double, I, D> bridge, final IDatasetSplitter<I, D> datasetSplitter, final int repeats, final double bestscore, final D data,
			final double trainingPortion, final long seed) {
		super();
		this.repeats = repeats;
		this.bridge = bridge;
		this.data = data;
		this.trainingPortion = trainingPortion;
		this.seed = seed;
		this.bestScore = bestscore;
		this.datasetSplitter = datasetSplitter;
	}

	public void cancel() {
		this.logger.info("Received cancel");
		this.canceled = true;
	}

	@Override
	public Double f(final IClassifier pl) throws ObjectEvaluationFailedException, InterruptedException {
		return this.f(pl, new DescriptiveStatistics());
	}

	public Double f(final IClassifier pl, final DescriptiveStatistics stats) throws ObjectEvaluationFailedException, InterruptedException {
		if (pl == null) {
			throw new IllegalArgumentException("Cannot compute score for null pipeline!");
		}

		/* perform random stratified split */
		this.logger.info("Starting evaluation of {}", pl);
		for (int i = 0; i < this.repeats && !this.canceled && !Thread.currentThread().isInterrupted(); i++) {
			this.logger.debug("Obtaining predictions of {} for split #{}/{}", pl, i + 1, this.repeats);
			try {
				List<D> split = this.datasetSplitter.split(this.data, this.seed + i);
				double score = this.bridge.evaluateSplit(pl, split.get(0), split.get(1));
				this.logger.info("Score for evaluation of {} with split #{}/{}: {}", pl, i + 1, this.repeats, score);
				stats.addValue(score);

				/* t-test */
				if (stats.getMean() > this.bestScore && stats.getN() >= 2) {
					TTest test = new TTest();
					if (test.tTest(this.bestScore, stats.getValues(), 0.02)) {
						Double result = stats.getMean();
						this.logger.info("Obtained score of {} for classifier {}. {}-MCCV was not completed because it would have been to unliky to beat best score.", result, pl, this.repeats);
						return result;
					}
				}
			}

			catch (InterruptedException e) {
				throw e;
			} catch (Exception e) {
				throw new ObjectEvaluationFailedException("Could not evaluate classifier!", e);

			}
		}
		if (Thread.interrupted()) {
			throw new InterruptedException("MCCV has been interrupted");
		}
		Double score = stats.getMean();
		this.logger.info("Obtained score of {} for classifier {}.", score, pl);
		return score;
	}

	public ISplitBasedClassifierEvaluator<Double, I, D> getBridge() {
		return this.bridge;
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
}
