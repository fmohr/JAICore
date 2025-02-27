package ai.libs.reduction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.ml.weka.WekaUtil;
import ai.libs.jaicore.ml.weka.classification.learner.reduction.MCTreeNodeReD;
import ai.libs.jaicore.ml.weka.classification.learner.reduction.splitter.RPNDSplitter;
import ai.libs.reduction.ensemble.simple.EnsembleOfSimpleOneStepReductionsExperiment;
import ai.libs.reduction.single.ReductionExperiment;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.Vote;
import weka.core.Instances;

public class Util {

	private static final Logger logger = LoggerFactory.getLogger(Util.class);

	private static final String LABEL_TRAIN_TIME = "trainTime";

	private Util() {
		/* Left blank to prevent instantiation of this class. */
	}

	public static List<Map<String, Object>> conductSingleOneStepReductionExperiment(final ReductionExperiment experiment) throws Exception {
		/* load data */
		Instances data = new Instances(new BufferedReader(new FileReader(experiment.getDataset())));
		data.setClassIndex(data.numAttributes() - 1);

		/* prepare basis for experiments */
		int seed = experiment.getSeed();
		Classifier classifierForRPNDSplit = AbstractClassifier.forName(experiment.getNameOfInnerClassifier(), null);
		Classifier leftClassifier = AbstractClassifier.forName(experiment.getNameOfLeftClassifier(), null);
		Classifier innerClassifier = AbstractClassifier.forName(experiment.getNameOfInnerClassifier(), null);
		Classifier rightClassifier = AbstractClassifier.forName(experiment.getNameOfRightClassifier(), null);

		RPNDSplitter splitter = new RPNDSplitter(new Random(seed), classifierForRPNDSplit);

		/* conduct experiments */
		List<Map<String, Object>> results = new ArrayList<>();
		for (int k = 0; k < 10; k++) {
			List<Collection<String>> classSplit;
			try {
				classSplit = new ArrayList<>(splitter.split(data));
			} catch (InterruptedException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException("Could not create RPND split.", e);
			}
			MCTreeNodeReD classifier = new MCTreeNodeReD(innerClassifier, classSplit.get(0), leftClassifier, classSplit.get(1), rightClassifier);
			long start = System.currentTimeMillis();
			Map<String, Object> result = new HashMap<>();
			List<Instances> dataSplit = WekaUtil.getStratifiedSplit(data, (seed + k), .7);
			classifier.buildClassifier(dataSplit.get(0));
			long time = System.currentTimeMillis() - start;
			Evaluation eval = new Evaluation(dataSplit.get(0));
			eval.evaluateModel(classifier, dataSplit.get(1));
			double loss = (100 - eval.pctCorrect()) / 100f;
			logger.info("Conducted experiment {} with split {}/{}. Loss: {}. Time: {}ms.", k, classSplit.get(0), classSplit.get(1), loss, time);
			result.put("errorRate", loss);
			result.put(LABEL_TRAIN_TIME, time);
			results.add(result);
		}
		return results;
	}

	public static List<Map<String, Object>> conductEnsembleOfOneStepReductionsExperiment(final EnsembleOfSimpleOneStepReductionsExperiment experiment) throws Exception {

		/* load data */
		Instances data = new Instances(new BufferedReader(new FileReader(experiment.getDataset())));
		data.setClassIndex(data.numAttributes() - 1);

		/* prepare basis for experiments */
		int seed = experiment.getSeed();
		String classifier = experiment.getNameOfClassifier();
		RPNDSplitter splitter = new RPNDSplitter(new Random(seed), AbstractClassifier.forName(classifier, null));

		/* conduct experiments */
		List<Map<String, Object>> results = new ArrayList<>();
		for (int k = 0; k < 10; k++) {

			Vote ensemble = new Vote();
			ensemble.setOptions(new String[] { "-R", "MAJ" });
			long start = System.currentTimeMillis();
			List<Instances> dataSplit = WekaUtil.getStratifiedSplit(data, (seed + k), .7);
			for (int i = 0; i < experiment.getNumberOfStumps(); i++) {

				List<Collection<String>> classSplit;
				classSplit = new ArrayList<>(splitter.split(data));
				MCTreeNodeReD tree = new MCTreeNodeReD(classifier, classSplit.get(0), classifier, classSplit.get(1), classifier);
				tree.buildClassifier(dataSplit.get(0));
				ensemble.addPreBuiltClassifier(tree);
			}
			Map<String, Object> result = new HashMap<>();
			result.put(LABEL_TRAIN_TIME, System.currentTimeMillis() - start);

			/* now evaluate the ensemble */
			ensemble.buildClassifier(data);
			Evaluation eval = new Evaluation(dataSplit.get(0));
			eval.evaluateModel(ensemble, dataSplit.get(1));
			double loss = (100 - eval.pctCorrect()) / 100f;
			logger.info("Conducted experiment {}. Loss: {}. Time: {}ms.", k, loss, result.get(LABEL_TRAIN_TIME));
			result.put("errorRate", loss);
			results.add(result);
		}
		return results;
	}
}
