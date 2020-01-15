package ai.libs.jaicore.ml.classification.singlelabel.timeseries.filter;

import java.util.Arrays;

import ai.libs.jaicore.basic.transform.vector.PiecewiseAggregateApproximationTransform;
import ai.libs.jaicore.ml.classification.singlelabel.timeseries.dataset.TimeSeriesDataset2;
import ai.libs.jaicore.ml.classification.singlelabel.timeseries.exception.NoneFittedFilterExeception;

public class SAX implements IFilter {

	private double[] alphabet;
	private boolean fitted;
	private int wordLength;
	private double[][] lookuptable;
	private ZTransformer ztransform;
	private PiecewiseAggregateApproximationTransform paa;

	public SAX(final double[] alphabet, final int wordLength) {
		this.ztransform = new ZTransformer();
		this.alphabet = alphabet;
		this.wordLength = wordLength;
		this.paa = new PiecewiseAggregateApproximationTransform(wordLength);
	}

	@Override
	public TimeSeriesDataset2 transform(final TimeSeriesDataset2 input) {
		if (!(input instanceof TimeSeriesDataset2)) {
			throw new IllegalArgumentException("This method only supports TimeSeriesDatasets");
		}
		if (input.isEmpty()) {
			throw new IllegalArgumentException("This method can not work with an empty dataset.");
		}
		if (!this.fitted) {
			throw new NoneFittedFilterExeception("Fit() must be called before transform()");
		}

		TimeSeriesDataset2 sAXTransformedDataset = new TimeSeriesDataset2(null, null, null);

		for (int matrix = 0; matrix < input.getNumberOfVariables(); matrix++) {
			double[][] newMatrix = new double[input.getNumberOfInstances()][this.wordLength];

			for (int instance = 0; instance < input.getNumberOfInstances(); instance++) {
				double[] ppaOfInstance = this.paa.transform(input.getValues(matrix)[instance]);
				double[] tsasString = new double[this.wordLength];
				double[] localLookupTable = this.lookuptable[matrix];

				for (int i = 0; i < ppaOfInstance.length; i++) {
					double ppaValue = ppaOfInstance[i];
					boolean valuefound = false;
					for (int j = 0; j < localLookupTable.length; j++) {
						if (ppaValue < localLookupTable[j]) {
							tsasString[i] = this.alphabet[j];
							valuefound = true;
						}
					}
					if (!valuefound) {
						tsasString[i] = this.alphabet[this.alphabet.length - 1];
					}
				}
				newMatrix[instance] = tsasString;
			}
			sAXTransformedDataset.add(newMatrix, null);
		}
		return sAXTransformedDataset;
	}

	@Override
	public void fit(final TimeSeriesDataset2 input) {
		if (!(input instanceof TimeSeriesDataset2)) {
			throw new IllegalArgumentException("This method only supports Timeseriesdatasets");
		}
		if (input.isEmpty()) {
			throw new IllegalArgumentException("This method can not work with an empty dataset.");
		}
		double[][] maxAndMin = new double[2][input.getNumberOfVariables()];
		this.ztransform.fitTransform(input);
		for (int matrix = 0; matrix < input.getNumberOfVariables(); matrix++) {

			double[] max = new double[input.getNumberOfInstances()];
			double[] min = new double[input.getNumberOfInstances()];
			for (int instance = 0; instance < input.getNumberOfInstances(); instance++) {
				max[instance] = Arrays.stream(input.getValues(matrix)[instance]).max().getAsDouble();
				min[instance] = Arrays.stream(input.getValues(matrix)[instance]).min().getAsDouble();
			}
			maxAndMin[0][matrix] = Arrays.stream(max).max().getAsDouble();
			maxAndMin[1][matrix] = Arrays.stream(min).min().getAsDouble();
		}

		// filling the lookuptable
		this.lookuptable = new double[input.getNumberOfVariables()][this.alphabet.length];

		for (int matrix = 0; matrix < input.getNumberOfVariables(); matrix++) {
			double[] localMaxMin = new double[] { maxAndMin[0][matrix], maxAndMin[1][matrix] };
			double totalsize = localMaxMin[0] - localMaxMin[1];
			double stepsize = totalsize / this.alphabet.length;

			this.lookuptable[matrix][0] = localMaxMin[1] + stepsize;
			for (int i = 1; i < this.alphabet.length; i++) {
				this.lookuptable[matrix][i] = this.lookuptable[matrix][i - 1] + stepsize;
			}
		}
		this.fitted = true;
	}

	@Override
	public TimeSeriesDataset2 fitTransform(final TimeSeriesDataset2 input) {
		this.fit(input);
		return this.transform(input);
	}

	@Override
	public double[] transform(final double[] input) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void fit(final double[] input) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double[] fitTransform(final double[] input) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double[][] transform(final double[][] input) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void fit(final double[][] input) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double[][] fitTransform(final double[][] input) {
		throw new UnsupportedOperationException();
	}

}
