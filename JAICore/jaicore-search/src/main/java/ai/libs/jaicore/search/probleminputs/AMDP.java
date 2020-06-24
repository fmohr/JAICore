package ai.libs.jaicore.search.probleminputs;

import java.util.Map;
import java.util.stream.Collectors;

public abstract class AMDP<N, A, V extends Comparable<V>> implements IMDP<N, A, V> {

	private final N initState;

	public AMDP(final N initState) {
		super();
		this.initState = initState;
	}

	@Override
	public N getInitState() {
		return this.initState;
	}

	@Override
	public double getProb(final N state, final A action, final N successor) throws InterruptedException {
		Map<N, Double> dist = this.getProb(state, action);
		if (!dist.containsKey(successor)) {
			throw new IllegalArgumentException("No probability defined for the following triplet:\n\tFrom state: " + state + "\n\tUsed action: " + action  + "\n\tTo state: " + successor + ".\nDistribution is: " + dist.entrySet().stream().map(e -> "\n\t" + e.toString()).collect(Collectors.joining()));
		}
		return dist.get(successor);
	}

	@Override
	public boolean isTerminalState(final N state) throws InterruptedException {
		return this.getApplicableActions(state).isEmpty();
	}
}
