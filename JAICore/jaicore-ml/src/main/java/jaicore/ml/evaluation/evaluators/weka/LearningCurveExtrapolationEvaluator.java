package jaicore.ml.evaluation.evaluators.weka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jaicore.basic.algorithm.exceptions.AlgorithmException;
import jaicore.basic.algorithm.exceptions.ObjectEvaluationFailedException;
import jaicore.ml.core.dataset.IDataset;
import jaicore.ml.core.dataset.IInstance;
import jaicore.ml.core.dataset.sampling.inmemory.ASamplingAlgorithm;
import jaicore.ml.core.dataset.sampling.inmemory.factories.interfaces.ISamplingAlgorithmFactory;
import jaicore.ml.interfaces.LearningCurve;
import jaicore.ml.learningcurve.extrapolation.InvalidAnchorPointsException;
import jaicore.ml.learningcurve.extrapolation.LearningCurveExtrapolationMethod;
import jaicore.ml.learningcurve.extrapolation.LearningCurveExtrapolator;
import weka.classifiers.Classifier;

/**
 * Evaluates a classifier by predicting its learning curve with a few
 * anchorpoints. The evaluation result is the accuracy or the error rate (configurable) for the
 * complete dataset. Depending on the chosen anchorpoints this evaluation method
 * will be really fast, but can be inaccurate depending on the learning curve
 * extrapolation method, since it will only give a prediction of the accuracy
 * and does not measure it.
 *
 * @author Lukas Brandt
 */
public class LearningCurveExtrapolationEvaluator implements IClassifierEvaluator {

	private static final Logger logger = LoggerFactory.getLogger(LearningCurveExtrapolationEvaluator.class);

	// Configuration for the learning curve extrapolator.
	private int[] anchorpoints;
	private ISamplingAlgorithmFactory<IInstance, ? extends ASamplingAlgorithm<IInstance>> samplingAlgorithmFactory;
	private IDataset<? extends IInstance> dataset;
	private double trainSplitForAnchorpointsMeasurement;
	private LearningCurveExtrapolationMethod extrapolationMethod;
	private long seed;
	private int fullDatasetSize = -1;
	private static final boolean EVALUATE_ACCURACY = false; // otherwise error rate

	/**
	 * Create a classifier evaluator with learning curve extrapolation.
	 *
	 * @param anchorpoints Anchorpoints for the learning
	 *            curve extrapolation.
	 * @param samplingAlgorithmFactory Subsampling factory to create a
	 *            subsampler for the samples at the given anchorpoints.
	 * @param dataset Dataset to evaluate the classifier with.
	 * @param trainSplitForAnchorpointsMeasurement Ratio to split the subsamples at
	 *            the anchorpoints into train and test.
	 * @param extrapolationMethod Method to extrapolate a learning
	 *            curve from the accuracy
	 *            measurements at the anchorpoints.
	 * @param seed Random seed.
	 */
	public LearningCurveExtrapolationEvaluator(final int[] anchorpoints, final ISamplingAlgorithmFactory<IInstance, ? extends ASamplingAlgorithm<IInstance>> samplingAlgorithmFactory, final IDataset<? extends IInstance> dataset,
			final double trainSplitForAnchorpointsMeasurement, final LearningCurveExtrapolationMethod extrapolationMethod, final long seed) {
		super();
		this.anchorpoints = anchorpoints;
		this.samplingAlgorithmFactory = samplingAlgorithmFactory;
		this.dataset = dataset;
		this.trainSplitForAnchorpointsMeasurement = trainSplitForAnchorpointsMeasurement;
		this.extrapolationMethod = extrapolationMethod;
		this.seed = seed;
	}

	public void setFullDatasetSize(final int fullDatasetSize) {
		this.fullDatasetSize = fullDatasetSize;
	}

	/**
	 * Computes the (estimated) measure of the classifier on the full dataset
	 */
	@Override
	public Double evaluate(final Classifier classifier) throws InterruptedException, ObjectEvaluationFailedException {

		// Create the learning curve extrapolator with the given configuration.
		LearningCurveExtrapolator extrapolator = new LearningCurveExtrapolator(this.extrapolationMethod, classifier, this.dataset, this.trainSplitForAnchorpointsMeasurement, this.samplingAlgorithmFactory, this.seed);

		try {
			/* Create the extrapolator and calculate the accuracy the classifier would have if it was trained on the complete dataset. */
			LearningCurve learningCurve = extrapolator.extrapolateLearningCurve(this.anchorpoints);

			int evaluationPoint = this.dataset.size();
			/* Overwrite evaluation point if a value was provided, otherwise evaluate on the size of the given dataset */
			if (this.fullDatasetSize != -1) {
				evaluationPoint = this.fullDatasetSize;
			}

			return this.EVALUATE_ACCURACY ? learningCurve.getCurveValue(evaluationPoint) : 1 - learningCurve.getCurveValue(evaluationPoint);
		} catch (AlgorithmException | InvalidAnchorPointsException e) {
			logger.warn("Evaluation of classifier failed due Exception {} with message {}. Returning null.", e.getClass().getName(), e.getMessage());
			return null;
		}
	}

}
