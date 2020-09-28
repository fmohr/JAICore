package ai.libs.mlplan.cli.module;

import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.api4.java.ai.ml.core.dataset.supervised.ILabeledDataset;
import org.api4.java.ai.ml.core.evaluation.execution.ILearnerRunReport;
import org.api4.java.ai.ml.core.learner.ISupervisedLearner;

import ai.libs.mlplan.cli.MLPlanCLI;
import ai.libs.mlplan.core.AMLPlanBuilder;

public interface IMLPlanCLIModule {

	public AMLPlanBuilder getMLPlanBuilderForSetting(CommandLine cl, ILabeledDataset fitDataset) throws IOException;

	public String getRunReportAsString(ISupervisedLearner learner, ILearnerRunReport runReport);

	public List<String> getSettingOptionValues();

	public String getDefaultSettingOptionValue();

	public List<String> getPerformanceMeasures();

	public String getDefaultPerformanceMeasure();

	default String getPerformanceMeasure(final CommandLine cl) {
		return cl.getOptionValue(MLPlanCLI.O_LOSS, this.getDefaultPerformanceMeasure());
	}

}
