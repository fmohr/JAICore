package ai.libs.mlplan.examples.multilabel.meka;

import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;

import ai.libs.jaicore.basic.IDatabaseConfig;
import ai.libs.jaicore.ml.core.evaluation.experiment.IMultiClassClassificationExperimentConfig;

@LoadPolicy(LoadType.MERGE)
@Sources({ "file:conf/ml2planAutoExperimenter.properties", "file:conf/ml2planAutodatabase.properties" })
public interface ML2PlanAutoMLCExperimenterConfig extends IMultiClassClassificationExperimentConfig, IDatabaseConfig {

	@Key("gui.show")
	@DefaultValue("false")
	public boolean showGUI();

}