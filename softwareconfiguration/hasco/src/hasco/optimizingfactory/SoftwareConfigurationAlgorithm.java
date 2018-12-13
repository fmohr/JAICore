package hasco.optimizingfactory;

import hasco.core.SoftwareConfigurationProblem;
import hasco.model.EvaluatedSoftwareConfigurationSolution;
import jaicore.basic.algorithm.AOptimizer;
import jaicore.basic.algorithm.IAlgorithmConfig;

public abstract class SoftwareConfigurationAlgorithm<P extends SoftwareConfigurationProblem<V>, O, C extends EvaluatedSoftwareConfigurationSolution<V>, V extends Comparable<V>> extends AOptimizer<P, O, C, V> {

	protected SoftwareConfigurationAlgorithm() {
		super();
	}

	protected SoftwareConfigurationAlgorithm(final P input) {
		super(input);
	}

	protected SoftwareConfigurationAlgorithm(final IAlgorithmConfig config, final P input) {
		super(config, input);
	}

	protected SoftwareConfigurationAlgorithm(final IAlgorithmConfig config) {
		super(config);
	}

}
