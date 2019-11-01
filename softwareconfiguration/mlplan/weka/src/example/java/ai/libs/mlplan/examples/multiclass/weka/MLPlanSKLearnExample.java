package ai.libs.mlplan.examples.multiclass.weka;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.api4.java.algorithm.TimeOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.ml.classification.singlelabel.loss.ZeroOneLoss;
import ai.libs.jaicore.ml.weka.WekaUtil;
import ai.libs.mlplan.core.AbstractMLPlanBuilder;
import ai.libs.mlplan.multiclass.sklearn.SKLearnMLPlanWekaClassifier;
import weka.core.Instances;

public class MLPlanSKLearnExample {

	private static final Logger L = LoggerFactory.getLogger(MLPlanSKLearnExample.class);

	private static final File DATASET = new File("testrsc/car.arff");
	private static final ZeroOneLoss LOSS_MEASURE = new ZeroOneLoss();

	private static final TimeOut TIMEOUT = new TimeOut(300, TimeUnit.SECONDS);

	private static final boolean ACTIVATE_VISUALIZATION = false;

	public static void main(final String[] args) throws Exception {
		Instances data = new Instances(new FileReader(DATASET));
		data.setClassIndex(data.numAttributes() - 1);
		List<Instances> testSplit = WekaUtil.getStratifiedSplit(data, 0, .7);

		AbstractMLPlanBuilder builder = AbstractMLPlanBuilder.forSKLearn();
		builder.withTimeOut(TIMEOUT);
		builder.withNodeEvaluationTimeOut(new TimeOut(90, TimeUnit.SECONDS));
		builder.withCandidateEvaluationTimeOut(new TimeOut(30, TimeUnit.SECONDS));

		SKLearnMLPlanWekaClassifier mlplan = new SKLearnMLPlanWekaClassifier(builder);
		mlplan.setLoggerName("sklmlplanc");
		mlplan.setVisualizationEnabled(ACTIVATE_VISUALIZATION);
		mlplan.buildClassifier(testSplit.get(0));

		List<Double> actual = Arrays.stream(mlplan.classifyInstances(testSplit.get(1))).mapToObj(x -> x).collect(Collectors.toList());
		List<Double> expected = testSplit.get(1).stream().map(x -> x.classValue()).collect(Collectors.toList());
		double loss = LOSS_MEASURE.calculateAvgMeasure(actual, expected);
		L.info("ML-Plan classifier has been chosen for dataset {} and framework SK-Learn. The measured test loss of the selected classifier is {}", DATASET.getAbsolutePath(), loss);
	}

}
