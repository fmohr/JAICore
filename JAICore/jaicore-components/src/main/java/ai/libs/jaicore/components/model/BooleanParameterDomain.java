package ai.libs.jaicore.components.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public class BooleanParameterDomain extends CategoricalParameterDomain {
	
	@JsonCreator
	public BooleanParameterDomain() {
		super(new String[] {"true", "false"});
	}
}
