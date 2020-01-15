package ai.libs.jaicore.basic.complexity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ai.libs.jaicore.basic.complexity.StretchingComplexity;

/**
 * Test suite for the {@link StretchingComplexity} implementation.
 * 
 * @author fischor
 */
public class StretchingComplexityTest {

    double[] timeSeries1 = { 1, 1, 1, 1, 1, 1 };
    double[] timeSeries2 = { .0, Math.sqrt(8), .0, Math.sqrt(8), .0, Math.sqrt(8) };

    @Test
    public void testComplexityCalculation() throws IllegalArgumentException {
        StretchingComplexity sc = new StretchingComplexity();
        double complexity = sc.complexity(timeSeries1);
        double expectation = 5;
        String message = "Calculated %f, but %f was expected";
        assertEquals(String.format(message, complexity, expectation), expectation, complexity, 0.001);
    }

    @Test
    public void testComplexityCalculation2() throws IllegalArgumentException {
        StretchingComplexity sc = new StretchingComplexity();
        double complexity = sc.complexity(timeSeries2);
        double expectation = 15;
        String message = "Calculated %f, but %f was expected";
        assertEquals(String.format(message, complexity, expectation), expectation, complexity, 0.001);
    }
}