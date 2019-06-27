package ai.libs.jaicore.ml.weka.dataset.splitter;

import java.util.List;

import ai.libs.jaicore.ml.WekaUtil;
import weka.core.Instances;

/**
 * Makes use of the WekaUtil to split the data into a class-oriented stratified split preserving the class distribution.
 *
 * @author mwever
 */
public class MulticlassClassStratifiedSplitter implements IDatasetSplitter {

	@Override
	public List<Instances> split(final Instances data, final long seed, final double portions) throws SplitFailedException, InterruptedException {
		return WekaUtil.getStratifiedSplit(data, seed, portions);
	}

}
