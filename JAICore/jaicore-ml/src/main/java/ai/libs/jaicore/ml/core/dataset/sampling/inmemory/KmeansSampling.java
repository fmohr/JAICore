package ai.libs.jaicore.ml.core.dataset.sampling.inmemory;

import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.random.JDKRandomGenerator;

import ai.libs.jaicore.basic.algorithm.events.AlgorithmEvent;
import ai.libs.jaicore.basic.algorithm.exceptions.AlgorithmException;
import ai.libs.jaicore.ml.core.dataset.IDataset;
import ai.libs.jaicore.ml.core.dataset.IInstance;

/**
 * Implementation of a sampling method using kmeans-clustering. This algorithm
 * produces clusters of the given points and checks weather all points in a
 * cluster have the same target Attribute. If yes only the point nearest to the
 * center is added, otherwise the whole cluster is added to the sample.
 * <p>
 * Caution: This does ignore the given sample size!
 * 
 * @author jnowack
 *
 */
public class KmeansSampling<I extends IInstance> extends ClusterSampling<I> {
	/* number of clusters, if -1 use sample size */
	private int k;

	/**
	 * Implementation of a sampling method using kmeans-clustering.
	 * 
	 * @param seed
	 *            RAndom Seed
	 * @param k
	 *            number of clusters
	 */
	public KmeansSampling(long seed, int k, IDataset<I> input) {
		super(seed, input);
		this.k = k;
	}

	/**
	 * Implementation of a sampling method using kmeans-clustering. The sample size
	 * will be used as the number of clusters.
	 * 
	 * @param seed
	 *            Random Seed
	 * @param dist
	 *            {@link DistanceMeasure} to be used
	 */
	public KmeansSampling(long seed, DistanceMeasure dist, IDataset<I> input) {
		super(seed, dist, input);
		this.k = -1;

	}

	/**
	 * Implementation of a sampling method using kmeans-clustering.
	 * 
	 * @param seed
	 *            Random Seed
	 * @param k
	 *            number of clusters
	 * @param dist
	 *            {@link DistanceMeasure} to be used
	 */
	public KmeansSampling(long seed, int k, DistanceMeasure dist, IDataset<I> input) {
		super(seed, dist, input);
		this.k = k;

	}

	@Override
	public AlgorithmEvent nextWithException() throws AlgorithmException {
		switch (this.getState()) {
		case created:
			// Initialize variables
			this.sample = getInput().createEmpty();

			// create cluster
			JDKRandomGenerator r = new JDKRandomGenerator();
			r.setSeed(seed);
			// update k if k=-1
			if (k == -1) {
				k = sampleSize;
			}
			if (clusterResults == null) {
				KMeansPlusPlusClusterer<I> kMeansCluster = new KMeansPlusPlusClusterer<>(k, -1, distanceMeassure, r);
				clusterResults = kMeansCluster.cluster(getInput());
			}

			return this.activate();
		case active:
			this.doAlgorithmStep();
			break;
		case inactive:
			this.doInactiveStep();
			break;
		default:
			throw new IllegalStateException("Unknown algorithm state " + this.getState());
		}
		return null;
	}

}
