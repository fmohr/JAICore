package ai.libs.jaicore.ml.weka.dataset;

import static ai.libs.jaicore.ml.weka.dataset.WekaInstancesUtil.transformInstanceToWekaInstance;

import java.util.stream.IntStream;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.api4.java.ai.ml.core.dataset.schema.ILabeledInstanceSchema;
import org.api4.java.ai.ml.core.dataset.serialization.UnsupportedAttributeTypeException;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledInstance;

import ai.libs.jaicore.basic.sets.ElementDecorator;
import ai.libs.jaicore.ml.weka.WekaUtil;
import weka.core.Attribute;
import weka.core.Instance;

public class WekaInstance extends ElementDecorator<Instance> implements IWekaInstance {

	public WekaInstance(final Instance instance) {
		super(instance);
	}

	public WekaInstance(final ILabeledInstanceSchema schema, final ILabeledInstance instance) throws UnsupportedAttributeTypeException {
		super(transformInstanceToWekaInstance(schema, instance));
		if (schema.getNumAttributes() != instance.getNumAttributes()) {
			throw new IllegalStateException("Number of attributes in the instance deviate from those in the scheme.");
		}
	}

	@Override
	public Number getLabel() {
		boolean isInteger = this.getElement().classAttribute().isNominal();
		double classValue = this.getElement().classValue();
		if (isInteger) {
			int intClassValue = (int) classValue; // do NOT return this value directly. The explicit cast into a new variable is required, because the var remains a double elsewise. Maybe this is a Java bug
			return intClassValue;
		} else {
			return classValue;
		}
	}

	@Override
	public Double getAttributeValue(final int pos) {
		return this.getElement().value(pos);
	}

	@Override
	public Object[] getAttributes() {
		return IntStream.range(0, this.getElement().numAttributes()).filter(x -> x != this.getElement().classIndex()).mapToObj(x -> this.getElement().attribute(x)).map(this::transformAttributeValueToData).toArray();
	}

	private Object transformAttributeValueToData(final Attribute att) {
		if (att.isNominal() || att.isString() || att.isRelationValued() || att.isDate() || att.isRegular()) {
			return att.value((int) this.getElement().value(att));
		} else {
			return this.getElement().value(att);
		}
	}

	@Override
	public double[] getPoint() {
		return this.getElement().toDoubleArray();
	}

	@Override
	public double getPointValue(final int pos) {
		return this.getElement().value(pos);
	}

	@Override
	public void removeColumn(final int columnPos) {
		throw new UnsupportedOperationException("Not yet implemented!");
	}

	@Override
	public void setLabel(final Object obj) {
		if (obj instanceof String) {
			this.getElement().setClassValue((String) obj);
		} else if (obj instanceof Double) {
			this.getElement().setClassValue((Double) obj);
		} else {
			throw new IllegalArgumentException("The value for the label must not be of type " + obj.getClass().getName() + ". The only valid types are Double and String.");
		}
	}

	@Override
	public void setAttributeValue(final int pos, final Object value) {
		if (value instanceof String) {
			this.getElement().setValue(pos, (String) value);
		} else if (value instanceof Double) {
			this.getElement().setValue(pos, (Double) value);
		} else {
			throw new IllegalArgumentException("The value for the label must not be of type " + value.getClass().getName() + ". The only valid types are Double and String.");
		}
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.getElement().toDoubleArray()).toHashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof WekaInstance)) {
			return false;
		}
		return WekaUtil.areInstancesEqual(this.getElement(), ((WekaInstance) obj).getElement());
	}

	@Override
	public String toString() {
		return this.getElement().toString();
	}

}
