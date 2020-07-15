package ai.libs.automl.mlplan.test;

import ai.libs.automl.MLPlanResultOrderTest;
import ai.libs.jaicore.ml.weka.classification.learner.IWekaClassifier;
import ai.libs.mlplan.core.AMLPlanBuilder;
import ai.libs.mlplan.multiclass.wekamlplan.MLPlanWekaBuilder;

public class WekaMLPlanResultOrderTest extends MLPlanResultOrderTest<IWekaClassifier> {

	@Override
	public AMLPlanBuilder<IWekaClassifier, ?> getMLPlanBuilder() throws Exception {
		return new MLPlanWekaBuilder();
	}

}
