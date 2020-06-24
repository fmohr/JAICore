package ai.libs.jaicore.search.algorithms.mdp.mcts.ensemble;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.api4.java.datastructure.graph.ILabeledPath;

import ai.libs.jaicore.graph.LabeledGraph;
import ai.libs.jaicore.search.algorithms.mdp.mcts.ActionPredictionFailedException;
import ai.libs.jaicore.search.algorithms.mdp.mcts.IGraphDependentPolicy;
import ai.libs.jaicore.search.algorithms.mdp.mcts.IPathUpdatablePolicy;
import ai.libs.jaicore.search.algorithms.mdp.mcts.IPolicy;

public class EnsembleTreePolicy<N, A> implements IPathUpdatablePolicy<N, A, Double>, IGraphDependentPolicy<N, A> {
	private final List<IPathUpdatablePolicy<N, A, Double>> treePolicies;
	private final Random rand = new Random(0);
	private IPolicy<N, A> lastPolicy;
	private Map<IPolicy<N, A>, Double> meansOfObservations = new HashMap<>();
	private Map<IPolicy<N, A>, Integer> numberOfTimesChosen = new HashMap<>();
	private int calls;


	public EnsembleTreePolicy(final Collection<? extends IPathUpdatablePolicy<N, A, Double>> treePolicies) {
		super();
		this.treePolicies = new ArrayList<>(treePolicies);
	}

	@Override
	public A getAction(final N node, final Collection<A> actions) throws ActionPredictionFailedException, InterruptedException {
		this.calls ++;
		if (this.rand.nextDouble() < 1.1) {
			this.lastPolicy = this.treePolicies.get(this.rand.nextInt(this.treePolicies.size()));
			return this.lastPolicy.getAction(node, actions);
		}
		else {
			double bestScore = Double.MAX_VALUE;
			IPolicy<N, A> bestPolicy = null;
			for (IPolicy<N, A> policy : this.treePolicies) {
				double score;
				if (!this.numberOfTimesChosen.containsKey(policy)) {
					score = 0;
				}
				else {
					double explorationTerm = -1 * Math.sqrt(2) * Math.sqrt(Math.log(this.calls) / this.numberOfTimesChosen.get(policy));
					score = this.meansOfObservations.get(policy) + explorationTerm;
				}
				if (score < bestScore) {
					bestScore = score;
					bestPolicy = policy;
				}
			}
			Objects.requireNonNull(bestPolicy);
			this.lastPolicy = bestPolicy;
			return bestPolicy.getAction(node, actions);
		}
	}

	@Override
	public void updatePath(final ILabeledPath<N, A> path, final List<Double> scores) {
		for (IPathUpdatablePolicy<N, A, Double> policy : this.treePolicies) {
			policy.updatePath(path, scores);
		}
		int visits = this.numberOfTimesChosen.computeIfAbsent(this.lastPolicy, p -> 0);
		this.numberOfTimesChosen.put(this.lastPolicy, visits + 1);
		double playoutScore = scores.stream().reduce((a,b) -> a + b).get(); // we neither discount nor care for the segmentation of the scores
		this.meansOfObservations.put(this.lastPolicy, (this.meansOfObservations.computeIfAbsent(this.lastPolicy, p -> 0.0) * visits + playoutScore ) / (visits + 1));
	}

	@Override
	public void setGraph(final LabeledGraph<N, A> graph) {
		for (IPathUpdatablePolicy<N, A, Double> policy : this.treePolicies) {
			if (policy instanceof IGraphDependentPolicy) {
				((IGraphDependentPolicy<N, A>)policy).setGraph(graph);
			}
		}
	}



}
