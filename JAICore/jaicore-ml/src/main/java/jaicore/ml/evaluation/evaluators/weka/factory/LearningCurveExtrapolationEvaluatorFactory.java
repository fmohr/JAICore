package jaicore.ml.evaluation.evaluators.weka.factory;

import jaicore.ml.core.dataset.sampling.inmemory.ASamplingAlgorithm;
import jaicore.ml.core.dataset.sampling.inmemory.factories.interfaces.ISamplingAlgorithmFactory;
import jaicore.ml.core.dataset.weka.WekaInstances;
import jaicore.ml.evaluation.evaluators.weka.IClassifierEvaluator;
import jaicore.ml.evaluation.evaluators.weka.LearningCurveExtrapolationEvaluator;
import jaicore.ml.learningcurve.extrapolation.LearningCurveExtrapolationMethod;
import weka.core.Instances;

public class LearningCurveExtrapolationEvaluatorFactory implements IClassifierEvaluatorFactory {

	private int[] anchorpoints;
	private ISamplingAlgorithmFactory<WekaInstances, ? extends ASamplingAlgorithm<WekaInstances>> subsamplingAlgorithmFactory;
	private double trainSplitForAnchorpointsMeasurement;
	private LearningCurveExtrapolationMethod extrapolationMethod;

	public LearningCurveExtrapolationEvaluatorFactory(final int[] anchorpoints, final ISamplingAlgorithmFactory<WekaInstances, ? extends ASamplingAlgorithm<WekaInstances>> subsamplingAlgorithmFactory, final double trainSplitForAnchorpointsMeasurement,
			final LearningCurveExtrapolationMethod extrapolationMethod) {
		super();
		this.anchorpoints = anchorpoints;
		this.subsamplingAlgorithmFactory = subsamplingAlgorithmFactory;
		this.trainSplitForAnchorpointsMeasurement = trainSplitForAnchorpointsMeasurement;
		this.extrapolationMethod = extrapolationMethod;
	}

	@Override
	public IClassifierEvaluator getIClassifierEvaluator(final Instances dataset, final long seed) {
		return new LearningCurveExtrapolationEvaluator<>(this.anchorpoints, this.subsamplingAlgorithmFactory, new WekaInstances(dataset), this.trainSplitForAnchorpointsMeasurement, this.extrapolationMethod, seed);
	}
}
