package ai.libs.jaicore.ml.tsc.classifier;

import java.util.List;

import ai.libs.jaicore.ml.core.exception.PredictionException;
import ai.libs.jaicore.ml.core.exception.TrainingException;
import ai.libs.jaicore.ml.tsc.dataset.TimeSeriesDataset;
import ai.libs.jaicore.ml.tsc.util.ClassMapper;

/**
 * Simplified batch-learning time series classifier which can be trained and
 * used as a predictor. Uses <code>algorithm</code> to train the model
 * parameters (if necessary).
 *
 * @author Julian Lienen
 *
 */
public abstract class ASimplifiedTSClassifier<T> {

	/**
	 * Class mapper object used to encode and decode predicted values if String
	 * values are used as classes. Can be null if the predicted values are not
	 * mapped to String values.
	 */
	protected ClassMapper classMapper;

	/**
	 * Variable indicating whether the classifier has been trained.
	 */
	protected boolean trained;

	public ASimplifiedTSClassifier() {
	}

	/**
	 * Performs a prediction based on the given univariate double[] instance
	 * representation and returns the result.
	 *
	 * @param univInstance Univariate instance given by a double vector of time
	 *            series values used for the prediction
	 * @return Returns the result of the prediction
	 * @throws PredictionException If something fails during the prediction process.
	 */
	public abstract T predict(final double[] univInstance) throws PredictionException;

	/**
	 * Performs a prediction based on the given multivariate list of double[]
	 * instance representation and returns the result.
	 *
	 * @param multivInstance Multivariate instance given by a list of multiple
	 *            double[] time series used for the prediction
	 * @return Returns the result of the prediction
	 * @throws PredictionException If something fails during the prediction process.
	 */
	public abstract T predict(final List<double[]> multivInstance) throws PredictionException;

	/**
	 * Performs predictions based on the given instances in the given dataset.
	 *
	 * @param dataset The {@link TimeSeriesDataset} for which predictions should be
	 *            made.
	 * @return Returns the result of the predictions
	 * @throws PredictionException If something fails during the prediction process
	 */
	public abstract List<T> predict(final TimeSeriesDataset dataset) throws PredictionException;

	public abstract <U extends ASimplifiedTSClassifier<T>> ASimplifiedTSCLearningAlgorithm<T, U> getLearningAlgorithm(final TimeSeriesDataset dataset);

	/**
	 * Trains the simplified time series classifier model using the given
	 * {@link TimeSeriesDataset}.
	 *
	 * @param dataset The {@link TimeSeriesDataset} which should be used for the
	 *            training.
	 * @throws TrainingException If something fails during the training process.
	 */
	public final void train(final TimeSeriesDataset dataset) throws TrainingException {

		// Set model which is trained
		ASimplifiedTSCLearningAlgorithm<T, ? extends ASimplifiedTSClassifier<T>> algorithm = this.getLearningAlgorithm(dataset);

		// Set input data from which the model should learn
		try {
			// Train
			algorithm.call();
			this.trained = true;
		} catch (Exception e) {
			throw new TrainingException("Could not train model " + this.getClass().getSimpleName(), e);
		}
	}

	/**
	 * Getter for the property <code>classMapper</code>.
	 *
	 * @return Returns the actual class mapper or null if no mapper is stored
	 */
	public ClassMapper getClassMapper() {
		return this.classMapper;
	}

	/**
	 * Setter for the property <code>classMapper</code>.
	 *
	 * @param classMapper The class mapper to be set
	 */
	public void setClassMapper(final ClassMapper classMapper) {
		this.classMapper = classMapper;
	}

	/**
	 * @return the trained
	 */
	public boolean isTrained() {
		return this.trained;
	}
}
