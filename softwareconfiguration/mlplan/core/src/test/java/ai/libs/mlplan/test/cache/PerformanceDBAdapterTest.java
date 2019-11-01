package ai.libs.mlplan.test.cache;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.serialization.ComponentLoader;
import ai.libs.jaicore.basic.SQLAdapter;
import ai.libs.jaicore.ml.classification.multilabel.loss.F1MacroAverageL;
import ai.libs.jaicore.ml.classification.singlelabel.loss.ZeroOneLoss;
import ai.libs.jaicore.ml.weka.WekaUtil;
import ai.libs.jaicore.ml.weka.dataset.ReproducibleInstances;
import ai.libs.mlpipeline_evaluation.PerformanceDBAdapter;
import ai.libs.mlplan.multiclass.wekamlplan.weka.MLPipelineComponentInstanceFactory;
import ai.libs.mlplan.multiclass.wekamlplan.weka.model.MLPipeline;
import weka.attributeSelection.OneRAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.trees.RandomForest;

/**
 * Tests the funcionality of the {@link PerformanceDBAdapter}. Tries to save and load different performance values.
 *
 * @author jmhansel
 *
 */
public class PerformanceDBAdapterTest {
	@Test
	public void test() {
		SQLAdapter adapter = new SQLAdapter("host", "user", "password", "database");
		PerformanceDBAdapter pAdapter = new PerformanceDBAdapter(adapter, "performance_cache_test");
		try {
			ComponentLoader loader = new ComponentLoader(new File("conf/automl/searchmodels/weka/weka-all-autoweka.json"));
			MLPipelineComponentInstanceFactory factory = new MLPipelineComponentInstanceFactory(loader.getComponents());
			ComponentInstance composition1 = factory.convertToComponentInstance(new MLPipeline(new Ranker(), new OneRAttributeEval(), new RandomForest()));

			ReproducibleInstances reproducibleInstances1 = ReproducibleInstances.fromOpenML(40983, "4350e421cdc16404033ef1812ea38c01");
			List<ReproducibleInstances> instances1 = WekaUtil.getStratifiedSplit(reproducibleInstances1, 5, 0.7);
			String className1 = ZeroOneLoss.class.getName();
			double score = Math.PI / 5.0;

			// Store the first sample
			pAdapter.store(composition1, instances1.getAttributeValue(0), instances1.getAttributeValue(1), score, className1, 6517L);

			// These should have no entry in the db, assuming it was empty when the test was
			// started
			ComponentInstance composition2 = factory.convertToComponentInstance(new MLPipeline(new Ranker(), new OneRAttributeEval(), new MultilayerPerceptron()));
			ReproducibleInstances reproducibleInstances2 = ReproducibleInstances.fromOpenML(181, "4350e421cdc16404033ef1812ea38c01");
			List<ReproducibleInstances> instances2 = WekaUtil.getStratifiedSplit(reproducibleInstances2, 5, 0.7);
			List<ReproducibleInstances> instances3 = WekaUtil.getStratifiedSplit(reproducibleInstances1, 4, 0.7);
			String className2 = F1MacroAverageL.class.getName();

			// This is a different dataset
			Optional<Double> shouldntExist1 = pAdapter.exists(composition1, instances2.getAttributeValue(0), instances2.getAttributeValue(1), className1);
			// This is a different is a different seed for splitting
			Optional<Double> shouldntExist2 = pAdapter.exists(composition1, instances3.getAttributeValue(0), instances3.getAttributeValue(1), className1);
			// This is a different composition
			Optional<Double> shouldntExist3 = pAdapter.exists(composition2, instances1.getAttributeValue(0), instances1.getAttributeValue(1), className1);
			// This is a different loss function (or measure resp.)
			Optional<Double> shouldntExist4 = pAdapter.exists(composition1, instances1.getAttributeValue(0), instances1.getAttributeValue(1), className2);
			// This is the entry we inserted above
			Optional<Double> shouldExist1 = pAdapter.exists(composition1, instances1.getAttributeValue(0), instances1.getAttributeValue(1), className1);

			assertFalse(shouldntExist1.isPresent());
			assertFalse(shouldntExist2.isPresent());
			assertFalse(shouldntExist3.isPresent());
			assertFalse(shouldntExist4.isPresent());
			assertTrue(shouldExist1.isPresent());
			pAdapter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
