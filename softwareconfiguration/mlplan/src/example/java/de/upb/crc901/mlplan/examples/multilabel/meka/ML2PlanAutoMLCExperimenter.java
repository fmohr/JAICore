package de.upb.crc901.mlplan.examples.multilabel.meka;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.aeonbits.owner.ConfigCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.upb.crc901.mlplan.core.AbstractMLPlanBuilder;
import de.upb.crc901.mlplan.core.MLPlan;
import de.upb.crc901.mlplan.core.MLPlanMekaBuilder;
import de.upb.crc901.mlplan.multiclass.MLPlanClassifierConfig;
import jaicore.basic.SQLAdapter;
import jaicore.basic.TimeOut;
import jaicore.basic.algorithm.exceptions.AlgorithmTimeoutedException;
import jaicore.experiments.ExperimentDBEntry;
import jaicore.experiments.IExperimentIntermediateResultProcessor;
import jaicore.experiments.IExperimentSetEvaluator;
import jaicore.experiments.exceptions.ExperimentEvaluationFailedException;
import jaicore.ml.core.evaluation.measure.ClassifierMetricGetter;
import jaicore.ml.core.evaluation.measure.multilabel.AutoMEKAGGPFitnessMeasureLoss;
import jaicore.ml.core.evaluation.measure.multilabel.F1MacroAverageLLoss;
import jaicore.ml.core.evaluation.measure.multilabel.HammingLoss;
import jaicore.ml.core.evaluation.measure.multilabel.InstanceWiseF1AsLoss;
import jaicore.ml.core.evaluation.measure.multilabel.RankLoss;
import jaicore.ml.weka.dataset.splitter.MultilabelDatasetSplitter;
import meka.classifiers.multilabel.Evaluation;
import meka.classifiers.multilabel.MultiLabelClassifier;
import meka.core.MLUtils;
import meka.core.Result;
import weka.core.Instances;

/**
 * Experimenter for ML2PLan & AutoMLC
 *
 * @author helegraf, mwever
 *
 */
public class ML2PlanAutoMLCExperimenter implements IExperimentSetEvaluator {

	private static final ML2PlanAutoMLCExperimenterConfig CONFIG = ConfigCache.getOrCreate(ML2PlanAutoMLCExperimenterConfig.class);

	/* Logging */
	private final SQLAdapter adapter;
	private Logger logger = LoggerFactory.getLogger(ML2PlanAutoMLCExperimenter.class);

	public ML2PlanAutoMLCExperimenter(final SQLAdapter adapter) {
		this.adapter = adapter;
	}

	@Override
	public void evaluate(final ExperimentDBEntry experimentEntry, final IExperimentIntermediateResultProcessor processor) throws ExperimentEvaluationFailedException {
		try {
			this.logger.info("Experiment ID: {}", experimentEntry.getId());
			this.logger.info("Experiment Description: {}", experimentEntry.getExperiment().getValuesOfKeyFields());

			Map<String, String> experimentDescription = experimentEntry.getExperiment().getValuesOfKeyFields();

			// Load dataset and prepare the dataset to be ready for multi-label classification
			File datasetFile = new File(CONFIG.getDatasetFolder(), experimentDescription.get("dataset") + ".arff");
			Instances data = new Instances(new FileReader(datasetFile));
			MLUtils.prepareData(data);

			// Get train / test splits
			String splitDescriptionTrainTest = experimentDescription.get("test_split_tech");
			String testFold = experimentDescription.get("test_fold");
			String testSeed = experimentDescription.get("seed");
			Instances train = MultilabelDatasetSplitter.getTrainSplit(data, splitDescriptionTrainTest, testFold, testSeed);
			Instances test = MultilabelDatasetSplitter.getTestSplit(data, splitDescriptionTrainTest, testFold, testSeed);

			TimeOut mlplanTimeOut = new TimeOut(Integer.parseInt(experimentDescription.get("timeout")), TimeUnit.MINUTES);
			TimeOut nodeEvalTimeOut = new TimeOut(Integer.parseInt(experimentDescription.get("node_timeout")), TimeUnit.MINUTES);

			// Prepare connection
			ResultsDBConnection connection = new ResultsDBConnection("intermediate_measurements", "final_measurements", "ordered_metric", experimentEntry.getId(), "ML2Plan", this.adapter);

			// Evaluation: test
			this.logger.info("Now test...");

			MLPlanMekaBuilder builder = AbstractMLPlanBuilder.forMeka();
			builder.withNodeEvaluationTimeOut(nodeEvalTimeOut);
			builder.withCandidateEvaluationTimeOut(nodeEvalTimeOut);

			int metricIdToOptimize = Integer.parseInt(experimentDescription.get("metric_id"));
			switch (metricIdToOptimize) {
			case 8: // rank loss
				builder.withPerformanceMeasure(new RankLoss());
				break;
			case 1: // hamming
				builder.withPerformanceMeasure(new HammingLoss());
				break;
			case 62: // F1Measure avgd by instances
				builder.withPerformanceMeasure(new InstanceWiseF1AsLoss());
				break;
			case 74: // F1Measure avgd by labels (standard F1 measure for MLC)
				builder.withPerformanceMeasure(new F1MacroAverageLLoss());
				break;
			case 73: // fitness
			default:
				builder.withPerformanceMeasure(new AutoMEKAGGPFitnessMeasureLoss());
				break;
			}

			MLPlanClassifierConfig algoConfig = builder.getAlgorithmConfig();
			algoConfig.setProperty(MLPlanClassifierConfig.SELECTION_PORTION, "0.0");
			builder.withAlgorithmConfig(algoConfig);

			MLPlan mlplan = null;
			try {
				mlplan = new MLPlan(builder, train);
				mlplan.setTimeout(mlplanTimeOut);
				mlplan.setNumCPUs(CONFIG.getNumberOfCPUs());
				mlplan.setLoggerName("ml2plan");

				MultiLabelClassifier classifier;
				try {
					mlplan.call();
				} catch (AlgorithmTimeoutedException e) {
					this.logger.warn("MLPlan got a delayed timeout exception", e);
				} finally {
					classifier = (MultiLabelClassifier) mlplan.getSelectedClassifier();
				}

				if (classifier == null) {
					throw new NullPointerException("No classifier was found by ML2Plan");
				}

				this.logger.info("Evaluate classifier...");
				Result result = Evaluation.evaluateModel(classifier, train, test);
				this.logger.info("Done evaluating Classifier.");
				this.logger.info("Store results in DB...");
				HashMap<String, Double> metrics = new HashMap<>();
				ClassifierMetricGetter.getMultiLabelMetrics().forEach(metric -> {
					try {
						metrics.put(metric, ClassifierMetricGetter.getValueOfMultilabelClassifier(result, metric));
					} catch (Exception e) {
						this.logger.warn("Could not measure metric {} for final classifier choice.", e);
					}
				});
				connection.addFinalMeasurements(metrics);
				this.logger.info("Stored results in DB.");
				this.logger.info("Done with evaluation. Send job result.");
				Map<String, Object> results = new HashMap<>();
				results.put("completed", true);
				results.put("classifier_string", mlplan.getComponentInstanceOfSelectedClassifier() + "");
				results.put("value", mlplan.getInternalValidationErrorOfSelectedClassifier());
				processor.processResults(results);

				this.logger.info("Evaluation task completed.");
			} finally {
				if (mlplan != null) {
					mlplan.cancel();
				}
			}
		} catch (Exception e) {
			throw new ExperimentEvaluationFailedException(e);
		}
	}
}