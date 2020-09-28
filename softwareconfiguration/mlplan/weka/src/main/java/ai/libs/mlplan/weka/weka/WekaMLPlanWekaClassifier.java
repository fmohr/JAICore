package ai.libs.mlplan.weka.weka;

import java.io.IOException;

import ai.libs.mlplan.weka.MLPlanWekaBuilder;
import ai.libs.mlplan.weka.MLPlanWekaClassifier;

public class WekaMLPlanWekaClassifier extends MLPlanWekaClassifier {

	/**
	 * Automatically generated version uid for serialization.
	 */
	private static final long serialVersionUID = 985257791846750757L;

	public WekaMLPlanWekaClassifier(final MLPlanWekaBuilder builder) {
		super(builder);
	}

	public WekaMLPlanWekaClassifier() throws IOException {
		this(new MLPlanWekaBuilder());

	}
}
