package ai.libs.jaicore.math.gradientdescent;

import org.api4.java.common.math.IVector;

import ai.libs.jaicore.math.linearalgebra.DenseDoubleVector;

/**
 * Difference quotient based gradient estimation. This class will give a
 * black-box gradient estimation by simply calculating
 *
 * (f(x + h) - f(x))/h
 *
 * where x is the provided point and x' is a point that slightly differs,
 * specified by the parameter <code>precision</code>. (Obviously it holds that
 * in lim_{precision -> 0} this yields the exact gradient.)
 *
 * If x is a vector (a_o, ..., a_n), then, instead we calculate each partial
 * derivative i by:
 *
 * (f(a_o, ... a_i +h, ... , a_n) - f((a_o, ..., a_n)))/h
 *
 * Obviously, this is a highly inefficient approach for estimating the gradient
 * (if we have n partial derivatives, we need 2 *n estimations).
 *
 * @author Mirko J�rgens
 *
 */
public class BlackBoxGradient implements IGradientFunction {

	private final double precision;

	private final IGradientDescendableFunction function;

	/**
	 * Sets up a gradient-estimator for the given function. The estimation of the gradient can be tuned by the precision parameter.
	 * 
	 * @param underlyingFunction the function for which the gradient shall be estimated
	 * @param precision the precision of the estimation, the close this value is to zero the better is the estimation
	 */
	public BlackBoxGradient(final IGradientDescendableFunction underlyingFunction, final double precision) {
		this.precision = precision;
		this.function = underlyingFunction;
	}

	@Override
	public IVector apply(final IVector xVec) {
		IVector gradient = new DenseDoubleVector(xVec.length());
		double fX = this.function.apply(xVec);
		IVector xPrime = new DenseDoubleVector(xVec.asArray());
		for (int i = 0; i < xVec.length(); i++) {
			if (i > 0) {
				xPrime.setValue(i - 1, xPrime.getValue(i - 1) - this.precision);
			}
			xPrime.setValue(i, xPrime.getValue(i) + this.precision);
			// now compute f(x') - f(x)
			double fXPrime = this.function.apply(xPrime);
			double partial = fXPrime - fX;
			gradient.setValue(i, partial);
		}
		return gradient;
	}

}
