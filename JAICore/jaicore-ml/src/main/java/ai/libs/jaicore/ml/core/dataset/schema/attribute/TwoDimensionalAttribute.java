package ai.libs.jaicore.ml.core.dataset.schema.attribute;

import org.api4.java.ai.ml.core.dataset.schema.attribute.IAttributeValue;

/**
 * A {@link MultidimensionalAttribute} that holds two dimensional double arrays.
 *
 * @author Lukas Fehring
 *
 */
public class TwoDimensionalAttribute extends MultidimensionalAttribute<double[][]> {

	private static final long serialVersionUID = -3300254871190010390L;
	private int xsize;
	private int ysize;

	public int getXsize() {
		return this.xsize;
	}

	public int getYsize() {
		return this.ysize;
	}

	public TwoDimensionalAttribute(final String name, final int xsize, final int ysize) {
		super(name);
		this.xsize = xsize;
		this.ysize = ysize;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + this.xsize;
		result = prime * result + this.ysize;
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		TwoDimensionalAttribute other = (TwoDimensionalAttribute) obj;
		return this.xsize == other.xsize && this.ysize == other.ysize;
	}

	/**
	 * {@inheritDoc} This method takes a parameter of type double[][] or {@link MultidimensionalAttributeValue} and returns a {@link MultidimensionalAttributeValue} that holds the same values
	 */
	@SuppressWarnings("unchecked")
	@Override
	public IAttributeValue getAsAttributeValue(final Object object) {
		if (object instanceof double[][]) {
			return new MultidimensionalAttributeValue<double[][]>(this, (double[][]) object);
		} else if (object instanceof MultidimensionalAttributeValue) {
			return new MultidimensionalAttributeValue<double[][]>(this, ((MultidimensionalAttributeValue<double[][]>) object).getValue());
		}

		throw new IllegalArgumentException("No valid value for this attribute");

	}

	@Override
	public boolean isValidValue(final Object value) {
		return (value instanceof MultidimensionalAttributeValue || value instanceof double[][]);
	}

	@Override
	public String getStringDescriptionOfDomain() {
		return "[2D] " + this.getName();
	}

	/**
	 * {@inheritDoc} parses String string to MutidimensionalAttributeValue
	 */
	@Override
	public double[][] formGenericMultidimensionalArray(final String[] stringvalues) {
		double[][] doubleValues = new double[this.xsize][this.ysize];

		int position = 0;
		for (int x = 0; x < this.xsize; x++) {
			double[] innerArray = new double[this.ysize];
			for (int y = 0; y < this.ysize; y++) {
				try {
					innerArray[y] = Double.parseDouble(stringvalues[position]);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("No valid value of this attribute");
				}
				position++;
			}
			doubleValues[x] = innerArray;
		}

		return doubleValues;

	}

}
