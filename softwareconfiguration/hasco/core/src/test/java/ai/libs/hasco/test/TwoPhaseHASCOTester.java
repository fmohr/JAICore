package ai.libs.hasco.test;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.aeonbits.owner.ConfigCache;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hasco.core.HASCO;
import ai.libs.hasco.core.HASCOSolutionCandidate;
import ai.libs.hasco.core.RefinementConfiguredSoftwareConfigurationProblem;
import ai.libs.hasco.variants.forwarddecomposition.HASCOViaFDAndBestFirstWithRandomCompletionsFactory;
import ai.libs.hasco.variants.forwarddecomposition.twophase.TwoPhaseHASCO;
import ai.libs.hasco.variants.forwarddecomposition.twophase.TwoPhaseHASCOConfig;
import ai.libs.hasco.variants.forwarddecomposition.twophase.TwoPhaseSoftwareConfigurationProblem;
import ai.libs.jaicore.basic.algorithm.AlgorithmTestProblemSetCreationException;

public class TwoPhaseHASCOTester extends SoftwareConfigurationAlgorithmTester {

	private final Logger logger = LoggerFactory.getLogger(TwoPhaseHASCOTester.class);

	@Override
	public TwoPhaseHASCO getAlgorithmForSoftwareConfigurationProblem(final RefinementConfiguredSoftwareConfigurationProblem<Double> problem) {

		/* produce an HASCO instance */
		HASCOViaFDAndBestFirstWithRandomCompletionsFactory factory = new HASCOViaFDAndBestFirstWithRandomCompletionsFactory(0, 3);
		factory.withDefaultAlgorithmConfig();
		HASCO hasco = factory.getAlgorithm(problem);

		/* produce two-phase HASCO */
		TwoPhaseSoftwareConfigurationProblem prob = new TwoPhaseSoftwareConfigurationProblem(problem, problem.getParamRefinementConfig(), problem.getCompositionEvaluator());
		TwoPhaseHASCOConfig config = ConfigCache.getOrCreate(TwoPhaseHASCOConfig.class);
		TwoPhaseHASCO twoPhaseHASCO = new TwoPhaseHASCO<>(prob, config);
		twoPhaseHASCO.setHasco(hasco);
		return twoPhaseHASCO;
	}

	//	@Test
	public void testThatSolutionQueueOfPhase1isOrderedByScores() throws AlgorithmTestProblemSetCreationException, AlgorithmTimeoutedException, InterruptedException, AlgorithmExecutionCanceledException, AlgorithmException {
		TwoPhaseHASCO twoPhaseHASCO = this.getAlgorithmForSoftwareConfigurationProblem(((SoftwareConfigurationProblemSet) this.getProblemSet()).getSimpleProblemInputForGeneralTestPurposes());
		twoPhaseHASCO.setLoggerName(TESTEDALGORITHM_LOGGERNAME);
		twoPhaseHASCO.call();
		List<HASCOSolutionCandidate<Double>> results = new ArrayList<>(twoPhaseHASCO.getPhase1ResultQueue());
		int n = results.size();
		HASCOSolutionCandidate<Double> last = null;
		for (int i = 0; i < n; i++) {
			HASCOSolutionCandidate<Double> current = results.get(i);
			if (last != null) {
				assertTrue("Ill-ordered solution queue for phase 1! Solution at position " + (i - 1) + " has worse score (" + last.getScore() + ") than solution at position " + i + " (" + current.getScore() + ").",
						last.getScore() <= current.getScore());
			}
			last = current;
		}
	}

	@Test
	public void testThatBestSolutionFoundByHASCOIsSuccessfullyEvaluatedInPhase2() throws AlgorithmTestProblemSetCreationException, AlgorithmTimeoutedException, InterruptedException, AlgorithmExecutionCanceledException, AlgorithmException {
		TwoPhaseHASCO twoPhaseHASCO = this.getAlgorithmForSoftwareConfigurationProblem(((SoftwareConfigurationProblemSet) this.getProblemSet()).getSimpleProblemInputForGeneralTestPurposes());
		twoPhaseHASCO.setLoggerName(TESTEDALGORITHM_LOGGERNAME);
		twoPhaseHASCO.call();
		HASCOSolutionCandidate<Double> bestCandidateOfPhase1 = (HASCOSolutionCandidate<Double>) twoPhaseHASCO.getBestSeenSolution();
		assertTrue("Best solution of phase 1 has not been successfully evaluated in phase 2!",
				twoPhaseHASCO.getSelectionScoresOfCandidates().containsKey(bestCandidateOfPhase1) && twoPhaseHASCO.getSelectionScoresOfCandidates().get(bestCandidateOfPhase1) != null);
	}

	@Test
	public void testThatHalfOfSelectionPoolIsSuccessfullyEvaluated() throws AlgorithmTestProblemSetCreationException, AlgorithmTimeoutedException, InterruptedException, AlgorithmExecutionCanceledException, AlgorithmException {
		TwoPhaseHASCO twoPhaseHASCO = this.getAlgorithmForSoftwareConfigurationProblem(((SoftwareConfigurationProblemSet)this.getProblemSet()).getSimpleProblemInputForGeneralTestPurposes());
		twoPhaseHASCO.setLoggerName(TESTEDALGORITHM_LOGGERNAME);
		twoPhaseHASCO.call();
		List<HASCOSolutionCandidate<Double>> selectionPoolForPhase2 = twoPhaseHASCO.getSelectionForPhase2();
		int n =  selectionPoolForPhase2.size();
		for (int i = 0; i < (int)Math.ceil(n * 0.5); i++) {
			assertTrue("Solution at position " + i + "/" + n + " in selection pool list has not been evaluated!", twoPhaseHASCO.getSelectionScoresOfCandidates().containsKey(selectionPoolForPhase2.get(i)) && twoPhaseHASCO.getSelectionScoresOfCandidates().get(selectionPoolForPhase2.get(i)) != null);
		}
		this.logger.info("The first {}/{} solutions have been evaluated successfully.", n, selectionPoolForPhase2.size());
	}
}
