package no.uio.ifi.qure.dataprovider;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet; 
import java.sql.ResultSetMetaData; 
import java.sql.SQLException;
import java.sql.Statement;

import no.uio.ifi.qure.*;
import no.uio.ifi.qure.bintree.Block;

public class DBDataProvider implements RawDataProvider<String> {

	private Config config;
	private Connection connect = null;
	private Statement statement = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;

	public DBDataProvider(Config config) {
		this.config = config;
	}

	public Set<Integer> getInsertURIs() {

		Set<Integer> insertUris = new HashSet<Integer>();

		try {
			Class.forName(config.jdbcDriver);
			if (config.verbose) {
				System.out.print("Connecting to database " + config.connectionStr + "...");
			}

			connect = DriverManager.getConnection(config.connectionStr);
			if (config.verbose) System.out.println(" Done");

			statement = connect.createStatement();

			String queryGeo = "SELECT DISTINCT " + config.uriColumn + " FROM " + config.geoTableName + ";";
			statement.execute(queryGeo);
			resultSet = statement.getResultSet();
			while (resultSet.next()) insertUris.add(resultSet.getInt(1));
 
			String queryBT = "SELECT DISTINCT " + config.uriColumn + " FROM " + config.btTableName + ";";
			statement.execute(queryBT);
			resultSet = statement.getResultSet();
			while (resultSet.next()) insertUris.remove(resultSet.getInt(1));
 
		} catch (SQLException e) {
			System.err.println("SQLError: " + e.toString());
			System.err.println(e.getNextException());
		} catch (Exception e) {
			System.err.println("Error in queryForInsert(): " + e.toString());
			e.printStackTrace();
			System.exit(1);
		} finally {
			close();
		}

		return insertUris;
	}

	public Set<Integer> getAllURIs() {

		Set<Integer> uris = new HashSet<Integer>();

		try {
			Class.forName(config.jdbcDriver);
			connect = DriverManager.getConnection(config.connectionStr);
			statement = connect.createStatement();

			String queryBT = "SELECT DISTINCT " + config.uriColumn + " FROM " + config.btTableName + ";";
			statement.execute(queryBT);
			resultSet = statement.getResultSet();
			while (resultSet.next()) uris.add(resultSet.getInt(1));
 
		} catch (SQLException e) {
			System.err.println("SQLError: " + e.toString());
			System.err.println(e.getNextException());
		} catch (Exception e) {
			System.err.println("Error in queryForInsert(): " + e.toString());
			e.printStackTrace();
			System.exit(1);
		} finally {
			close();
		}

		return uris;
	}

	public DBUnparsedIterator getExternalOverlapping(String whereClause) {

		String query = config.geoQuerySelectFromStr;
		query += " WHERE " + whereClause;	
		int total = queryForTotal(config.geoTableName);
		return new DBUnparsedIterator(total, config.limit, config.jdbcDriver, 
		                              config.connectionStr, query, config.uriColumn);
	}

	public int queryForTotal(String tableName) {

		Integer total = -1;

		try {
			Class.forName(config.jdbcDriver);

			connect = DriverManager.getConnection(config.connectionStr);

			statement = connect.createStatement();
			statement.execute("select count(*) from " + tableName + ";");
			resultSet = statement.getResultSet();
			resultSet.next();
			total = resultSet.getInt(1);
		} catch (Exception e) {
			System.err.println("Error in query process: " + e.toString());
			e.printStackTrace();
			System.exit(1);
		} finally {
			close();
		}

		return total.intValue();
	}

	public UnparsedSpace<String> getUniverse() {

		List<String> universeStrs = null;

		try {
			Class.forName(config.jdbcDriver);
			connect = DriverManager.getConnection(config.connectionStr);

			statement = connect.createStatement();
			String query = "SELECT " + config.geoColumn + " FROM " + config.universeTable + 
			               " WHERE table_name = '" + config.btTableName + "';";
			
			statement.execute(query);
			resultSet = statement.getResultSet();
			resultSet.next(); 

			int numCol = resultSet.getMetaData().getColumnCount();
			for (int i = 1; i <= numCol; i++) {
				universeStrs.add(resultSet.getString(i));
			}
		} catch (Exception e) {
			System.err.println("Error querying for universe: " + e.toString());
			e.printStackTrace();
			System.exit(1);
		} finally {
			close();
		}

		return new UnparsedSpace<String>(0, universeStrs);
	}

	public DBUnparsedIterator getSpaces() {

		int total = queryForTotal(config.geoTableName);
		return new DBUnparsedIterator(total, config.limit, config.jdbcDriver, config.connectionStr,
		                              config.geoQuerySelectFromStr, config.uriColumn);
	}

	public DBUnparsedIterator getSpaces(Set<Integer> uris) {

		if (uris.isEmpty()) return new DBUnparsedIterator(0, 0, "", "", "", "");

		return new DBUnparsedIterator(uris.size(), uris.size(), config.jdbcDriver, config.connectionStr,
		                              makeValuesQuery(uris), config.uriColumn);
	}

	private String makeValuesQuery(Set<Integer> uris) {

		String query = config.geoQuerySelectFromStr + ", (VALUES ";

		Integer[] urisArr = uris.toArray(new Integer[uris.size()]);

		for (int i = 0; i < urisArr.length - 1; i++) {
			query += "(" + urisArr[i] + "), ";
		}

		query += "(" + urisArr[urisArr.length-1] + ")"; // Last element, no need for comma after.
		query += ") AS V(uri) WHERE V.uri = " + config.uriColumn + ";";
		
		return query;
	}

	public Map<Block, Block> getEvenSplits() {

		Map<Block, Block> res = new HashMap<Block, Block>();

		try {
			Class.forName(config.jdbcDriver);

			if (config.verbose) System.out.print("Retrieving splitting blocks...");

			connect = DriverManager.getConnection(config.connectionStr);
			statement = connect.createStatement();
			
			statement.execute(config.splitQuery);
			ResultSet resultSet = statement.getResultSet();

			while (resultSet.next()) {
				long block = resultSet.getLong(1);
				long split = resultSet.getLong(2);
				res.put(new Block(block), new Block(split));
			}

			if (config.verbose) System.out.println(" Done");
		} catch (Exception e) {
			System.err.println("Error in query process: " + e.toString());
			e.printStackTrace();
			System.exit(1);
		} finally {
			close();
		}

		return res;
	}

	private void close() {
		try {
			if (resultSet != null) {
				resultSet.close();
			}
  
			if (statement != null) {
				statement.close();
			}
  
			if (connect != null) {
				connect.close();
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.toString()); 
		}
	}

	/**
	 * Iterator-class that buffers DB-reading, so that it keeps at most limit
	 * number of DB-rows in memory. Calls to next() automatically
	 * reads in new rows when necessary.
	 */
	private class DBUnparsedIterator extends UnparsedIterator<String> {
		
		private int total; // Total number of results in DB
		private int limit; // Maximum number of rows to keep in main-memory
		private int offset; // Tracks how far into the results we have come
		private String connectionStr;
		private String baseQuery;
		private String jdbcDriver; 
		private String uriCol;
		
		private Iterator<UnparsedSpace<String>> batch;
		
		public DBUnparsedIterator(int total, int limit, String jdbcDriver, 
		                          String connectionStr, String baseQuery, String uriCol) {
			this.total = total;
			this.limit = limit;
			this.offset = 0;
			this.jdbcDriver = jdbcDriver;
			this.connectionStr = connectionStr;
			this.baseQuery = baseQuery;
			this.uriCol = uriCol;

			batch = new ArrayList<UnparsedSpace<String>>().iterator();
		}
	
		public boolean hasNext() { return batch.hasNext() || offset < total; }

		public UnparsedSpace<String> next() {

			if (!batch.hasNext() && offset < total) {

				try {

					Class.forName(jdbcDriver);
					connect = DriverManager.getConnection(connectionStr);
					statement = connect.createStatement();
					String query = baseQuery + " WHERE " + uriCol + " >= " + offset + " AND " + 
								   uriCol + " < " + (offset + limit) + ";";
					statement.execute(query);
					ResultSet resultSet = statement.getResultSet();

					batchResults(resultSet);	
					offset += limit;

				} catch (Exception e) {
					System.err.println("Error in query process: " + e.toString());
					e.printStackTrace();
					System.exit(1);
				} finally {
			   		close();
				}
			}

			UnparsedSpace<String> next = batch.next();
			batch.remove();
			return next;
		}

		public int size() { return total; }

		private void batchResults(ResultSet resultSet) throws SQLException {

			int numCol = resultSet.getMetaData().getColumnCount();
			List<UnparsedSpace<String>> lst = new ArrayList<UnparsedSpace<String>>(limit);

			while (resultSet.next()) {
				Integer uri = resultSet.getInt(1);
				List<String> spaceStrs = new ArrayList<String>();
				for (int i = 2; i <= numCol; i++) {
					spaceStrs.add(resultSet.getString(i));
				}
				lst.add(new UnparsedSpace<String>(uri, spaceStrs));
			}

			batch = lst.iterator();
		}
	}
}
