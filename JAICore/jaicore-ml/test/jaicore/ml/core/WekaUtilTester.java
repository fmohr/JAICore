package jaicore.ml.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

public class WekaUtilTester {

	private static final File folder = new File("testsrc/ml/orig/");
	Classifier[] portfolio = {
//			new BayesNet(), new NaiveBayes(), 
//			new SimpleLogistic(),
//			new IBk(), new KStar(),
//			new DecisionTable(),
//			new JRip(), new OneR(),
//			new PART(),
//			new ZeroR(), new DecisionStump(), new J48(),
//			new LMT(),
			new RandomForest(),
//			new RandomTree(),
//			new REPTree(),
//			new Logistic(),
//			new MultilayerPerceptron()
			};
	@Test
	public void checkSplit() throws Exception {
		
		Instances inst = new Instances(new BufferedReader(new FileReader(folder + File.separator + "vowel.arff")));
		inst.setClassIndex(inst.numAttributes() - 1);
		for (Classifier c : portfolio) {
			
			/* eval for CV */
			inst.stratify(10);
			Instances train = inst.trainCV(10, 0);
			Instances test  = inst.testCV(10, 0);
			Assert.assertEquals(train.size() + test.size(), inst.size());
			Evaluation eval = new Evaluation(train);
			eval.crossValidateModel(c, inst, 10, new Random(0));
			double acc = eval.pctCorrect();
			
			c.buildClassifier(train);
			eval.evaluateModel(c, test);
			System.out.println(eval.pctCorrect());
		}
	}

}
