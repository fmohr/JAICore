package ai.libs.mlplan.core;

import java.io.File;

import ai.libs.hasco.core.HASCOFactory;
import ai.libs.jaicore.ml.core.evaluation.evaluator.factory.ClassifierEvaluatorConstructionFailedException;
import ai.libs.jaicore.ml.weka.dataset.splitter.IDatasetSplitter;
import ai.libs.jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNode;
import ai.libs.jaicore.search.probleminputs.GraphSearchWithPathEvaluationsInput;
import ai.libs.mlpipeline_evaluation.PerformanceDBAdapter;
import ai.libs.mlplan.multiclass.MLPlanClassifierConfig;
import ai.libs.mlplan.multiclass.wekamlplan.IClassifierFactory;
import weka.core.Instances;

/**
 * The IMLPlanBuilder provides the general interface of an ML-Plan builder independent
 * of the problem domain or specific library that is used for the configuration of machine
 * learning pipelines.
 *
 * @author mwever
 *
 */
public interface IMLPlanBuilder {

	public IDatasetSplitter getSearchSelectionDatasetSplitter();

	public PipelineEvaluator getClassifierEvaluationInSearchPhase(Instances dataShownToSearch, int randomSeed, int size) throws ClassifierEvaluatorConstructionFailedException;

	public PipelineEvaluator getClassifierEvaluationInSelectionPhase(Instances dataShownToSearch, int randomSeed) throws ClassifierEvaluatorConstructionFailedException;

	public String getPerformanceMeasureName();

	public String getRequestedInterface();

	public File getSearchSpaceConfigFile();

	public IClassifierFactory getClassifierFactory();

	public HASCOFactory<GraphSearchWithPathEvaluationsInput<TFDNode, String, Double>, TFDNode, String, Double> getHASCOFactory();

	public MLPlanClassifierConfig getAlgorithmConfig();

	public void prepareNodeEvaluatorInFactoryWithData(Instances data);

	public PerformanceDBAdapter getDBAdapter();

	public boolean getUseCache();

}
