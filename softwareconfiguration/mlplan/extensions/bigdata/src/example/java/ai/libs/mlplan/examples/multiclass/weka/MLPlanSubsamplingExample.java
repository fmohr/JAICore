package ai.libs.mlplan.examples.multiclass.weka;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.api4.java.algorithm.TimeOut;

import ai.libs.hasco.gui.statsplugin.HASCOModelStatisticsPlugin;
import ai.libs.jaicore.graphvisualizer.events.recorder.property.AlgorithmEventPropertyComputer;
import ai.libs.jaicore.graphvisualizer.plugin.graphview.GraphViewPlugin;
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeDisplayInfoAlgorithmEventPropertyComputer;
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeInfoAlgorithmEventPropertyComputer;
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeInfoGUIPlugin;
import ai.libs.jaicore.graphvisualizer.plugin.solutionperformanceplotter.ScoredSolutionCandidateInfoAlgorithmEventPropertyComputer;
import ai.libs.jaicore.graphvisualizer.plugin.solutionperformanceplotter.SolutionPerformanceTimelinePlugin;
import ai.libs.jaicore.graphvisualizer.window.AlgorithmVisualizationWindow;
import ai.libs.jaicore.ml.core.filter.sampling.infiles.AFileSamplingAlgorithm;
import ai.libs.jaicore.ml.core.filter.sampling.infiles.stratified.sampling.ClassStratiFileAssigner;
import ai.libs.jaicore.ml.core.filter.sampling.infiles.stratified.sampling.StratifiedFileSampling;
import ai.libs.jaicore.ml.weka.WekaUtil;
import ai.libs.jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNodeInfoGenerator;
import ai.libs.jaicore.search.gui.plugins.rollouthistograms.RolloutInfoAlgorithmEventPropertyComputer;
import ai.libs.jaicore.search.gui.plugins.rollouthistograms.SearchRolloutHistogramPlugin;
import ai.libs.jaicore.search.model.travesaltree.JaicoreNodeInfoGenerator;
import ai.libs.mlplan.core.AbstractMLPlanBuilder;
import ai.libs.mlplan.core.MLPlan;
import ai.libs.mlplan.gui.outofsampleplots.OutOfSampleErrorPlotPlugin;
import ai.libs.mlplan.gui.outofsampleplots.WekaClassifierSolutionCandidateRepresenter;
import ai.libs.mlplan.multiclass.MLPlanWekaBuilder;
import ai.libs.mlplan.multiclass.wekamlplan.weka.model.MLPipeline;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.core.Instances;

public class MLPlanSubsamplingExample {

	public static void main(final String[] args) throws Exception {

		/* create a subsample of the input with 1000 datapoints */
		File file = new File("testrsc/car.arff");
		File sampleFile = new File("testrsc/car_sample.arff");
		sampleFile.deleteOnExit();
		AFileSamplingAlgorithm samplingAlgorithm = new StratifiedFileSampling(new Random(1l), new ClassStratiFileAssigner(), file);
		samplingAlgorithm.setSampleSize(1000);
		samplingAlgorithm.setOutputFileName(sampleFile.getAbsolutePath());
		samplingAlgorithm.call();

		/* create a train-test-split */
		Instances data = new Instances(new FileReader(sampleFile));
		data.setClassIndex(data.numAttributes() - 1);
		List<Instances> split = WekaUtil.getStratifiedSplit(data, 0, .7f);

		/* initialize mlplan with a tiny search space, and let it run for 30 seconds */
		MLPlanWekaBuilder builder = AbstractMLPlanBuilder.forWeka();
		builder.withNodeEvaluationTimeOut(new TimeOut(30, TimeUnit.SECONDS));
		builder.withCandidateEvaluationTimeOut(new TimeOut(10, TimeUnit.SECONDS));
		builder.withTimeOut(new TimeOut(300, TimeUnit.SECONDS));
		builder.withNumCpus(1);
		MLPlan mlplan = new MLPlan(builder, split.get(0));
		mlplan.setPortionOfDataForPhase2(0f);
		mlplan.setLoggerName("mlplan");

		new JFXPanel();

		NodeInfoAlgorithmEventPropertyComputer nodeInfoAlgorithmEventPropertyComputer = new NodeInfoAlgorithmEventPropertyComputer();
		List<AlgorithmEventPropertyComputer> algorithmEventPropertyComputers = Arrays.asList(nodeInfoAlgorithmEventPropertyComputer,
				new NodeDisplayInfoAlgorithmEventPropertyComputer<>(new JaicoreNodeInfoGenerator<>(new TFDNodeInfoGenerator())), new RolloutInfoAlgorithmEventPropertyComputer(nodeInfoAlgorithmEventPropertyComputer),
				new ScoredSolutionCandidateInfoAlgorithmEventPropertyComputer(new WekaClassifierSolutionCandidateRepresenter()));

		AlgorithmVisualizationWindow window = new AlgorithmVisualizationWindow(mlplan, algorithmEventPropertyComputers, new GraphViewPlugin(), new NodeInfoGUIPlugin(), new SearchRolloutHistogramPlugin(),
				new SolutionPerformanceTimelinePlugin(), new HASCOModelStatisticsPlugin(), new OutOfSampleErrorPlotPlugin(split.get(0), split.get(1)));
		Platform.runLater(window);

		try {
			long start = System.currentTimeMillis();
			Classifier optimizedClassifier = mlplan.call();
			long trainTime = (int) (System.currentTimeMillis() - start) / 1000;
			System.out.println("Finished build of the classifier.");
			System.out.println("Chosen model is: " + ((MLPipeline) mlplan.getSelectedClassifier()).toString());
			System.out.println("Training time was " + trainTime + "s.");

			/* evaluate solution produced by mlplan */
			Evaluation eval = new Evaluation(split.get(0));
			eval.evaluateModel(optimizedClassifier, split.get(1));
			System.out.println("Error Rate of the solution produced by ML-Plan: " + ((100 - eval.pctCorrect()) / 100f) + ". Internally believed error was " + mlplan.getInternalValidationErrorOfSelectedClassifier());
		} catch (NoSuchElementException e) {
			System.out.println("Building the classifier failed: " + e.getMessage());
		}
	}

}
