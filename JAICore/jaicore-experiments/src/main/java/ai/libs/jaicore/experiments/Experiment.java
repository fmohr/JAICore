package ai.libs.jaicore.experiments;

import java.util.Map;

/**
 * Basic experiment class that describes an experiment conceptually in terms of hardware information and semantic keys.
 *
 * @author fmohr
 *
 */
public class Experiment {
	private final int memoryInMB;
	private final int numCPUs;
	private Map<String, String> valuesOfKeyFields;
	private Map<String, Object> valuesOfResultFields;

	public Experiment(final int memoryInMB, final int numCPUs, final Map<String, String> valuesOfKeyFields) {
		this(memoryInMB, numCPUs, valuesOfKeyFields, null);
	}

	public Experiment(final int memoryInMB, final int numCPUs, final Map<String, String> valuesOfKeyFields, final Map<String, Object> valuesOfResultFields) {
		super();
		this.memoryInMB = memoryInMB;
		this.numCPUs = numCPUs;
		this.valuesOfKeyFields = valuesOfKeyFields;
		this.valuesOfResultFields = valuesOfResultFields;
	}

	public Map<String, String> getValuesOfKeyFields() {
		return this.valuesOfKeyFields;
	}

	public Map<String, Object> getValuesOfResultFields() {
		return this.valuesOfResultFields;
	}

	public void setValuesOfResultFields(final Map<String, Object> valuesOfResultFields) {
		this.valuesOfResultFields = valuesOfResultFields;
	}

	public void setKeys(final Map<String, String> keys) {
		this.valuesOfKeyFields = keys;
	}

	public int getMemoryInMB() {
		return this.memoryInMB;
	}

	public int getNumCPUs() {
		return this.numCPUs;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.memoryInMB;
		result = prime * result + this.numCPUs;
		result = prime * result + ((this.valuesOfKeyFields == null) ? 0 : this.valuesOfKeyFields.hashCode());
		result = prime * result + ((this.valuesOfResultFields == null) ? 0 : this.valuesOfResultFields.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		Experiment other = (Experiment) obj;
		if (this.memoryInMB != other.memoryInMB) {
			return false;
		}
		if (this.numCPUs != other.numCPUs) {
			return false;
		}
		if (this.valuesOfKeyFields == null) {
			if (other.valuesOfKeyFields != null) {
				return false;
			}
		} else if (!this.valuesOfKeyFields.equals(other.valuesOfKeyFields)) {
			return false;
		}
		if (this.valuesOfResultFields == null) {
			if (other.valuesOfResultFields != null) {
				return false;
			}
		} else if (!this.valuesOfResultFields.equals(other.valuesOfResultFields)) {
			return false;
		}
		return true;
	}
}
