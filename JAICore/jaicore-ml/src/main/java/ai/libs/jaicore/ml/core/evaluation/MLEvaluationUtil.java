package ai.libs.jaicore.ml.core.evaluation;

import java.util.Random;

import org.api4.java.ai.ml.classification.singlelabel.evaluation.ISingleLabelClassification;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledInstance;
import org.api4.java.ai.ml.core.evaluation.ISupervisedLearnerEvaluator;
import org.api4.java.ai.ml.core.evaluation.execution.ILearnerRunReport;
import org.api4.java.ai.ml.core.evaluation.execution.LearnerExecutionFailedException;
import org.api4.java.ai.ml.core.evaluation.supervised.loss.IDeterministicPredictionPerformanceMeasure;
import org.api4.java.ai.ml.core.learner.ISupervisedLearner;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;

import ai.libs.jaicore.ml.classification.loss.dataset.EAggregatedClassifierMetric;
import ai.libs.jaicore.ml.core.evaluation.evaluator.MonteCarloCrossValidationEvaluator;
import ai.libs.jaicore.ml.core.evaluation.evaluator.SupervisedLearnerExecutor;

public class MLEvaluationUtil {

	private MLEvaluationUtil() {
		/* avoids instantiation */
	}

	public static double evaluate(final ISupervisedLearner<ILabeledInstance, ILabeledDataset<? extends ILabeledInstance>> learner, final ISupervisedLearnerEvaluator<ILabeledInstance, ILabeledDataset<? extends ILabeledInstance>> evaluator)
			throws ObjectEvaluationFailedException, InterruptedException {
		return evaluator.evaluate(learner);
	}

	public static double getLossForTrainedClassifier(final ISupervisedLearner<ILabeledInstance, ILabeledDataset<? extends ILabeledInstance>> learner, final ILabeledDataset<? extends ILabeledInstance> testData,
			final IDeterministicPredictionPerformanceMeasure<Integer, ISingleLabelClassification> measure) throws LearnerExecutionFailedException {
		SupervisedLearnerExecutor executor = new SupervisedLearnerExecutor();
		ILearnerRunReport report = executor.execute(learner, testData);
		return measure.loss(report.getPredictionDiffList().getCastedView(Integer.class, ISingleLabelClassification.class));
	}

	public static double mccv(final ISupervisedLearner<ILabeledInstance, ILabeledDataset<? extends ILabeledInstance>> learner, final ILabeledDataset<? extends ILabeledInstance> data, final int repeats, final double trainFoldSize,
			final long seed, final EAggregatedClassifierMetric metric) throws ObjectEvaluationFailedException, InterruptedException {
		return evaluate(learner, new MonteCarloCrossValidationEvaluator(false, data, repeats, trainFoldSize, new Random(seed), metric));
	}

	public static double mccv(final ISupervisedLearner<ILabeledInstance, ILabeledDataset<? extends ILabeledInstance>> learner, final ILabeledDataset<? extends ILabeledInstance> data, final int repeats, final double trainFoldSize,
			final long seed) throws ObjectEvaluationFailedException, InterruptedException {
		return mccv(learner, data, repeats, trainFoldSize, seed, EAggregatedClassifierMetric.MEAN_ERRORRATE);
	}
}
