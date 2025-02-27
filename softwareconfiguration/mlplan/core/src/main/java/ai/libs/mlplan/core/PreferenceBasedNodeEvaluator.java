package ai.libs.mlplan.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.IPathEvaluator;
import org.api4.java.common.control.ILoggingCustomizable;
import org.api4.java.datastructure.graph.ILabeledPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.hasco.core.HASCOUtil;
import ai.libs.jaicore.components.api.IComponent;
import ai.libs.jaicore.components.model.ComponentInstance;
import ai.libs.jaicore.components.serialization.ComponentSerialization;
import ai.libs.jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNode;
import ai.libs.jaicore.search.model.travesaltree.BackPointerPath;

public class PreferenceBasedNodeEvaluator implements IPathEvaluator<TFDNode, String, Double>, ILoggingCustomizable {

	public static final String COMPNAME_LEARNER= "learner";
	private static final double EXPAND_NODE_SCORE = (-1) * Double.MAX_VALUE;

	private final Collection<IComponent> components;
	private final List<String> orderingOfComponents;
	private final String nameOfMethodToResolveBaseLearner;
	private final String nameOfMethodToResolveLearnerInPipeline;
	private Logger logger = LoggerFactory.getLogger(PreferenceBasedNodeEvaluator.class);
	private boolean sentLogMessageForHavingEnteredSecondSubPhase = false;

	public PreferenceBasedNodeEvaluator(final Collection<? extends IComponent> components, final List<String> orderingOfComponents, final String nameOfMethodToResolveBareLearner, final String nameOfMethodToResolveLearnerInPipeline) {
		super();
		this.components = new ArrayList<>(components);
		this.orderingOfComponents = orderingOfComponents;
		this.nameOfMethodToResolveBaseLearner = nameOfMethodToResolveBareLearner;
		this.nameOfMethodToResolveLearnerInPipeline = nameOfMethodToResolveLearnerInPipeline;
	}

	@Override
	public Double evaluate(final ILabeledPath<TFDNode, String> n) {
		try {
			this.logger.info("Received request for node evaluation.");
			List<String> appliedMethods = new LinkedList<>();
			for (TFDNode x : n.getNodes()) {
				if (x.getAppliedMethodInstance() != null) {
					appliedMethods.add(x.getAppliedMethodInstance().getMethod().getName());
				}
			}
			this.logger.debug("Determined {} applied methods: {}", appliedMethods.size(), appliedMethods);

			/* get partial component */
			ComponentInstance instance = HASCOUtil.getSolutionCompositionFromState(this.components, n.getHead().getState(), false);
			boolean isPipeline = appliedMethods.stream().anyMatch(x -> x.toLowerCase().contains("pipeline"));
			String classifierName = null;

			/* first check whether any decision about an instance is recognizable. If not, return 0.0 */
			if (instance == null) {
				this.logger.info("No decision recognizable *in state* yet, returning quasi-null score {}", EXPAND_NODE_SCORE);
				return EXPAND_NODE_SCORE;
			}

			/* now check whether the classifier has already been chosen. If the classifier has NOT been chosen, continue with BFS */
			String nameOfLastAppliedMethod = appliedMethods.get(appliedMethods.size() - 1);
			String compactStringOfCI = new ComponentSerialization().serialize(instance).toString();
			this.logger.debug("The associated component instance is {}. Constitutes a pipeline? {}. Name of last applied method: {}", compactStringOfCI, isPipeline ? "yes" : "no", nameOfLastAppliedMethod);
			Double score = EXPAND_NODE_SCORE;
			if (isPipeline) {
				if (instance.getSatisfactionOfRequiredInterfaces().containsKey(COMPNAME_LEARNER) && !instance.getSatisfactionOfRequiredInterface(COMPNAME_LEARNER).isEmpty()) {
					classifierName = instance.getSatisfactionOfRequiredInterface(COMPNAME_LEARNER).iterator().next().getComponent().getName();
				}
				else {
					this.logger.debug("Exact decision about learner used in the pipeline is recognizable in state yet. Returning {}.", EXPAND_NODE_SCORE);
					return EXPAND_NODE_SCORE;
				}
			} else {
				classifierName = instance.getComponent().getName();
			}
			this.logger.debug("Identified classifier {}.", classifierName);

			/* check whether this is the last step before stepping to random completions  */
			boolean lastMethodBeforeSteppingToRandomCompletions = nameOfLastAppliedMethod.startsWith(this.nameOfMethodToResolveLearnerInPipeline) || nameOfLastAppliedMethod.startsWith(this.nameOfMethodToResolveBaseLearner);;
			if (lastMethodBeforeSteppingToRandomCompletions) {
				if (isPipeline) {
					score /= Math.pow(10.0, this.orderingOfComponents.size() + 1.0);
				}
				double exp;
				if (this.orderingOfComponents.contains(classifierName)) {
					exp = this.orderingOfComponents.indexOf(classifierName);
					this.logger.debug("This is a preferred component with preference index {}.", exp);
				}
				else {
					exp = this.orderingOfComponents.size();
					this.logger.debug("This is not a preferred component. Preference index will be {}.", exp);
				}
				score /= Math.pow(10.0, exp + 1.0);
			} else {
				score = null;
				if (!this.sentLogMessageForHavingEnteredSecondSubPhase) {
					double scoreOfParent;
					if ((scoreOfParent = ((BackPointerPath<TFDNode, String, Double>) n.getPathToParentOfHead()).getScore()) > (EXPAND_NODE_SCORE / 1E100)) {
						this.sentLogMessageForHavingEnteredSecondSubPhase = true;
						this.logger.info("Entering phase 1b! Breadth first search ends here, because the search is asking for the f-value of a node whose parent has been truely evaluated with an f-value of {}", scoreOfParent);
					}
				}
			}

			this.logger.info("Returning score {} for instance {}", score, compactStringOfCI);
			return score;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
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
