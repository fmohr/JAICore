package jaicore.search.algorithms.standard.astar;

import jaicore.search.algorithms.standard.bestfirst.nodeevaluation.INodeEvaluator;
import jaicore.search.core.interfaces.IOptimalPathInORGraphSearch;
import jaicore.search.core.interfaces.StandardORGraphSearchFactory;
import jaicore.search.model.other.EvaluatedSearchGraphPath;
import jaicore.search.probleminputs.GraphSearchWithNumberBasedAdditivePathEvaluation;

public class AStarFactory<T, A> extends StandardORGraphSearchFactory<GraphSearchWithNumberBasedAdditivePathEvaluation<T,A>, EvaluatedSearchGraphPath<T, A, Double>, T, A, Double> {

	private int timeoutForFInMS;
	private INodeEvaluator<T, Double> timeoutEvaluator;
	private String loggerName;

	public AStarFactory() {
		super();
	}

	public AStarFactory(final int timeoutForFInMS) {
		this();
		if (timeoutForFInMS > 0) {
			this.timeoutForFInMS = timeoutForFInMS;
		}
	}

	@Override
	public IOptimalPathInORGraphSearch<GraphSearchWithNumberBasedAdditivePathEvaluation<T,A>, T, A, Double> getAlgorithm() {
		AStar<T, A> search = new AStar<T,A>(getInput());
		search.setTimeoutForComputationOfF(this.timeoutForFInMS, this.timeoutEvaluator);
		if (loggerName != null && loggerName.length() > 0)
			search.setLoggerName(loggerName);
		return search;
	}

	public void setTimeoutForFComputation(final int timeoutInMS, final INodeEvaluator<T, Double> timeoutEvaluator) {
		this.timeoutForFInMS = timeoutInMS;
		this.timeoutEvaluator = timeoutEvaluator;
	}

	public int getTimeoutForFInMS() {
		return this.timeoutForFInMS;
	}

	public INodeEvaluator<T, Double> getTimeoutEvaluator() {
		return this.timeoutEvaluator;
	}

	public String getLoggerName() {
		return loggerName;
	}

	public void setLoggerName(String loggerName) {
		this.loggerName = loggerName;
	}
}
