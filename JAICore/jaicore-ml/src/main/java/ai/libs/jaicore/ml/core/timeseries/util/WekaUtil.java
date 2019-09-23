package ai.libs.jaicore.ml.core.timeseries.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;
import org.api4.java.ai.ml.core.exception.TrainingException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import ai.libs.jaicore.ml.core.timeseries.dataset.TimeSeriesDataset;
import ai.libs.jaicore.ml.core.timeseries.dataset.TimeSeriesInstance;
import ai.libs.jaicore.ml.core.timeseries.model.INDArrayTimeseries;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * WekaUtil
 */
public class WekaUtil {

	private static final String I_NAME = "Instances";

	private WekaUtil() {
		/* no instantiation desired */
	}

	/**
	 * Stacks the given matrices horizontally.
	 *
	 * @param matrices
	 *            List of INDArray matrices to be stacked
	 * @return Returns one INDArray containing all <code>matrices</code>. New
	 *         dimensionality is (originalShape[0] x sum of originalShape[1]s)
	 */
	private static INDArray hstackINDArrays(final List<INDArray> matrices) {
		// Check first shape dimension
		if (!matrices.isEmpty()) {
			long[] shape = matrices.get(0).shape();
			for (int i = 1; i < matrices.size(); i++) {
				if (matrices.get(i).shape()[0] != shape[0]) {
					throw new IllegalArgumentException("First dimensionality of the given matrices must be equal!");
				}
			}
		}

		INDArray combinedMatrix;
		if (!matrices.isEmpty()) {
			combinedMatrix = matrices.get(0).dup();
			for (int i = 1; i < matrices.size(); i++) {
				combinedMatrix = Nd4j.hstack(combinedMatrix, matrices.get(i));
			}
		} else {
			// If an empty list was given, return an empty matrix
			combinedMatrix = Nd4j.create(0, 0);
		}
		return combinedMatrix;

	}

	/**
	 * Maps a time series instance to a Weka instance.
	 *
	 * @param instance
	 *            The time series instance storing the time series data and the
	 *            target value
	 * @return Returns the Weka instance containing the time series data and the
	 *         class information.
	 */
	public static Instance tsInstanceToWekaInstance(final TimeSeriesInstance<?> instance) {
		List<INDArray> indArrays = new ArrayList<>();

		for (INDArrayTimeseries timeseries : instance) {
			indArrays.add(timeseries.getValue());
		}

		INDArray combinedMatrix = hstackINDArrays(indArrays);

		final Instance finalInstance = new DenseInstance(1, Nd4j.toFlattened(combinedMatrix).toDoubleVector());
		finalInstance.setClassValue(ai.libs.jaicore.ml.weka.WekaUtil.getIntValOfClassName(finalInstance, (String) instance.getLabel()));
		return finalInstance;
	}

	/**
	 * Maps an univariate simplified time series instance to a Weka instance.
	 *
	 * @param instance
	 *            The time series instance storing the time series data
	 * @return Returns the Weka instance containing the time series
	 */
	public static Instance simplifiedTSInstanceToWekaInstance(final double[] instance) {
		return new DenseInstance(1, instance);
	}

	/**
	 * Trains a given Weka <code>classifier</code> using the time series data set
	 * <code>timeSeriesDataset</code>.
	 *
	 * @param classifier
	 *            The Weka {@link weka.Classifier} object
	 * @param timeSeriesDataset
	 *            The time series data set which is transformed to Weka instances
	 *            used for the training
	 * @throws TrainingException
	 *             Throws exception if the training could not be finished
	 *             successfully
	 */
	public static <L> void buildWekaClassifierFromTS(final Classifier classifier, final TimeSeriesDataset<L> timeSeriesDataset) throws TrainingException {

		final Instances trainingInstances = timeSeriesDatasetToWekaInstances(timeSeriesDataset);

		try {
			classifier.buildClassifier(trainingInstances);
		} catch (Exception e) {
			throw new TrainingException("Could not train classifier " + classifier.getClass().getName() + " due to a Weka exception.", e);
		}
	}

	/**
	 * Trains a given Weka <code>classifier</code> using the simplified time series
	 * data set <code>timeSeriesDataset</code>.
	 *
	 * @param classifier
	 *            The Weka {@link weka.Classifier} object
	 * @param timeSeriesDataset
	 *            The time series data set which is transformed to Weka instances
	 *            used for the training
	 * @throws TrainingException
	 *             Throws exception if the training could not be finished
	 *             successfully
	 */
	public static void buildWekaClassifierFromSimplifiedTS(final Classifier classifier, final ai.libs.jaicore.ml.core.timeseries.dataset.TimeSeriesDataset2 timeSeriesDataset) throws TrainingException {

		final Instances trainingInstances = simplifiedTimeSeriesDatasetToWekaInstances(timeSeriesDataset);

		try {
			classifier.buildClassifier(trainingInstances);
		} catch (Exception e) {
			throw new TrainingException(String.format("Could not train classifier %s due to a Weka exception.", classifier.getClass().getName()), e);
		}
	}

	/**
	 * Converts Weka instances to an INDArray matrix.
	 *
	 * @param instances
	 *            Weka instances to be converted.
	 * @param keepClass
	 *            Determines whether the class attribute should be stored in the
	 *            result matrix
	 * @return Returns an INDArray consisting of all instances with the shape
	 *         (number instances x number attributes)
	 */
	public static INDArray wekaInstancesToINDArray(final Instances instances, final boolean keepClass) {
		if (instances == null || instances.isEmpty()) {
			throw new IllegalArgumentException("Instances must not be null or empty!");
		}

		int classSub = (keepClass || instances.classIndex() < -1) ? 0 : 1;
		int numAttributes = instances.numAttributes() - classSub;
		int numInstances = instances.numInstances();

		INDArray result = Nd4j.create(numInstances, numAttributes);

		for (int i = 0; i < numInstances; i++) {
			double[] instValues = instances.get(i).toDoubleArray();
			for (int j = 0; j < numAttributes; j++) {
				result.putScalar(new int[] { i, j }, instValues[j]);
			}
		}

		return result;
	}

	/**
	 * Converts a given {@link TimeSeriesDataset} object to a Weka Instances object.
	 * Works with {@link CategoricalAttributeType} target values.
	 *
	 * @param dataSet
	 *            Data set which is transformed
	 * @return Transformed Weka Instances object
	 */
	public static <L> Instances timeSeriesDatasetToWekaInstances(final TimeSeriesDataset<L> dataSet) {
		List<INDArray> matrices = new ArrayList<>();
		for (int i = 0; i < dataSet.getNumberOfVariables(); i++) {
			matrices.add(dataSet.getValues(i));
		}

		// Create attributes
		final ArrayList<Attribute> attributes = new ArrayList<>();
		for (int m = 0; m < matrices.size(); m++) {
			INDArray matrix = matrices.get(m);
			for (int i = 0; i < matrix.shape()[1]; i++) {
				final Attribute newAtt = new Attribute(String.format("val_%d_%d", m, i));
				attributes.add(newAtt);
			}
		}

		// Add class attribute
		final INDArray targets = dataSet.getTargetsAsINDArray();
		attributes.add(new Attribute("class", IntStream.rangeClosed((int) targets.minNumber().longValue(), (int) targets.maxNumber().longValue()).boxed().map(String::valueOf).collect(Collectors.toList())));
		final Instances result = new Instances(I_NAME, attributes, (int) dataSet.getNumberOfInstances());
		result.setClassIndex(result.numAttributes() - 1);

		// Concatenate multiple matrices if series is multivariate
		INDArray combinedMatrix = hstackINDArrays(matrices);

		// Create instances
		for (int i = 0; i < dataSet.getNumberOfInstances(); i++) {

			// Initialize instance
			final Instance inst = new DenseInstance(1, Nd4j.hstack(Nd4j.toFlattened(combinedMatrix.getRow(i)), Nd4j.create(new double[] { targets.getDouble(i) })).toDoubleVector());

			inst.setDataset(result);
			result.add(inst);
		}

		return result;
	}

	/**
	 * Converts a given simplified {@link ai.libs.jaicore.ml.core.timeseries.dataset.TimeSeriesDataset2}
	 * object to a Weka Instances object.
	 *
	 * @param dataSet
	 *            Data set which is transformed
	 * @return Transformed Weka Instances object
	 */
	public static Instances simplifiedTimeSeriesDatasetToWekaInstances(final ai.libs.jaicore.ml.core.timeseries.dataset.TimeSeriesDataset2 dataSet) {

		final int[] targets = dataSet.getTargets();
		List<Integer> targetList = Arrays.asList(ArrayUtils.toObject(targets));

		int min = Collections.min(targetList);
		int max = Collections.max(targetList);
		List<String> classValues = IntStream.rangeClosed(min, max).boxed().map(String::valueOf).collect(Collectors.toList());

		return simplifiedTimeSeriesDatasetToWekaInstances(dataSet, classValues);
	}

	/**
	 * Converts a given simplified {@link ai.libs.jaicore.ml.core.timeseries.dataset.TimeSeriesDataset2}
	 * object to a Weka Instances object.
	 *
	 * @param dataSet
	 *            Data set which is transformed
	 * @return Transformed Weka Instances object
	 */
	public static Instances simplifiedTimeSeriesDatasetToWekaInstances(final ai.libs.jaicore.ml.core.timeseries.dataset.TimeSeriesDataset2 dataSet, final List<String> classValues) {

		List<double[][]> matrices = new ArrayList<>();
		for (int i = 0; i < dataSet.getNumberOfVariables(); i++) {
			matrices.add(dataSet.getValues(i));
		}

		// Create attributes
		final ArrayList<Attribute> attributes = new ArrayList<>();
		for (int m = 0; m < matrices.size(); m++) {
			double[][] matrix = matrices.get(m);
			if (matrix == null) {
				continue;
			}

			for (int i = 0; i < matrix[0].length; i++) {
				final Attribute newAtt = new Attribute(String.format("val_%d_%d", m, i));
				attributes.add(newAtt);
			}
		}

		// Add class attribute
		final int[] targets = dataSet.getTargets();
		attributes.add(new Attribute("class", classValues));
		final Instances result = new Instances(I_NAME, attributes, dataSet.getNumberOfInstances());
		result.setClassIndex(result.numAttributes() - 1);

		// Create instances
		for (int i = 0; i < dataSet.getNumberOfInstances(); i++) {

			double[] concatenatedRow = matrices.get(0)[i];
			for (int j = 1; j < matrices.size(); j++) {
				concatenatedRow = ArrayUtils.addAll(concatenatedRow, matrices.get(j)[i]);
			}

			concatenatedRow = ArrayUtils.addAll(concatenatedRow, targets[i]);

			// Initialize instance
			final Instance inst = new DenseInstance(1, concatenatedRow);
			inst.setDataset(result);
			result.add(inst);
		}

		return result;
	}

	/**
	 * Converts an INDArray matrix (number of instances x number of attributes) to
	 * Weka instances without any class attribute.
	 *
	 * @param matrix
	 *            INDArray matrix storing all the attribute values of the instances
	 * @return Returns the Weka Instances object consisting of all instances and the
	 *         attribute values
	 */
	public static Instances indArrayToWekaInstances(final INDArray matrix) {
		if (matrix == null || matrix.length() == 0) {
			throw new IllegalArgumentException("Matrix must not be null or empty!");
		}
		if (matrix.shape().length != 2) {
			throw new IllegalArgumentException(String.format("Parameter matrix must be a matrix with 2 axis (instances x attributes). Actual shape: (%s)", Arrays.toString(matrix.shape())));
		}

		final int numInstances = (int) matrix.shape()[0];
		final int numAttributes = (int) matrix.shape()[1];

		// Create attributes
		final ArrayList<Attribute> attributes = new ArrayList<>();
		for (int i = 0; i < numAttributes; i++) {
			final Attribute newAtt = new Attribute("val" + i);
			attributes.add(newAtt);
		}

		final Instances result = new Instances(I_NAME, attributes, numInstances);

		for (int i = 0; i < numInstances; i++) {

			// Initialize instance
			final Instance inst = new DenseInstance(1, Nd4j.toFlattened(matrix.getRow(i)).toDoubleVector());
			inst.setDataset(result);

			result.add(inst);
		}

		return result;
	}

	/**
	 * Converts a double[][] matrix (number of instances x number of attributes) to
	 * Weka instances without any class attribute.
	 *
	 * @param matrix
	 *            The double[][] matrix storing all the attribute values of the
	 *            instances
	 * @return Returns the Weka Instances object consisting of all instances and the
	 *         attribute values
	 */
	public static Instances matrixToWekaInstances(final double[][] matrix) {
		final ArrayList<Attribute> attributes = new ArrayList<>();
		for (int i = 0; i < matrix[0].length; i++) {
			final Attribute newAtt = new Attribute("val" + i);
			attributes.add(newAtt);
		}
		Instances wekaInstances = new Instances(I_NAME, attributes, matrix.length);
		for (int i = 0; i < matrix[0].length; i++) {
			final Instance inst = new DenseInstance(1, matrix[i]);
			inst.setDataset(wekaInstances);
			wekaInstances.add(inst);
		}

		return wekaInstances;
	}

}