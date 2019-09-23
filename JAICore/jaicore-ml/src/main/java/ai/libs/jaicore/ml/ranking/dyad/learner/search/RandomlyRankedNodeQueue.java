package ai.libs.jaicore.ml.ranking.dyad.learner.search;

import java.util.LinkedList;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.search.model.travesaltree.BackPointerPath;

/**
 * A node queue for the best first search that inserts new nodes at a random
 * position in the list.
 *
 * @author Helena Graf
 *
 * @param <N>
 * @param <V>
 */
@SuppressWarnings("serial")
public class RandomlyRankedNodeQueue<N, A, V extends Comparable<V>> extends LinkedList<BackPointerPath<N, A, V>> {

	private Random random;
	private transient Logger logger = LoggerFactory.getLogger(RandomlyRankedNodeQueue.class);

	public RandomlyRankedNodeQueue(final int seed) {
		this.random = new Random(seed);
	}

	/**
	 * Adds an element at a random position within the
	 */
	@Override
	public boolean add(final BackPointerPath<N, A, V> e) {
		int position = this.random.nextInt(this.size() + 1);
		this.logger.debug("Add node at random position {} to OPEN list of size {}.", position, this.size());
		super.add(position, e);
		return true;
	}

	@Override
	public void add(final int position, final BackPointerPath<N, A, V> e) {
		throw new UnsupportedOperationException("Cannot place items at a specific position wihtin a randomly ranked queue!");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.random == null) ? 0 : this.random.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		RandomlyRankedNodeQueue other = (RandomlyRankedNodeQueue) obj;
		if (this.random == null) {
			if (other.random != null) {
				return false;
			}
		} else if (!this.random.equals(other.random)) {
			return false;
		}
		return true;
	}
}
