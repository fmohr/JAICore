package ai.libs.mlplan.core;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.IPathEvaluator;
import org.api4.java.common.control.ILoggingCustomizable;
import org.api4.java.datastructure.graph.ILabeledPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hasco.core.HASCOUtil;
import ai.libs.jaicore.components.model.Component;
import ai.libs.jaicore.components.model.ComponentInstance;
import ai.libs.jaicore.components.serialization.CompositionSerializer;
import ai.libs.jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNode;
import ai.libs.jaicore.search.model.travesaltree.BackPointerPath;

public class PreferenceBasedNodeEvaluator implements IPathEvaluator<TFDNode, String, Double>, ILoggingCustomizable {

	private final Collection<Component> components;
	private final List<String> orderingOfComponents;
	private final String nameOfMethodToResolveBareLearner;
	private final String nameOfMethodToResolveLearnerInPipeline;
	private Logger logger = LoggerFactory.getLogger(PreferenceBasedNodeEvaluator.class);
	private boolean sentLogMessageForHavingEnteredSecondSubPhase = false;

	public PreferenceBasedNodeEvaluator(final Collection<Component> components, final List<String> orderingOfComponents, final String nameOfMethodToResolveBareLearner, final String nameOfMethodToResolveLearnerInPipeline) {
		super();
		this.components = components;
		this.orderingOfComponents = orderingOfComponents;
		this.nameOfMethodToResolveBareLearner = nameOfMethodToResolveBareLearner;
		this.nameOfMethodToResolveLearnerInPipeline = nameOfMethodToResolveLearnerInPipeline;
	}

	@Override
	public Double evaluate(final ILabeledPath<TFDNode, String> n) {
		this.logger.info("Received request for node evaluation.");
		List<String> appliedMethods = new LinkedList<>();
		for (TFDNode x : n.getNodes()) {
			if (x.getAppliedMethodInstance() != null) {
				appliedMethods.add(x.getAppliedMethodInstance().getMethod().getName());
			}
		}
		this.logger.debug("Determined {} applied methods: {}", appliedMethods.size(), appliedMethods);

		//		resolveAbstractClassifierWith
		//		resolveAbstractClassifierWithweka.classifiers.functions.SMO

		/* get partial component */
		ComponentInstance instance = HASCOUtil.getSolutionCompositionFromState(this.components, n.getHead().getState(), false);
		boolean isPipeline = appliedMethods.stream().anyMatch(x -> x.toLowerCase().contains("pipeline"));
		String classifierName = null;

		/* first check whether any decision about an instance is recognizable. If not, return 0.0 */
		if (instance == null) {
			this.logger.info("No decision recognizable *in state* yet, returning 0.0");
			return 0.0;
		}

		/* now check whether this is a pipeline */
		String nameOfLastAppliedMethod = appliedMethods.get(appliedMethods.size() - 1);
		String compactStringOfCI = CompositionSerializer.serializeComponentInstance(instance).toString();
		this.logger.debug("The associated component instance is {}. Constitutes a pipeline? {}. Name of last applied method: {}", compactStringOfCI, isPipeline ? "yes" : "no", nameOfLastAppliedMethod);
		Double score = 0.0;
		boolean lastMethodBeforeSteppingToRandomCompletions = false;
		if (instance.getComponent().getName().toLowerCase().contains("pipeline")) {
			lastMethodBeforeSteppingToRandomCompletions = nameOfLastAppliedMethod.startsWith(this.nameOfMethodToResolveLearnerInPipeline);

			if (instance.getSatisfactionOfRequiredInterfaces().containsKey("classifier")) {
				classifierName = instance.getSatisfactionOfRequiredInterfaces().get("classifier").getComponent().getName();
			} else if (instance.getSatisfactionOfRequiredInterfaces().containsKey("regressor")) {
				classifierName = instance.getSatisfactionOfRequiredInterfaces().get("regressor").getComponent().getName();
			} else {
				this.logger.debug("Exact decision about pipeline fillup not recognizable in state yet. Returning 0.0.");
				return 0.0;
			}
		} else {
			classifierName = instance.getComponent().getName();
			lastMethodBeforeSteppingToRandomCompletions = nameOfLastAppliedMethod.startsWith(this.nameOfMethodToResolveBareLearner);
		}
		this.logger.debug("Identified classifier {}.", classifierName);

		if (lastMethodBeforeSteppingToRandomCompletions) {
			if (isPipeline) {
				score += this.orderingOfComponents.size() + 1;
			}

			score += (this.orderingOfComponents.contains(classifierName) ? this.orderingOfComponents.indexOf(classifierName) + 1 : this.orderingOfComponents.size() + 1);
			score *= 1.0e-10;
		} else {
			score = null;
			if (!this.sentLogMessageForHavingEnteredSecondSubPhase) {
				double scoreOfParent;
				if ((scoreOfParent = ((BackPointerPath<TFDNode, String, Double>) n.getPathToParentOfHead()).getScore()) > 1.0e-6) {
					this.sentLogMessageForHavingEnteredSecondSubPhase = true;
					this.logger.info("Entering phase 1b! Breadth first search ends here, because the search is asking for the f-value of a node whose parent has been truely evaluated with an f-value of {}", scoreOfParent);
				}
			}
		}
		this.logger.info("Returning score {} for instance {}", score, compactStringOfCI);
		return score;

	}

	@Override
	public String toString() {
		return "PreferenceBasedNodeEvaluator [ORDERING_OF_CLASSIFIERS=" + this.orderingOfComponents + "]";
	}

	@Override
	public String getLoggerName() {
		return this.logger.getName();
	}

	@Override
	public void setLoggerName(final String name) {
		this.logger = LoggerFactory.getLogger(name);
	}
}
