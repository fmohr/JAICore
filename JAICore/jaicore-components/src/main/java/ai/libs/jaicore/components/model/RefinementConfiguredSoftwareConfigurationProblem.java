package ai.libs.jaicore.components.model;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.api4.java.common.attributedobjects.IObjectEvaluator;

import ai.libs.jaicore.components.api.IComponent;
import ai.libs.jaicore.components.api.IComponentInstance;
import ai.libs.jaicore.components.api.IParameter;
import ai.libs.jaicore.components.serialization.ComponentLoader;

/**
 * In this problem, the core software configuration problem is extended by predefining how the the parameters may be refined
 *
 * @author fmohr
 *
 * @param <V>
 */
public class RefinementConfiguredSoftwareConfigurationProblem<V extends Comparable<V>> extends SoftwareConfigurationProblem<V> {
	private final Map<IComponent, Map<IParameter, ParameterRefinementConfiguration>> paramRefinementConfig;

	public RefinementConfiguredSoftwareConfigurationProblem(final Collection<? extends IComponent> components, final String requiredInterface, final IObjectEvaluator<IComponentInstance, V> compositionEvaluator,
			final Map<IComponent, Map<IParameter, ParameterRefinementConfiguration>> paramRefinementConfig) {
		this(new SoftwareConfigurationProblem<>(components, requiredInterface, compositionEvaluator), paramRefinementConfig);
	}

	public RefinementConfiguredSoftwareConfigurationProblem(final RefinementConfiguredSoftwareConfigurationProblem<V> problemTemplate, final Collection<? extends IComponent> components,
			final Map<IComponent, Map<IParameter, ParameterRefinementConfiguration>> paramRefinementConfig) {
		this(components, problemTemplate.getRequiredInterface(), problemTemplate.getCompositionEvaluator(), paramRefinementConfig);
	}

	public RefinementConfiguredSoftwareConfigurationProblem(final RefinementConfiguredSoftwareConfigurationProblem<V> problemTemplate, final IObjectEvaluator<IComponentInstance, V> evaluator) {
		this(problemTemplate.getComponents(), problemTemplate.getRequiredInterface(), evaluator, problemTemplate.getParamRefinementConfig());
	}

	public RefinementConfiguredSoftwareConfigurationProblem(final RefinementConfiguredSoftwareConfigurationProblem<V> problemTemplate, final String requiredInterface) {
		this(problemTemplate.getComponents(), requiredInterface, problemTemplate.getCompositionEvaluator(), problemTemplate.getParamRefinementConfig());
	}

	public RefinementConfiguredSoftwareConfigurationProblem(final File configurationFile, final String requiredInterface, final IObjectEvaluator<IComponentInstance, V> compositionEvaluator) throws IOException {
		super(configurationFile, requiredInterface, compositionEvaluator);
		this.paramRefinementConfig = new ComponentLoader(configurationFile).getParamConfigs();

		/* check that parameter refinements are defined for all components */
		for (IComponent c : this.getComponents()) {
			if (!this.paramRefinementConfig.containsKey(c)) {
				throw new IllegalArgumentException("Error in parsing config file " + configurationFile.getAbsolutePath() + ". Component " + c.getName() + " has not parameter refinement configs associated.");
			}
			for (IParameter p : c.getParameters()) {
				if (p.isNumeric() && !this.paramRefinementConfig.get(c).containsKey(p)) {
					throw new IllegalArgumentException("Error in parsing config file " + configurationFile.getAbsolutePath() + ". No refinement config was delivered for numeric parameter " + p.getName() + " of component " + c.getName());
				}
			}
		}
	}

	public RefinementConfiguredSoftwareConfigurationProblem(final SoftwareConfigurationProblem<V> coreProblem, final Map<IComponent, Map<IParameter, ParameterRefinementConfiguration>> paramRefinementConfig) {
		super(coreProblem);
		this.paramRefinementConfig = paramRefinementConfig;
	}

	public Map<IComponent, Map<IParameter, ParameterRefinementConfiguration>> getParamRefinementConfig() {
		return this.paramRefinementConfig;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		return prime + ((this.paramRefinementConfig == null) ? 0 : this.paramRefinementConfig.hashCode());
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		RefinementConfiguredSoftwareConfigurationProblem<?> other = (RefinementConfiguredSoftwareConfigurationProblem<?>) obj;
		if (this.paramRefinementConfig == null) {
			if (other.paramRefinementConfig != null) {
				return false;
			}
		} else if (!this.paramRefinementConfig.equals(other.paramRefinementConfig)) {
			return false;
		}
		return true;
	}
}
