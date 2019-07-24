package ai.libs.jaicore.search.testproblems.knapsack;

import java.util.Set;

import org.api4.java.ai.graphsearch.problem.implicit.graphgenerator.NodeGoalTester;
import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.IPathEvaluator;
import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.PathEvaluationException;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;
import org.api4.java.datastructure.graph.IPath;

import ai.libs.jaicore.basic.algorithm.reduction.AlgorithmicProblemReduction;
import ai.libs.jaicore.search.model.other.SearchGraphPath;
import ai.libs.jaicore.search.probleminputs.GraphSearchWithSubpathEvaluationsInput;
import ai.libs.jaicore.testproblems.knapsack.KnapsackConfiguration;
import ai.libs.jaicore.testproblems.knapsack.KnapsackProblem;

public class KnapsackToGraphSearchReducer implements AlgorithmicProblemReduction<KnapsackProblem, Set<String>, GraphSearchWithSubpathEvaluationsInput<KnapsackConfiguration, String, Double>, SearchGraphPath<KnapsackConfiguration, String>> {

	@Override
	public GraphSearchWithSubpathEvaluationsInput<KnapsackConfiguration, String, Double> encodeProblem(final KnapsackProblem problem) {
		return new GraphSearchWithSubpathEvaluationsInput<>(new KnapsackProblemGraphGenerator(problem), new NodeGoalTester<KnapsackConfiguration, String>() {

			@Override
			public boolean isGoal(final KnapsackConfiguration n) {
				for (String object : n.getRemainingObjects()) {
					if (n.getUsedCapacity() + problem.getWeights().get(object) <= problem.getKnapsackCapacity()) {
						return false;
					}
				}
				return true;
			}
		}, new IPathEvaluator<KnapsackConfiguration, String, Double>() {

			@Override
			public Double evaluate(final IPath<KnapsackConfiguration, String> path) throws PathEvaluationException, InterruptedException {
				try {
					return problem.getSolutionEvaluator().evaluate(path.getHead());
				}
				catch (ObjectEvaluationFailedException e) {
					throw new PathEvaluationException("Could not evaluate node due to an algorithm exception: " + e.getMessage(), e);
				}
			}
		});
	}

	@Override
	public Set<String> decodeSolution(final SearchGraphPath<KnapsackConfiguration, String> solution) {
		return solution.getNodes().get(solution.getNodes().size() - 1).getPackedObjects();
	}

}
