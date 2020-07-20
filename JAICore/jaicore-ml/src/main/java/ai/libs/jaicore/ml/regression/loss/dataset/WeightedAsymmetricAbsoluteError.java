package ai.libs.jaicore.ml.regression.loss.dataset;

import java.util.ArrayList;
import java.util.List;

import org.api4.java.ai.ml.regression.evaluation.IRegressionPrediction;

import ai.libs.jaicore.basic.StatisticsUtil;

public class WeightedAsymmetricAbsoluteError extends AUnboundedRegressionMeasure {

	private double weightUnderestimation = 1;
	private double weightOverestimation = 1;

	public WeightedAsymmetricAbsoluteError() {
	}

	public WeightedAsymmetricAbsoluteError(final double weightUnderestimation, final double weightOverestimation) {
		this.weightUnderestimation = weightUnderestimation;
		this.weightOverestimation = weightOverestimation;
	}

	@Override
	public double loss(final List<? extends Double> expected, final List<? extends IRegressionPrediction> predicted) {
		List<Double> errors = new ArrayList<>();
		for (int i = 0; i < expected.size(); i++) {
			double d = predicted.get(i).getPrediction() - expected.get(i);
			Double error;
			if (d <= 0) {
				error = -this.weightUnderestimation * d;
			} else {
				error = this.weightOverestimation * d;
			}
			errors.add(error);
		}
		return StatisticsUtil.mean(errors);
	}
}