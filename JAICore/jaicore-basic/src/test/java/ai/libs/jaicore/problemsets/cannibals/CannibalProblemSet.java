package ai.libs.jaicore.problemsets.cannibals;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.libs.jaicore.basic.algorithm.AAlgorithmTestProblemSet;
import ai.libs.jaicore.basic.algorithm.IAlgorithmTestProblemSetForSolutionIterators;
import ai.libs.jaicore.problems.cannibals.CannibalProblem;

public class CannibalProblemSet extends AAlgorithmTestProblemSet<CannibalProblem> implements IAlgorithmTestProblemSetForSolutionIterators<CannibalProblem, List<String>>  {

	public CannibalProblemSet() {
		super("Cannibal-Missionaries");
	}

	@Override
	public CannibalProblem getSimpleProblemInputForGeneralTestPurposes() {
		return new CannibalProblem(true, 3, 3, 0, 0);
	}

	@Override
	public CannibalProblem getDifficultProblemInputForGeneralTestPurposes() {
		return new CannibalProblem(true, 1000, 1000, 0, 0);
	}

	@Override
	public Map<CannibalProblem, Collection<List<String>>> getProblemsWithSolutions() throws InterruptedException {
		Map<CannibalProblem, Collection<List<String>>> problemsWithSolutions = new HashMap<>();
		return problemsWithSolutions;
	}
}
