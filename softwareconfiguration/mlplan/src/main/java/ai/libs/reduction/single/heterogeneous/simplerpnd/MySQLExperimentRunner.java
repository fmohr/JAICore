package ai.libs.reduction.single.heterogeneous.simplerpnd;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import ai.libs.jaicore.basic.SQLAdapter;
import ai.libs.reduction.Util;
import ai.libs.reduction.single.MySQLReductionExperiment;
import ai.libs.reduction.single.ReductionExperiment;

public class MySQLExperimentRunner {

	private static final String ERROR_RATE_MIN = "error_rate_min";
	private static final String ERROR_RATE_MAX = "error_rate_max";
	private static final String ERROR_RATE_MEAN = "error_rate_mean";
	private static final String ERROR_RATE_STD = "error_rate_std";
	private static final String RUNTIME_MIN = "runtime_min";
	private static final String RUNTIME_MAX = "runtime_max";
	private static final String RUNTIME_MEAN = "runtime_mean";
	private static final String RUNTIME_STD = "runtime_std";

	private static final String TABLE_NAME = "reductionstumps";
	private final SQLAdapter adapter;
	private final Collection<MySQLReductionExperiment> knownExperiments = new HashSet<>();

	public MySQLExperimentRunner(final String host, final String user, final String password, final String database) throws SQLException {
		this.adapter = new SQLAdapter(host, user, password, database);
		this.knownExperiments.addAll(this.getConductedExperiments());
	}

	public Collection<MySQLReductionExperiment> getConductedExperiments() throws SQLException {
		Collection<MySQLReductionExperiment> experiments = new HashSet<>();
		ResultSet rs = this.adapter.getRowsOfTable(TABLE_NAME);
		while (rs.next()) {
			experiments.add(new MySQLReductionExperiment(rs.getInt("evaluation_id"), new ReductionExperiment(rs.getInt("seed"), rs.getString("dataset"), rs.getString("left_classifier"), rs.getString("inner_classifier"), rs.getString("right_classifier"), rs.getString("exception_left"), rs.getString("exception_inner"), rs.getString("exception_right"))));
		}
		return experiments;
	}

	public MySQLReductionExperiment createAndGetExperimentIfNotConducted(final int seed, final File dataFile, final String nameOfLeftClassifier, final String nameOfInnerClassifier,
			final String nameOfRightClassifier) throws SQLException {

		/* first check whether exactly the same experiment (with the same seed) has been conducted previously */
		ReductionExperiment exp = new ReductionExperiment(seed, dataFile.getAbsolutePath(), nameOfLeftClassifier, nameOfInnerClassifier, nameOfRightClassifier);
		Optional<MySQLReductionExperiment> existingExperiment = this.knownExperiments.stream().filter(e -> e.getExperiment().equals(exp)).findAny();
		if (existingExperiment.isPresent()) {
			return null;
		}

		Map<String, String> map = new HashMap<>();
		map.put("seed", String.valueOf(seed));
		map.put("dataset", dataFile.getAbsolutePath());
		map.put("rpnd_classifier", nameOfInnerClassifier);
		map.put("left_classifier", nameOfLeftClassifier);
		map.put("inner_classifier", nameOfInnerClassifier);
		map.put("right_classifier", nameOfRightClassifier);
		int id = this.adapter.insert(TABLE_NAME, map);
		return new MySQLReductionExperiment(id, exp);

	}

	private void updateExperiment(final MySQLReductionExperiment exp, final Map<String,? extends Object> values) throws SQLException {
		Map<String,String> where = new HashMap<>();
		where.put("evaluation_id", String.valueOf(exp.getId()));
		this.adapter.update(TABLE_NAME, values, where);
	}

	public void conductExperiment(final MySQLReductionExperiment exp) throws Exception {
		List<Map<String,Object>> mccvResults = Util.conductSingleOneStepReductionExperiment(exp.getExperiment());
		DescriptiveStatistics errorRate = new DescriptiveStatistics();
		DescriptiveStatistics runtime = new DescriptiveStatistics();
		for (Map<String,Object> result : mccvResults) {
			errorRate.addValue((double)result.get("errorRate"));
			runtime.addValue((long)result.get("trainTime"));
		}

		/* prepapre values for experiment update */
		Map<String, Object> values = new HashMap<>();
		values.put(ERROR_RATE_MIN, errorRate.getMin());
		values.put(ERROR_RATE_MAX, errorRate.getMax());
		values.put(ERROR_RATE_MEAN, errorRate.getMean());
		values.put(ERROR_RATE_STD, errorRate.getStandardDeviation());
		values.put(RUNTIME_MIN, runtime.getMin());
		values.put(RUNTIME_MAX, runtime.getMax());
		values.put(RUNTIME_MEAN, runtime.getMean());
		values.put(RUNTIME_STD, runtime.getStandardDeviation());
		this.updateExperiment(exp, values);
	}

	public void markExperimentAsUnsolvable(final MySQLReductionExperiment exp) throws SQLException {
		Map<String, String> values = new HashMap<>();
		for (String key : new String[] {ERROR_RATE_MIN, ERROR_RATE_MAX, ERROR_RATE_MEAN, ERROR_RATE_STD, RUNTIME_MIN, RUNTIME_MAX, RUNTIME_MEAN, RUNTIME_STD }) {
			values.put(key, "-1");
		}
		this.updateExperiment(exp, values);
	}

	public void associateExperimentWithException(final MySQLReductionExperiment exp, final String classifier, final Throwable e) throws SQLException {
		Map<String, String> values = new HashMap<>();
		for (String key : new String[] {ERROR_RATE_MIN, ERROR_RATE_MAX, ERROR_RATE_MEAN, ERROR_RATE_STD, RUNTIME_MIN, RUNTIME_MAX, RUNTIME_MEAN, RUNTIME_STD }) {
			values.put(key, "-1");
		}
		values.put("exception_" + classifier, e.getClass().getName() + "\n" + e.getMessage());
		this.updateExperiment(exp, values);
	}
}
