package ai.libs.jaicore.ml.ranking;

import org.api4.java.ai.ml.core.learner.algorithm.IPrediction;
import org.api4.java.ai.ml.ranking.dataset.IRanking;

public interface IRankingPrediction extends IPrediction {

	@Override
	public IRanking<?> getPrediction();

}
