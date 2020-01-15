package ai.libs.mlplan.multiclass;

import java.io.File;

import org.aeonbits.owner.Config.Sources;

import ai.libs.hasco.variants.forwarddecomposition.twophase.TwoPhaseHASCOConfig;

@Sources({ "file:conf/mlplan.properties" })
public interface MLPlanClassifierConfig extends TwoPhaseHASCOConfig {

	public static final String PREFERRED_COMPONENTS = "mlplan.preferredComponents";
	public static final String SELECTION_PORTION = "mlplan.selectionportion";

	@Key(SELECTION_PORTION)
	@DefaultValue("0.3")
	public double dataPortionForSelection();

	@Key(PREFERRED_COMPONENTS)
	@DefaultValue("conf/mlplan/precedenceList.txt")
	public File preferredComponents();
}
