package ai.libs.jaicore.ml.core.filter.sampling.inmemory.stratified.sampling;

import java.util.List;

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.distance.ManhattanDistance;
import org.api4.java.ai.ml.core.dataset.IDataset;

import ai.libs.jaicore.basic.sets.ListView;
import ai.libs.jaicore.ml.clustering.learner.GMeans;

/**
 * Combined strati amount selector and strati assigner via g-means. IT can be
 * used in 3 combinations:
 *
 * 1) Amount Selector and Strati Assigner: A g-means clustering is initially
 * performed to select a strati amount via the amount of found clusters and to
 * assign datapoints with this clusters afterwards.
 *
 * 2) Amount Selector: A g-means clustering is initially performed to select a
 * suitable strati amount with the amount of found clusters. 3) Strati Assigner:
 * Since the amount of strati is selected with another component g-means cannot
 * be used and k-means will be used to be conform with the given strati amount.
 *
 * It is recommended to use combination 1, because only using this component for
 * only one of the two tasks could yield in inconsistent results.
 *
 * @author Lukas Brandt
 */
public class GMeansStratifier extends ClusterStratiAssigner implements IStratifier {

	/**
	 * Constructor for GMeansStratiAmountSelectorAndAssigner with Manhattan
	 * distanceMeasure as a default.
	 *
	 * @param randomSeed
	 *            Seed for random numbers.
	 */
	public GMeansStratifier(final int randomSeed) {
		this.randomSeed = randomSeed;
		this.distanceMeasure = new ManhattanDistance();
	}

	/**
	 * Constructor for GMeansStratiAmountSelectorAndAssigner with custom
	 * distanceMeasure.
	 *
	 * @param distanceMeasure
	 *            Distance measure for datapoints, for example Manhattan or
	 *            Euclidian.
	 * @param randomSeed
	 *            Seed for random numbers.
	 */
	public GMeansStratifier(final DistanceMeasure distanceMeasure, final int randomSeed) {
		this.randomSeed = randomSeed;
		this.distanceMeasure = distanceMeasure;
	}

	@Override
	public int createStrati(final IDataset<?> dataset) {
		// Perform g-means to get a fitting k and the corresponding clusters.
		List<Clusterable> cDataset = new ListView<>(dataset);
		GMeans<Clusterable> clusterer = new GMeans<>(cDataset, this.distanceMeasure, this.randomSeed);
		this.setClusters(clusterer.cluster());
		return this.getClusters().size();
	}
}
