package de.upb.crc901.mlplan.test.cache;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.upb.crc901.mlpipeline_evaluation.CacheEvaluatorMeasureBridge;
import de.upb.crc901.mlplan.multiclass.wekamlplan.weka.WEKAPipelineFactory;
import hasco.model.ComponentInstance;
import hasco.serialization.HASCOJacksonModule;
import jaicore.ml.cache.Instruction;
import jaicore.ml.cache.ReproducibleInstances;
import jaicore.ml.evaluation.evaluators.weka.MonteCarloCrossValidationEvaluator;
import jaicore.ml.evaluation.measures.multiclass.ZeroOneLoss;
import weka.classifiers.Classifier;

public class ReproducabilityTest {

	@Test
	public void test() {
		String trainJson = "[{\"inputs\": {\"id\": \"40983\", \"provider\": \"openml.org\"}, \"command\": \"loadDataset\"}, {\"inputs\": {\"seed\": \"-4962768465676381896\", \"ratios\": \"[0.7]\", \"outIndex\": \"0\"}, \"command\": \"split\"}, {\"inputs\": {\"seed\": \"0\", \"ratios\": \"[0.3]\", \"outIndex\": \"1\"}, \"command\": \"split\"}, {\"inputs\": {\"seed\": \"1534718591\", \"ratios\": \"[0.7]\", \"outIndex\": \"0\"}, \"command\": \"split\"}]";
		String validationJson = "[{\"inputs\": {\"id\": \"40983\", \"provider\": \"openml.org\"}, \"command\": \"loadDataset\"}, {\"inputs\": {\"seed\": \"-4962768465676381896\", \"ratios\": \"[0.7]\", \"outIndex\": \"0\"}, \"command\": \"split\"}, {\"inputs\": {\"seed\": \"0\", \"ratios\": \"[0.3]\", \"outIndex\": \"1\"}, \"command\": \"split\"}, {\"inputs\": {\"seed\": \"1534718591\", \"ratios\": \"[0.7]\", \"outIndex\": \"1\"}, \"command\": \"split\"}]";
		String compositionJson = "{\"component\": {\"name\": \"weka.classifiers.bayes.NaiveBayesMultinomial\", \"parameters\": [], \"dependencies\": [], \"providedInterfaces\": [\"weka.classifiers.bayes.NaiveBayesMultinomial\", \"AbstractClassifier\", \"WekaBaseClassifier\", \"BaseClassifier\"], \"requiredInterfaces\": {}}, \"parameterValues\": {}, \"satisfactionOfRequiredInterfaces\": {}, \"parametersThatHaveBeenSetExplicitly\": [], \"parametersThatHaveNotBeenSetExplicitly\": []}";
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new HASCOJacksonModule());
		
		try {
			List<Instruction> trainHistory = mapper.readValue(trainJson, new TypeReference<List<Instruction>>(){});
			List<Instruction> validationHistory = mapper.readValue(validationJson, new TypeReference<List<Instruction>>(){});
			ComponentInstance composition = mapper.readValue(compositionJson, ComponentInstance.class);
			ReproducibleInstances trainInstances = ReproducibleInstances.FromHistory(trainHistory, "4350e421cdc16404033ef1812ea38c01");
			ReproducibleInstances validationInstances = ReproducibleInstances.FromHistory(validationHistory, "4350e421cdc16404033ef1812ea38c01");
			ZeroOneLoss basicEvaluator = new ZeroOneLoss();
			CacheEvaluatorMeasureBridge bridge = new CacheEvaluatorMeasureBridge(basicEvaluator, null);
			trainInstances.setCacheLookup(false);
			validationInstances.setCacheLookup(false);
			trainInstances.setCacheStorage(false);
			validationInstances.setCacheStorage(false);
			WEKAPipelineFactory factory = new WEKAPipelineFactory();
			Classifier pipeline = factory.getComponentInstantiation(composition);
			Double score = bridge.evaluateSplit(pipeline, trainInstances, validationInstances);
			System.out.println(score);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
