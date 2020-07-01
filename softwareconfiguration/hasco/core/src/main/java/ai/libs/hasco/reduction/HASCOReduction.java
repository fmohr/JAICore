package ai.libs.hasco.reduction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import org.api4.java.datastructure.graph.implicit.IGraphGenerator;

import ai.libs.hasco.core.HASCOSolutionCandidate;
import ai.libs.hasco.core.IHASCOPlanningReduction;
import ai.libs.hasco.core.IsNotRefinable;
import ai.libs.hasco.core.IsRefinementCompletedPredicate;
import ai.libs.hasco.core.IsValidParameterRangeRefinementPredicate;
import ai.libs.hasco.core.RefinementConfiguredSoftwareConfigurationProblem;
import ai.libs.hasco.core.Util;
import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.NumericParameterDomain;
import ai.libs.hasco.model.Parameter;
import ai.libs.hasco.model.ParameterRefinementConfiguration;
import ai.libs.jaicore.basic.algorithm.reduction.AlgorithmicProblemReduction;
import ai.libs.jaicore.logic.fol.structure.CNFFormula;
import ai.libs.jaicore.logic.fol.structure.ConstantParam;
import ai.libs.jaicore.logic.fol.structure.Literal;
import ai.libs.jaicore.logic.fol.structure.LiteralParam;
import ai.libs.jaicore.logic.fol.structure.Monom;
import ai.libs.jaicore.logic.fol.structure.VariableParam;
import ai.libs.jaicore.logic.fol.theories.EvaluablePredicate;
import ai.libs.jaicore.planning.classical.problems.ceoc.CEOCOperation;
import ai.libs.jaicore.planning.core.interfaces.IEvaluatedPlan;
import ai.libs.jaicore.planning.core.interfaces.IPlan;
import ai.libs.jaicore.planning.hierarchical.problems.ceocipstn.CEOCIPSTNPlanningDomain;
import ai.libs.jaicore.planning.hierarchical.problems.ceocipstn.CEOCIPSTNPlanningProblem;
import ai.libs.jaicore.planning.hierarchical.problems.ceocipstn.OCIPMethod;
import ai.libs.jaicore.planning.hierarchical.problems.htn.CostSensitiveHTNPlanningProblem;
import ai.libs.jaicore.planning.hierarchical.problems.stn.TaskNetwork;

/**
 * This is the class that conducts the actual problem reduction of software configuration to HTN Planning
 *
 * @author fmohr
 *
 */
public class HASCOReduction<V extends Comparable<V>>
implements AlgorithmicProblemReduction<RefinementConfiguredSoftwareConfigurationProblem<V>, ComponentInstance, CostSensitiveHTNPlanningProblem<CEOCIPSTNPlanningProblem, V>, IEvaluatedPlan<V>> {

	private static final boolean CONFIGURE_PARAMS = true; // this could be determined automatically later

	// component selection
	private static final String RESOLVE_COMPONENT_IFACE_PREFIX = "1_tResolve";
	private static final String SATISFY_PREFIX = "1_satisfy";

	// component configuration
	private static final String REFINE_PARAMETERS_PREFIX = "2_tRefineParamsOf";
	private static final String REFINE_PARAMETER_PREFIX = "2_tRefineParam";
	private static final String DECLARE_CLOSED_PREFIX = "2_declareClosed";
	private static final String REDEF_VALUE_PREFIX = "2_redefValue";

	private static final String COMPONENT_OF_C1 = "component(c1)";

	private RefinementConfiguredSoftwareConfigurationProblem<V> originalProblem;

	/* working variables */
	private Collection<Component> components;
	private Map<Component, Map<Parameter, ParameterRefinementConfiguration>> paramRefinementConfig;

	private Supplier<HASCOSolutionCandidate<V>> bestSolutionSupplier;

	public HASCOReduction(final Supplier<HASCOSolutionCandidate<V>> bestSolutionSupplier) {
		this.bestSolutionSupplier = bestSolutionSupplier;
	}

	public Monom getInitState() {
		if (this.originalProblem == null) {
			throw new IllegalStateException("Cannot compute init state before transformation has been invoked.");
		}
		Monom init = new Monom();
		this.getExistingInterfaces().forEach(s -> init.add(new Literal("iface('" + s + "')")));
		init.add(new Literal("component('request')"));
		return init;
	}

	public Collection<String> getExistingInterfaces() {
		if (this.originalProblem == null) {
			throw new IllegalStateException("Cannot compute existing interfaces before transformation has been invoked.");
		}
		Collection<String> ifaces = new HashSet<>();
		for (Component c : this.components) {
			ifaces.addAll(c.getProvidedInterfaces());
			ifaces.addAll(c.getRequiredInterfaces().values());
		}
		return ifaces;
	}

	public CEOCIPSTNPlanningDomain getPlanningDomain() {

		/* create operations */
		Collection<CEOCOperation> operations = new ArrayList<>();
		for (Component c : this.components) {
			for (String i : c.getProvidedInterfaces()) {
				List<VariableParam> params = new ArrayList<>();
				params.add(new VariableParam("c1"));
				params.add(new VariableParam("c2"));
				int j = 0;
				Map<CNFFormula, Monom> addList = new HashMap<>();
				Monom standardKnowledgeAboutNewComponent = new Monom("component(c2) & resolves(c1, '" + i + "', '" + c.getName() + "'," + " c2" + ")");
				for (Parameter p : c.getParameters()) {
					String paramIdentifier = "p" + (++j);
					params.add(new VariableParam(paramIdentifier));

					/* add the information about this parameter container */
					List<LiteralParam> literalParams = new ArrayList<>();
					literalParams.clear();
					literalParams.add(new ConstantParam(c.getName()));
					literalParams.add(new ConstantParam(p.getName()));
					literalParams.add(new VariableParam("c2"));
					literalParams.add(new VariableParam(paramIdentifier));
					standardKnowledgeAboutNewComponent.add(new Literal("parameterContainer", literalParams));

					/* add knowledge about initial value */
					List<LiteralParam> valParams = new ArrayList<>();
					valParams.add(new VariableParam(paramIdentifier));
					if (p.isNumeric()) {
						standardKnowledgeAboutNewComponent.add(new Literal("parameterFocus(c2, '" + p.getName() + "', '" + this.paramRefinementConfig.get(c).get(p).getFocusPoint() + "')"));
						NumericParameterDomain np = (NumericParameterDomain) p.getDefaultDomain();
						valParams.add(new ConstantParam("[" + np.getMin() + "," + np.getMax() + "]"));
					} else {
						valParams.add(new ConstantParam(p.getDefaultValue().toString()));
					}
					standardKnowledgeAboutNewComponent.add(new Literal("val", valParams));
				}
				int k = 0;
				for (String requiredInterfaceID : c.getRequiredInterfaces().keySet()) {
					String reqIntIdentifier = "sc" + (++k);
					params.add(new VariableParam(reqIntIdentifier));

					List<LiteralParam> literalParams = new ArrayList<>();
					literalParams.clear();
					literalParams.add(new ConstantParam(c.getName()));
					literalParams.add(new ConstantParam(requiredInterfaceID));
					literalParams.add(new VariableParam("c2"));
					literalParams.add(new VariableParam(reqIntIdentifier));
					standardKnowledgeAboutNewComponent.add(new Literal("interfaceIdentifier", literalParams));
				}

				addList.put(new CNFFormula(), standardKnowledgeAboutNewComponent);
				CEOCOperation newOp = new CEOCOperation(SATISFY_PREFIX + i + "With" + c.getName(), params, new Monom(COMPONENT_OF_C1), addList, new HashMap<>(), new ArrayList<>());
				operations.add(newOp);
			}
		}

		/* create operations for parameter initialization */
		Map<CNFFormula, Monom> addList = new HashMap<>();
		addList.put(new CNFFormula(), new Monom("val(container,newValue) & overwritten(container)"));
		Map<CNFFormula, Monom> deleteList = new HashMap<>();
		deleteList.put(new CNFFormula(), new Monom("val(container,previousValue)"));
		operations.add(new CEOCOperation(REDEF_VALUE_PREFIX, "container,previousValue,newValue", new Monom("val(container,previousValue)"), addList, deleteList, ""));
		addList = new HashMap<>();
		addList.put(new CNFFormula(), new Monom("closed(container)"));
		deleteList = new HashMap<>();
		operations.add(new CEOCOperation(DECLARE_CLOSED_PREFIX, "container", new Monom(), addList, deleteList, ""));

		/* create methods */
		Collection<OCIPMethod> methods = new ArrayList<>();
		for (Component c : this.components) {

			/*
			 * create methods for the refinement of the interfaces offered by this component
			 */
			for (String i : c.getProvidedInterfaces()) {
				List<VariableParam> params = new ArrayList<>();
				VariableParam inputParam = new VariableParam("c1");
				params.add(inputParam);
				params.add(new VariableParam("c2"));
				Map<String, String> requiredInterfaces = c.getRequiredInterfaces();
				List<Literal> network = new ArrayList<>();

				StringBuilder refinementArgumentsSB = new StringBuilder();
				int j = 0;
				if (CONFIGURE_PARAMS) {
					for (j = 1; j <= c.getParameters().size(); j++) {
						String paramIdentifier = "p" + j;
						refinementArgumentsSB.append(", " + paramIdentifier);
					}
				}

				for (int k = 1; k <= requiredInterfaces.entrySet().size(); k++) {
					refinementArgumentsSB.append(",sc" + k);
				}

				int sc = 0;
				network.add(new Literal(SATISFY_PREFIX + i + "With" + c.getName() + "(" + "c1" + "," + "c2" + refinementArgumentsSB.toString() + ")"));
				for (Entry<String, String> requiredInterface : requiredInterfaces.entrySet()) {
					String paramName = "sc" + (++sc);
					params.add(new VariableParam(paramName));
					network.add(new Literal(RESOLVE_COMPONENT_IFACE_PREFIX + requiredInterface.getValue() + "(c2," + paramName + ")"));
				}

				refinementArgumentsSB = new StringBuilder();
				if (CONFIGURE_PARAMS) {
					for (j = 1; j <= c.getParameters().size(); j++) {
						String paramIdentifier = "p" + j;
						params.add(new VariableParam(paramIdentifier));
						refinementArgumentsSB.append(", " + paramIdentifier);
					}
				}
				network.add(new Literal(REFINE_PARAMETERS_PREFIX + c.getName() + "(" + "c1" + "," + "c2" + refinementArgumentsSB.toString() + ")"));
				List<VariableParam> outputs = new ArrayList<>(params);
				outputs.remove(inputParam);
				methods.add(new OCIPMethod("resolve" + i + "With" + c.getName(), params, new Literal(RESOLVE_COMPONENT_IFACE_PREFIX + i + "(c1,c2)"), new Monom(COMPONENT_OF_C1), new TaskNetwork(network), false, outputs, new Monom()));
			}

			/* create methods for choosing/refining parameters */
			List<VariableParam> params = new ArrayList<>();
			params.add(new VariableParam("c1"));
			List<Literal> initNetwork = new ArrayList<>();
			StringBuilder refinementArgumentsSB = new StringBuilder();
			int j = 0;

			/*
			 * go, in an ordering that is consistent with the pre-order on the params
			 * imposed by the dependencies, over the set of params
			 */
			if (CONFIGURE_PARAMS) {
				for (Parameter p : c.getParameters()) {
					String paramName = "p" + (++j);
					refinementArgumentsSB.append(", " + paramName);
					params.add(new VariableParam(paramName));
					initNetwork.add(new Literal(REFINE_PARAMETER_PREFIX + p.getName() + "Of" + c.getName() + "(c2, " + paramName + ")"));
					methods.add(new OCIPMethod("ignoreParamRefinementFor" + p.getName() + "Of" + c.getName(), "object, container, curval", new Literal(REFINE_PARAMETER_PREFIX + p.getName() + "Of" + c.getName() + "(object,container)"),
							new Monom("parameterContainer('" + c.getName() + "', '" + p.getName() + "', object, container) & val(container,curval) & overwritten(container)"), new TaskNetwork(DECLARE_CLOSED_PREFIX + "(container)"), false,
							"", new Monom("notRefinable('" + c.getName() + "', object, '" + p.getName() + "', container, curval)")));

					methods.add(new OCIPMethod("refineParam" + p.getName() + "Of" + c.getName(), "object, container, curval, newval", new Literal(REFINE_PARAMETER_PREFIX + p.getName() + "Of" + c.getName() + "(object,container)"),
							new Monom("parameterContainer('" + c.getName() + "', '" + p.getName() + "', object, container) & val(container,curval)"), new TaskNetwork(REDEF_VALUE_PREFIX + "(container,curval,newval)"), false, "",
							new Monom("isValidParameterRangeRefinement('" + c.getName() + "', object, '" + p.getName() + "', container, curval, newval)")));
				}
				initNetwork.add(new Literal(REFINE_PARAMETERS_PREFIX + c.getName() + "(" + "c1" + "," + "c2" + refinementArgumentsSB.toString() + ")"));
				params = new ArrayList<>(params);
				params.add(1, new VariableParam("c2"));
				methods.add(new OCIPMethod("refineParamsOf" + c.getName(), params, new Literal(REFINE_PARAMETERS_PREFIX + c.getName() + "(c1,c2" + refinementArgumentsSB.toString() + ")"), new Monom(COMPONENT_OF_C1),
						new TaskNetwork(initNetwork), false, new ArrayList<>(), new Monom("!refinementCompleted('" + c.getName() + "', c2)")));
				methods.add(new OCIPMethod("closeRefinementOfParamsOf" + c.getName(), params, new Literal(REFINE_PARAMETERS_PREFIX + c.getName() + "(c1,c2" + refinementArgumentsSB.toString() + ")"), new Monom(COMPONENT_OF_C1),
						new TaskNetwork(), false, new ArrayList<>(), new Monom("refinementCompleted('" + c.getName() + "', c2)")));
			}
		}
		return new CEOCIPSTNPlanningDomain(operations, methods);
	}

	public CEOCIPSTNPlanningProblem getPlanningProblem(final CEOCIPSTNPlanningDomain domain, final CNFFormula knowledge, final Monom init) {
		Map<String, EvaluablePredicate> evaluablePredicates = new HashMap<>();
		evaluablePredicates.put("isValidParameterRangeRefinement", new IsValidParameterRangeRefinementPredicate(this.components, this.paramRefinementConfig));
		evaluablePredicates.put("notRefinable", new IsNotRefinable(this.components, this.paramRefinementConfig));
		evaluablePredicates.put("refinementCompleted", new IsRefinementCompletedPredicate(this.components, this.paramRefinementConfig));
		return new CEOCIPSTNPlanningProblem(domain, knowledge, init, new TaskNetwork(RESOLVE_COMPONENT_IFACE_PREFIX + this.originalProblem.getRequiredInterface() + "('request', 'solution')"), evaluablePredicates, new HashMap<>());
	}

	public CEOCIPSTNPlanningProblem getPlanningProblem() {
		return this.getPlanningProblem(this.getPlanningDomain(), new CNFFormula(), this.getInitState());
	}

	/**
	 * This method is a utility for everybody who wants to work on the graph obtained from HASCO's reduction but without using the search logic of HASCO
	 *
	 * @param plannerFactory
	 * @return
	 */
	public <T, A> IGraphGenerator<T, A> getGraphGeneratorUsedByHASCOForSpecificPlanner(final IHASCOPlanningReduction<T, A> transformer) {
		return transformer.encodeProblem(this.getPlanningProblem()).getGraphGenerator();
	}

	@Override
	public CostSensitiveHTNPlanningProblem<CEOCIPSTNPlanningProblem, V> encodeProblem(final RefinementConfiguredSoftwareConfigurationProblem<V> problem) {

		if (problem.getRequiredInterface() == null) {
			throw new IllegalArgumentException("No required interface defined in the problem!");
		}

		/* set object variables that will be important for several methods in the reduction */
		this.originalProblem = problem;
		this.components = this.originalProblem.getComponents();
		this.paramRefinementConfig = this.originalProblem.getParamRefinementConfig();

		/* build the cost insensitive planning problem */
		CEOCIPSTNPlanningProblem planningProblem = this.getPlanningProblem();

		/* derive a plan evaluator from the configuration evaluator */
		return new CostSensitiveHTNPlanningProblem<>(planningProblem, new HASCOReductionSolutionEvaluator<>(problem, this));
	}

	@Override
	public ComponentInstance decodeSolution(final IEvaluatedPlan<V> solution) {
		return this.decodeSolution((IPlan) solution);
	}

	public ComponentInstance decodeSolution(final IPlan plan) {
		return Util.getSolutionCompositionForPlan(HASCOReduction.this.components, HASCOReduction.this.getInitState(), plan, true);
	}

	public Supplier<HASCOSolutionCandidate<V>> getBestSolutionSupplier() {
		return this.bestSolutionSupplier;
	}
}
