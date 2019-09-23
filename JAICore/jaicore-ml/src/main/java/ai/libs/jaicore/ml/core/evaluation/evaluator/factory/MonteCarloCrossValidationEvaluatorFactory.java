package ai.libs.jaicore.ml.core.evaluation.evaluator.factory;

import org.api4.java.ai.ml.core.dataset.splitter.IDatasetSplitter;

import ai.libs.jaicore.ml.core.evaluation.evaluator.MonteCarloCrossValidationEvaluator;
import ai.libs.jaicore.ml.core.evaluation.evaluator.splitevaluation.ISplitBasedClassifierEvaluator;
import weka.core.Instances;

/**
 * Factory for configuring standard Monte Carlo cross-validation evaluators.
 * @author mwever
 *
 */
public class MonteCarloCrossValidationEvaluatorFactory extends AMonteCarloCrossValidationBasedEvaluatorFactory {

	/**
	 * Standard C'tor.
	 */
	public MonteCarloCrossValidationEvaluatorFactory() {
		super();
	}

	@Override
	public MonteCarloCrossValidationEvaluatorFactory withDatasetSplitter(final IDatasetSplitter datasetSplitter) {
		return (MonteCarloCrossValidationEvaluatorFactory) super.withDatasetSplitter(datasetSplitter);
	}

	@Override
	public MonteCarloCrossValidationEvaluatorFactory withSplitBasedEvaluator(final ISplitBasedClassifierEvaluator<Double> splitBasedClassifierEvaluator) {
		return (MonteCarloCrossValidationEvaluatorFactory) super.withSplitBasedEvaluator(splitBasedClassifierEvaluator);
	}

	@Override
	public MonteCarloCrossValidationEvaluatorFactory withSeed(final int seed) {
		return (MonteCarloCrossValidationEvaluatorFactory) super.withSeed(seed);
	}

	@Override
	public MonteCarloCrossValidationEvaluatorFactory withNumMCIterations(final int numMCIterations) {
		return (MonteCarloCrossValidationEvaluatorFactory) super.withNumMCIterations(numMCIterations);
	}

	@Override
	public MonteCarloCrossValidationEvaluatorFactory withData(final Instances data) {
		return (MonteCarloCrossValidationEvaluatorFactory) super.withData(data);
	}

	@Override
	public MonteCarloCrossValidationEvaluatorFactory withTrainFoldSize(final double trainFoldSize) {
		return (MonteCarloCrossValidationEvaluatorFactory) super.withTrainFoldSize(trainFoldSize);
	}

	@Override
	public MonteCarloCrossValidationEvaluatorFactory withTimeoutForSolutionEvaluation(final int timeoutForSolutionEvaluation) {
		return (MonteCarloCrossValidationEvaluatorFactory) super.withTimeoutForSolutionEvaluation(timeoutForSolutionEvaluation);
	}

	@Override
	public MonteCarloCrossValidationEvaluator getIClassifierEvaluator(final Instances dataset, final long seed) {
		if (this.getSplitBasedEvaluator() == null) {
			throw new IllegalStateException("Cannot create MCCV, because no splitBasedEvaluator has been set!");
		}
		return new MonteCarloCrossValidationEvaluator(this.getSplitBasedEvaluator(), this.getDatasetSplitter(), this.getNumMCIterations(), dataset, this.getTrainFoldSize(), seed);
	}
}
