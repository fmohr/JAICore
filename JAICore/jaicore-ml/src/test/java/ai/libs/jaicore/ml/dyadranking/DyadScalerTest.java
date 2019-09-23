package ai.libs.jaicore.ml.dyadranking;

import java.util.Arrays;
import java.util.List;

import org.api4.java.ai.ml.ranking.dyad.dataset.IDyadRankingInstance;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ai.libs.jaicore.math.linearalgebra.DenseDoubleVector;
import ai.libs.jaicore.math.linearalgebra.IVector;
import ai.libs.jaicore.ml.ranking.dyad.dataset.DyadRankingDataset;
import ai.libs.jaicore.ml.ranking.dyad.dataset.DyadRankingInstance;
import ai.libs.jaicore.ml.ranking.dyad.learner.Dyad;
import ai.libs.jaicore.ml.ranking.dyad.learner.util.AbstractDyadScaler;
import ai.libs.jaicore.ml.ranking.dyad.learner.util.DyadMinMaxScaler;
import ai.libs.jaicore.ml.ranking.dyad.learner.util.DyadStandardScaler;
import ai.libs.jaicore.ml.ranking.dyad.learner.util.DyadUnitIntervalScaler;

/**
 * Tests our basic scalers.
 *
 * @author Mirko J�rgens
 *
 */
public class DyadScalerTest {

	private double[] expectedStdResultX;

	private double[] expectedUnitResultX;

	private double[] expectedMinMaxResultX;

	private double[] expectedStdResultY;

	private double[] expectedUnitResultY;

	private double[] expectedMinMaxResultY;

	private DyadRankingDataset testingSet;

	public void setupDataset() {
		IVector instance = new DenseDoubleVector(new double[] { 1.0d });
		IVector alternative = new DenseDoubleVector(new double[] { 4.0d });
		Dyad dOne = new Dyad(instance, alternative);
		instance = new DenseDoubleVector(new double[] { 2.0d });
		alternative = new DenseDoubleVector(new double[] { 4.0d });
		Dyad dTwo = new Dyad(instance, alternative);
		instance = new DenseDoubleVector(new double[] { 3.0d });
		alternative = new DenseDoubleVector(new double[] { 4.0d });
		Dyad dThree = new Dyad(instance, alternative);
		instance = new DenseDoubleVector(new double[] { 10.0d });
		alternative = new DenseDoubleVector(new double[] { 4.0d });
		Dyad dFour = new Dyad(instance, alternative);
		DyadRankingInstance ranking = new DyadRankingInstance(Arrays.asList(dOne, dTwo, dThree, dFour));
		List<IDyadRankingInstance> allRankings = Arrays.asList(ranking);
		this.testingSet = new DyadRankingDataset(allRankings);
	}

	@Before
	public void setupExpectedResults() {
		// standardization
		// derived by wolfram alpha
		double mean = 4.0;
		double stdDev = 4.0825;
		double stdOne = (1.0 - mean) / stdDev;
		double stdTwo = (2.0 - mean) / stdDev;
		double stdThree = (3.0 - mean) / stdDev;
		double stdFour = (10.0 - mean) / stdDev;
		double stdAlt = 0.0d;
		this.expectedStdResultX = new double[] { stdOne, stdTwo, stdThree, stdFour };
		this.expectedStdResultY = new double[] { stdAlt, stdAlt, stdAlt, stdAlt };

		// unit interval scaler
		double lengthOfVector = Math.sqrt(1 + 4 + 9 + 100);
		double lengthOfAltVector = Math.sqrt(16 + 16 + 16 + 16);
		double unitOne = (1.0 / lengthOfVector);
		double unitTwo = (2.0 / lengthOfVector);
		double unitThree = (3.0 / lengthOfVector);
		double unitFour = (10.0 / lengthOfVector);
		double unitAlt = (4.0 / lengthOfAltVector);
		this.expectedUnitResultX = new double[] { unitOne, unitTwo, unitThree, unitFour };
		this.expectedUnitResultY = new double[] { unitAlt, unitAlt, unitAlt, unitAlt };

		// min max scaler
		double min = 1.0d;
		double max = 10.0d;
		double minmaxDiff = max - min;
		// would be a division b 0
		double minmaxOne = (1.0d - min) / minmaxDiff;
		double minmaxTwo = (2.0d - min) / minmaxDiff;
		double minmaxThree = (3.0d - min) / minmaxDiff;
		double minmaxFour = (10.0d - min) / minmaxDiff;
		double minmaxAlt = 0.0d;
		this.expectedMinMaxResultX = new double[] { minmaxOne, minmaxTwo, minmaxThree, minmaxFour };
		this.expectedMinMaxResultY = new double[] { minmaxAlt, minmaxAlt, minmaxAlt, minmaxAlt };
	}

	@Test
	public void testStandardScaler() {
		this.setupDataset();
		System.out.println("Testing DyadStandardScaler...");
		AbstractDyadScaler stdScaler = new DyadStandardScaler();
		stdScaler.fit(this.testingSet);
		System.out.println("Testing transform instances...");
		stdScaler.transformInstances(this.testingSet);
		for (int i = 0; i < 4; i++) {
			Assert.assertEquals(this.expectedStdResultX[i], this.testingSet.get(0).getAttributeValue(i).getInstance().getValue(0), 0.0001);
		}
		System.out.println("Testing transform alternatives...");
		stdScaler.transformAlternatives(this.testingSet);
		for (int i = 0; i < 4; i++) {
			Assert.assertEquals(this.expectedStdResultY[i], this.testingSet.get(0).getAttributeValue(i).getAlternative().getValue(0), 0.0001);
		}
	}

	@Test
	public void testUnitIntervalScaler() {
		this.setupDataset();
		System.out.println("Testing UnitIntervalScaler...");
		AbstractDyadScaler stdScaler = new DyadUnitIntervalScaler();
		stdScaler.fit(this.testingSet);
		System.out.println("Testing transform instances...");
		stdScaler.transformInstances(this.testingSet);
		for (int i = 0; i < 4; i++) {
			Assert.assertEquals(this.expectedUnitResultX[i], this.testingSet.get(0).getAttributeValue(i).getInstance().getValue(0), 0.0001);
		}
		System.out.println("Testing transform alternatives...");
		stdScaler.transformAlternatives(this.testingSet);
		for (int i = 0; i < 4; i++) {
			Assert.assertEquals(this.expectedUnitResultY[i], this.testingSet.get(0).getAttributeValue(i).getAlternative().getValue(0), 0.0001);
		}
	}

	@Test
	public void testMinMaxScaler() {
		this.setupDataset();
		System.out.println("Testing MinMaxScaler...");
		AbstractDyadScaler stdScaler = new DyadMinMaxScaler();
		stdScaler.fit(this.testingSet);
		System.out.println("Testing transform instances...");
		stdScaler.transformInstances(this.testingSet);
		for (int i = 0; i < 4; i++) {
			Assert.assertEquals(this.expectedMinMaxResultX[i], this.testingSet.get(0).getAttributeValue(i).getInstance().getValue(0), 0.0001);
		}
		System.out.println("Testing transform alternatives...");
		stdScaler.transformAlternatives(this.testingSet);
		for (int i = 0; i < 4; i++) {
			Assert.assertEquals(this.expectedMinMaxResultY[i], this.testingSet.get(0).getAttributeValue(i).getAlternative().getValue(0), 0.0001);
		}
	}
}
