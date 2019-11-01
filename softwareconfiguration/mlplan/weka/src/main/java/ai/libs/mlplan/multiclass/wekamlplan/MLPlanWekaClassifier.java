package ai.libs.mlplan.multiclass.wekamlplan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

import org.api4.java.algorithm.TimeOut;
import org.api4.java.common.control.ILoggingCustomizable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hasco.gui.statsplugin.HASCOModelStatisticsPlugin;
import ai.libs.hasco.model.Component;
import ai.libs.jaicore.basic.events.IEventEmitter;
import ai.libs.jaicore.graphvisualizer.events.recorder.property.AlgorithmEventPropertyComputer;
import ai.libs.jaicore.graphvisualizer.plugin.graphview.GraphViewPlugin;
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeDisplayInfoAlgorithmEventPropertyComputer;
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeInfoAlgorithmEventPropertyComputer;
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeInfoGUIPlugin;
import ai.libs.jaicore.graphvisualizer.plugin.solutionperformanceplotter.ScoredSolutionCandidateInfoAlgorithmEventPropertyComputer;
import ai.libs.jaicore.graphvisualizer.plugin.solutionperformanceplotter.SolutionPerformanceTimelinePlugin;
import ai.libs.jaicore.graphvisualizer.window.AlgorithmVisualizationWindow;
import ai.libs.jaicore.ml.weka.classification.IInstancesClassifier;
import ai.libs.jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNodeInfoGenerator;
import ai.libs.jaicore.search.gui.plugins.rollouthistograms.RolloutInfoAlgorithmEventPropertyComputer;
import ai.libs.jaicore.search.gui.plugins.rollouthistograms.SearchRolloutHistogramPlugin;
import ai.libs.jaicore.search.model.travesaltree.JaicoreNodeInfoGenerator;
import ai.libs.mlplan.core.AbstractMLPlanBuilder;
import ai.libs.mlplan.core.MLPlan;
import ai.libs.mlplan.gui.outofsampleplots.WekaClassifierSolutionCandidateRepresenter;
import ai.libs.mlplan.multiclass.MLPlanClassifierConfig;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import weka.classifiers.Classifier;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.CapabilitiesHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;

/**
 * A WEKA classifier wrapping the functionality of ML-Plan where the constructed object is a WEKA classifier.
 *
 * It implements the algorithm interface with itself (with modified state) as an output
 *
 * @author wever, fmohr
 *
 */
@SuppressWarnings("serial")
public class MLPlanWekaClassifier implements Classifier, CapabilitiesHandler, OptionHandler, ILoggingCustomizable, IInstancesClassifier, IEventEmitter {

	/* Logger for controlled output. */
	private transient Logger logger = LoggerFactory.getLogger(MLPlanWekaClassifier.class);
	private String loggerName;

	private boolean visualizationEnabled = false;

	/* MLPlan Builder and the instance of mlplan */
	private final transient AbstractMLPlanBuilder builder;

	/* The timeout for the selecting a classifier. */
	private TimeOut timeout;

	/* The output of mlplan, i.e., the selected classifier and the internal validation error measured on the given data. */
	private Classifier classifierFoundByMLPlan;
	private double internalValidationErrorOfSelectedClassifier;

	private final transient List<Object> listeners = new ArrayList<>();

	public MLPlanWekaClassifier(final AbstractMLPlanBuilder builder) {
		this.builder = builder;
		this.timeout = builder.getTimeOut();
	}

	@Override
	public void buildClassifier(final Instances data) throws Exception {
		Objects.requireNonNull(this.timeout, "Timeout must be set before running ML-Plan.");

		MLPlan mlplan = new MLPlan(this.builder, data);
		this.listeners.forEach(mlplan::registerListener);
		mlplan.setTimeout(this.timeout);
		if (this.loggerName != null) {
			mlplan.setLoggerName(this.loggerName + "." + "mlplan");
		}

		if (this.visualizationEnabled) {
			new JFXPanel();

			NodeInfoAlgorithmEventPropertyComputer nodeInfoAlgorithmEventPropertyComputer = new NodeInfoAlgorithmEventPropertyComputer();
			List<AlgorithmEventPropertyComputer> algorithmEventPropertyComputers = Arrays.asList(nodeInfoAlgorithmEventPropertyComputer,
					new NodeDisplayInfoAlgorithmEventPropertyComputer<>(new JaicoreNodeInfoGenerator<>(new TFDNodeInfoGenerator())), new RolloutInfoAlgorithmEventPropertyComputer(nodeInfoAlgorithmEventPropertyComputer),
					new ScoredSolutionCandidateInfoAlgorithmEventPropertyComputer(new WekaClassifierSolutionCandidateRepresenter()));

			AlgorithmVisualizationWindow window = new AlgorithmVisualizationWindow(mlplan, algorithmEventPropertyComputers, new GraphViewPlugin(), new NodeInfoGUIPlugin(), new SearchRolloutHistogramPlugin(),
					new SolutionPerformanceTimelinePlugin(), new HASCOModelStatisticsPlugin());
			Platform.runLater(window);
		}

		this.classifierFoundByMLPlan = mlplan.call();
	}

	@Override
	public double[] classifyInstances(final Instances instances) throws Exception {
		/* If the selected classifier can handle batch classification, use this feature. */
		if (this.getSelectedClassifier() instanceof IInstancesClassifier) {
			return ((IInstancesClassifier) this.getSelectedClassifier()).classifyInstances(instances);
		}

		double[] predictions = new double[instances.size()];
		for (int i = 0; i < instances.size(); i++) {
			predictions[i] = this.getSelectedClassifier().classifyInstance(instances.get(i));
		}
		return predictions;
	}

	@Override
	public double classifyInstance(final Instance instance) throws Exception {
		if (this.classifierFoundByMLPlan == null) {
			throw new IllegalStateException("Classifier has not been built yet.");
		}
		return this.classifierFoundByMLPlan.classifyInstance(instance);
	}

	@Override
	public double[] distributionForInstance(final Instance instance) throws Exception {
		if (this.classifierFoundByMLPlan == null) {
			throw new IllegalStateException("Classifier has not been built yet.");
		}
		return this.classifierFoundByMLPlan.distributionForInstance(instance);
	}

	@Override
	public Capabilities getCapabilities() {
		Capabilities result = new Capabilities(this);
		result.disableAll();

		// attributes
		result.enable(Capability.NOMINAL_ATTRIBUTES);
		result.enable(Capability.NUMERIC_ATTRIBUTES);
		result.enable(Capability.DATE_ATTRIBUTES);
		result.enable(Capability.STRING_ATTRIBUTES);
		result.enable(Capability.RELATIONAL_ATTRIBUTES);
		result.enable(Capability.MISSING_VALUES);

		// class
		result.enable(Capability.NOMINAL_CLASS);
		result.enable(Capability.NUMERIC_CLASS);
		result.enable(Capability.DATE_CLASS);
		result.enable(Capability.MISSING_CLASS_VALUES);

		// instances
		result.setMinimumNumberInstances(1);
		return result;
	}

	@Override
	public Enumeration<Option> listOptions() {
		/* As there are no options, simply return null. */
		return null;
	}

	@Override
	public void setOptions(final String[] options) throws Exception {
		/* Intentionally left blank. */
	}

	@Override
	public String[] getOptions() {
		/* As there are no options, simply return an empty array. */
		return new String[] {};
	}

	public void setTimeout(final TimeOut timeout) {
		this.timeout = timeout;
	}

	public MLPlanClassifierConfig getMLPlanConfig() {
		return this.builder.getAlgorithmConfig();
	}

	public Collection<Component> getComponents() throws IOException {
		return this.builder.getComponents();
	}

	/**
	 * Enables the GUI of the MLPlanWekaClassifier if set to true. By default the visualization is deactivated. The flag needs to be set before buildClassifier is called.
	 *
	 * @param visualizationEnabled
	 *            Flag whether the visualization is enabled or not.
	 */
	public void setVisualizationEnabled(final boolean visualizationEnabled) {
		this.visualizationEnabled = visualizationEnabled;
	}

	/**
	 * @return An object of the classifier ML-Plan has selected during the build.
	 */
	public Classifier getSelectedClassifier() {
		return this.classifierFoundByMLPlan;
	}

	/**
	 * @return The internal validation error (during selection phase) of the selected classifier.
	 */
	public double getInternalValidationErrorOfSelectedClassifier() {
		return this.internalValidationErrorOfSelectedClassifier;
	}

	@Override
	public void setLoggerName(final String name) {
		this.loggerName = name;
		this.logger.info("Switching logger name to {}", name);
		this.logger = LoggerFactory.getLogger(name);
		this.logger.info("Switched ML-Plan logger to {}", name);
	}

	@Override
	public String getLoggerName() {
		return this.loggerName;
	}

	@Override
	public void registerListener(final Object listener) {
		this.listeners.add(listener);
	}

}