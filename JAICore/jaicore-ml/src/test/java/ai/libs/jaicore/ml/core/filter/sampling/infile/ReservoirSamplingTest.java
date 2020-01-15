package ai.libs.jaicore.ml.core.filter.sampling.infile;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.api4.java.algorithm.IAlgorithm;

import ai.libs.jaicore.ml.core.filter.sampling.infiles.AFileSamplingAlgorithm;
import ai.libs.jaicore.ml.core.filter.sampling.infiles.ArffUtilities;
import ai.libs.jaicore.ml.core.filter.sampling.infiles.ReservoirSampling;

public class ReservoirSamplingTest extends GeneralFileSamplingTester {

	private static final long RANDOM_SEED = 1;

	@Override
	public IAlgorithm<?, ?> getAlgorithm(final Object problem) {
		File input = (File) problem;
		Random r = new Random(RANDOM_SEED);
		AFileSamplingAlgorithm algorithm = new ReservoirSampling(r, input);
		try {
			algorithm.setOutputFileName(OUTPUT_FILE_NAME);
			if (input != null) {
				int inputSize = ArffUtilities.countDatasetEntries(input, true);
				int sampleSize = (int) (inputSize * DEFAULT_SAMPLE_FRACTION);
				algorithm.setSampleSize(sampleSize);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return algorithm;
	}

}
