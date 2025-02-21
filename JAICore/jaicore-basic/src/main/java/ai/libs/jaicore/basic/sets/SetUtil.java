package ai.libs.jaicore.basic.sets;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.geometry.euclidean.oned.Interval;
import org.api4.java.common.attributedobjects.GetPropertyFailedException;
import org.api4.java.common.attributedobjects.IGetter;

import ai.libs.jaicore.basic.MathExt;

/**
 * Utility class for sets.
 *
 * @author fmohr, mbunse, mwever
 */
public class SetUtil {
	private static final String DEFAULT_LIST_ITEM_SEPARATOR = ",";

	private SetUtil() {
		// prevent instantiation of this util class
	}

	/* BASIC SET OPERATIONS */
	@SafeVarargs
	public static <T> Collection<T> union(final Collection<T>... set) {
		Collection<T> union = new HashSet<>();
		for (int i = 0; i < set.length; i++) {
			Objects.requireNonNull(set[i]);
			union.addAll(set[i]);
		}
		return union;
	}

	public static <T> List<T> union(final List<T>... lists) {
		List<T> union = new ArrayList<>();
		for (int i = 0; i < lists.length; i++) {
			if (lists[i] != null) {
				union.addAll(lists[i]);
			}
		}
		return union;
	}

	public static <T> Collection<T> symmetricDifference(final Collection<T> a, final Collection<T> b) {
		return SetUtil.union(SetUtil.difference(a, b), SetUtil.difference(b, a));
	}

	public static <T> Collection<T> getMultiplyContainedItems(final List<T> list) {
		Set<T> doubleEntries = new HashSet<>();
		Set<T> observed = new HashSet<>();
		for (T item : list) {
			if (observed.contains(item)) {
				doubleEntries.add(item);
			} else {
				observed.add(item);
			}
		}
		return doubleEntries;
	}

	/**
	 * @param a
	 *            The set A.
	 * @param b
	 *            The set B.
	 * @return The intersection of sets A and B.
	 */
	public static <S, T extends S, U extends S> Collection<S> intersection(final Collection<T> a, final Collection<U> b) {
		List<S> out = new ArrayList<>();
		Collection<? extends S> bigger = a.size() < b.size() ? b : a;
		for (S item : ((a.size() >= b.size()) ? b : a)) {
			if (bigger.contains(item)) {
				out.add(item);
			}
		}
		return out;
	}

	public static <S, T extends S, U extends S> boolean disjoint(final Collection<T> a, final Collection<U> b) {
		Collection<? extends S> bigger = a.size() < b.size() ? b : a;
		for (S item : ((a.size() >= b.size()) ? b : a)) {
			if (bigger.contains(item)) {
				return false;
			}
		}
		return true;
	}

	public static <T> Collection<Collection<T>> getPotenceOfSet(final Collection<T> set, final byte exponent) {
		Collection<Collection<T>> items = new ArrayList<>();
		for (byte i = 0; i < exponent; i++) {
			items.add(set);
		}
		return getCartesianProductOfSetsOfSameClass(items);
	}

	public static <T> Collection<Collection<T>> getCartesianProductOfSetsOfSameClass(final Collection<Collection<T>> items) {

		/* recursion abortion */
		if (items.isEmpty()) {
			return new ArrayList<>();
		}
		if (items.size() == 1) {
			Collection<Collection<T>> tuples = new ArrayList<>();
			for (Collection<T> set : items) { // only one run exists here
				for (T value : set) {
					Collection<T> trivialTuple = new ArrayList<>();
					trivialTuple.add(value);
					tuples.add(trivialTuple);
				}
			}
			return tuples;
		}

		/* compute cartesian product of n-1 */
		Collection<Collection<T>> subproblem = new ArrayList<>();
		Collection<T> unconsideredDomain = null;
		int i = 0;
		int limit = items.size();
		for (Collection<T> set : items) {
			if (i < limit - 1) {
				subproblem.add(set);
			} else if (i == limit - 1) {
				unconsideredDomain = set;
				break;
			}
			i++;
		}
		Collection<Collection<T>> subsolution = getCartesianProductOfSetsOfSameClass(subproblem);

		/* compute solution */
		Collection<Collection<T>> solution = new ArrayList<>();
		for (Collection<T> tuple : subsolution) {
			for (T value : unconsideredDomain) {
				List<T> newTuple = new ArrayList<>();
				newTuple.addAll(tuple);
				newTuple.add(value);
				solution.add(newTuple);
			}
		}
		return solution;
	}

	/* SUBSETS */
	public static <T> Collection<Collection<T>> powerset(final Collection<T> items) throws InterruptedException {
		/* |M| = 0 */
		if (items.isEmpty()) {
			Collection<Collection<T>> setWithEmptySet = new ArrayList<>();
			setWithEmptySet.add(new ArrayList<>());
			return setWithEmptySet;
		}

		/* |M| >= 1 */
		T baseElement = null;
		Collection<T> restList = new ArrayList<>();
		int i = 0;
		for (T item : items) {
			if (i == 0) {
				baseElement = item;
			} else {
				restList.add(item);
			}
			i++;
		}
		Collection<Collection<T>> toAdd = new ArrayList<>();
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedException("Interrupted during calculation of power set");
		}
		Collection<Collection<T>> subsets = powerset(restList);
		for (Collection<T> existingSubset : subsets) {
			Collection<T> additionalList = new ArrayList<>();
			additionalList.addAll(existingSubset);
			additionalList.add(baseElement);
			toAdd.add(additionalList);
		}
		subsets.addAll(toAdd);
		return subsets;
	}

	public static <T> Collection<Collection<T>> getAllPossibleSubsets(final Collection<T> items) {

		/* |M| = 0 */
		if (items.isEmpty()) {
			Collection<Collection<T>> setWithEmptySet = new ArrayList<>();
			setWithEmptySet.add(new ArrayList<>());
			return setWithEmptySet;
		}

		/* |M| >= 1 */
		T baseElement = null;
		Collection<T> restList = new ArrayList<>();
		int i = 0;
		for (T item : items) {
			if (i == 0) {
				baseElement = item;
			} else {
				restList.add(item);
			}
			i++;
		}
		Collection<Collection<T>> subsets = getAllPossibleSubsets(restList);
		Collection<Collection<T>> toAdd = new ArrayList<>();
		for (Collection<T> existingSubset : subsets) {
			Collection<T> additionalList = new ArrayList<>();
			additionalList.addAll(existingSubset);
			additionalList.add(baseElement);
			toAdd.add(additionalList);
		}
		subsets.addAll(toAdd);
		return subsets;
	}

	private static class SubSetComputer<T> implements Runnable {

		private List<T> superSet;
		private ExecutorService pool;
		private int k;
		private int idx;
		private Set<T> current;
		private List<Set<T>> allSolutions;
		private Semaphore semThreads;
		private Semaphore semComplete;
		private long goalSize;

		public SubSetComputer(final List<T> superSet, final int k, final int idx, final Set<T> current, final List<Set<T>> allSolutions, final ExecutorService pool, final Semaphore sem, final long goalSize, final Semaphore semComplete) {
			super();
			this.superSet = superSet;
			this.pool = pool;
			this.k = k;
			this.idx = idx;
			this.current = current;
			this.allSolutions = allSolutions;
			this.semThreads = sem;
			this.semComplete = semComplete;
			this.goalSize = goalSize;
		}

		@Override
		public void run() {
			List<Set<T>> localSolutions = new ArrayList<>();
			this.performStep(this.superSet, this.k, this.idx, this.current, localSolutions);
			synchronized (this.allSolutions) {
				this.allSolutions.addAll(localSolutions);
				if (this.allSolutions.size() == this.goalSize) {
					this.semComplete.release();
				}
			}
			this.semThreads.release();
		}

		public void performStep(final List<T> superSet, final int k, final int idx, final Set<T> current, final List<Set<T>> solution) {

			// successful stop clause
			if (current.size() == k) {
				solution.add(new HashSet<>(current));
				return;
			}
			// unseccessful stop clause
			if (idx == superSet.size()) {
				return;
			}
			T x = superSet.get(idx);
			current.add(x);

			// "guess" x is in the subset
			if (this.semThreads.tryAcquire()) {

				/* outsource first task in a new thread */
				this.pool.submit(new SubSetComputer<T>(superSet, k, idx + 1, new HashSet<>(current), this.allSolutions, this.pool, this.semThreads, this.goalSize, this.semComplete));

				/* also try to outsorce the second task into its own thread */
				current.remove(x);
				if (this.semThreads.tryAcquire()) {
					this.pool.submit(new SubSetComputer<T>(superSet, k, idx + 1, new HashSet<>(current), this.allSolutions, this.pool, this.semThreads, this.goalSize, this.semComplete));
				} else {

					/* solve the second task in this same thread */
					this.performStep(superSet, k, idx + 1, current, solution);
				}
			} else {
				this.performStep(superSet, k, idx + 1, current, solution);
				current.remove(x);

				/* now check if a new thread is available for the second task */
				if (this.semThreads.tryAcquire()) {
					this.pool.submit(new SubSetComputer<T>(superSet, k, idx + 1, new HashSet<>(current), this.allSolutions, this.pool, this.semThreads, this.goalSize, this.semComplete));
				} else {
					this.performStep(superSet, k, idx + 1, current, solution);
				}
			}
		}
	}

	public static <T> Collection<Set<T>> subsetsOfSize(final Collection<T> set, final int size) throws InterruptedException {
		List<Set<T>> subsets = new ArrayList<>();
		List<T> setAsList = new ArrayList<>(); // for easier access
		setAsList.addAll(set);
		getSubsetOfSizeRec(setAsList, size, 0, new HashSet<T>(), subsets);
		return subsets;
	}

	private static <T> void getSubsetOfSizeRec(final List<T> superSet, final int k, final int idx, final Set<T> current, final Collection<Set<T>> solution) throws InterruptedException {
		// successful stop clause
		if (current.size() == k) {
			solution.add(new HashSet<>(current));
			return;
		}
		// unseccessful stop clause
		if (idx == superSet.size()) {
			return;
		}
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedException("Interrupted during calculation of subsets with special size");
		}
		T x = superSet.get(idx);
		current.add(x);
		// "guess" x is in the subset
		getSubsetOfSizeRec(superSet, k, idx + 1, current, solution);
		current.remove(x);
		// "guess" x is not in the subset
		getSubsetOfSizeRec(superSet, k, idx + 1, current, solution);
	}

	public static <T> List<Set<T>> getAllPossibleSubsetsWithSizeParallely(final Collection<T> superSet, final int k) throws InterruptedException {
		List<Set<T>> res = new ArrayList<>();
		int n = 1;
		ExecutorService pool = Executors.newFixedThreadPool(n);
		Semaphore solutionSemaphore = new Semaphore(1);
		solutionSemaphore.acquire();
		pool.submit(new SubSetComputer<>(new ArrayList<>(superSet), k, 0, new HashSet<>(), res, pool, new Semaphore(n - 1), MathExt.binomial(superSet.size(), k), solutionSemaphore));
		solutionSemaphore.acquire();
		pool.shutdown();
		return res;
	}

	private static <T> void getAllPossibleSubsetsWithSizeRecursive(final List<T> superSet, final int k, final int idx, final Set<T> current, final List<Set<T>> solution) {
		// successful stop clause
		if (current.size() == k) {
			solution.add(new HashSet<>(current));
			return;
		}
		// unseccessful stop clause
		if (idx == superSet.size()) {
			return;
		}
		T x = superSet.get(idx);
		current.add(x);
		// "guess" x is in the subset
		getAllPossibleSubsetsWithSizeRecursive(superSet, k, idx + 1, current, solution);
		current.remove(x);
		// "guess" x is not in the subset
		getAllPossibleSubsetsWithSizeRecursive(superSet, k, idx + 1, current, solution);
	}

	public static <T> List<Set<T>> getAllPossibleSubsetsWithSize(final Collection<T> superSet, final int k) {
		List<Set<T>> res = new ArrayList<>();
		getAllPossibleSubsetsWithSizeRecursive(new ArrayList<>(superSet), k, 0, new HashSet<T>(), res);
		return res;
	}

	public static List<Integer> invertPermutation(final List<Integer> permutation) {
		int n = permutation.size();
		List<Integer> inverse = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			inverse.add(permutation.indexOf(i));
		}
		return inverse;
	}

	/**
	 * Determines the permutation that makes l2 result from l1
	 **/
	public static <T> List<Integer> getPermutation(final List<T> l1, final List<T> l2) {
		int n = l1.size();
		if (n != l2.size()) {
			throw new IllegalArgumentException("Expecting two lists of same length!");
		}
		List<Integer> p = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			int pos = l1.indexOf(l2.get(i));
			if (pos < 0) {
				throw new IllegalArgumentException("The second list does not contain the element " + l1.get(i) + ". Cannot compute permutation between lists with different elements!");
			}
			p.add(pos);
		}
		return p;
	}

	/**
	 * Permutates the elements of the given list according to the given permutation
	 *
	 * @param <T>
	 * @param list
	 * @param permutation
	 * @return
	 */
	public static <T> List<T> applyPermutation(final List<T> list, final List<Integer> permutation) {
		int n = list.size();
		if (permutation.size() != n) {
			throw new IllegalArgumentException("The permutation must have the same length as the list.");
		}
		List<T> out = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			out.add(list.get(permutation.get(i)));
		}
		return out;
	}

	public static <T> List<T> applyInvertedPermutation(final List<T> list, final List<Integer> permutation) {
		return applyPermutation(list, invertPermutation(permutation));
	}

	public static <T> Collection<List<T>> getPermutations(final Collection<T> set) {
		Collection<List<T>> permutations = new ArrayList<>();
		List<T> setAsList = new ArrayList<>(set);
		getPermutationsRec(setAsList, 0, permutations);
		return permutations;
	}

	private static <T> void getPermutationsRec(final List<T> list, final int pointer, final Collection<List<T>> solution) {
		if (pointer == list.size()) {
			solution.add(list);
			return;
		}
		for (int i = pointer; i < list.size(); i++) {
			List<T> permutation = new ArrayList<>(list);
			permutation.set(pointer, list.get(i));
			permutation.set(i, list.get(pointer));
			getPermutationsRec(permutation, pointer + 1, solution);
		}
	}

	/**
	 * @param a
	 *            The set A.
	 * @param b
	 *            The set B.
	 * @return The difference A \ B.
	 */
	public static <S, T extends S, U extends S> Collection<S> difference(final Collection<T> a, final Collection<U> b) {

		List<S> out = new ArrayList<>();

		for (S item : a) {
			if (b == null || !b.contains(item)) {
				out.add(item);
			}
		}

		return out;
	}

	/**
	 * Computes the set of elements which are disjoint, i.e., elements from the set (A \cup B) \ (A \cap B)
	 *
	 * @param a
	 *            The set A.
	 * @param b
	 *            The set B.
	 * @return The difference A \ B.
	 */
	public static <S, T extends S, U extends S> Collection<S> getDisjointSet(final Collection<T> a, final Collection<U> b) {
		List<S> out = new ArrayList<>(difference(a, b));

		for (S item : difference(b, a)) {
			if (!out.contains(item)) {
				out.add(item);
			}
		}

		return out;
	}

	/**
	 * @param a
	 *            The set A.
	 * @param b
	 *            The set B.
	 * @return The difference A \ B.
	 */
	public static <S, T extends S, U extends S> List<S> difference(final List<T> a, final Collection<U> b) {

		List<S> out = new ArrayList<>();

		for (S item : a) {
			if (b == null || !b.contains(item)) {
				out.add(item);
			}
		}

		return out;
	}

	public static <S, T extends S, U extends S> boolean differenceEmpty(final Collection<T> a, final Collection<U> b) {
		if (a == null || a.isEmpty()) {
			return true;
		}
		for (S item : a) {
			if (!b.contains(item)) {
				return false;
			}
		}
		return true;
	}

	public static <S, T extends S, U extends S> boolean differenceNotEmpty(final Collection<T> a, final Collection<U> b) {
		if (b == null) {
			return !a.isEmpty();
		}
		for (S item : a) {
			if (!b.contains(item)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param a
	 *            The set A.
	 * @param b
	 *            The set B.
	 * @return The Cartesian product A x B.
	 */
	public static <S, T> Collection<Pair<S, T>> cartesianProduct(final Collection<S> a, final Collection<T> b) {
		Set<Pair<S, T>> product = new HashSet<>();

		for (S item1 : a) {
			for (T item2 : b) {
				product.add(new Pair<>(item1, item2));
			}
		}
		return product;
	}

	public static <T> Collection<List<T>> cartesianProduct(final List<? extends Collection<T>> listOfSets) {
		return cartesianProductReq(new ArrayList<>(listOfSets));
	}

	/**
	 * @param a
	 *            The set A.
	 * @param b
	 *            The set B.
	 * @return The Cartesian product A x B.
	 */
	private static <T> Collection<List<T>> cartesianProductReq(final List<? extends Collection<T>> listOfSets) {

		/* compute expected number of items of the result */
		int expectedSize = 1;
		for (Collection<T> items : listOfSets) {
			assert items.size() == new HashSet<>(items).size() : "One of the collection is effectively a multi-set, which is forbidden for CP computation: " + items;
			expectedSize *= items.size();
		}

		/* there must be at least one set */
		if (listOfSets.isEmpty()) {
			throw new IllegalArgumentException("Empty list of sets");
		}

		/*
		 * if there is only one set, create tuples of size 1 and return the set of tuples
		 */
		if (listOfSets.size() == 1) {
			Set<List<T>> product = new HashSet<>();
			for (T obj : listOfSets.get(0)) {
				List<T> tupleOfSize1 = new ArrayList<>();
				tupleOfSize1.add(obj);
				product.add(tupleOfSize1);
			}
			assert product.size() == expectedSize : "Invalid number of expected entries! Expected " + expectedSize + " but computed " + product.size() + " for a single set: " + listOfSets.get(0);
			return product;
		}

		/*
		 * if there are more sets, remove the last one, compute the cartesian for the rest, and append the removed one afterwards
		 */
		Collection<T> removed = listOfSets.get(listOfSets.size() - 1);
		listOfSets.remove(listOfSets.size() - 1);
		Collection<List<T>> subSolution = cartesianProduct(listOfSets);
		Set<List<T>> product = new HashSet<>();
		for (List<T> tuple : subSolution) {
			for (T item : removed) {
				List<T> newTuple = new ArrayList<>(tuple);
				newTuple.add(item);
				product.add(newTuple);
			}
		}
		assert product.size() == expectedSize : "Invalid number of expected entries! Expected " + expectedSize + " but computed " + product.size();
		return product;
	}

	/**
	 * @param a
	 *            The set A.
	 * @throws InterruptedException
	 */
	public static <S> Collection<List<S>> cartesianProduct(final Collection<S> set, final int number) throws InterruptedException {
		List<List<S>> product = new ArrayList<>();
		List<S> setAsList = new ArrayList<>(set);
		if (number <= 1) {
			for (S elem : set) {
				List<S> tuple = new ArrayList<>();
				tuple.add(elem);
				product.add(tuple);
			}
			return product;
		}
		for (List<S> restProduct : cartesianProduct(setAsList, number - 1)) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}
			for (S elem : set) {
				if (Thread.currentThread().isInterrupted()) {
					throw new InterruptedException();
				}
				List<S> tuple = new ArrayList<>(restProduct.size() + 1);
				for (S elementOfRestProduct : restProduct) {
					if (Thread.currentThread().isInterrupted()) {
						throw new InterruptedException();
					}
					tuple.add(elementOfRestProduct);
				}
				tuple.add(0, elem);
				product.add(tuple);
			}
		}
		return product;
	}

	/* RELATIONS */
	public static <K, V> Collection<Pair<K, V>> relation(final Collection<K> keys, final Collection<V> values, final Predicate<Pair<K, V>> relationPredicate) {
		Collection<Pair<K, V>> relation = new HashSet<>();
		for (K key : keys) {
			for (V val : values) {
				Pair<K, V> p = new Pair<>(key, val);
				if (relationPredicate.test(p)) {
					relation.add(p);
				}
			}
		}
		return relation;
	}

	public static <K, V> Map<K, Collection<V>> relationAsFunction(final Collection<K> keys, final Collection<V> values, final Predicate<Pair<K, V>> relationPredicate) {
		Map<K, Collection<V>> relation = new HashMap<>();
		for (K key : keys) {
			relation.put(key, new HashSet<>());
			for (V val : values) {
				Pair<K, V> p = new Pair<>(key, val);
				if (relationPredicate.test(p)) {
					relation.get(key).add(val);
				}
			}
		}
		return relation;
	}

	/* FUNCTIONS */
	public static <K, V> Collection<Map<K, V>> allMappings(final Collection<K> domain, final Collection<V> range, final boolean totalsOnly, final boolean injectivesOnly, final boolean surjectivesOnly) throws InterruptedException {

		Collection<Map<K, V>> mappings = new ArrayList<>();

		/* compute possible domains of the functions */
		if (totalsOnly) {

			if (domain.isEmpty()) {
				return mappings;
			}
			List<K> domainAsList = new ArrayList<>(domain);
			int n = domainAsList.size();
			for (List<V> reducedRange : cartesianProduct(range, domain.size())) {
				if (Thread.currentThread().isInterrupted()) {
					throw new InterruptedException("Interrupted during calculating all mappings");
				}
				/*
				 * create map that corresponds to this entry of the cartesian product
				 */
				boolean considerMap = true;
				Map<K, V> map = new HashMap<>();
				List<V> coveredRange = new ArrayList<>();
				for (int i = 0; i < n; i++) {
					V val = reducedRange.get(i);

					/* check injectivity (if required) */
					if (injectivesOnly && coveredRange.contains(val)) {
						considerMap = false;
						break;
					}
					coveredRange.add(val);
					map.put(domainAsList.get(i), val);
				}

				/* check surjectivity (if required) */
				if (surjectivesOnly && !coveredRange.containsAll(range)) {
					considerMap = false;
				}

				/* if all criteria are satisfied, add map */
				if (considerMap) {
					mappings.add(map);
				}
			}
		} else {
			for (Collection<K> reducedDomain : powerset(domain)) {
				mappings.addAll(allMappings(reducedDomain, range, true, injectivesOnly, surjectivesOnly));
			}
			if (!surjectivesOnly) {
				mappings.add(new HashMap<>()); // add the empty mapping
			}
		}
		return mappings;
	}

	public static <K, V> Collection<Map<K, V>> allTotalMappings(final Collection<K> domain, final Collection<V> range) throws InterruptedException {
		return allMappings(domain, range, true, false, false);
	}

	public static <K, V> Collection<Map<K, V>> allPartialMappings(final Collection<K> domain, final Collection<V> range) throws InterruptedException {
		return allMappings(domain, range, false, false, false);
	}

	/**
	 * Computes all total mappings that satisfy some given predicate. The predicate is already applied to the partial mappings from which the total mappings are computed in order to prune and speed up
	 * the computation.
	 *
	 * @param domain
	 *            The domain set.
	 * @param range
	 *            The range set.
	 * @param pPredicate
	 *            The predicate that is evaluated for every partial
	 * @return All partial mappings from the domain set to the range set.
	 */
	public static <K, V> Set<Map<K, V>> allTotalAndInjectiveMappingsWithConstraint(final Collection<K> domain, final Collection<V> range, final Predicate<Map<K, V>> pPredicate) throws InterruptedException {
		Set<Map<K, V>> mappings = new HashSet<>();
		if (domain.isEmpty()) {
			return mappings;
		}

		/* now run breadth first search */
		List<K> domainAsList = new ArrayList<>(domain);
		int domainSize = domainAsList.size();
		List<Map<K, V>> open = new ArrayList<>();
		open.add(new HashMap<>());
		while (!open.isEmpty()) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException("Interrupted during calculation of allTotalMappingsWithConstraint.");
			}
			Map<K, V> partialMap = open.get(0);
			open.remove(0);

			/* add partial map if each key has a value assigned (map is total) */
			int index = partialMap.keySet().size();
			if (index >= domainSize) {
				mappings.add(partialMap);
				continue;
			}

			/* add new assignment to partial map */
			K key = domainAsList.get(index);
			for (V val : range) {

				/* due to injectivity, skip this option */
				if (partialMap.containsValue(val)) {
					continue;
				}
				Map<K, V> extendedMap = new HashMap<>(partialMap);
				extendedMap.put(key, val);
				if (pPredicate.test(extendedMap)) {
					open.add(extendedMap);
				}
			}
		}
		return mappings;

	}

	public static <K, V> Set<Map<K, V>> allTotalMappingsWithLocalConstraints(final Collection<K> domain, final Collection<V> range, final Predicate<Pair<K, V>> pPredicate) throws InterruptedException {
		Map<K, Collection<V>> pairsThatSatisfyCondition = relationAsFunction(domain, range, pPredicate);
		return allFuntionsFromFunctionallyDenotedRelation(pairsThatSatisfyCondition);

	} // allPartialMappings

	public static <K, V> Set<Map<K, V>> allFuntionsFromFunctionallyDenotedRelation(final Map<K, Collection<V>> pRelation) throws InterruptedException {
		return allFunctionsFromFunctionallyDenotedRelationRewritingReference(new HashMap<>(pRelation));
	}

	private static <K, V> Set<Map<K, V>> allFunctionsFromFunctionallyDenotedRelationRewritingReference(final Map<K, Collection<V>> pRelation) throws InterruptedException {
		Set<Map<K, V>> out = new HashSet<>();
		if (pRelation.isEmpty()) {
			out.add(new HashMap<>(0, 1.0f));
			return out;
		}

		/* compute all pairs that share one particular entry as key */
		K firstKey = pRelation.keySet().iterator().next();
		Collection<V> vals = pRelation.get(firstKey);
		pRelation.remove(firstKey);

		/* if the domain has size 1 or 0, return the set of mappings for the element in the domain */
		if (pRelation.isEmpty()) {
			for (V val : vals) {
				final Map<K, V> mapWithOneEntry = new HashMap<>(1);
				mapWithOneEntry.put(firstKey, val);
				out.add(mapWithOneEntry);
			}
		}

		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedException("Interrupted during allFunctionsFromFunctionallyDenotedRelationRewritingReference");
		}
		/* otherwise decompose by recursion */
		else {
			Set<Map<K, V>> recursivelyObtainedFunctions = allFunctionsFromFunctionallyDenotedRelationRewritingReference(pRelation);
			for (Map<K, V> func : recursivelyObtainedFunctions) {
				for (V val : vals) {
					Map<K, V> newFunc = new HashMap<>(func);
					newFunc.put(firstKey, val);
					out.add(newFunc);
				}
			}
		}
		return out;
	}

	/* ORDER OPERATIONS (SHUFFLE, SORT, PERMUTATE) */
	public static <T> void shuffle(final List<T> list, final long seed) {

		/* preliminaries */
		List<Integer> unusedItems = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			unusedItems.add(i);
		}
		List<T> copy = new ArrayList<>();
		copy.addAll(list);
		list.clear();

		/* select randomly from unusedItems until unusedItems is empty */
		while (!unusedItems.isEmpty()) {
			int index = new Random(seed).nextInt(unusedItems.size());
			list.add(copy.get(unusedItems.get(index)));
		}
	}

	public static <T> T getRandomElement(final Collection<T> set, final long seed) {
		return getRandomElement(set, new Random(seed));
	}

	public static <T> T getRandomElement(final Collection<T> set, final Random random) {
		int choice = random.nextInt(set.size());
		if (set instanceof List) {
			return ((List<T>) set).get(choice);
		}
		int i = 0;
		for (T elem : set) {
			if (i++ == choice) {
				return elem;
			}
		}
		return null;
	}

	public static <T> T getRandomElement(final List<T> list, final Random random, final List<Double> probabilityVector) {

		/* sanity check */
		int n = list.size();
		if (probabilityVector.size() != n) {
			throw new IllegalArgumentException("Probability vector should have length " + n + " but has " + probabilityVector.size());
		}

		/* normalize probabilities if necessary */
		double alpha = probabilityVector.stream().reduce((a, b) -> a + b).get();
		List<Double> probabilities = alpha == 1 ? probabilityVector : probabilityVector.stream().map(d -> d / alpha).collect(Collectors.toList());

		/* draw random number and loop over elements until the accumulated density is the desired one */
		double randomNumber = random.nextDouble();
		double accumulatedProbability = 0;
		for (int i = 0; i < n; i++) {
			accumulatedProbability += probabilities.get(i);
			if (accumulatedProbability >= randomNumber) {
				return list.get(i);
			}
		}
		throw new IllegalStateException("Probability has been accumulated to " + accumulatedProbability + " but no element was returned.");
	}

	public static <T> Collection<T> getRandomSubset(final Collection<T> set, final int k, final Random random) {
		List<T> copy = new ArrayList<>(set);
		Collections.shuffle(copy, random);
		return copy.stream().limit(k).collect(Collectors.toList());
	}

	public static Collection<Integer> getRandomSetOfIntegers(final int maxExclusive, final int k, final Random random) {
		List<Integer> ints = new ArrayList<>(k);
		IntStream.range(0, maxExclusive).forEach(ints::add);
		return getRandomSubset(ints, k, random);
	}

	public static <T extends Comparable<T>> List<T> mergeSort(final Collection<T> set) {
		if (set.isEmpty()) {
			return new ArrayList<>();
		}
		if (set.size() == 1) {
			List<T> result = new ArrayList<>();
			result.addAll(set);
			return result;
		}

		/* create sublists */
		List<T> sublist1 = new ArrayList<>();
		List<T> sublist2 = new ArrayList<>();
		int mid = (int) Math.ceil(set.size() / 2.0);
		int i = 0;
		for (T elem : set) {
			if (i++ < mid) {
				sublist1.add(elem);
			} else {
				sublist2.add(elem);
			}
		}

		/* sort sublists */
		return mergeLists(mergeSort(sublist1), mergeSort(sublist2));
	}

	private static <T extends Comparable<T>> List<T> mergeLists(final List<T> list1, final List<T> list2) {
		List<T> result = new ArrayList<>();
		while (!list1.isEmpty() && !list2.isEmpty()) {
			if (list1.get(0).compareTo(list2.get(0)) < 0) {
				result.add(list1.get(0));
				list1.remove(0);
			} else {
				result.add(list2.get(0));
				list2.remove(0);
			}
		}
		while (!list1.isEmpty()) {
			result.add(list1.get(0));
			list1.remove(0);
		}
		while (!list2.isEmpty()) {
			result.add(list2.get(0));
			list2.remove(0);
		}
		return result;
	}

	public static <K, V extends Comparable<V>> List<K> keySetSortedByValues(final Map<K, V> map, final boolean asc) {
		if (map.isEmpty()) {
			return new ArrayList<>();
		}
		if (map.size() == 1) {
			List<K> result = new ArrayList<>();
			result.addAll(map.keySet());
			return result;
		}

		/* create submaps */
		Map<K, V> submap1 = new HashMap<>();
		Map<K, V> submap2 = new HashMap<>();
		int mid = (int) Math.ceil(map.size() / 2.0);
		int i = 0;
		for (Entry<K, V> entry : map.entrySet()) {
			if (i++ < mid) {
				submap1.put(entry.getKey(), entry.getValue());
			} else {
				submap2.put(entry.getKey(), entry.getValue());
			}
		}

		/* sort sublists */
		return mergeMaps(keySetSortedByValues(submap1, asc), keySetSortedByValues(submap2, asc), map, asc);
	}

	private static <K, V extends Comparable<V>> List<K> mergeMaps(final List<K> keys1, final List<K> keys2, final Map<K, V> map, final boolean asc) {
		List<K> result = new ArrayList<>();
		while (!keys1.isEmpty() && !keys2.isEmpty()) {
			double comp = map.get(keys1.get(0)).compareTo(map.get(keys2.get(0)));
			if (asc && comp < 0 || !asc && comp >= 0) {
				result.add(keys1.get(0));
				keys1.remove(0);
			} else {
				result.add(keys2.get(0));
				keys2.remove(0);
			}
		}
		while (!keys1.isEmpty()) {
			result.add(keys1.get(0));
			keys1.remove(0);
		}
		while (!keys2.isEmpty()) {
			result.add(keys2.get(0));
			keys2.remove(0);
		}
		return result;
	}

	public static int calculateNumberOfTotalOrderings(final PartialOrderedSet<?> set) throws InterruptedException {
		return getAllTotalOrderings(set).size();
	}

	public static <E> Collection<List<E>> getAllTotalOrderings(final PartialOrderedSet<E> set) throws InterruptedException {

		/* for an empty set, create a list that only contains the empty list */
		if (set.isEmpty()) {
			return Arrays.asList(new ArrayList<>());
		}

		/* otherwise get the list of all elements that could be the last item and fix them once */
		Collection<List<E>> candidates = new ArrayList<>();
		Map<E, Set<E>> order = new HashMap<>(set.getOrder());
		set.getLinearization();
		Collection<E> itemsWithoutSuccessor = set.stream().filter(s -> !order.containsKey(s) || order.get(s).isEmpty()).collect(Collectors.toList());
		for (E item : itemsWithoutSuccessor) {

			/* create a new set without the item; this basically means that we enforce that it will be the last item */
			PartialOrderedSet<E> reducedSet = new PartialOrderedSet<>(set);
			reducedSet.remove(item);

			/* now get all ordering for the reduced set */
			for (List<E> completionOfReducedSet : getAllTotalOrderings(reducedSet)) {
				completionOfReducedSet.add(item);
				candidates.add(completionOfReducedSet);
			}
		}
		return candidates;
	}

	public static String serializeAsSet(final Collection<String> set) {
		return set.toString().replace("\\[", "{").replace("\\]", "}");
	}

	public static Set<String> unserializeSet(final String setDescriptor) {
		Set<String> items = new HashSet<>();
		for (String item : setDescriptor.substring(1, setDescriptor.length() - 1).split(",")) {
			if (!item.trim().isEmpty()) {
				items.add(item.trim());
			}
		}
		return items;
	}

	public static List<String> unserializeList(final String listDescriptor) {
		if (listDescriptor == null) {
			throw new IllegalArgumentException("Invalid list descriptor NULL.");
		}
		if (!listDescriptor.startsWith("[") || !listDescriptor.endsWith("]")) {
			throw new IllegalArgumentException("Invalid list descriptor \"" + listDescriptor + "\". Must start with '[' and end with ']'");
		}
		List<String> items = new ArrayList<>();
		for (String item : listDescriptor.substring(1, listDescriptor.length() - 1).split(",")) {
			if (!item.trim().isEmpty()) {
				items.add(item.trim());
			}
		}
		return items;
	}

	public static Interval unserializeInterval(final String intervalDescriptor) {
		List<String> interval = unserializeList(intervalDescriptor);
		double min = Double.parseDouble(interval.get(0));
		return new Interval(min, interval.size() == 1 ? min : Double.valueOf(interval.get(1)));
	}

	public static <T> List<T> getInvertedCopyOfList(final List<T> list) {
		List<T> copy = new ArrayList<>();
		int n = list.size();
		for (int i = 0; i < n; i++) {
			copy.add(list.get(n - i - 1));
		}
		return copy;
	}

	public static <T> List<T> addAndGet(final List<T> list, final T item) {
		list.add(item);
		return list;
	}

	public static <T, U> Map<U, Collection<T>> groupCollectionByAttribute(final Collection<T> collection, final IGetter<T, U> getter) throws InterruptedException, GetPropertyFailedException {
		Map<U, Collection<T>> groupedCollection = new HashMap<>();
		for (T i : collection) {
			groupedCollection.computeIfAbsent(getter.getPropertyOf(i), t -> new ArrayList<>()).add(i);
		}
		return groupedCollection;
	}

	/**
	 * Splits a string into multiple strings using "," as a separator and returns the result as a list.
	 *
	 * @param stringList The list in the form of a string.
	 * @return
	 */
	public static List<String> explode(final String stringList) {
		return explode(stringList, DEFAULT_LIST_ITEM_SEPARATOR);
	}

	/**
	 * Splits a string into multiple strings by the given separator and returns the result as a list.
	 *
	 * @param stringList The list in the form of a string.
	 * @param separator The separator to be used for splitting.
	 * @return The list representing the split string.
	 */
	public static List<String> explode(final String stringList, final String separator) {
		return Arrays.stream(stringList.split(separator)).collect(Collectors.toList());
	}

	/**
	 * Concatenates toString representations of objects separated by the given separator to a single string.
	 * @param collection The collection of objects to be concatenated.
	 * @param separator The separator for separating elements.
	 * @return The collection of objects concatenated to a string.
	 */
	public static String implode(final Collection<? extends Object> collection, final String separator) {
		StringBuilder sb = new StringBuilder();

		boolean first = true;
		for (Object o : collection) {
			if (first) {
				first = false;
			} else {
				sb.append(separator);
			}
			sb.append(o + "");
		}

		return sb.toString();
	}

	public static boolean doesStringCollectionOnlyContainNumbers(final Collection<String> strings) {
		try {
			for (String s : strings) {
				Double.parseDouble(s);
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	public static Type getGenericClass(final Collection<?> c) {
		ParameterizedType stringListType = (ParameterizedType) c.getClass().getGenericSuperclass();
		return stringListType.getActualTypeArguments()[0];
	}

	public static <T extends Comparable<T>> int argmax(final List<T> list) {
		int n = list.size();
		T best = null;
		int index = -1;
		for (int i = 0; i < n; i++) {
			T x = list.get(i);
			if (best == null || best.compareTo(x) > 0) {
				best = x;
				index = i;
			}
		}
		return index;
	}

	public static <T extends Comparable<T>> int argmax(final T[] arr) {
		return argmax(Arrays.asList(arr));
	}

	public static <T extends Comparable<T>> int argmin(final List<T> list) {
		int n = list.size();
		T best = null;
		int index = -1;
		for (int i = 0; i < n; i++) {
			T x = list.get(i);
			if (best == null || best.compareTo(x) < 0) {
				best = x;
				index = i;
			}
		}
		return index;
	}

	public static <T extends Comparable<T>> int argmin(final T[] arr) {
		return argmin(Arrays.asList(arr));
	}

	public static int argmin(final int[] arr) {
		int minIndex = -1;
		int min = Integer.MAX_VALUE;
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] < min) {
				min = arr[i];
				minIndex = i;
			}
		}
		return minIndex;
	}

	public static int argmax(final int[] arr) {
		int maxIndex = -1;
		int max = Integer.MIN_VALUE;
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] > max) {
				max = arr[i];
				maxIndex = i;
			}
		}
		return maxIndex;
	}

	public static <T> Collection<List<T>> getSubGridRelationFromDomains(final List<List<T>> hypercubeDomains, final int numSamples) {
		return getSubGridRelationFromRelation(cartesianProduct(hypercubeDomains), numSamples);
	}

	public static <T> Collection<List<T>> getSubGridRelationFromRelation(final Collection<List<T>> relation, final int numSamples) {

		/* determine total number of entries of the hypercube */
		long totalSize = relation.size();
		if (totalSize < numSamples) {
			throw new IllegalArgumentException("Cannot generate a sample of size " + numSamples + " for a hypercube with only " + totalSize + " entries.");
		}
		int stepSize = (int) Math.floor(totalSize * 1.0 / numSamples);

		/* compute full hypercube */
		int i = 0;
		Collection<List<T>> subSample = new ArrayList<>();
		for (List<T> tuple : relation) {
			if (i % stepSize == 0) {
				subSample.add(tuple);
			}
			i++;
			if (subSample.size() == numSamples) {
				break;
			}
		}
		return subSample;
	}

	public static double sum(final Collection<? extends Number> nums) {
		double sum = 0;
		for (Number n : nums) {
			if (n == null) {
				return Double.NaN;
			}
			sum += n.doubleValue();
		}
		return sum;
	}

}