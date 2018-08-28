package hasco.metamining.factories;

import hasco.metamining.MetaMinerBasedSorter;
import jaicore.planning.graphgenerators.task.tfd.TFDNode;
import jaicore.search.algorithms.interfaces.IObservableORGraphSearch;
import jaicore.search.algorithms.standard.core.INodeEvaluator;
import jaicore.search.algorithms.standard.core.ORGraphSearch;
import jaicore.search.algorithms.standard.core.ORGraphSearchFactory;
import jaicore.search.algorithms.standard.lds.BestFirstLimitedDiscrepancySearch;
import jaicore.search.algorithms.standard.lds.LimitedDiscrepancySearch;
import jaicore.search.algorithms.standard.lds.NodeOrderList;
import jaicore.search.structure.core.GraphGenerator;

/**
 * @author Helena Graf
 *
 * @param <T>
 * @param <A>
 * @param <V>
 */
public class LimitedDiscrepancyOrGraphSearchFactory extends ORGraphSearchFactory<TFDNode, String, Double> {

	private MetaMinerBasedSorter sorter;
	
	public LimitedDiscrepancyOrGraphSearchFactory(MetaMinerBasedSorter sorter) {
		this.sorter = sorter;
	}

	@Override
	public IObservableORGraphSearch<TFDNode, String, Double> createSearch(final GraphGenerator<TFDNode, String> graphGenerator,
			final INodeEvaluator<TFDNode, Double> nodeEvaluator, final int numberOfCPUs) {
		IObservableORGraphSearch<TFDNode, String, Double> search = new LimitedDiscrepancySearch<TFDNode>(graphGenerator, sorter);
		return search;
	}
}
