package jaicore.ml.extendedtree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Sets;

import jaicore.ml.core.FeatureSpace;
import jaicore.ml.intervaltree.ExtendedRandomTree;
import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;

public class ExtendedRandomTreeTest {
	// private static String testFile = "resources/regression_data/cpu-medium.arff";
	// private static String testFile =
	// "resources/regression_data/cpu_verysmall.arff";
	// private static String testFile = "resources/regression_data/cloud.arff";
	// private static String testFile = "resources/regression_data/cpu.small.arff";
	private static String testFile = "resources/regression_data/performance.arff";

	@Test
	public void testTrain() {
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(testFile), StandardCharsets.UTF_8)) {
			ArffReader arffReader = new ArffReader(reader);
			Instances data = arffReader.getData();

			data.setClassIndex(data.numAttributes() - 1);
			System.out.println(data);
			ExtendedRandomTree tree = new ExtendedRandomTree();
			// tree.setMinNum(10);
			tree.setFeatureSpace(new FeatureSpace(data));
			tree.buildClassifier(data);
			tree.preprocess();
			tree.printSizeOfFeatureSpaceAndPartitioning();
			double sum = 0.0d;
			HashSet<Integer> allFeatures = new HashSet<Integer>();
			for (int i = 0; i < tree.getFeatureSpace().getDimensionality(); i++) {
				allFeatures.add(i);
			}
			// Set<Set<Integer>> powerset = Sets.powerSet(allFeatures);
			List<Set<Integer>> powerlist = new ArrayList<Set<Integer>>();
			Set<Set<Integer>> subsets;
			// powerlist.addAll(powerset);
			for (int k = 1; k <= allFeatures.size(); k++) {
				// for (int k = 1; k <= 2; k++) {
				subsets = Sets.combinations(allFeatures, k);
				// for (int i = 0; i < powerlist.size(); i++) {
				// Set<Integer> features = powerlist.get(i);
				for (Set<Integer> features : subsets) {
					System.out.println("Features in this iteration = " + features);
					if (features.size() == k) {
						double cont = tree.computeMarginalVarianceContributionForSubsetOfFeatures(features);
						assertTrue(cont >= 0.0d);
						sum += cont;
						System.out.println("Contribution of " + features + ": " + cont);
					}
				}
			}
			// Set<Integer> cSet = new HashSet<Integer>();
			// cSet.add(0);
			// cSet.add(1);
			// System.out.println("cont: " +
			// tree.computeMarginalVarianceContributionForSubsetOfFeatures(cSet));
			System.out.println("sum of contributions = " + sum);
			assertEquals(1.0d, sum, 0.001d);
			// for(FeatureDomain domain : tree.getFeatureSpace().getFeatureDomains()) {
			// System.out.println(domain.compactString());
			// }
			// System.out.println("max in dataset: " +
			// data.attributeStats(1).numericStats.max);

			// tree.printSplitPoints();
			// tree.printObservations();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
