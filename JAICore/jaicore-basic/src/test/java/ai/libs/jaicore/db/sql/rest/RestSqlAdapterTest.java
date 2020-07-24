package ai.libs.jaicore.db.sql.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.aeonbits.owner.ConfigFactory;
import org.api4.java.datastructure.kvstore.IKVStore;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import ai.libs.jaicore.basic.FileUtil;
import ai.libs.jaicore.db.DBTester;

public class RestSqlAdapterTest extends DBTester {

	private static File CONFIG_FILE = new File("testrsc/test.restSqlAdapter.properties");

	private static RestSqlAdapter adapter;

	private static final String SELECT_TABLE = "test_select_table";
	private static final String DELETE_FROM_INSERT_TABLE = "test_insert_table";
	private static final String CREATE_DROP_TABLE = "test_createdrop_table";

	@BeforeClass
	public static void setup() throws IOException {
		assertTrue(CONFIG_FILE.exists());
		IRestDatabaseConfig config = ConfigFactory.create(IRestDatabaseConfig.class, FileUtil.readPropertiesFile(CONFIG_FILE));
		assertNotNull(config.getHost());
		setConnectionConfigIfEmpty(config);
		adapter = new RestSqlAdapter(config);
	}

	@Test
	@Ignore
	public void testSelectQuery() throws SQLException {
		List<IKVStore> res = adapter.getResultsOfQuery("SELECT * FROM " + SELECT_TABLE);
		if (res.isEmpty() || res.size() > 1) {
			fail("No result or too many results returned for select query.");
		}
		IKVStore store = res.get(0);
		assertEquals("ID not as expected.", "1", store.getAsString("id"));
		assertEquals("Column 'a' not as expected.", "1", store.getAsString("a"));
		assertEquals("Column 'b' not as expected.", "y", store.getAsString("b"));
		assertEquals("Column 'c' not as expected.", "3", store.getAsString("c"));
	}

	@Test
	@Ignore
	public void testInsertQuery() throws SQLException {
		int numEntriesBefore = this.numEntries(DELETE_FROM_INSERT_TABLE);
		adapter.insert("INSERT INTO " + DELETE_FROM_INSERT_TABLE + " (y) VALUES (2)");
		int numEntriesAfter = this.numEntries(DELETE_FROM_INSERT_TABLE);
		assertTrue("No entry added!", numEntriesAfter > numEntriesBefore);
	}

	@Test
	@Ignore
	public void testRemoveEntryQuery() throws SQLException {
		int numEntriesBefore = this.numEntries(DELETE_FROM_INSERT_TABLE);
		adapter.insert("DELETE FROM " + DELETE_FROM_INSERT_TABLE + " LIMIT 1");
		int numEntriesAfter = this.numEntries(DELETE_FROM_INSERT_TABLE);
		assertTrue("No entry added!", numEntriesAfter < numEntriesBefore);
	}

	@Test
	@Ignore
	public void testCreateAndDropTable() throws SQLException {
		this.logger.info("Create table...");
		adapter.query("CREATE TABLE " + CREATE_DROP_TABLE + " (a VARCHAR(1))");
		this.logger.info("Insert into table...");
		adapter.insert("INSERT INTO " + CREATE_DROP_TABLE + " (a) VALUES ('x')");
		assertTrue("Table could not be created correctly", this.numEntries(CREATE_DROP_TABLE) > 0);
		adapter.query("DROP TABLE " + CREATE_DROP_TABLE);
	}

	public int numEntries(final String table) throws SQLException {
		return adapter.select("SELECT * FROM " + table).size();
	}

}
