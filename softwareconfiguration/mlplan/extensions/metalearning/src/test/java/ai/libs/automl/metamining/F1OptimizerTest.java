package ai.libs.automl.metamining;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import ai.libs.mlplan.metamining.similaritymeasures.F1Optimizer;

public class F1OptimizerTest {

	@Disabled("This project is currently not maintained")
	@Test
	public void test() {

		/* invent some R and X matrices */
		INDArray vR = Nd4j.create(new float[] { 1, 2, 3, 4, 5, 6, 7, 8 }, new int[] { 2, 3 });
		INDArray vX = Nd4j.create(new float[] { 1, 2, 3, 4, 5, 6, 7, 8 }, new int[] { 2, 3 });

		/* learn the U matrix */
		F1Optimizer uLearner = new F1Optimizer();
		uLearner.build(vX, null, vR);

		INDArray vW = Nd4j.create(new float[] { 1, 2, 3, 4, 5, 6, 7, 8 }, new int[] { 4, 2 });
		assertNotEquals(Double.NaN, uLearner.computeSimilarity(vX, vW));
	}

}
