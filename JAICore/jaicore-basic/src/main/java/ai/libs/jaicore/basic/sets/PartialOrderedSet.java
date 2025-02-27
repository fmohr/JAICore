package ai.libs.jaicore.basic.sets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A {@link Set} with a partial order added to it.
 *
 * @author David Niehues - davnie@mail.upb.de
 */
public class PartialOrderedSet<E> extends HashSet<E> {

	/**
	 * Automatically generated version UID for serialization.
	 */
	private static final long serialVersionUID = 5450009458863214917L;
	/**
	 * The order of this set. For an a the b's with a < b are stored.
	 */
	private final Map<E, Set<E>> order;

	/**
	 * Creates a new partial ordered set with the same elements as
	 * <code>original</code> and the same order.
	 *
	 * @param original
	 *            The {@link PartialOrderedSet} to copy.
	 */
	public PartialOrderedSet(final PartialOrderedSet<E> original) {
		super(original);
		this.order = new HashMap<>();
		original.order.forEach((k, v) -> this.order.put(k, new HashSet<>(v)));
	}

	/**
	 * Creates a new empty {@link PartialOrderedSet}.
	 */
	public PartialOrderedSet() {
		this.order = new HashMap<>();
	}

	/**
	 * Adds the a < b to the relation.
	 *
	 * @param a
	 *            the smaller element
	 * @param b
	 *            the bigger element
	 *
	 * @throws IllegalStateException
	 *             if b < a is in the relation.
	 */
	public void addABeforeB(final E a, final E b) {
		if (!this.allowsABeforeB(a, b)) {
			throw new IllegalStateException("By transitivity " + a + " before " + b + "isn't allowed.");
		}
		if (!this.contains(a)) {
			this.add(a);
		}
		if (!this.contains(b)) {
			this.add(b);
		}
		Set<E> directlyAfterA = this.order.computeIfAbsent(a, k -> new HashSet<>());
		directlyAfterA.add(b);
	}

	/**
	 * Require that A is before b.
	 *
	 * @param a The A which is to be tested.
	 * @param b The B which is to be tested.
	 */
	public void requireABeforeB(final E a, final E b) {
		if (!this.allowsABeforeB(a, b)) {
			throw new IllegalStateException("By transitivity " + a + " before " + b + "isn't allowed.");
		}
		Set<E> directlyAfterA = this.order.computeIfAbsent(a, k -> new HashSet<>());
		directlyAfterA.add(b);
	}

	/**
	 * Tests if the relation allows for a < b.
	 *
	 * @param a
	 *            The element that is tested to be allowed before b.
	 * @param b
	 *            The element that is tested to be allowed after a.
	 * @return
	 */
	public boolean allowsABeforeB(final E a, final E b) {
		Set<E> transitiveClosure = this.getTransitiveClosure(b);
		return !transitiveClosure.contains(a);
	}

	/**
	 * Returns the map containing the forward dependencies defining the order.
	 *
	 * @return The map containing the order.
	 */
	public Map<E, Set<E>> getOrder() {
		return Collections.unmodifiableMap(this.order);
	}

	/**
	 * Tests whether a < b is directly, not just transitively, specified by the
	 * order.
	 *
	 * @param a
	 *            The first element.
	 * @param b
	 *            The second element.
	 * @return Whether the order specifies directly, that a < b has to hold.
	 */
	public boolean isADirectlyBeforeB(final E a, final E b) {
		final Set<E> directlyAfterA = this.order.get(a);
		if (directlyAfterA != null) {
			return directlyAfterA.contains(b);
		}
		return false;
	}

	/**
	 * Gets the transitive closure of an element under this relation.
	 *
	 * @param e
	 *            The element of which the transitive closure is asked for.
	 * @return The transitive closure of e.
	 * @throws NullPointerException
	 *             if e is null.
	 */
	public Set<E> getTransitiveClosure(final E e) {
		if (e == null) {
			throw new NullPointerException("'e' mustn't be null.");
		}
		Set<E> set = new HashSet<>();
		set.add(e);
		return this.getTransitiveClosure(set);
	}

	/**
	 * Gets the transitive closure of a set.
	 *
	 * @param subSet
	 *            The set of which the transitive closure is asked for.
	 * @return The transitive closure of subSet.
	 * @throws NullPointerException
	 *             if subSet is null.
	 */
	public Set<E> getTransitiveClosure(final Set<E> subSet) {
		if (subSet == null) {
			throw new NullPointerException("subSet mustn't be null.");
		}
		Set<E> transitiveClosure = new HashSet<>(subSet);
		for (E e : subSet) {
			Set<E> directlyAfterE = this.order.get(e);
			if (directlyAfterE != null) {
				transitiveClosure.addAll(directlyAfterE);
			}
		}
		if (subSet.containsAll(transitiveClosure)) {
			return transitiveClosure;
		}
		return this.getTransitiveClosure(transitiveClosure);
	}

	/**
	 * Tests whether the relation is reflexive.
	 *
	 * @return Whether the relation is reflexive.
	 */
	public boolean isReflexive() {
		for (E e : this) {
			if (!this.order.containsKey(e)) {
				return false;
			}
			if (!this.order.get(e).contains(e)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Creates a total order of the elements stored in this
	 * {@link PartialOrderedSet}. The order is created by iterating over all
	 * elements in the set and inserting each element in the list at the position of
	 * the element with the currently smallest index that has to be after the
	 * current element.
	 *
	 * If no elements needs to be after the current element, it is appended to the
	 * end. The runtime is O(n²).
	 *
	 *
	 * @return A total ordering of the elements in this {@link PartialOrderedSet}.
	 */
	public List<E> getTotalOrder() {
		final List<E> list = new LinkedList<>();
		for (E element : this) { // O(n)
			Set<E> followingElements = this.order.get(element); // O(?)
			int index = list.size();
			if (followingElements != null) {
				for (E followingElement : followingElements) {
					int followingIndex = list.indexOf(followingElement); // O(n)
					if (followingIndex != -1 && followingIndex < index) {
						index = followingIndex;
					}
				}
			}
			list.add(index, element); // O(n)
		}
		return list;
	}

	/**
	 * If the collection is a {@link PartialOrderedSet}, the order will also be
	 * added. If the that would destroy asymmetry an {@link IllegalStateException}
	 * will be thrown.
	 *
	 * @throws IllegalStateException
	 *             if adding the order of another {@link PartialOrderedSet} would
	 *             destroy asymmetry.
	 */
	public void merge(final PartialOrderedSet<? extends E> set) {
		super.addAll(set);
		PartialOrderedSet<? extends E> tmpSet = set;
		tmpSet.order.forEach((k, vSet) -> vSet.forEach(v -> this.addABeforeB(k, v)));
	}

	/**
	 * Adds all elements of the other partial order set.
	 *
	 * @param set The set to be added to this partial-order set.
	 */
	public void addAll(final PartialOrderedSet<? extends E> set) {
		this.merge(set);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		Iterator<E> it = this.iterator();
		while (it.hasNext()) {
			sb.append(it.next().toString());
			if (it.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append("} with order ");
		sb.append(this.order);
		it = this.order.keySet().iterator();
		while (it.hasNext()) {
			E e = it.next();
			Set<E> diretclyAfterE = this.order.get(e);
			Iterator<E> innerIt = diretclyAfterE.iterator();

			while (innerIt.hasNext()) {
				sb.append(e.toString());
				sb.append(" < ");
				sb.append(innerIt.next().toString());
				if (innerIt.hasNext()) {
					sb.append(", ");
				}
			}

			if (it.hasNext()) {
				sb.append("; ");
			}
		}
		return sb.toString();
	}

	/**
	 * Getter for a linearization of the partial order set.
	 *
	 * @return A list representing a linearization of the partial order set.
	 */
	public List<E> getLinearization() {

		if (this.isEmpty()) {
			return new ArrayList<>();
		}

		/* create a copy of all elements */
		List<E> elements = new ArrayList<>();
		Iterator<E> iterator = super.iterator();
		while (iterator.hasNext()) {
			elements.add(iterator.next());
		}

		/* compute initial values of working variables */
		List<E> linearization = new ArrayList<>();
		Map<E, Set<E>> workingCopyOfOrder = new HashMap<>();
		this.order.forEach((k, v) -> workingCopyOfOrder.put(k, new HashSet<>(v))); // create a deep copy
		Collection<E> itemsWithoutSuccessor = elements.stream().filter(s -> !workingCopyOfOrder.containsKey(s) || workingCopyOfOrder.get(s).isEmpty()).collect(Collectors.toList());
		Collection<E> uninsertedItems = new HashSet<>(elements);

		/* if all items have a successor, we have a cycle. Return that order then. */
		if (itemsWithoutSuccessor.isEmpty()) {
			throw new IllegalStateException("Partially Oredered Set contains a cycle: " + workingCopyOfOrder);
		}

		/* now compute the linearization from the back */
		while (!itemsWithoutSuccessor.isEmpty()) {
			List<E> itemsToInsert = new ArrayList<>(itemsWithoutSuccessor);
			itemsWithoutSuccessor.clear();
			for (E itemToInsert : itemsToInsert) {
				if (linearization.contains(itemToInsert)) {
					continue;
				}
				assert !linearization.contains(itemToInsert) : "The object " + itemToInsert + " is already contained in the linearization " + linearization;
				linearization.add(0, itemToInsert);
				uninsertedItems.remove(itemToInsert);
				for (E uninsertedItem : uninsertedItems) {
					if (workingCopyOfOrder.containsKey(uninsertedItem)) {
						workingCopyOfOrder.get(uninsertedItem).remove(itemToInsert);
						if (workingCopyOfOrder.get(uninsertedItem).isEmpty()) {
							itemsWithoutSuccessor.add(uninsertedItem);
						}
					}
				}
			}
		}

		/* consistency check */
		assert linearization.size() == super.size() : "The linearization of " + elements + " with order " + this.order + " has produced another number of elements: " + linearization.toString();
		return linearization;
	}

	@Override
	public Iterator<E> iterator() {
		return this.getLinearization().iterator();
	}

	@Override
	public void clear() {
		super.clear();
		this.order.clear();
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		for (Object o : c) {
			this.order.remove(o);
		}
		return super.removeAll(c);
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		Iterator<E> it = this.order.keySet().iterator();
		while (it.hasNext()) {
			E e = it.next();
			if (!c.contains(e)) {
				// this also removes from the map, not just from the set.
				it.remove();
			}
		}
		return super.retainAll(c);
	}

	@Override
	public boolean removeIf(final Predicate<? super E> filter) {
		Objects.requireNonNull(filter);
		boolean removed = false;
		final Iterator<E> each = this.order.keySet().iterator();
		while (each.hasNext()) {
			E next = each.next();
			if (filter.test(next)) {
				this.remove(next);
				removed = true;
			}
		}
		return removed;
	}

	@Override
	public boolean remove(final Object e) {
		this.order.remove(e);
		List<E> emptyDependencies = new ArrayList<>();
		for (Entry<E, Set<E>> dependency : this.order.entrySet()) {
			dependency.getValue().remove(e);
			if (dependency.getValue().isEmpty()) {
				emptyDependencies.add(dependency.getKey());
			}
		}
		emptyDependencies.forEach(this.order::remove);
		return super.remove(e);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.order == null) ? 0 : this.order.hashCode());
		return result;
	}

	@SuppressWarnings("rawtypes")
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
		PartialOrderedSet other = (PartialOrderedSet) obj;
		if (this.order == null) {
			if (other.order != null) {
				return false;
			}
		} else if (!this.order.equals(other.order)) {
			return false;
		}
		return true;
	}
}
