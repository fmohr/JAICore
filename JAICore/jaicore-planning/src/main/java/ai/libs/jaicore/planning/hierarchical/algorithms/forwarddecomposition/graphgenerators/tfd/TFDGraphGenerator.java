package ai.libs.jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.api4.java.common.control.ILoggingCustomizable;
import org.api4.java.datastructure.graph.implicit.IGraphGenerator;
import org.api4.java.datastructure.graph.implicit.ISingleRootGenerator;
import org.api4.java.datastructure.graph.implicit.ISuccessorGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.logging.ToJSONStringUtil;
import ai.libs.jaicore.logic.fol.structure.Literal;
import ai.libs.jaicore.logic.fol.structure.Monom;
import ai.libs.jaicore.planning.classical.algorithms.strips.forward.StripsUtil;
import ai.libs.jaicore.planning.classical.problems.strips.Operation;
import ai.libs.jaicore.planning.core.Action;
import ai.libs.jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.TaskPlannerUtil;
import ai.libs.jaicore.planning.hierarchical.problems.htn.IHTNPlanningProblem;
import ai.libs.jaicore.planning.hierarchical.problems.stn.Method;
import ai.libs.jaicore.planning.hierarchical.problems.stn.MethodInstance;
import ai.libs.jaicore.search.model.NodeExpansionDescription;

public class TFDGraphGenerator implements IGraphGenerator<TFDNode, String>, ILoggingCustomizable {

	private Logger logger = LoggerFactory.getLogger(TFDGraphGenerator.class);
	protected TaskPlannerUtil util = new TaskPlannerUtil(null);
	protected final IHTNPlanningProblem problem;
	protected final Map<String, Operation> primitiveTasks = new HashMap<>();

	public TFDGraphGenerator(final IHTNPlanningProblem problem) {
		this.problem = problem;
		for (Operation op : problem.getDomain().getOperations()) {
			this.primitiveTasks.put(op.getName(), op);
		}
	}

	protected Collection<TFDNode> getSuccessorsResultingFromResolvingPrimitiveTask(final Monom state, final Literal taskToBeResolved, final List<Literal> remainingOtherTasks) throws InterruptedException {
		Collection<TFDNode> successors = new ArrayList<>();
		for (Action applicableAction : this.util.getActionsForPrimitiveTaskThatAreApplicableInState(null, this.primitiveTasks.get(taskToBeResolved.getPropertyName()), taskToBeResolved, state)) {
			Monom stateCopy = new Monom(state);
			StripsUtil.updateState(stateCopy, applicableAction);
			successors.add(this.postProcessPrimitiveTaskNode(new TFDNode(stateCopy, remainingOtherTasks, null, applicableAction)));
		}
		return successors;
	}

	protected Collection<TFDNode> getSuccessorsResultingFromResolvingComplexTask(final Monom state, final Literal taskToBeResolved, final List<Literal> remainingOtherTasks) throws InterruptedException {
		Collection<TFDNode> successors = new ArrayList<>();
		Collection<MethodInstance> applicableMethodInstances = this.util.getMethodInstancesForTaskThatAreApplicableInState(null, this.problem.getDomain().getMethods(), taskToBeResolved, state, remainingOtherTasks);
		this.logger.debug("Identified {} applicable method instances." , applicableMethodInstances.size());
		assert this.areLonelyMethodsContainedAtMostOnce(applicableMethodInstances);
		for (MethodInstance instance : applicableMethodInstances) {

			/* derive remaining network for this instance */
			List<Literal> remainingTasks = this.stripTNPrefixes(this.util.getTaskChainOfTotallyOrderedNetwork(instance.getNetwork()));
			remainingTasks.addAll(remainingOtherTasks);
			successors.add(this.postProcessComplexTaskNode(new TFDNode(state, remainingTasks, instance, null)));
		}
		return successors;
	}

	private boolean areLonelyMethodsContainedAtMostOnce(final Collection<MethodInstance> instances) {
		List<Method> usedMethods = new ArrayList<>();
		for (MethodInstance mi : instances) {
			if (!mi.getMethod().isLonely()) {
				continue;
			}
			boolean doubleUseDetected = usedMethods.contains(mi.getMethod());
			assert !doubleUseDetected : "Lonely method " + mi.getMethod() + " has been generated several times as being applicable!";
			usedMethods.add(mi.getMethod());
		}
		return true;
	}

	protected List<Literal> stripTNPrefixes(final List<Literal> taskList) {
		return taskList.stream().map(l -> {
			String taskName = l.getPropertyName().substring(l.getPropertyName().indexOf('-') + 1, l.getPropertyName().length());
			return new Literal(taskName, l.getParameters(), l.isPositive());
		}).collect(Collectors.toList());
	}

	/**
	 * A hook for extending classes that can be used to change the nodes before they are attached
	 *
	 * @param node
	 * @return
	 */
	protected TFDNode postProcessPrimitiveTaskNode(final TFDNode node) {
		return node;
	}

	/**
	 * A hook for extending classes that can be used to change the nodes before they are attached
	 *
	 * @param node
	 * @return
	 */
	protected TFDNode postProcessComplexTaskNode(final TFDNode node) {
		return node;
	}

	@Override
	public ISingleRootGenerator<TFDNode> getRootGenerator() {
		return () -> new TFDNode(this.problem.getInit(), this.stripTNPrefixes(new TaskPlannerUtil(null).getTaskChainOfTotallyOrderedNetwork(this.problem.getNetwork())));
	}

	@Override
	public ISuccessorGenerator<TFDNode, String> getSuccessorGenerator() {
		return l -> {
			this.logger.debug("Starting node generation for node {}", l);
			Monom state = l.getState();
			List<Literal> currentlyRemainingTasks = new ArrayList<>(l.getRemainingTasks());
			if (currentlyRemainingTasks.isEmpty()) {
				return new ArrayList<>();
			}
			Literal nextTask = currentlyRemainingTasks.get(0);
			currentlyRemainingTasks.remove(0);

			/* get the child nodes */
			long creationStartTime = System.currentTimeMillis();
			Collection<TFDNode> successors = this.primitiveTasks.containsKey(nextTask.getPropertyName()) ? this.getSuccessorsResultingFromResolvingPrimitiveTask(state, nextTask, currentlyRemainingTasks)
					: this.getSuccessorsResultingFromResolvingComplexTask(state, nextTask, currentlyRemainingTasks);

			if (successors.isEmpty()) {
				this.logger.warn("Could not produce any successors for next task {}", nextTask);
				return Arrays.asList();
			}

			this.logger.info("Node generation finished and took {}ms. Generated {} successors.", System.currentTimeMillis() - creationStartTime, successors.size());

			/* change order in remaining tasks based on numbered prefixes */
			successors = successors.stream().map(this::orderRemainingTasksByPriority).collect(Collectors.toList());

			/* derive successor descriptions from the nodes */
			return successors.stream().map(n -> new NodeExpansionDescription<>(n, n.getAppliedAction() != null ? n.getAppliedAction().getEncoding() : n.getAppliedMethodInstance().getEncoding())).collect(Collectors.toList());
		};
	}

	public TFDNode orderRemainingTasksByPriority(final TFDNode node) {

		/* determine order of tasks based on the prefixes */
		Pattern p = Pattern.compile("(\\d+)_");
		List<Literal> unorderedLiterals = new ArrayList<>();
		Map<Integer, List<Literal>> orderedLiterals = new HashMap<>();
		node.getRemainingTasks().forEach(t -> {
			Matcher m = p.matcher(t.getPropertyName());
			if (m.find()) {
				int order = Integer.parseInt(m.group(1));
				if (!orderedLiterals.containsKey(order)) {
					orderedLiterals.put(order, new ArrayList<>());
				}
				List<Literal> tasksWithorder = orderedLiterals.get(order);
				tasksWithorder.add(t);
			} else {
				unorderedLiterals.add(t);
			}
		});

		/* reorganize task network */
		List<Literal> newLiteralList = new ArrayList<>();
		orderedLiterals.keySet().stream().sorted().forEach(order -> newLiteralList.addAll(orderedLiterals.get(order)));
		newLiteralList.addAll(unorderedLiterals);
		return new TFDNode(node.getState(), newLiteralList, node.getAppliedMethodInstance(), node.getAppliedAction());
	}

	public boolean isPathSemanticallySubsumed(final List<TFDNode> path, final List<TFDNode> potentialSuperPath) throws InterruptedException {
		int n = path.size();
		for (int i = 0; i < n; i++) {
			if (!path.get(i).equals(potentialSuperPath.get(i))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		Map<String, Object> fields = new HashMap<>();
		fields.put("util", this.util);
		fields.put("problem", this.problem);
		fields.put("primitiveTasks", this.primitiveTasks);
		return ToJSONStringUtil.toJSONString(this.getClass().getSimpleName(), fields);
	}

	@Override
	public String getLoggerName() {
		return this.logger.getName();
	}

	@Override
	public void setLoggerName(final String name) {
		this.logger = LoggerFactory.getLogger(name);
		this.util.setLoggerName(name + ".util");
	}
}
