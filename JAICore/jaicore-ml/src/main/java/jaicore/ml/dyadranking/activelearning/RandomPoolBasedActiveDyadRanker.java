package jaicore.ml.dyadranking.activelearning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.upb.isys.linearalgebra.Vector;
import jaicore.ml.core.exception.TrainingException;
import jaicore.ml.dyadranking.Dyad;
import jaicore.ml.dyadranking.algorithm.PLNetDyadRanker;
import jaicore.ml.dyadranking.dataset.IDyadRankingInstance;
import jaicore.ml.dyadranking.dataset.SparseDyadRankingInstance;

/**
 * A random active dyad ranker. The sampling strategy picks a problem instance
 * at random and then picks two alternatives at random for pairwise comparison.
 * This is repeated for a constant number of times to create a minibatch for
 * updating the ranker.
 * 
 * @author Jonas Hanselle
 *
 */
public class RandomPoolBasedActiveDyadRanker extends ActiveDyadRanker {
	
	private static final Logger log = LoggerFactory.getLogger(RandomPoolBasedActiveDyadRanker.class);

	private Random random;
	private int maxBatchSize;

	public RandomPoolBasedActiveDyadRanker(PLNetDyadRanker ranker, IDyadRankingPoolProvider poolProvider,
			int maxBatchSize, long seed) {
		super(ranker, poolProvider);
		this.maxBatchSize = maxBatchSize;
		this.random = new Random(seed);
	}

	@Override
	public void activelyTrain(int numberOfQueries) {
		for (int i = 0; i < numberOfQueries; i++) {
			Set<IDyadRankingInstance> minibatch = new HashSet<>();
			for (int batchIndex = 0; batchIndex < maxBatchSize; batchIndex++) {
				// get random instance
				List<Vector> instanceFeatures = new ArrayList<>(poolProvider.getInstanceFeatures());
				Collections.shuffle(instanceFeatures, random);
				if (instanceFeatures.isEmpty())
					break;
				Vector instance = instanceFeatures.get(0);

				// get random pair of dyads
				List<Dyad> dyads = new ArrayList<>(poolProvider.getDyadsByInstance(instance));
				Collections.shuffle(dyads, random);

				// query them
				LinkedList<Vector> alternatives = new LinkedList<>();
				alternatives.add(dyads.get(0).getAlternative());
				alternatives.add(dyads.get(1).getAlternative());
				SparseDyadRankingInstance queryInstance = new SparseDyadRankingInstance(dyads.get(0).getInstance(),
						alternatives);
				IDyadRankingInstance trueRanking = (IDyadRankingInstance) poolProvider.query(queryInstance);
				minibatch.add(trueRanking);
			}
			// feed it to the ranker
			try {
				ranker.update(minibatch);
			} catch (TrainingException e) {
				log.error(e.getMessage());
			}
		}
	}

	public int getMaxBatchSize() {
		return maxBatchSize;
	}

	public void setMaxBatchSize(int maxBatchSize) {
		this.maxBatchSize = maxBatchSize;
	}

}
