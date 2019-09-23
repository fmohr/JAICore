package ai.libs.jaicore.ml.core.evaluation.evaluator;

import org.api4.java.common.attributedobjects.IObjectEvaluator;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;

import ai.libs.jaicore.timing.TimedObjectEvaluator;
import weka.classifiers.Classifier;

public class TimeoutableEvaluator extends TimedObjectEvaluator<Classifier, Double> implements IClassifierEvaluator {

	/** The object evaluator that shall actually be called. */
	private IObjectEvaluator<Classifier, Double> ce;

	/** The timeout in milliseconds, i.e. after what time the evaluator shall be interrupted. */
	private int timeoutInMS;

	/**
	 * C'tor create a timeoutable evaluator out of any other IObjectEvaluator. Note that these evaluators need to check whether their thread has been interrupted to actually react on the timeout.
	 *
	 * @param iSolutionEvaluator
	 *            The evaluator which shall actually be called and maybe needs to be interrupted after the given timeout.
	 * @param timeoutInMS
	 *            The timeout in milliseconds to interrupt an evaluation with the {iSolutionEvaluator}
	 */
	public TimeoutableEvaluator(final IObjectEvaluator<Classifier, Double> iSolutionEvaluator, final int timeoutInMS) {
		this.ce = iSolutionEvaluator;
		this.timeoutInMS = timeoutInMS;
	}

	@Override
	public Double evaluateSupervised(final Classifier object) throws ObjectEvaluationFailedException, InterruptedException {
		return this.ce.evaluate(object);
	}

	@Override
	public long getTimeout(final Classifier item) {
		return this.timeoutInMS;
	}

	@Override
	public String getMessage(final Classifier item) {
		return "Evaluation of classifier " + item + " has timeouted (" + TimeoutableEvaluator.class.getName() + ")";
	}

}
