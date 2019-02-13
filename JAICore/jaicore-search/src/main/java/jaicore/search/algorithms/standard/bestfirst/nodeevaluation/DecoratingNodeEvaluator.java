package jaicore.search.algorithms.standard.bestfirst.nodeevaluation;

import jaicore.search.algorithms.standard.bestfirst.exceptions.NodeEvaluationException;
import jaicore.search.core.interfaces.GraphGenerator;
import jaicore.search.model.travesaltree.Node;

public abstract class DecoratingNodeEvaluator<T, V extends Comparable<V>> implements INodeEvaluator<T, V>, ICancelableNodeEvaluator {

	private boolean canceled = false;
	private final INodeEvaluator<T, V> evaluator;

	public DecoratingNodeEvaluator(final INodeEvaluator<T, V> evaluator) {
		super();
		if (evaluator == null)
			throw new IllegalArgumentException("The decorated evaluator must not be null!");
		this.evaluator = evaluator;
	}

	public INodeEvaluator<T, V> getEvaluator() {
		return this.evaluator;
	}

	@Override
	public V f(final Node<T, ?> node) throws NodeEvaluationException, InterruptedException {
		return this.evaluator.f(node);
	}

	public boolean isDecoratedEvaluatorCancelable() {
		return this.evaluator instanceof ICancelableNodeEvaluator;
	}

	public boolean isDecoratedEvaluatorGraphDependent() {
		if (this.evaluator instanceof DecoratingNodeEvaluator<?, ?>) {
			return ((DecoratingNodeEvaluator<T, V>) this.evaluator).isGraphDependent();
		}
		return this.evaluator instanceof IGraphDependentNodeEvaluator<?, ?, ?>;
	}

	public boolean isGraphDependent() {
		return this instanceof IGraphDependentNodeEvaluator<?, ?, ?> || this.isDecoratedEvaluatorGraphDependent();
	}

	public boolean isDecoratedEvaluatorSolutionReporter() {
		if (this.evaluator instanceof DecoratingNodeEvaluator<?, ?>) {
			return ((DecoratingNodeEvaluator<T, V>) this.evaluator).isSolutionReporter();
		}
		return (this.evaluator instanceof ISolutionReportingNodeEvaluator<?, ?>);
	}

	public boolean isSolutionReporter() {
		return this instanceof ISolutionReportingNodeEvaluator<?, ?> || this.isDecoratedEvaluatorSolutionReporter();
	}

	/* here we have the default implementations for the GraphDependent and SolutionReporter interfaces */
	@SuppressWarnings("unchecked")
	public <A> void setGenerator(final GraphGenerator<T, A> generator) {
		if (!this.isGraphDependent()) {
			throw new UnsupportedOperationException("This node evaluator is not graph dependent");
		}
		if (!this.isDecoratedEvaluatorGraphDependent()) {
			return;
		}
		((IGraphDependentNodeEvaluator<T, A, V>) this.evaluator).setGenerator(generator);
	}

	public void registerSolutionListener(final Object listener) {
		if (!this.isDecoratedEvaluatorSolutionReporter()) {
			throw new UnsupportedOperationException(this.getClass().getName() + " is not a solution reporting node evaluator");
		}
		((ISolutionReportingNodeEvaluator<T, V>) this.evaluator).registerSolutionListener(listener);
	}

	@Override
	public void cancel() {
		if (this.canceled) {
			return;
		}
		this.canceled = true;
		if (this.isDecoratedEvaluatorCancelable()) {
			((ICancelableNodeEvaluator) this.evaluator).cancel();
		}
		if (this instanceof ICancelableNodeEvaluator) {
			((ICancelableNodeEvaluator) this).cancel();
		}
	}
}
