package ai.libs.jaicore.ml.core.dataset.sampling.inmemory;

import java.io.File;
import java.io.IOException;

import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledInstance;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.DataSetDescription;

import ai.libs.jaicore.basic.algorithm.AAlgorithmTestProblemSet;
import ai.libs.jaicore.basic.algorithm.AlgorithmTestProblemSetCreationException;
import weka.core.Attribute;
import weka.core.converters.ConverterUtils.DataSource;

public class SamplingAlgorithmTestProblemSet extends AAlgorithmTestProblemSet<ILabeledDataset<ILabeledInstance>> {

	private static final String OPENML_API_KEY = "4350e421cdc16404033ef1812ea38c01";

	public SamplingAlgorithmTestProblemSet() {
		super("Sampling");
	}

	@Override
	public ILabeledDataset<ILabeledInstance> getSimpleProblemInputForGeneralTestPurposes() throws AlgorithmTestProblemSetCreationException {
		// Load whine quality data set
		try {
			return this.loadDatasetFromOpenML(287);
		} catch (IOException | ClassNotFoundException e) {
			throw new AlgorithmTestProblemSetCreationException(e);
		}
	}

	@Override
	public ILabeledDataset<ILabeledInstance> getDifficultProblemInputForGeneralTestPurposes() throws AlgorithmTestProblemSetCreationException {
		// Load higgs data set
		try {
			return this.loadDatasetFromOpenML(23512);
		} catch (IOException | ClassNotFoundException e) {
			throw new AlgorithmTestProblemSetCreationException(e);
		}
	}

	private ILabeledDataset<ILabeledInstance> loadDatasetFromOpenML(final int id) throws IOException, ClassNotFoundException {
		Dataset dataset = null;
		OpenmlConnector client = new OpenmlConnector();
		try {
			DataSetDescription description = client.dataGet(id);
			File file = client.datasetGet(description);
			DataSource source = new DataSource(file.getCanonicalPath());
			dataset = source.getDataSet();
			dataset.setClassIndex(dataset.numAttributes() - 1);
			Attribute targetAttribute = dataset.attribute(description.getDefault_target_attribute());
			dataset.setClassIndex(targetAttribute.index());
		} catch (Exception e) {
			throw new IOException("Could not load data set from OpenML!", e);
		}

		return new Dataset<>(dataset);
	}

}
