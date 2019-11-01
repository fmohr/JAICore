package ai.libs.mlplan.examples.multiclass.simple;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.api4.java.ai.ml.classification.execution.ILearnerRunReport;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.ai.ml.core.learner.ISupervisedLearner;
import org.api4.java.algorithm.TimeOut;

import ai.libs.jaicore.ml.core.evaluation.ClassifierMetric;
import ai.libs.jaicore.ml.core.evaluation.evaluator.SupervisedLearnerExecutor;
import ai.libs.jaicore.ml.core.filter.FilterBasedDatasetSplitter;
import ai.libs.jaicore.ml.core.filter.sampling.inmemory.factories.LabelBasedStratifiedSamplingFactory;
import ai.libs.jaicore.ml.core.olddataset.ArffToNumericDatasetDeserializer;
import ai.libs.jaicore.ml.core.olddataset.Dataset;
import ai.libs.mlplan.core.MLPlan;
import ai.libs.mlplan.core.MLPlanSimpleBuilder;

/**
 * This is an example class that illustrates the usage of ML-Plan on the segment dataset of OpenML. It is configured to run for 30 seconds and to use 70% of the data for search and 30% for selection in its second phase.
 *
 * The API key used for OpenML is ML-Plan's key (read only).
 *
 * @author fmohr
 *
 */
public class MLPlanOpenMLExample {

	public static void main(final String[] args) throws Exception {

		ArffToNumericDatasetDeserializer deserializer = new ArffToNumericDatasetDeserializer();
		Dataset ds = deserializer.deserializeDataset(new File("testrsc/car.arff"));
		System.out.println(ds.getNumAttributes());
		List<ILabeledDataset> split = new FilterBasedDatasetSplitter(new LabelBasedStratifiedSamplingFactory<>(), .7d, new Random(0)).split(ds);

		/* initialize mlplan, and let it run for 30 seconds */
		MLPlanSimpleBuilder builder = new MLPlanSimpleBuilder();
		builder.withNodeEvaluationTimeOut(new TimeOut(10, TimeUnit.SECONDS));
		builder.withCandidateEvaluationTimeOut(new TimeOut(5, TimeUnit.SECONDS));
		builder.withTimeOut(new TimeOut(30, TimeUnit.SECONDS));
		builder.withNumCpus(1);

		MLPlan mlplan = new MLPlan(builder, split.get(0));
		mlplan.setRandomSeed(1);
		mlplan.setPortionOfDataForPhase2(0f);
		mlplan.setLoggerName("mlplan");

		try {
			long start = System.currentTimeMillis();
			ISupervisedLearner optimizedClassifier = mlplan.call();
			long trainTime = (int) (System.currentTimeMillis() - start) / 1000;
			System.out.println("Finished build of the classifier. Training time was " + trainTime + "s.");

			/* evaluate solution produced by mlplan */
			SupervisedLearnerExecutor executor = new SupervisedLearnerExecutor();
			ILearnerRunReport report = executor.execute(optimizedClassifier, split.get(1));
			System.out.println("Error Rate of the solution produced by ML-Plan: " + ClassifierMetric.MEAN_ERRORRATE.evaluateToDouble(Arrays.asList(report)));
		} catch (NoSuchElementException e) {
			System.out.println("Building the classifier failed: " + e.getMessage());
		}
	}
}
