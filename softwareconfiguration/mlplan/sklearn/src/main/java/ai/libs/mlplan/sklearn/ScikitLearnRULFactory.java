package ai.libs.mlplan.sklearn;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import ai.libs.jaicore.components.api.IComponentInstance;
import ai.libs.jaicore.components.model.ComponentInstance;
import ai.libs.jaicore.ml.core.EScikitLearnProblemType;

public class ScikitLearnRULFactory extends AScikitLearnLearnerFactory {

	private static final String ELEMENT_SEPARATOR = ", ";

	public ScikitLearnRULFactory() {
		super(EScikitLearnProblemType.RUL);
	}

	@Override
	public String getPipelineBuildString(final IComponentInstance groundComponent, final Set<String> importSet) {
		StringBuilder sb = new StringBuilder();
		List<IComponentInstance> timeseriesFeatureGenerator = groundComponent.getSatisfactionOfRequiredInterface("timeseries_feature_generator");
		sb.append(this.getTimeseriesConstructionInstruction(timeseriesFeatureGenerator, importSet));
		sb.append(ELEMENT_SEPARATOR);
		sb.append(this.extractSKLearnConstructInstruction(groundComponent.getSatisfactionOfRequiredInterface("regressor").iterator().next(), importSet));
		return sb.toString();
	}

	private String getTimeseriesConstructionInstruction(final List<IComponentInstance> timeseriesComponentInstances, final Set<String> importSet) {
		StringJoiner stringJoiner = new StringJoiner(ELEMENT_SEPARATOR);
		int numberOfComponentInstancesFound = 0;
		for (IComponentInstance componentInstance : timeseriesComponentInstances.stream().sorted((o1, o2) -> o1.getComponent().getName().compareTo(o2.getComponent().getName()))
				.collect(Collectors.toList())) {
			if (componentInstance.getComponent().getName().endsWith("UniToMultivariateNumpyBasedFeatureGenerator")) {
				for (IComponentInstance satCI : componentInstance.getSatisfactionOfRequiredInterface("univariate_ts_feature_generator").stream()
						.sorted((o1, o2) -> o1.getComponent().getName().compareTo(o2.getComponent().getName())).collect(Collectors.toList())) {
					Map<String, List<IComponentInstance>> satisfactionOfRequiredInterfaces = new HashMap<>();
					satisfactionOfRequiredInterfaces.put("univariate_ts_feature_generator", Arrays.asList(satCI));
					IComponentInstance newCI = new ComponentInstance(componentInstance.getComponent(), componentInstance.getParameterValues(), satisfactionOfRequiredInterfaces);
					stringJoiner.add(this.extractSKLearnConstructInstruction(newCI, importSet));
					numberOfComponentInstancesFound++;
				}
			} else {
				numberOfComponentInstancesFound++;
				stringJoiner.add(this.extractSKLearnConstructInstruction(componentInstance, importSet));
			}
		}

		if (numberOfComponentInstancesFound > 1) {
			return "make_union(" + stringJoiner.toString() + ")";
		}
		return stringJoiner.toString();
	}
}
