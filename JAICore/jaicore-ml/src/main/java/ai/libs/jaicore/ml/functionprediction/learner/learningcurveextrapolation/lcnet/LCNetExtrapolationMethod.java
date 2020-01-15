package ai.libs.jaicore.ml.functionprediction.learner.learningcurveextrapolation.lcnet;

import java.io.IOException;

import org.api4.java.ai.ml.core.evaluation.learningcurve.ILearningCurve;
import org.api4.java.ai.ml.core.exception.TrainingException;

import ai.libs.jaicore.ml.functionprediction.learner.learningcurveextrapolation.InvalidAnchorPointsException;
import ai.libs.jaicore.ml.functionprediction.learner.learningcurveextrapolation.LearningCurveExtrapolationMethod;

/**
 * This class represents a learning curve extrapolation using the LCNet
 * from pybnn. It communicates with pybnn via the LCNetClient. If you use
 * if do not forget to call setConfigurations before you call
 * extrapolateLearningCurveFromAnchorPoints, because the LCNet requires
 * the configurations of you classifier
 *
 * @author noni4
 */

public class LCNetExtrapolationMethod implements LearningCurveExtrapolationMethod {

	private LCNetClient lcNet = new LCNetClient();
	private String identifier;
	private double[] configurations;

	public LCNetExtrapolationMethod(final String identifier) {
		this.identifier = identifier;
		this.configurations = null;
	}

	public void setConfigurations(final double[] configurations) {
		this.configurations = configurations;
	}

	@Override
	public ILearningCurve extrapolateLearningCurveFromAnchorPoints(final int[] xValues, final double[] yValues, final int dataSetSize) throws InvalidAnchorPointsException {
		if (this.configurations == null) {
			throw new UnsupportedOperationException("Configurations is not allowed to be null");
		} else {
			return new PointWiseLearningCurve(dataSetSize, this.configurations, this.identifier);
		}
	}

	// It is not clarified yet if this method should be called from this class
	public void deleteNet() throws IOException {
		this.lcNet.deleteNet(this.identifier);
	}

	// It is not clarified yet if this method should be called from this class
	public void trainNet(final int[] xValues, final double[] yValues, final int dataSetSize, final double[][] configurations) throws TrainingException {
		this.lcNet.train(xValues, yValues, dataSetSize, configurations, this.identifier);
	}
}
