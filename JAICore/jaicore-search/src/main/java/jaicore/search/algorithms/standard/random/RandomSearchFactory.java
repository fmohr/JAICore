package jaicore.search.algorithms.standard.random;

import jaicore.search.core.interfaces.StandardORGraphSearchFactory;
import jaicore.search.model.other.SearchGraphPath;
import jaicore.search.probleminputs.GraphSearchInput;

public class RandomSearchFactory<N, A> extends StandardORGraphSearchFactory<GraphSearchInput<N, A>, SearchGraphPath<N, A>,N, A, Double> {

	private String loggerName;
	private int seed;

	public RandomSearchFactory() {
		super();
	}

	@Override
	public RandomSearch<N, A> getAlgorithm() {
		if (getInput().getGraphGenerator() == null)
			throw new IllegalStateException("Cannot produce RandomSearch searches before the graph generator is set in the problem.");
		RandomSearch<N, A> search = new RandomSearch<>(getInput(), seed);
		return search;
	}

	public int getSeed() {
		return seed;
	}

	public void setSeed(int seed) {
		this.seed = seed;
	}

	public String getLoggerName() {
		return loggerName;
	}

	public void setLoggerName(String loggerName) {
		this.loggerName = loggerName;
	}
}
