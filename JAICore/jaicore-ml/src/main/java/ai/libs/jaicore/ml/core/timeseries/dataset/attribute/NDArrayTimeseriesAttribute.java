package ai.libs.jaicore.ml.core.timeseries.dataset.attribute;

import org.api4.java.ai.ml.core.dataset.schema.attribute.ITimeseriesAttributeValue;
import org.api4.java.ai.ml.core.dataset.schema.attribute.NoValidAttributeValueException;
import org.api4.java.common.timeseries.ITimeseries;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * Describes a time series type as an 1-NDArray with a fixed length.
 */
public class NDArrayTimeseriesAttribute extends ATimeseriesAttribute<INDArray> {

	/**
	 *
	 */
	private static final long serialVersionUID = -9188360800052241944L;

	protected NDArrayTimeseriesAttribute(final String name, final int length) {
		super(name, length);
	}

	/**
	 * Validates whether a INDArray conforms to this time series. An INDArray
	 * confirms to this value, if its rank is 1 and its length equals the length of
	 * this time series.
	 *
	 * @param value The value to validated.
	 * @return Returns true if the given value conforms
	 */
	@Override
	public boolean isValidValue(final Object value) {
		if (value instanceof ITimeseries<?> && ((ITimeseries<?>) value).getValue() instanceof INDArray) {
			INDArray castedValue = (INDArray) ((ITimeseries<?>) value).getValue();
			return castedValue.rank() == 1 && castedValue.length() == this.getLength();
		}
		return value instanceof NDArrayTimeseriesAttributeValue;
	}

	@Override
	public String getStringDescriptionOfDomain() {
		return "[NDATS] " + this.getName();
	}

	@Override
	public ITimeseriesAttributeValue<INDArray> getAsAttributeValue(final Object object) {
		return new NDArrayTimeseriesAttributeValue(this.getValueAsTypeInstance(object));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected ITimeseries<INDArray> getValueAsTypeInstance(final Object object) {
		if (this.isValidValue(object)) {
			if (object instanceof NDArrayTimeseriesAttributeValue) {
				return ((NDArrayTimeseriesAttributeValue) object).getValue();
			} else {
				return (ITimeseries<INDArray>) object;
			}
		} else {
			throw new NoValidAttributeValueException();
		}
	}

}
