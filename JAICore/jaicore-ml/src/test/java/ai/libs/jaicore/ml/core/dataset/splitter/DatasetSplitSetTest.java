package ai.libs.jaicore.ml.core.dataset.splitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.api4.java.ai.ml.core.dataset.serialization.DatasetDeserializationFailedException;
import org.api4.java.ai.ml.core.dataset.splitter.SplitFailedException;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledInstance;
import org.api4.java.common.reconstruction.IReconstructible;
import org.api4.java.common.reconstruction.IReconstructionPlan;
import org.api4.java.common.reconstruction.ReconstructionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ai.libs.jaicore.basic.FileUtil;
import ai.libs.jaicore.ml.core.dataset.serialization.OpenMLDatasetReader;
import ai.libs.jaicore.ml.core.filter.SplitterUtil;
import ai.libs.jaicore.test.MediumTest;

public class DatasetSplitSetTest {
	private static ILabeledDataset<ILabeledInstance> d;

	private List<List<ILabeledDataset<ILabeledInstance>>> testSplits;
	private DatasetSplitSet<ILabeledDataset<ILabeledInstance>> splitSet;

	@BeforeAll
	public static void setup() throws DatasetDeserializationFailedException, InterruptedException {
		d = new OpenMLDatasetReader().deserializeDataset(3);
	}

	@BeforeEach
	public void loadSplits() {
		this.testSplits = Arrays.asList(Arrays.asList(d, d, d), Arrays.asList(d, d));
		this.splitSet = new DatasetSplitSet<>(this.testSplits);
	}

	@Test
	public void testAddSplit() {
		List<ILabeledDataset<ILabeledInstance>> newSplit = Arrays.asList(d, d, d, d);
		this.splitSet.addSplit(newSplit);
		assertEquals("Number of splits does not match", this.testSplits.size() + 1, this.splitSet.getNumberOfSplits());
		assertEquals("Last split does not match the shape of the added split", this.splitSet.getNumberOfFoldsForSplit(this.splitSet.getNumberOfSplits() - 1), newSplit.size());
	}

	@Test
	public void testGetNumberofFoldsPerSplit() {
		assertEquals("The number of folds does not match for the first split.", this.testSplits.get(0).size(), this.splitSet.getNumberOfFoldsPerSplit());
	}

	@Test
	public void testGetFolds() {
		for (int i = 0; i < this.splitSet.getNumberOfSplits(); i++) {
			assertEquals("The folds of split " + i + " do not match.", this.testSplits.get(i), this.splitSet.getFolds(i));
		}
	}

	@Test
	@MediumTest
	public void testReproducibilityOfStratifiedSplit() throws SplitFailedException, InterruptedException, IOException, ClassNotFoundException, ReconstructionException {
		ReproducibleSplit<ILabeledDataset<ILabeledInstance>> folds = (ReproducibleSplit<ILabeledDataset<ILabeledInstance>>) SplitterUtil.getLabelStratifiedTrainTestSplit(d, 0, .7);

		/* test reproducibility of the split object itself */
		assertTrue(folds instanceof IReconstructible);
		IReconstructionPlan reconstructionPlan = ((IReconstructible) folds).getConstructionPlan();
		String filename = "testrsc/tmp/tests/plan.tmp";
		FileUtil.serializeObject(reconstructionPlan, filename);
		IReconstructionPlan unserializedReproductionPlan = (IReconstructionPlan) FileUtil.unserializeObject(filename);
		ReproducibleSplit<ILabeledDataset<ILabeledInstance>> reproducedSplit = (ReproducibleSplit<ILabeledDataset<ILabeledInstance>>) unserializedReproductionPlan.reconstructObject();
		assertNotNull("Reproduction of split is NULL", reproducedSplit);
		assertTrue(reproducedSplit instanceof IReconstructible);
		int n = reconstructionPlan.getInstructions().size();
		for (int i = 0; i < n; i++) {
			assertEquals(reconstructionPlan.getInstructions().get(i), reproducedSplit.getConstructionPlan().getInstructions().get(i));
		}
		assertEquals(folds.getConstructionPlan(), reproducedSplit.getConstructionPlan());
		assertEquals(folds.getClass(), reproducedSplit.getClass());
		assertEquals(folds.size(), reproducedSplit.size());
		assertEquals(folds.get(0), reproducedSplit.get(0));
		assertEquals(folds.get(1), reproducedSplit.get(1));
		assertEquals(folds, reproducedSplit);

		/* test reproducibility of train fold */
		assertTrue(folds.get(0) instanceof IReconstructible);
		ILabeledDataset<?> reproducedTrainFold = (ILabeledDataset<?>) ((IReconstructible) folds.get(0)).getConstructionPlan().reconstructObject();
		assertNotNull("Reproduction of train fold is NULL", reproducedTrainFold);
		assertEquals(folds.get(0).get(0), reproducedTrainFold.get(0));
		assertEquals(folds.get(0), reproducedTrainFold);
		assertTrue(reproducedTrainFold instanceof IReconstructible);

		/* test reproducibility of test fold */
		assertTrue(folds.get(1) instanceof IReconstructible);
		ILabeledDataset<?> reproducedTestFold = (ILabeledDataset<?>) ((IReconstructible) folds.get(1)).getConstructionPlan().reconstructObject();
		assertNotNull("Reproduction of test fold is NULL", reproducedTestFold);
		assertEquals(folds.get(1).get(0), reproducedTestFold.get(0));
		assertEquals(folds.get(1), reproducedTestFold);
		assertTrue(reproducedTestFold instanceof IReconstructible);
	}
}
