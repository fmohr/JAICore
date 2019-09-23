package ai.libs.jaicore.ml.core.evaluation.evaluator.factory;

import org.api4.java.ai.ml.core.dataset.splitter.IDatasetSplitter;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledInstance;

import ai.libs.jaicore.ml.core.evaluation.evaluator.splitevaluation.ISplitBasedClassifierEvaluator;
import weka.core.Instances;

/**
 * An abstract factory for configuring Monte Carlo cross-validation based evaluators.
 *
 * @author mwever
 *
 */
public abstract class AMonteCarloCrossValidationBasedEvaluatorFactory<I extends ILabeledInstance, D extends ILabeledDataset<I>> implements IClassifierEvaluatorFactory<D> {

	private IDatasetSplitter<I, D> datasetSplitter;
	private ISplitBasedClassifierEvaluator<Double, I, D> splitBasedEvaluator;
	private int seed;
	private int numMCIterations;
	private Instances data;
	private double trainFoldSize;
	private int timeoutForSolutionEvaluation;

	/**
	 * Standard c'tor.
	 */
	protected AMonteCarloCrossValidationBasedEvaluatorFactory() {
		super();
	}

	/**
	 * Getter for the dataset splitter.
	 * @return Returns the dataset spliiter.
	 */
	public IDatasetSplitter<I, D> getDatasetSplitter() {
		return this.datasetSplitter;
	}

	/**
	 * Getter for the evaluator that is used for evaluating each split.
	 * @return The split evaluator.
	 */
	public ISplitBasedClassifierEvaluator<Double, I, D> getSplitBasedEvaluator() {
		return this.splitBasedEvaluator;
	}

	/**
	 * Getter for the random seed.
	 * @return Seed used for generating randomized dataset splits.
	 */
	public int getSeed() {
		return this.seed;
	}

	/**
	 * Getter for the number of iterations, i.e. the number of splits considered.
	 * @return The number of iterations.
	 */
	public int getNumMCIterations() {
		return this.numMCIterations;
	}

	/**
	 * Getter for the dataset which is used for splitting.
	 * @return The original dataset that is being split.
	 */
	public Instances getData() {
		return this.data;
	}

	/**
	 * Getter for the size of the train fold.
	 * @return The portion of the training data.
	 */
	public double getTrainFoldSize() {
		return this.trainFoldSize;
	}

	/**
	 * Getter for the timeout for evaluating a solution.
	 * @return The timeout for evaluating a solution.
	 */
	public int getTimeoutForSolutionEvaluation() {
		return this.timeoutForSolutionEvaluation;
	}

	/**
	 * Configures the evaluator to use the given dataset splitter.
	 * @param datasetSplitter The dataset splitter to be used.
	 * @return The factory object.
	 */
	public AMonteCarloCrossValidationBasedEvaluatorFactory<I, D> withDatasetSplitter(final IDatasetSplitter<I, D> datasetSplitter) {
		this.datasetSplitter = datasetSplitter;
		return this;
	}

	/**
	 * Configures the evaluator to use the given classifier evaluator.
	 * @param splitBasedClassifierEvaluator The classifier evaluator to be used.
	 * @return The factory object.
	 */
	public AMonteCarloCrossValidationBasedEvaluatorFactory<I, D> withSplitBasedEvaluator(final ISplitBasedClassifierEvaluator<Double, I, D> splitBasedClassifierEvaluator) {
		this.splitBasedEvaluator = splitBasedClassifierEvaluator;
		return this;
	}

	/**
	 * Configures the evaluator to use the given random seed.
	 * @param seed The seed to be used for pseudo-randomization.
	 * @return The factory object.
	 */
	public AMonteCarloCrossValidationBasedEvaluatorFactory<I, D> withSeed(final int seed) {
		this.seed = seed;
		return this;
	}

	/**
	 * Configures the number of monte carlo cross-validation iterations.
	 * @param numMCIterations The number of iterations to run.
	 * @return The factory object.
	 */
	public AMonteCarloCrossValidationBasedEvaluatorFactory<I, D> withNumMCIterations(final int numMCIterations) {
		this.numMCIterations = numMCIterations;
		return this;
	}

	/**
	 * Configures the dataset which is split into train and test data.
	 * @param data The dataset to be split.
	 * @return The factory object.
	 */
	public AMonteCarloCrossValidationBasedEvaluatorFactory<I, D> withData(final Instances data) {
		this.data = data;
		return this;
	}

	/**
	 * Configures the portion of the training data relative to the entire dataset size.
	 * @param trainFoldSize The size of the training fold (0,1).
	 * @return The factory object.
	 */
	public AMonteCarloCrossValidationBasedEvaluatorFactory<I, D> withTrainFoldSize(final double trainFoldSize) {
		this.trainFoldSize = trainFoldSize;
		return this;
	}

	/**
	 * Configures a timeout for evaluating a solution.
	 * @param timeoutForSolutionEvaluation The timeout for evaluating a solution.
	 * @return The factory object.
	 */
	public AMonteCarloCrossValidationBasedEvaluatorFactory<I, D> withTimeoutForSolutionEvaluation(final int timeoutForSolutionEvaluation) {
		this.timeoutForSolutionEvaluation = timeoutForSolutionEvaluation;
		return this;
	}
}
