package ai.libs.mlplan.examples.gui;


import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.api4.java.ai.ml.classification.IClassifier;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.algorithm.Timeout;

import ai.libs.jaicore.graphvisualizer.plugin.graphview.GraphViewPlugin;
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeInfoGUIPlugin;
import ai.libs.jaicore.graphvisualizer.window.AlgorithmVisualizationWindow;
import ai.libs.jaicore.ml.classification.loss.dataset.EClassificationPerformanceMeasure;
import ai.libs.jaicore.ml.core.dataset.serialization.OpenMLDatasetReader;
import ai.libs.jaicore.ml.core.evaluation.MLEvaluationUtil;
import ai.libs.jaicore.ml.core.filter.SplitterUtil;
import ai.libs.jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNodeInfoGenerator;
import ai.libs.jaicore.search.gui.plugins.rollouthistograms.SearchRolloutHistogramPlugin;
import ai.libs.jaicore.search.model.travesaltree.JaicoreNodeInfoGenerator;
import ai.libs.mlplan.core.MLPlan;
import ai.libs.mlplan.core.MLPlanSimpleBuilder;
import ai.libs.mlplan.gui.outofsampleplots.OutOfSampleErrorPlotPlugin;
import javafx.embed.swing.JFXPanel;

/**
 * This is an example class that illustrates the usage of ML-Plan on the segment dataset of OpenML. It is configured to run for 30 seconds and to use 70% of the data for search and 30% for selection in its second phase.
 *
 * The API key used for OpenML is ML-Plan's key (read only).
 *
 * @author fmohr
 *
 */
public class MLPlanVisualizationExample {

	public static void main(final String[] args) throws Exception {

		ILabeledDataset<?> ds = new OpenMLDatasetReader().deserializeDataset(3);
		List<ILabeledDataset<?>> split = SplitterUtil.getLabelStratifiedTrainTestSplit(ds, new Random(0), .7);

		/* initialize mlplan, and let it run for 30 seconds */
		MLPlanSimpleBuilder builder = new MLPlanSimpleBuilder();
		builder.withNodeEvaluationTimeOut(new Timeout(10, TimeUnit.SECONDS));
		builder.withCandidateEvaluationTimeOut(new Timeout(5, TimeUnit.SECONDS));
		builder.withTimeOut(new Timeout(30, TimeUnit.SECONDS));
		builder.withNumCpus(1);

		MLPlan<IClassifier> mlplan = builder.withDataset(split.get(0)).build();
		mlplan.setRandomSeed(1);
		mlplan.setPortionOfDataForPhase2(.3f);
		mlplan.setLoggerName("testedalgorithm");

		/* start visualization */
		new JFXPanel();
		AlgorithmVisualizationWindow window = new AlgorithmVisualizationWindow(mlplan);
		window.withMainPlugin(new GraphViewPlugin());
		window.withPlugin(new NodeInfoGUIPlugin(new JaicoreNodeInfoGenerator<>(new TFDNodeInfoGenerator())), new SearchRolloutHistogramPlugin(), new OutOfSampleErrorPlotPlugin(split.get(0), split.get(1)));

		try {
			long start = System.currentTimeMillis();
			IClassifier optimizedClassifier = mlplan.call();
			long trainTime = (int) (System.currentTimeMillis() - start) / 1000;
			System.out.println("Finished build of the classifier. Training time was " + trainTime + "s.");

			/* evaluate solution produced by mlplan */
			System.out.println("Error Rate of the solution produced by ML-Plan: " + MLEvaluationUtil.getLossForTrainedClassifier(optimizedClassifier, split.get(1), EClassificationPerformanceMeasure.ERRORRATE));
		} catch (NoSuchElementException e) {
			System.out.println("Building the classifier failed: " + e.getMessage());
		}
	}
}
