package ai.libs.jaicore.ml.weka.classification.pipeline;

import weka.core.Instance;
import weka.core.Instances;

public interface FeaturePreprocessor {

	public void prepare(Instances data) throws Exception;

	public Instance apply(Instance data) throws Exception;

	public Instances apply(Instances data) throws Exception;

	public boolean isPrepared();
}
