package ai.libs.jaicore.ml.core.evaluation.evaluator;

import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledInstance;
import org.api4.java.ai.ml.core.evaluation.ISupervisedLearnerEvaluator;
import org.api4.java.ai.ml.core.evaluation.learningcurve.ILearningCurve;
import org.api4.java.ai.ml.core.learner.ISupervisedLearner;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.ml.core.filter.sampling.inmemory.ASamplingAlgorithm;
import ai.libs.jaicore.ml.core.filter.sampling.inmemory.factories.interfaces.ISamplingAlgorithmFactory;
import ai.libs.jaicore.ml.functionprediction.learner.learningcurveextrapolation.ConfigurationLearningCurveExtrapolator;

/**
 * Predicts the accuracy of a classifier with certain configurations on a point
 * of its learning curve, given some anchorpoint and its configurations using
 * the LCNet of pybnn
 *
 * Note: This code was copied from LearningCurveExtrapolationEvaluator and
 * slightly reworked
 *
 * @author noni4
 */

public class ConfigurationLearningCurveExtrapolationEvaluator implements ISupervisedLearnerEvaluator<ILabeledInstance, ILabeledDataset<? extends ILabeledInstance>> {

	private Logger logger = LoggerFactory.getLogger(ConfigurationLearningCurveExtrapolationEvaluator.class);

	// Configuration for the learning curve extrapolator.
	private int[] anchorpoints;
	private ISamplingAlgorithmFactory<ILabeledDataset<?>, ASamplingAlgorithm<ILabeledDataset<?>>> samplingAlgorithmFactory;
	private ILabeledDataset<?> dataset;
	private double trainSplitForAnchorpointsMeasurement;
	private long seed;
	private String identifier;
	private double[] configurations;
	private int fullDatasetSize = -1;

	public ConfigurationLearningCurveExtrapolationEvaluator(final int[] anchorpoints, final ISamplingAlgorithmFactory<ILabeledDataset<?>, ASamplingAlgorithm<ILabeledDataset<?>>> samplingAlgorithmFactory, final ILabeledDataset<?> dataset,
			final double trainSplitForAnchorpointsMeasurement, final long seed, final String identifier, final double[] configurations) {
		super();
		this.anchorpoints = anchorpoints;
		this.samplingAlgorithmFactory = samplingAlgorithmFactory;
		this.dataset = dataset;
		this.trainSplitForAnchorpointsMeasurement = trainSplitForAnchorpointsMeasurement;
		this.seed = seed;
		this.identifier = identifier;
		this.configurations = configurations;
	}

	public void setFullDatasetSize(final int fullDatasetSize) {
		this.fullDatasetSize = fullDatasetSize;
	}

	@Override
	public Double evaluate(final ISupervisedLearner<ILabeledInstance, ILabeledDataset<? extends ILabeledInstance>> classifier) throws InterruptedException, ObjectEvaluationFailedException {
		// Create the learning curve extrapolator with the given configuration.
		try {
			ConfigurationLearningCurveExtrapolator extrapolator = new ConfigurationLearningCurveExtrapolator(classifier, this.dataset, this.trainSplitForAnchorpointsMeasurement, this.anchorpoints, this.samplingAlgorithmFactory, this.seed,
					this.identifier, this.configurations);

			// Create the extrapolator and calculate the accuracy the classifier would have
			// if it was trained on the complete dataset.
			ILearningCurve learningCurve = extrapolator.extrapolateLearningCurve();

			int evaluationPoint = this.dataset.size();
			// Overwrite evaluation point if a value was provided, otherwise evaluate on the
			// size of the given dataset
			if (this.fullDatasetSize != -1) {
				evaluationPoint = this.fullDatasetSize;
			}

			return learningCurve.getCurveValue(evaluationPoint) * 100.0d;
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception e) {
			this.logger.warn("Evaluation of classifier failed due Exception {} with message {}. Returning null.", e.getClass().getName(), e.getMessage());
			return null;
		}
	}

}
