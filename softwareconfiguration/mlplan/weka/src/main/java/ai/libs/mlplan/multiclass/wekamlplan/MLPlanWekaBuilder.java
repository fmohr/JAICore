package wekamlplan;

import java.io.File;
import java.io.IOException;

import ai.libs.jaicore.basic.FileUtil;
import ai.libs.jaicore.basic.MathExt;
import ai.libs.jaicore.basic.ResourceUtil;
import ai.libs.jaicore.ml.classification.singlelabel.loss.ZeroOneLoss;
import ai.libs.jaicore.ml.core.evaluation.evaluator.factory.LearningCurveExtrapolationEvaluatorFactory;
import ai.libs.jaicore.ml.core.evaluation.evaluator.factory.MonteCarloCrossValidationEvaluatorFactory;
import ai.libs.jaicore.ml.core.evaluation.evaluator.splitevaluation.SimpleSLCSplitBasedClassifierEvaluator;
import ai.libs.jaicore.ml.core.filter.sampling.inmemory.ASamplingAlgorithm;
import ai.libs.jaicore.ml.core.filter.sampling.inmemory.factories.interfaces.ISamplingAlgorithmFactory;
import ai.libs.jaicore.ml.functionprediction.learner.learningcurveextrapolation.LearningCurveExtrapolationMethod;
import ai.libs.jaicore.ml.weka.dataset.WekaInstance;
import ai.libs.jaicore.ml.weka.dataset.WekaInstances;
import ai.libs.jaicore.ml.weka.dataset.splitter.MulticlassClassStratifiedSplitter;
import ai.libs.mlplan.core.AbstractMLPlanSingleLabelBuilder;
import ai.libs.mlplan.multiclass.wekamlplan.weka.WekaPipelineFactory;

public class MLPlanWekaBuilder extends AbstractMLPlanSingleLabelBuilder<MLPlanWekaBuilder> {

	private static final String RES_SSC_TINY_WEKA = "automl/searchmodels/weka/tinytest.json";
	private static final String RES_SSC_WEKA_COMPLETE = "automl/searchmodels/weka/weka-all-autoweka.json";
	private static final String FS_SSC_WEKA = "conf/mlplan-weka.json";

	private static final String RES_PREFERRED_COMPONENTS = "mlplan/weka-preferenceList.txt";
	private static final String FS_PREFERRED_COMPONENTS = "conf/mlpan-weka-preferenceList.txt";

	/* Default configuration values. */
	private static final String DEF_REQUESTED_HASCO_INTERFACE = "AbstractClassifier";
	private static final String DEF_PREFERRED_COMPONENT_NAME_PREFIX = "resolveAbstractClassifierWith";

	private static final ILearnerFactory DEF_CLASSIFIER_FACTORY = new WekaPipelineFactory();
	private static final File DEF_PREFERRED_COMPONENTS = FileUtil.getExistingFileWithHighestPriority(RES_PREFERRED_COMPONENTS, FS_PREFERRED_COMPONENTS);
	private static final File DEF_SEARCH_SPACE_CONFIG = FileUtil.getExistingFileWithHighestPriority(RES_SSC_WEKA_COMPLETE, FS_SSC_WEKA);
	private static final MonteCarloCrossValidationEvaluatorFactory DEF_SEARCH_PHASE_EVALUATOR = new MonteCarloCrossValidationEvaluatorFactory().withNumMCIterations(DEFAULT_SEARCH_NUM_MC_ITERATIONS).withTrainFoldSize(DEFAULT_SEARCH_TRAIN_FOLD_SIZE)
			.withSplitBasedEvaluator(new SimpleSLCSplitBasedClassifierEvaluator(DEFAULT_PERFORMANCE_MEASURE)).withDatasetSplitter(new MulticlassClassStratifiedSplitter());
	private static final MonteCarloCrossValidationEvaluatorFactory DEF_SELECTION_PHASE_EVALUATOR = new MonteCarloCrossValidationEvaluatorFactory().withNumMCIterations(DEFAULT_SELECTION_NUM_MC_ITERATIONS).withTrainFoldSize(DEFAULT_SELECTION_TRAIN_FOLD_SIZE)
			.withSplitBasedEvaluator(new SimpleSLCSplitBasedClassifierEvaluator(DEFAULT_PERFORMANCE_MEASURE)).withDatasetSplitter(new MulticlassClassStratifiedSplitter());

	public MLPlanWekaBuilder() throws IOException {
		super();
		this.withSearchSpaceConfigFile(DEF_SEARCH_SPACE_CONFIG);
		this.withPreferredComponentsFile(DEF_PREFERRED_COMPONENTS, DEF_PREFERRED_COMPONENT_NAME_PREFIX);
		this.withRequestedInterface(DEF_REQUESTED_HASCO_INTERFACE);
		this.withClassifierFactory(DEF_CLASSIFIER_FACTORY);
		this.withSearchPhaseEvaluatorFactory(DEF_SEARCH_PHASE_EVALUATOR);
		this.withSelectionPhaseEvaluatorFactory(DEF_SELECTION_PHASE_EVALUATOR);
		this.withPerformanceMeasure(DEFAULT_PERFORMANCE_MEASURE);

		// /* configure blow-ups for MCCV */
		double blowUpInSelectionPhase = MathExt.round(1f / DEFAULT_SEARCH_TRAIN_FOLD_SIZE * DEFAULT_SELECTION_NUM_MC_ITERATIONS / DEFAULT_SEARCH_NUM_MC_ITERATIONS, 2);
		this.getAlgorithmConfig().setProperty(MLPlanClassifierConfig.K_BLOWUP_SELECTION, String.valueOf(blowUpInSelectionPhase));
		double blowUpInPostprocessing = MathExt.round((1 / (1 - this.getAlgorithmConfig().dataPortionForSelection())) / DEFAULT_SELECTION_NUM_MC_ITERATIONS, 2);
		this.getAlgorithmConfig().setProperty(MLPlanClassifierConfig.K_BLOWUP_POSTPROCESS, String.valueOf(blowUpInPostprocessing));
	}

	/**
	 * Sets the search space to a tiny weka search space configuration.
	 * @throws IOException Thrown if the resource file cannot be read.
	 */
	public MLPlanWekaBuilder withTinyWekaSearchSpace() throws IOException {
		this.withSearchSpaceConfigFile(ResourceUtil.getResourceAsFile(RES_SSC_TINY_WEKA));
		return this;
	}

	/**
	 * Allows to use learning curve extrapolation for predicting the quality of candidate solutions.
	 * @param anchorpoints The anchor points for which samples are actually evaluated on the respective data.
	 * @param subsamplingAlgorithmFactory The factory for the sampling algorithm that is to be used to randomly draw training instances.
	 * @param trainSplitForAnchorpointsMeasurement The training fold size for measuring the acnhorpoints.
	 * @param extrapolationMethod The method to be used in order to extrapolate the learning curve from the anchorpoints.
	 */
	public void withLearningCurveExtrapolationEvaluation(final int[] anchorpoints, final ISamplingAlgorithmFactory<WekaInstance<Object>, WekaInstances<Object>, ? extends ASamplingAlgorithm<WekaInstance<Object>, WekaInstances<Object>>> subsamplingAlgorithmFactory,
			final double trainSplitForAnchorpointsMeasurement, final LearningCurveExtrapolationMethod extrapolationMethod) {
		this.withSearchPhaseEvaluatorFactory(new LearningCurveExtrapolationEvaluatorFactory(anchorpoints, subsamplingAlgorithmFactory, trainSplitForAnchorpointsMeasurement, extrapolationMethod));
		this.withSelectionPhaseEvaluatorFactory(new MonteCarloCrossValidationEvaluatorFactory().withNumMCIterations(3).withTrainFoldSize(.7).withSplitBasedEvaluator(new SimpleSLCSplitBasedClassifierEvaluator(new ZeroOneLoss())));
		this.getAlgorithmConfig().setProperty(MLPlanClassifierConfig.K_BLOWUP_SELECTION, "" + 10);
	}

}
