package ai.libs.jaicore.ml.classification.singlelabel.timeseries.learner.neighbors;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.stream.Stream;

import org.api4.java.ai.ml.core.exception.PredictionException;
import org.api4.java.ai.ml.core.exception.TrainingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ai.libs.jaicore.basic.metric.EuclideanDistance;
import ai.libs.jaicore.basic.sets.Pair;
import ai.libs.jaicore.ml.classification.singlelabel.timeseries.dataset.TimeSeriesDataset2;

/**
 * Test suite for the
 * {@link jaicore.ml.tsc.classifier.neighbors.NearestNeighborClassifer}
 * implementation.
 *
 * @author fischor
 */
public class NearestNeighborTest {

	/**
	 * Dataset containing the values <code>
	 * {
	 *  { 0.4, 0.5 },
	 *  { 0.4, 0.6 },
	 *  { 0.4, 0.4 },
	 *  { 0.7, 0.6 },
	 *  { 0.7, 0.3 }
	 * }
	 * </code> after set up.
	 */
	private TimeSeriesDataset2 dataset;

	/**
	 * Priority queue containing the (targetClass, distance)-pairs <code>
	 * {
	 *  (1, 0.1),
	 *  (2, 0.2),
	 *  (3, 0.3),
	 *  (2, 0.4),
	 *  (3, 0.8),
	 *  (3, 1.6)
	 * }</code>.
	 */
	private PriorityQueue<Pair<Integer, Double>> nearestNeighbors;

	@BeforeEach
	public void setUp() {
		// Set up dataset.
		double data[][] = { { 0.4, 0.5 }, { 0.4, 0.6 }, { 0.4, 0.4 }, { 0.7, 0.6 }, { 0.7, 0.3 } };
		int[] targets = { 1, 2, 2, 1, 1 };
		ArrayList<double[][]> values = new ArrayList<>(1);
		values.add(data);
		this.dataset = new TimeSeriesDataset2(values, targets);

		// Set up priority queue.
		this.nearestNeighbors = new PriorityQueue<>(NearestNeighborClassifier.nearestNeighborComparator);
		this.nearestNeighbors.add(new Pair<Integer, Double>(1, 0.1));
		this.nearestNeighbors.add(new Pair<Integer, Double>(2, 0.2));
		this.nearestNeighbors.add(new Pair<Integer, Double>(3, 0.3));
		this.nearestNeighbors.add(new Pair<Integer, Double>(2, 0.4));
		this.nearestNeighbors.add(new Pair<Integer, Double>(3, 0.8));
		this.nearestNeighbors.add(new Pair<Integer, Double>(3, 1.6));
	}

	@ParameterizedTest
	@MethodSource("getKNearestNeighborTestCase")
	public void testPredictionWithK(final double[] instance, final int k, final int expectedPrediction) throws TrainingException, PredictionException, InterruptedException {
		NearestNeighborClassifier classifier = new NearestNeighborClassifier(k, new EuclideanDistance());
		classifier.train(this.dataset);
		// Predict.
		int prediction = classifier.predict(instance);
		assertEquals(expectedPrediction, prediction);
	}

	/**
	 * Create prediction for the test instance <code>t = { 0.5, 0.5 }</code> on the
	 * dataset with <code>k=1</code> and euclidean distance. Since the nearest
	 * neighbor of <code>t</code> is <code>s = {0.4, 0.5}</code> the predicition
	 * should yield the class of <code>s</code>, that is <code>1</code>.
	 *
	 * @throws TrainingException
	 * @throws PredictionException
	 * @throws InterruptedException
	 */
	public static Stream<Arguments> getKNearestNeighborTestCase() {
		return Stream.of(Arguments.of(new double[] { 0.5, 0.5 }, 1, 1), Arguments.of(new double[] { 0.5, 0.5 }, 3, 2), Arguments.of(new double[] { 0.5, 0.5 }, 5, 1));
	}

	/**
	 * Test the @see NearestNeighborComparator by first adding elements to a
	 * priority queue using the comparator and then polling the elements out and
	 * check the sequence of polled elements.
	 */
	@Test
	public void testNearestNeighborComparator() {
		// Create priority queue and fill (in not sorted order).
		PriorityQueue<Pair<Integer, Double>> queue = new PriorityQueue<>(NearestNeighborClassifier.nearestNeighborComparator);
		queue.add(new Pair<Integer, Double>(3, 1.6));
		queue.add(new Pair<Integer, Double>(3, 0.3));
		queue.add(new Pair<Integer, Double>(1, 0.1));
		queue.add(new Pair<Integer, Double>(3, 0.8));
		queue.add(new Pair<Integer, Double>(2, 0.2));
		queue.add(new Pair<Integer, Double>(2, 0.4));

		// Poll every element and assure correct sequence of polled elements.
		Pair<Integer, Double> pair;
		pair = queue.poll();
		assertEquals(1.6, pair.getY(), .0);
		pair = queue.poll();
		assertEquals(0.8, pair.getY(), .0);
		pair = queue.poll();
		assertEquals(0.4, pair.getY(), .0);
		pair = queue.poll();
		assertEquals(0.3, pair.getY(), .0);
		pair = queue.poll();
		assertEquals(0.2, pair.getY(), .0);
		pair = queue.poll();
		assertEquals(0.1, pair.getY(), .0);
	}

	/**
	 * Tests the majority vote. Since he nearest neighbors priority queue contains
	 * the elements with the classes <code>1, 2, 3, 2, 3, 3</code> the expected
	 * result is <code>3</code>.
	 */
	@Test
	public void testVoteMajority() {
		NearestNeighborClassifier classifier = new NearestNeighborClassifier(6, new EuclideanDistance());
		int votedTargetClass = classifier.voteMajority(this.nearestNeighbors);
		int expectation = 3;
		assertEquals(expectation, votedTargetClass);
	}

	/**
	 * Tests the weighted stepwise vote. The nearest neighbors priority queue
	 * contains the elements with the classes <code>1, 2, 3, 2, 3, 3</code> (in
	 * order). Thus,the weight for each class is <code>
	 * class 1: 6,
	 * class 2: 8,
	 * class 3: 7</code>. Thus, the expected result should be class <code>2</code>.
	 */
	@Test
	public void testVoteWeightedStepwise() {
		NearestNeighborClassifier classifier = new NearestNeighborClassifier(6, new EuclideanDistance());
		int votedTargetClass = classifier.voteWeightedStepwise(this.nearestNeighbors);
		int expectation = 2;
		assertEquals(expectation, votedTargetClass);
	}

	/**
	 * Tests the weighted proportional to distance vote. The elements of the nearest
	 * neighbors priority queue and its weight <code>w_i</code> are <code>
	 * {
	 *  (1, 0.1), w_1 = 10;
	 *  (2, 0.2), w_2 = 5;
	 *  (3, 0.3), w_3 = 3.3;
	 *  (2, 0.4), w_4 = 2.5;
	 *  (3, 0.8), w_5 = 1.25;
	 *  (3, 1.6), w_6 = 0.625;
	 * }</code>. Thus the weights for each class are <code>
	 * class 1: 10,
	 * class 2: 7.5,
	 * class 3: 5.175.</code> Thus, the expected result shuld be class
	 * <code>1</code>.
	 */
	@Test
	public void testVoteWeightedProportionalToDistance() {
		NearestNeighborClassifier classifier = new NearestNeighborClassifier(6, new EuclideanDistance());
		int votedTargetClass = classifier.voteWeightedProportionalToDistance(this.nearestNeighbors);
		int expectation = 1;
		assertEquals(expectation, votedTargetClass);
	}

	/**
	 * Tests if IllegalArgumetnExceptions are thrown when making calls with
	 * <code>null</code> objects.
	 * @throws TrainingException
	 * @throws PredictionException
	 * @throws InterruptedException
	 */
	@Test
	public void testPredictionOnSingleInstanceWithNullInstanceThrowsIllegalArgumentException() throws TrainingException, PredictionException, InterruptedException {
		// For single instance prediciton.
		NearestNeighborClassifier classifier = new NearestNeighborClassifier(6, new EuclideanDistance());
		classifier.train(this.dataset);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			classifier.predict((double[]) null);
		});
	}

	/**
	 * Tests if IllegalArgumetnExceptions are thrown when making calls with
	 * <code>null</code> objects.
	 * @throws InterruptedException
	 * @throws TrainingException
	 */
	@Test
	public void testPredictionOnDatasetWithNullInstanceThrowsIllegalArgumentException() throws TrainingException, InterruptedException {
		// For prediction on dataset
		NearestNeighborClassifier classifier = new NearestNeighborClassifier(6, new EuclideanDistance());
		classifier.train(this.dataset);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			classifier.predict((TimeSeriesDataset2) null);
		});
	}

}