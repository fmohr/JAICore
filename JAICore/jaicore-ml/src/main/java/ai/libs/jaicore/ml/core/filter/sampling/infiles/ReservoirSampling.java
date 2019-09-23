package ai.libs.jaicore.ml.core.filter.sampling.infiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import org.api4.java.algorithm.events.AlgorithmEvent;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;

import ai.libs.jaicore.ml.core.dataset.ArffUtilities;
import ai.libs.jaicore.ml.core.filter.sampling.SampleElementAddedEvent;

/**
 * Implementation of the Reservoir Sampling algorithm(comparable to a Simple
 * Random Sampling for streamed data). For a desired sample of size n, the first
 * n elements of the stream are directly selected and the remaining ones will
 * replace these with decreasing probability.
 *
 * @author Lukas Brandt
 */
public class ReservoirSampling extends AFileSamplingAlgorithm {

	private Random random;
	private BufferedReader reader;
	private int datapointAmount;
	private int streamedDatapoints;
	private String[] sampledDatapoints;

	public ReservoirSampling(final Random random, final File input) {
		super(input);
		this.random = random;
	}

	@Override
	public AlgorithmEvent nextWithException()
			throws InterruptedException, AlgorithmExecutionCanceledException, AlgorithmException {
		switch (this.getState()) {
		case CREATED:
			// Initialize variables.
			try {
				this.datapointAmount = ArffUtilities.countDatasetEntries(this.getInput(), true);
				this.streamedDatapoints = 0;
				this.sampledDatapoints = new String[this.sampleSize];
				this.reader = new BufferedReader(new FileReader(this.getInput()));
				ArffUtilities.skipWithReaderToDatapoints(this.reader);
				return this.activate();
			} catch (IOException e) {
				throw new AlgorithmException("Was not able to count the datapoints.", e);
			}
		case ACTIVE:
			if (this.streamedDatapoints < this.datapointAmount) {
				try {
					// Get current datapoint.
					String datapoint = this.reader.readLine();
					if (datapoint != null && datapoint.trim().length() > 0 && datapoint.trim().charAt(0) != '%') {
						if (this.streamedDatapoints < this.sampleSize) {
							// Take the first n elements directly for the sample.
							this.sampledDatapoints[this.streamedDatapoints] = datapoint.trim();
						} else {
							// Replace elements with decreasing probability.
							int j = this.random.nextInt(this.streamedDatapoints);
							if (j < this.sampleSize) {
								this.sampledDatapoints[j] = datapoint.trim();
							}
						}
					}
					this.streamedDatapoints++;
					return new SampleElementAddedEvent(this.getId());
				} catch (IOException e) {
					throw new AlgorithmException("Was not able to read datapoint line from input file", e);
				}
			} else {
				try {
					this.reader.close();
					// Write sampled datapoints into output file and terminate.
					for (int i = 0; i < this.sampledDatapoints.length; i++) {
						this.outputFileWriter.write(this.sampledDatapoints[i] + "\n");
					}
					return this.terminate();
				} catch (IOException e) {
					throw new AlgorithmException("Was not able to write sampled datapoints into output files.", e);
				}
			}
		case INACTIVE:
			if (this.streamedDatapoints < this.datapointAmount) {
				throw new AlgorithmException("Expected sample size was not reached before termination");
			} else {
				return this.terminate();
			}
		default:
			throw new IllegalStateException("Unknown algorithm state " + this.getState());
		}
	}

	@Override
	protected void cleanUp() {
	}

}
