package jaicore.experiments;

public class ExperimentDBEntry {
	private final int id;
	private final Experiment experiment;

	public ExperimentDBEntry(int id, Experiment experiment) {
		super();
		if (experiment == null)
			throw new IllegalArgumentException("Experiment must not be null");
		this.id = id;
		this.experiment = experiment;
	}

	public int getId() {
		return id;
	}

	public Experiment getExperiment() {
		return experiment;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((experiment == null) ? 0 : experiment.hashCode());
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExperimentDBEntry other = (ExperimentDBEntry) obj;
		if (experiment == null) {
			if (other.experiment != null)
				return false;
		} else if (!experiment.equals(other.experiment))
			return false;
		if (id != other.id)
			return false;
		return true;
	}

}
