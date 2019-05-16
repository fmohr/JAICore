package jaicore.ml.tsc.classifier.trees;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jaicore.ml.tsc.classifier.SimplifiedTSClassifierTest;
import jaicore.ml.tsc.exceptions.TimeSeriesLoadingException;
import timeseriesweka.classifiers.LPS;

/**
 * Reference tests for the {@link LearnPatternSimilarityClassifier}.
 * 
 * Info: This was only used for experimental purposes. The tests performed in
 * the package <code>jaicore.ml.tsc.classifier</code> in the test environment
 * were used for empirical performance study.
 * 
 * @author Julian Lienen
 *
 */
public class LearnPatternSimilarityRefTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(TimeSeriesBagOfFeaturesRefTest.class);

	private static final String UNIVARIATE_PREFIX = "data/univariate/";

	@Test
	public void compareClassifierPredictions() throws TimeSeriesLoadingException, Exception {

		String dataset = "ItalyPowerDemand";
		final String trainPath = UNIVARIATE_PREFIX + dataset + File.separator + dataset + "_TRAIN.arff";
		final String testPath = UNIVARIATE_PREFIX + dataset + File.separator + dataset + "_TEST.arff";

		int seed = 10;

		LearnPatternSimilarityClassifier ownClf = new LearnPatternSimilarityClassifier(seed, 200, 6, 20);

		LPS refClf = new LPS();
		refClf.setParamSearch(false);

		Map<String, Object> result = SimplifiedTSClassifierTest.compareClassifiers(refClf, ownClf, seed, null, null,
				new File(trainPath), new File(testPath));

		System.out.println("Ref clf parameters: " + refClf.getParameters());
		System.out.println(result.toString());
	}
}
