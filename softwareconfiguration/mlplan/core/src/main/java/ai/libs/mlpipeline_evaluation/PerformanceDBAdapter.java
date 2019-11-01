package ai.libs.mlpipeline_evaluation;

import java.io.Closeable;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.jaicore.basic.SQLAdapter;
import ai.libs.jaicore.basic.kvstore.IKVStore;
import ai.libs.jaicore.ml.weka.dataset.ReproducibleInstances;

/**
 * Database adapter for performance data. Functionality to store and save performance values in a database. json to reproduce the {@link ReproducibleInstances} is saved as well as the solution that produced the performance value.
 *
 * @author jmhansel
 *
 */
public class PerformanceDBAdapter implements Closeable {
	/** Logger for controlled output. */
	private static final Logger logger = LoggerFactory.getLogger(PerformanceDBAdapter.class);

	private final SQLAdapter sqlAdapter;
	private final String performanceSampleTableName;

	public PerformanceDBAdapter(final SQLAdapter sqlAdapter, final String performanceSampleTableName) {
		this.sqlAdapter = sqlAdapter;
		this.performanceSampleTableName = performanceSampleTableName;

		/* initialize tables if not existent */
		try {
			List<IKVStore> rs = sqlAdapter.getResultsOfQuery("SHOW TABLES");
			boolean hasPerformanceTable = false;

			for (IKVStore store : rs) {
				Optional<String> tableNameKeyOpt = rs.get(0).keySet().stream().filter(x -> x.startsWith("Tables_in")).findFirst();
				if (tableNameKeyOpt.isPresent()) {
					String tableName = store.getAsString(tableNameKeyOpt.get());
					if (tableName.equals(this.performanceSampleTableName)) {
						hasPerformanceTable = true;
					}
				}
			}

			// if there is no performance table, create it. we hash the composition and
			// trajectory and use the hash value as primary key for performance reasons.
			if (!hasPerformanceTable) {
				logger.info("Creating table for evaluations");
				sqlAdapter.update(
						"CREATE TABLE `" + this.performanceSampleTableName + "` (\r\n" + " `evaluation_id` int(10) NOT NULL AUTO_INCREMENT,\r\n" + " `composition` json NOT NULL,\r\n" + " `train_trajectory` json NOT NULL,\r\n"
								+ " `test_trajectory` json NOT NULL,\r\n" + " `loss_function` varchar(200) NOT NULL,\r\n" + " `score` double NOT NULL,\r\n" + " `evaluation_time_ms` bigint NOT NULL,\r\n"
								+ "`evaluation_date` timestamp NULL DEFAULT NULL," + "`hash_value` char(64) NOT NULL," + " PRIMARY KEY (`evaluation_id`)\r\n" + ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COLLATE=utf8_bin",
						new ArrayList<>());
			}

		} catch (SQLException e) {
			logger.error("Exception: {}", e);
		}

	}

	/**
	 * Checks whether there is an entry for the composition and corresponding evaluation specified by the reproducable instances. If so, it returns the corresponding performance score.
	 *
	 *
	 * @param composition
	 *            - Solution composition.
	 * @param reproducableInstances
	 *            - Instances object that includes the trajectory, i.e. all operations that have been applied to the instances like loading, splitting etc.
	 * @param testData
	 *            - The reproducible instances of the test data used for this evaluation process
	 * @param className
	 *            - the java qualified class name of the loss function that was used
	 * @return opt - Optional that contains the score corresponding to the composition and the reproducible instances or is empty if no suiting entry is found in the database.
	 */
	public Optional<Double> exists(final ComponentInstance composition, final ReproducibleInstances reproducibleInstances, final ReproducibleInstances testData, final String className) {
		Optional<Double> opt = Optional.empty();
		try {
			List<IKVStore> rs = this.getScoreOfCompositions(composition, reproducibleInstances, testData, className);
			for (IKVStore store : rs) {
				double score = store.getAsDouble("score");
				opt = Optional.of(score);
			}
		} catch (JsonProcessingException | SQLException | NoSuchAlgorithmException e) {
			logger.error("Observed exception during existence check: {}", e);
		}
		return opt;
	}

	private List<IKVStore> getScoreOfCompositions(final ComponentInstance composition, final ReproducibleInstances reproducibleInstances, final ReproducibleInstances testData, final String className)
			throws SQLException, JsonProcessingException, NoSuchAlgorithmException {
		String hexHash = this.getHexHash(this.getSettingObjectsAsString(composition, reproducibleInstances, testData), className);
		return this.sqlAdapter.getResultsOfQuery("SELECT score FROM " + this.performanceSampleTableName + " WHERE hash_value = '" + hexHash + "'");
	}

	private String getHexHash(final String[] settingValuesAsString, final String className) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(settingValuesAsString[0].getBytes());
		md.update(settingValuesAsString[1].getBytes());
		md.update(settingValuesAsString[2].getBytes());
		md.update(className.getBytes());
		byte[] digest = md.digest();
		return (new HexBinaryAdapter()).marshal(digest);
	}

	private String[] getSettingObjectsAsString(final ComponentInstance composition, final ReproducibleInstances reproducibleInstances, final ReproducibleInstances testData) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return new String[] { mapper.writeValueAsString(composition), mapper.writeValueAsString(reproducibleInstances.getInstructions()), mapper.writeValueAsString(testData.getInstructions()) };
	}

	/**
	 * Stores the composition, the trajectory and the achieved score in the database.
	 *
	 * @param composition
	 *            - Solution composition
	 * @param reproducableInstances
	 *            - Instances object that includes the trajectory, i.e. all operations that have been applied to the instances like loading, splitting etc.
	 * @param testData
	 *            - The reproducible instances of the test data used for this evaluation process
	 * @param score
	 *            - Score achieved by the composition on the reproducible instances
	 * @param className
	 *            - The java qualified class name of the loss function that was used
	 * @param evaluationTime
	 *            - The time it took for the corresponding evaluation in milliseconds
	 */
	public void store(final ComponentInstance composition, final ReproducibleInstances reproducibleInstances, final ReproducibleInstances testData, final double score, final String className, final long evaluationTime) {
		try {
			List<IKVStore> rs = this.getScoreOfCompositions(composition, reproducibleInstances, testData, className);
			if (!rs.isEmpty()) {
				return;
			}
			Map<String, String> valueMap = new HashMap<>();
			String[] settingObjectStrings = this.getSettingObjectsAsString(composition, reproducibleInstances, testData);
			valueMap.put("composition", settingObjectStrings[0]);
			valueMap.put("train_trajectory", settingObjectStrings[1]);
			valueMap.put("test_trajectory", settingObjectStrings[2]);
			valueMap.put("loss_function", className);
			valueMap.put("evaluation_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date.from(Instant.now())));
			valueMap.put("evaluation_time_ms", Long.toString(evaluationTime));
			valueMap.put("hash_value", this.getHexHash(settingObjectStrings, className));
			valueMap.put("score", Double.toString(score));
			this.sqlAdapter.insert(this.performanceSampleTableName, valueMap);
		} catch (JsonProcessingException | NoSuchAlgorithmException | SQLException e) {
			logger.warn("Error while storing results: {}", e);
		}
	}

	@Override
	public void close() throws IOException {
		this.sqlAdapter.close();
	}

}
