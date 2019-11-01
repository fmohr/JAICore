package ai.libs.mlplan.examples.dyadranking;

import java.io.File;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.api4.java.algorithm.TimeOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.ml.weka.WekaUtil;
import ai.libs.jaicore.ml.weka.dataset.ReproducibleInstances;
import ai.libs.jaicore.search.algorithms.standard.bestfirst.DyadRankedBestFirstFactory;
import ai.libs.jaicore.search.problemtransformers.GraphSearchProblemInputToGraphSearchWithSubpathEvaluationInputTransformer;
import ai.libs.mlplan.core.AbstractMLPlanBuilder;
import ai.libs.mlplan.metamining.dyadranking.WEKADyadRankedNodeQueueConfig;
import ai.libs.mlplan.multiclass.MLPlanWekaBuilder;
import ai.libs.mlplan.multiclass.wekamlplan.MLPlanWekaClassifier;
import ai.libs.mlplan.multiclass.wekamlplan.weka.WekaMLPlanWekaClassifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

/**
 * Demonstrated the usage of ML-Plan with a dyad ranked OPEN list
 *
 * @author Helena Graf
 *
 */
public class WekaDyadRankingExample {

	private static final Logger logger = LoggerFactory.getLogger(WekaDyadRankingExample.class);

	public static void main(final String[] args) throws Exception {
		long starttime = System.currentTimeMillis();
		ReproducibleInstances data = ReproducibleInstances.fromOpenML(40983, "4350e421cdc16404033ef1812ea38c01");
		data.setClassIndex(data.numAttributes() - 1);
		List<Instances> split = WekaUtil.getStratifiedSplit((Instances) data, (new Random(0)).nextLong(), 0.7d);

		WEKADyadRankedNodeQueueConfig openConfig = new WEKADyadRankedNodeQueueConfig();
		MLPlanWekaBuilder builder = AbstractMLPlanBuilder.forWeka();
		builder.withSearchSpaceConfigFile(new File("resources/automl/searchmodels/weka/weka-approach-5-autoweka.json"))
		.withAlgorithmConfigFile(new File("conf/mlplan.properties"))
		.withSearchFactory(new DyadRankedBestFirstFactory<>(openConfig), new GraphSearchProblemInputToGraphSearchWithSubpathEvaluationInputTransformer<>(n -> 1.0)).withPreferredNodeEvaluator(n -> 1.0);

		MLPlanWekaClassifier mlplan = new WekaMLPlanWekaClassifier(builder);
		openConfig.setComponents(mlplan.getComponents());
		openConfig.setData(split.get(0));

		try {
			long start = System.currentTimeMillis();
			mlplan.setTimeout(new TimeOut(60 - (start - starttime) / 1000, TimeUnit.SECONDS));
			mlplan.buildClassifier(split.get(0));
			long trainTime = (int) (System.currentTimeMillis() - start) / 1000;
			logger.info("Finished build of the classifier. Training time was {}s", trainTime);

			/* evaluate solution produced by mlplan */
			Evaluation eval = new Evaluation(split.get(0));
			eval.evaluateModel(mlplan, split.get(1));
			logger.info("Error Rate of the solution produced by ML-Plan: {}", (100 - eval.pctCorrect()) / 100f);
		} catch (NoSuchElementException e) {
			logger.error("Building the classifier failed: {}", e.getMessage());
		}
		logger.info("Total experiment time: {}", (System.currentTimeMillis() - starttime) / 1000);
	}
}
