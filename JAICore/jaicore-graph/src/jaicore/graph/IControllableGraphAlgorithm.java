package jaicore.graph;

public interface IControllableGraphAlgorithm<V,E> extends IObservableGraphAlgorithm<V, E> {

	/**
	 * Conduct the next step
	 */
	public void step();
	
	/**
	 * Conduct the next step but enforce that the object on which it is conducted is the given node
	 * @param node
	 */
	public void step(V node);
}
