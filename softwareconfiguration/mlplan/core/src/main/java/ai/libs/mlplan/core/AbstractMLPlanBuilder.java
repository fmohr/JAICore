package ai.libs.mlplan.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.aeonbits.owner.ConfigFactory;
import org.api4.java.ai.graphsearch.problem.IOptimalPathInORGraphSearchFactory;
import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.IPathEvaluator;
import org.api4.java.ai.ml.classification.execution.ISupervisedLearnerMetric;
import org.api4.java.ai.ml.classification.singlelabel.evaluation.loss.ISingleLabelClassificationMeasure;
import org.api4.java.ai.ml.core.dataset.splitter.IFoldSizeConfigurableRandomDatasetSplitter;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledInstance;
import org.api4.java.ai.ml.core.evaluation.ISupervisedLearnerEvaluator;
import org.api4.java.ai.ml.core.learner.ISupervisedLearner;
import org.api4.java.algorithm.TimeOut;
import org.api4.java.common.control.ILoggingCustomizable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hasco.core.HASCOFactory;
import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.Parameter;
import ai.libs.hasco.model.ParameterRefinementConfiguration;
import ai.libs.hasco.serialization.ComponentLoader;
import ai.libs.hasco.variants.forwarddecomposition.HASCOViaFDAndBestFirstFactory;
import ai.libs.hasco.variants.forwarddecomposition.HASCOViaFDFactory;
import ai.libs.jaicore.basic.FileUtil;
import ai.libs.jaicore.basic.ResourceFile;
import ai.libs.jaicore.basic.ResourceUtil;
import ai.libs.jaicore.basic.algorithm.reduction.AlgorithmicProblemReduction;
import ai.libs.jaicore.ml.core.evaluation.ClassifierMetric;
import ai.libs.jaicore.ml.core.evaluation.evaluator.LearningCurveExtrapolationEvaluator;
import ai.libs.jaicore.ml.core.evaluation.evaluator.factory.ISupervisedLearnerEvaluatorFactory;
import ai.libs.jaicore.ml.core.evaluation.evaluator.factory.LearnerEvaluatorConstructionFailedException;
import ai.libs.jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNode;
import ai.libs.jaicore.search.algorithms.standard.bestfirst.StandardBestFirstFactory;
import ai.libs.jaicore.search.algorithms.standard.bestfirst.nodeevaluation.AlternativeNodeEvaluator;
import ai.libs.jaicore.search.probleminputs.GraphSearchWithPathEvaluationsInput;
import ai.libs.jaicore.search.problemtransformers.GraphSearchProblemInputToGraphSearchWithSubpathEvaluationInputTransformerViaRDFS;
import ai.libs.mlpipeline_evaluation.PerformanceDBAdapter;
import ai.libs.mlplan.multiclass.MLPlanClassifierConfig;
import ai.libs.mlplan.multiclass.MLPlanWekaBuilder;
import ai.libs.mlplan.multiclass.sklearn.MLPlanSKLearnBuilder;
import ai.libs.mlplan.multiclass.wekamlplan.ILearnerFactory;
import ai.libs.mlplan.multiclass.wekamlplan.weka.PreferenceBasedNodeEvaluator;
import ai.libs.mlplan.multilabel.MLPlanMekaBuilder;

/**
 * The MLPlanBuilder helps to easily configure and initialize ML-Plan with specific parameter settings.
 * For convenient use, the MLPlanBuilder also offers methods for initializing ML-Plan with default
 * configuration to use ML-Plan for single label classification in combination with WEKA or scikit-learn
 * or for multi-label classification in combination with MEKA and consequently with WEKA (for baselearners
 * of multi-label reduction strategies).
 *
 * @author mwever, fmohr
 */
public abstract class AbstractMLPlanBuilder<I extends ILabeledInstance, D extends ILabeledDataset<I>, L extends ISupervisedLearner<I, D>, B extends AbstractMLPlanBuilder<I, D, L, B>>
		implements IMLPlanBuilder<I, D, L, B>, ILoggingCustomizable {

	/* Logging */
	private Logger logger = LoggerFactory.getLogger(AbstractMLPlanBuilder.class);
	private String loggerName = AbstractMLPlanBuilder.class.getName();

	private static final String RES_ALGORITHM_CONFIG = "mlplan/mlplan.properties";
	private static final String FS_ALGORITHM_CONFIG = "conf/mlplan.properties";

	/* Default configuration values */
	private static final File DEF_ALGORITHM_CONFIG = FileUtil.getExistingFileWithHighestPriority(RES_ALGORITHM_CONFIG, FS_ALGORITHM_CONFIG);

	/* Builder (self) status variables */
	private boolean factoryPreparedWithData = false;

	/* Data for initializing ML-Plan */
	private MLPlanClassifierConfig algorithmConfig;

	@SuppressWarnings("rawtypes")
	private HASCOViaFDFactory hascoFactory = new HASCOViaFDFactory<GraphSearchWithPathEvaluationsInput<TFDNode, String, Double>, Double>();
	private Predicate<TFDNode> priorizingPredicate = null;
	private File searchSpaceFile;
	private String requestedHASCOInterface;
	private ILearnerFactory<L> classifierFactory;
	private ISupervisedLearnerMetric performanceMeasure = ClassifierMetric.MEAN_ERRORRATE;

	private IPathEvaluator<TFDNode, String, Double> preferredNodeEvaluator = null;
	private PipelineValidityCheckingNodeEvaluator<D> pipelineValidityCheckingNodeEvaluator;

	/* The splitter is used to create the split for separating search and selection data */
	private IFoldSizeConfigurableRandomDatasetSplitter<D> searchSelectionDatasetSplitter;
	private ISupervisedLearnerEvaluatorFactory<I, D> factoryForPipelineEvaluationInSearchPhase = null;
	private ISupervisedLearnerEvaluatorFactory<I, D> factoryForPipelineEvaluationInSelectionPhase = null;

	private Collection<Component> components = new LinkedList<>();

	/* Use caching */
	private boolean useCache;
	private PerformanceDBAdapter dbAdapter = null;

	/* The problem input for ML-Plan. */
	private D dataset;

	protected AbstractMLPlanBuilder() {
		super();
		this.withAlgorithmConfigFile(DEF_ALGORITHM_CONFIG);
		this.withRandomCompletionBasedBestFirstSearch();
	}

	public static MLPlanSKLearnBuilder forSKLearn() throws IOException {
		return new MLPlanSKLearnBuilder();
	}

	public static MLPlanWekaBuilder forWeka() throws IOException {
		return new MLPlanWekaBuilder();
	}

	public static MLPlanMekaBuilder forMeka() throws IOException {
		return new MLPlanMekaBuilder();
	}

	/**
	 * This ADDs a new preferred node evaluator; requires that the search will be a best-first search.
	 *
	 * It is possible to specify several preferred node evaluators, which will be ordered by the order in which they are specified. The latest given evaluator is the most preferred one.
	 *
	 * @param preferredNodeEvaluator
	 * @return
	 */
	public B withPreferredNodeEvaluator(final IPathEvaluator<TFDNode, String, Double> preferredNodeEvaluator) {
		if (this.factoryPreparedWithData) {
			throw new IllegalStateException("The method prepareNodeEvaluatorInFactoryWithData has already been called. No changes to the preferred node evaluator possible anymore");
		}

		/* first update the preferred node evaluator */
		if (this.preferredNodeEvaluator == null) {
			this.preferredNodeEvaluator = preferredNodeEvaluator;
		} else {
			this.preferredNodeEvaluator = new AlternativeNodeEvaluator<>(preferredNodeEvaluator, this.preferredNodeEvaluator);
		}
		this.update();
		return this.getSelf();
	}

	@SuppressWarnings("unchecked")
	public B withSearchFactory(@SuppressWarnings("rawtypes") final IOptimalPathInORGraphSearchFactory searchFactory, @SuppressWarnings("rawtypes") final AlgorithmicProblemReduction transformer) {
		this.hascoFactory.setSearchFactory(searchFactory);
		this.hascoFactory.setSearchProblemTransformer(transformer);
		return this.getSelf();
	}

	@SuppressWarnings("unchecked")
	public B withRandomCompletionBasedBestFirstSearch() {
		this.hascoFactory.setSearchFactory(new StandardBestFirstFactory<TFDNode, String, Double>());
		this.update();
		return this.getSelf();
	}

	public Collection<Component> getComponents() throws IOException {
		return new ComponentLoader(this.searchSpaceFile).getComponents();
	}

	public Map<Component, Map<Parameter, ParameterRefinementConfiguration>> getComponentParameterConfigurations() throws IOException {
		return new ComponentLoader(this.searchSpaceFile).getParamConfigs();
	}

	/***********************************************************************************************************************************************************************************************************************/
	/***********************************************************************************************************************************************************************************************************************/
	/***********************************************************************************************************************************************************************************************************************/
	/***********************************************************************************************************************************************************************************************************************/

	/**
	 * Loads the MLPlanClassifierConfig with default values and replaces all properties according to the properties defined in the given config file.
	 *
	 * @param algorithmConfigFile The file specifying the property values to replace the default configuration.
	 * @return The MLPlanBuilder object.
	 * @throws IOException An IOException is thrown if there are issues reading the config file.
	 */
	public B withAlgorithmConfigFile(final File algorithmConfigFile) {
		return this.withAlgorithmConfig((MLPlanClassifierConfig) ConfigFactory.create(MLPlanClassifierConfig.class).loadPropertiesFromFile(algorithmConfigFile));
	}

	/**
	 * Loads the MLPlanClassifierConfig with default values and replaces all properties according to the properties defined in the given config file.
	 *
	 * @param config The algorithm configuration.
	 * @return The MLPlanBuilder object.
	 * @throws IOException An IOException is thrown if there are issues reading the config file.
	 */
	public B withAlgorithmConfig(final MLPlanClassifierConfig config) {
		this.algorithmConfig = config;
		this.hascoFactory.withAlgorithmConfig(this.algorithmConfig);
		this.update();
		return this.getSelf();
	}

	/**
	 * Creates a preferred node evaluator that can be used to prefer components over other components.
	 *
	 * @param preferredComponentsFile The file containing a priority list of component names.
	 * @param preferableCompnentMethodPrefix The prefix of a method's name for refining a complex task to preferable components.
	 * @return The builder object.
	 * @throws IOException Thrown if a problem occurs while trying to read the file containing the priority list.
	 */
	public B withPreferredComponentsFile(final File preferredComponentsFile, final String preferableCompnentMethodPrefix) throws IOException {
		this.getAlgorithmConfig().setProperty(MLPlanClassifierConfig.PREFERRED_COMPONENTS, preferredComponentsFile.getAbsolutePath());
		List<String> ordering;
		if (preferredComponentsFile instanceof ResourceFile) {
			ordering = ResourceUtil.readResourceFileToStringList((ResourceFile) preferredComponentsFile);
		} else if (!preferredComponentsFile.exists()) {
			this.logger.warn("The configured file for preferred components \"{}\" does not exist. Not using any particular ordering.", preferredComponentsFile.getAbsolutePath());
			ordering = new ArrayList<>();
		} else {
			ordering = FileUtil.readFileAsList(preferredComponentsFile);
		}
		return this.withPreferredNodeEvaluator(new PreferenceBasedNodeEvaluator(this.components, ordering, preferableCompnentMethodPrefix));
	}

	/**
	 * Set the data for which ML-Plan is supposed to find the best pipeline.
	 *
	 * @param dataset The dataset for which ML-Plan is to be run.
	 * @return The builder object.
	 */
	public B withDataset(final D dataset) {
		this.dataset = dataset;
		return this.getSelf();
	}

	/**
	 * Specify the search space in which ML-Plan is required to work.
	 *
	 * @param searchSpaceConfig The file of the search space configuration.
	 * @return The builder object.
	 * @throws IOException Thrown if the given file does not exist.
	 */
	public B withSearchSpaceConfigFile(final File searchSpaceConfig) throws IOException {
		FileUtil.requireFileExists(searchSpaceConfig);
		this.searchSpaceFile = searchSpaceConfig;
		this.components.clear();
		this.components.addAll(new ComponentLoader(this.searchSpaceFile).getComponents());
		return this.getSelf();
	}

	/**
	 * Set the classifier factory that translates <code>CompositionInstance</code> objects to classifiers that can be evaluated.
	 *
	 * @param classifierFactory The classifier factory to be used to translate CompositionInstance objects to classifiers.
	 * @return The builder object.
	 */
	public B withClassifierFactory(final ILearnerFactory<L> classifierFactory) {
		this.classifierFactory = classifierFactory;
		return this.getSelf();
	}

	/**
	 * Set the dataset splitter that is used for generating the holdout data portion that is put aside during search.
	 *
	 * @param datasetSplitter The dataset splitter to be used.
	 * @return The builder obect.
	 */
	public B withDatasetSplitterForSearchSelectionSplit(final IFoldSizeConfigurableRandomDatasetSplitter<D> datasetSplitter) {
		this.searchSelectionDatasetSplitter = datasetSplitter;
		return this.getSelf();
	}

	public B withRequestedInterface(final String requestedInterface) {
		this.requestedHASCOInterface = requestedInterface;
		return this.getSelf();
	}

	/**
	 * @param timeout The timeout for ML-Plan to search for the best classifier.
	 * @return The builder object.
	 */
	public B withTimeOut(final TimeOut timeout) {
		this.algorithmConfig.setProperty(MLPlanClassifierConfig.K_TIMEOUT, timeout.milliseconds() + "");
		this.update();
		return this.getSelf();
	}

	/**
	 * @return The timeout for ML-Plan to search for the best classifier.
	 */
	public TimeOut getTimeOut() {
		return new TimeOut(this.algorithmConfig.timeout(), TimeUnit.MILLISECONDS);
	}

	/**
	 * @param timeout The timeout for a single candidate evaluation.
	 * @return The builder object.
	 */
	public B withNodeEvaluationTimeOut(final TimeOut timeout) {
		this.algorithmConfig.setProperty(MLPlanClassifierConfig.K_RANDOM_COMPLETIONS_TIMEOUT_NODE, timeout.milliseconds() + "");
		this.update();
		return this.getSelf();
	}

	/**
	 * @return The timeout for ML-Plan to search for the best classifier.
	 */
	public TimeOut getNodeEvaluationTimeOut() {
		return new TimeOut(this.algorithmConfig.timeoutForNodeEvaluation(), TimeUnit.MILLISECONDS);
	}

	/**
	 * @param timeout The timeout for a single candidate evaluation.
	 * @return The builder object.
	 */
	public B withCandidateEvaluationTimeOut(final TimeOut timeout) {
		this.algorithmConfig.setProperty(MLPlanClassifierConfig.K_RANDOM_COMPLETIONS_TIMEOUT_PATH, timeout.milliseconds() + "");
		this.update();
		return this.getSelf();
	}

	/**
	 * @return The timeout for ML-Plan to search for the best classifier.
	 */
	public TimeOut getCandidateEvaluationTimeOut() {
		return new TimeOut(this.algorithmConfig.timeoutForCandidateEvaluation(), TimeUnit.MILLISECONDS);
	}

	@Override
	public PipelineEvaluator<I, D> getClassifierEvaluationInSearchPhase(final D data, final int seed, final int fullDatasetSize) throws LearnerEvaluatorConstructionFailedException {
		Objects.requireNonNull(this.factoryForPipelineEvaluationInSearchPhase, "No factory for pipeline evaluation in search phase has been set!");

		ISupervisedLearnerEvaluator<I, D> evaluator = this.factoryForPipelineEvaluationInSearchPhase.getDataspecificRandomizedLearnerEvaluator(data, ClassifierMetric.MEAN_ERRORRATE, new Random(seed));
		if (evaluator instanceof LearningCurveExtrapolationEvaluator) {
			((LearningCurveExtrapolationEvaluator) evaluator).setFullDatasetSize(fullDatasetSize);
		}

		return new PipelineEvaluator<>(this.getLearnerFactory(), evaluator, this.getAlgorithmConfig().timeoutForCandidateEvaluation());
	}

	@Override
	public PipelineEvaluator<I, D> getClassifierEvaluationInSelectionPhase(final D data, final int seed) throws LearnerEvaluatorConstructionFailedException {
		if (this.factoryForPipelineEvaluationInSelectionPhase == null) {
			throw new IllegalStateException("No factory for pipeline evaluation in selection phase has been set!");
		}
		return new PipelineEvaluator<>(this.getLearnerFactory(), this.factoryForPipelineEvaluationInSelectionPhase.getDataspecificRandomizedLearnerEvaluator(data, ClassifierMetric.MEAN_ERRORRATE, new Random(seed)), Integer.MAX_VALUE);
	}

	/**
	 * Sets the evaluator factory for the search phase.
	 *
	 * @param evaluatorFactory The evaluator factory for the search phase.
	 * @return The builder object.
	 */
	public void withSearchPhaseEvaluatorFactory(final ISupervisedLearnerEvaluatorFactory<I, D> evaluatorFactory) {
		this.factoryForPipelineEvaluationInSearchPhase = evaluatorFactory;
	}

	/**
	 * @return The factory for the classifier evaluator of the search phase.
	 */
	protected ISupervisedLearnerEvaluatorFactory<I, D> getSearchEvaluatorFactory() {
		return this.factoryForPipelineEvaluationInSearchPhase;
	}

	/**
	 * Sets the evaluator factory for the selection phase.
	 *
	 * @param evaluatorFactory The evaluator factory for the selection phase.
	 * @return The builder object.
	 */
	public B withSelectionPhaseEvaluatorFactory(final ISupervisedLearnerEvaluatorFactory<I, D> evaluatorFactory) {
		this.factoryForPipelineEvaluationInSelectionPhase = evaluatorFactory;
		return this.getSelf();
	}

	/**
	 * Sets the performance measure to evaluate a candidate solution's generalization performance. Caution: This resets the evaluators to MCCV for both search and selection phase if these are not already MCCVs.
	 * @param performanceMeasure The loss function to be used.
	 * @return The builder object.
	 */
	public B withPerformanceMeasure(final ISingleLabelClassificationMeasure performanceMeasure) {
		this.performanceMeasure = performanceMeasure;
		return this.getSelf();
	}

	/**
	 * Sets the number of cpus that may be used by ML-Plan.
	 *
	 * @param numCpus The number of cpus to use.
	 * @return The builder object.
	 */
	public B withNumCpus(final int numCpus) {
		this.algorithmConfig.setProperty(MLPlanClassifierConfig.K_CPUS, numCpus + "");
		this.update();
		return this.getSelf();
	}

	/**
	 * @return The factory for the classifier evaluator of the selection phase.
	 */
	protected ISupervisedLearnerEvaluatorFactory<I, D> getSelectionEvaluatorFactory() {
		return this.factoryForPipelineEvaluationInSelectionPhase;
	}

	@Override
	public ISupervisedLearnerMetric getPerformanceMeasure() {
		return this.performanceMeasure;
	}

	@SuppressWarnings("unchecked")
	@Override
	public HASCOFactory getHASCOFactory() {
		return this.hascoFactory;
	}

	@Override
	public ILearnerFactory<L> getLearnerFactory() {
		return this.classifierFactory;
	}

	@Override
	public String getLoggerName() {
		return this.loggerName;
	}

	@Override
	public void setLoggerName(final String name) {
		this.logger = LoggerFactory.getLogger(name);
		this.loggerName = name;
	}

	@Override
	public String getRequestedInterface() {
		return this.requestedHASCOInterface;
	}

	@Override
	public IFoldSizeConfigurableRandomDatasetSplitter<D> getSearchSelectionDatasetSplitter() {
		return this.searchSelectionDatasetSplitter;
	}

	@Override
	public File getSearchSpaceConfigFile() {
		return this.searchSpaceFile;
	}

	@Override
	public MLPlanClassifierConfig getAlgorithmConfig() {
		return this.algorithmConfig;
	}

	@Override
	public boolean getUseCache() {
		return this.useCache;
	}

	@Override
	public PerformanceDBAdapter getDBAdapter() {
		return this.dbAdapter;
	}

	@Override
	public void prepareNodeEvaluatorInFactoryWithData(final D data) {
		if (!(this.hascoFactory instanceof HASCOViaFDAndBestFirstFactory)) {
			return;
		}
		if (this.factoryPreparedWithData) {
			throw new IllegalStateException("Factory has already been prepared with data. This can only be done once!");
		}
		this.factoryPreparedWithData = true;

		/* nothing to do if there are no preferred node evaluators */
		if (this.pipelineValidityCheckingNodeEvaluator == null && this.preferredNodeEvaluator == null) {
			return;
		}

		/* now determine the real node evaluator to be used. A semantic node evaluator has highest priority */
		IPathEvaluator<TFDNode, String, Double> actualNodeEvaluator;
		if (this.pipelineValidityCheckingNodeEvaluator != null) {
			this.pipelineValidityCheckingNodeEvaluator.setComponents(this.components);
			this.pipelineValidityCheckingNodeEvaluator.setData(data);
			if (this.preferredNodeEvaluator != null) {
				actualNodeEvaluator = new AlternativeNodeEvaluator<>(this.pipelineValidityCheckingNodeEvaluator, this.preferredNodeEvaluator);
			} else {
				actualNodeEvaluator = this.pipelineValidityCheckingNodeEvaluator;
			}
		} else {
			actualNodeEvaluator = this.preferredNodeEvaluator;
		}

		/* update the preferred node evaluator in the HascoFactory */
		this.preferredNodeEvaluator = actualNodeEvaluator;
		this.update();
	}

	@SuppressWarnings("unchecked")
	private void update() {
		this.hascoFactory.setSearchProblemTransformer(new GraphSearchProblemInputToGraphSearchWithSubpathEvaluationInputTransformerViaRDFS<TFDNode, String, Double>(this.preferredNodeEvaluator, this.priorizingPredicate,
				this.algorithmConfig.randomSeed(), this.algorithmConfig.numberOfRandomCompletions(), this.algorithmConfig.timeoutForCandidateEvaluation(), this.algorithmConfig.timeoutForNodeEvaluation()));
		this.hascoFactory.withAlgorithmConfig(this.getAlgorithmConfig());
	}

	/**
	 * Builds an ML-Plan object for the given dataset as input.
	 *
	 * @param dataset The dataset for which an ML-Plan object is to be built.
	 * @return The ML-Plan object configured with this builder.
	 */
	public MLPlan<I, D, L> build(final D dataset) {
		this.dataset = dataset;
		return this.build();
	}

	/**
	 * Builds an ML-Plan object with the dataset provided earlier to this builder.
	 *
	 * @return The ML-Plan object configured with this builder.
	 */
	public MLPlan<I, D, L> build() {
		Objects.requireNonNull(this.dataset, "A dataset needs to be provided as input to ML-Plan");
		MLPlan<I, D, L> mlplan = new MLPlan<>(this, this.dataset);
		mlplan.setTimeout(this.getTimeOut());
		return mlplan;
	}

}
