package ai.libs.jaicore.ml.scikitwrapper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.api4.java.ai.ml.classification.singlelabel.evaluation.ISingleLabelClassification;
import org.api4.java.ai.ml.classification.singlelabel.evaluation.ISingleLabelClassificationPredictionBatch;
import org.api4.java.ai.ml.core.dataset.schema.attribute.ICategoricalAttribute;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledInstance;
import org.api4.java.ai.ml.core.exception.PredictionException;
import org.api4.java.ai.ml.core.exception.TrainingException;

import ai.libs.jaicore.ml.classification.singlelabel.SingleLabelClassification;
import ai.libs.jaicore.ml.classification.singlelabel.SingleLabelClassificationPredictionBatch;
import ai.libs.jaicore.ml.core.EScikitLearnProblemType;

public class ScikitLearnClassificationWrapper extends AScikitLearnWrapper<ISingleLabelClassification, ISingleLabelClassificationPredictionBatch> {

	public ScikitLearnClassificationWrapper(final String pipeline, final String imports) throws IOException, InterruptedException {
		super(EScikitLearnProblemType.CLASSIFICATION, pipeline, imports);
	}

	@Override
	protected boolean doLabelsFitToProblemType(final ILabeledDataset<? extends ILabeledInstance> data) {
		return data.getLabelAttribute() instanceof ICategoricalAttribute;
	}

	@Override
	protected ISingleLabelClassificationPredictionBatch handleOutput(final File outputFile) throws PredictionException, TrainingException {
		List<List<Double>> rawLastPredictionResults = this.getRawPredictionResults(outputFile);
		if (!rawLastPredictionResults.isEmpty()) {
			if (rawLastPredictionResults.get(0).size() == 1) {
				int numClasses = ((ICategoricalAttribute) this.data.getLabelAttribute()).getLabels().size();
				return new SingleLabelClassificationPredictionBatch(rawLastPredictionResults.stream().flatMap(List::stream).map(x -> new SingleLabelClassification(numClasses, x.intValue())).collect(Collectors.toList()));
			}
			return new SingleLabelClassificationPredictionBatch(rawLastPredictionResults.stream().map(x -> x.stream().mapToDouble(y -> y).toArray()).map(SingleLabelClassification::new).collect(Collectors.toList()));
		}
		throw new PredictionException("Reading the output file lead to empty predictions.");
	}

}
