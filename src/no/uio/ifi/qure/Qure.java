package no.uio.ifi.qure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.Scanner;
import java.util.Iterator;

import java.sql.DatabaseMetaData;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet; 
import java.sql.SQLException;
import java.sql.Statement;

import java.io.FileWriter;
import java.io.IOException;

import static no.uio.ifi.qure.Relation.*;
import java.time.LocalDateTime;
import no.uio.ifi.qure.dataprovider.*;
import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.bintree.*;

public class Qure {

	private static Connection connect = null;
	private static Statement statement = null;
	private static PreparedStatement preparedStatement = null;
	private static ResultSet resultSet = null;

	private static Map<String, Long> times = new HashMap<String, Long>();

	public static void main(String[] args) {

		// Relation r = partOf(0,0,0,1).and(not(partOf(0,0,0,2))).and(overlaps(0,0,1,2));
		// System.out.println(r.eval(new Space[]{new TimeSpace(LocalDateTime.of(2016, 10, 1, 0, 0),
		//													 LocalDateTime.of(2016, 11, 1, 0, 0)),
		//									   new TimeSpace(LocalDateTime.of(2016, 9, 1, 0, 0),
		//													 LocalDateTime.of(2016, 11, 11, 11, 11)),
		//									   new TimeSpace(LocalDateTime.of(2016, 8, 10, 10, 10),
		//													 LocalDateTime.of(2016, 9, 12, 11, 11))
		//									  }));

		ArrayList<Config> rfs = new ArrayList<Config>();
		//rfs.add(new Config("dallas", "f3", 13, 3, 30, 10));
		//rfs.add(new Config("osm_dk", "upsa", 15, 3, 30, 10));
		//rfs.add(new Config("npd",	"upsa", 10, 3, 30, 10));
		//rfs.add(new Config("dallas", "upsa", 13, 3, 30, 10));

		//rfs.add(new Config("npd",	"geom_test", 10, 3, 30, 10));
		rfs.add(new Config("dallas", "ns2", 13, 3, 30, 10));
		//rfs.add(new Config("osm_no", "geom_test", 15, 3, 30, 10));
		//rfs.add(new Config("osm_dk", "geom_test", 15, 3, 30, 10));

		//runMany(rfs);
		//writeDBSizes(rfs);

		//ArrayList<Config> configs = new ArrayList<Config>();

		//configs.add(new Config("osm_no", "full", 15, 4, 30, 10));
		//configs.add(new Config("dallas", "full", 15, 4, 30, 10));
		//configs.add(new Config("osm_dk", "full", 15, 3, 30, 10));
		//configs.add(new Config("npd",	"full", 13, 3, 30, 10));

		//configs.add(new Config("dallas", "full", 17, 3, 30, 10));
		//configs.add(new Config("osm_no", "full", 17, 3, 30, 10));
		//configs.add(new Config("osm_dk", "full", 13, 3, 30, 10));
		//configs.add(new Config("npd",	"full", 10, 3, 30, 10));

		//Config o1 = new Config("dallas", "unbal", 13, 3, 30, 0);
		//o1.blockSize = 63;
		//configs.add(o1);
		//Config o2 = new Config("osm_no", "unbal", 15, 3, 30, 0);
		//o2.blockSize = 63;
		//configs.add(o2);
		//Config o3 = new Config("osm_dk", "unbal", 15, 3, 30, 0);
		//o3.blockSize = 63;
		//configs.add(o3);

		//configs2.add(new Config("npd",   "unbal", 10, 3, 30, 0));

		//configs.add(new Config("dallas", "full", 10, 3, 30, 10));
		//configs.add(new Config("osm_no", "full", 10, 3, 30, 10));
		//configs.add(new Config("osm_dk", "full", 10, 3, 30, 10));
		//configs.add(new Config("npd",	"full", 8, 3, 30, 10));

		
		runMany(rfs);
		//writeDBSizes(rfs);
		//times = new HashMap<String, Long>();
		//runManyQueryBM(rfs);
		//times = new HashMap<String, Long>();
		//runAllInsertBM(configs, 100, 20, false);
		//runAllInsertBM(rfs, 100, 20, true);
	}

	private static void runMany(Collection<Config> configs) {
		for (Config config : configs) {
			try {
				runBulk(config);
				Thread.sleep(1000*60*0);
			} catch (InterruptedException ex) {
				try {
					FileWriter fw = new FileWriter("errs.txt", true);
					fw.write(ex.toString() + "\n\n");
					fw.flush();
					fw.close();
				} catch (IOException ioe) {
					System.err.println(ioe.toString());
				}
			}
		}
	}

	private static void runAllInsertBM(Collection<Config> configs, int n, int iterations, boolean loc) {
		for (Config config : configs) {
			try {
				runManyInsertBM(config, n, iterations, loc);
				Thread.sleep(1000*30*3);
			} catch (InterruptedException ex) {
				try {
					FileWriter fw = new FileWriter("errs.txt", true);
					fw.write(ex.toString() + "\n\n");
					fw.flush();
					fw.close();
				} catch (IOException ioe) {
					System.err.println(ioe.toString());
				}
			}
		}
	}

	public static void takeTime(long before, long after, String name,
								 String what, boolean print, boolean writeToFile,
								 String filename, boolean ms) {


		String timeStr, mapStr;
		mapStr = name + " - " + what;
		long totalSec;

		if (!ms) {
			totalSec = (long) Math.round(((after - before) / 1000.0));
			long mins = (long) Math.floor(totalSec / 60.0);
			long restSec = totalSec - (mins*60);
			timeStr = what + ": " + totalSec + "s" + " = " + mins + "m" + restSec + "s";
		} else {
			totalSec = after-before;
			timeStr = what + ": " + totalSec + "ms"; 
		}
		if (!times.containsKey(mapStr)) {
			times.put(mapStr, 0L);
		}
		times.put(mapStr, times.get(mapStr)+totalSec);

		if (print) {
			System.out.println(timeStr);
		}

		if (writeToFile) {
			try {
				FileWriter fw = new FileWriter(filename, true);
				fw.write(name + " - " + timeStr + "\n");
				fw.flush();
				fw.close();
			} catch (IOException ioe) {
				System.err.println(ioe.toString());
			} 
		}
	}

	public static void finishTime(String filename, boolean ms) {
		for (String what : times.keySet()) {
			long total = times.get(what);
			String timeStr;
			if (!ms) {
				long mins = (long) Math.floor(total / 60.0);
				long restSec = total - (mins*60);
				timeStr = what + ": " + total + "s = " + mins + "m" + restSec + "s";
			} else {
				timeStr = what + ": " + total + "ms";
			}
			try {
				FileWriter fw = new FileWriter(filename, true);
				fw.write("Finish:: " + timeStr + "\n");
				fw.flush();
				fw.close();
			} catch (IOException ioe) {
				System.err.println(ioe.toString());
			} 
		}
		times = new HashMap<String, Long>();
	}
			
	public static void printConfig(Config config) {
		System.out.println("--------------------------------------");
		System.out.println("Config:");
		System.out.println(" # Geo. table: " + config.geoTableName);
		System.out.println(" # Max depth: " + config.maxIterDepth);
		System.out.println(" # Block member count: " + config.blockMemberCount);
		System.out.println(" # Overlaps arity: " + config.overlapsArity);
		System.out.println(" # Write to: " + config.btTableName);
		System.out.println("--------------------------------------");
	}
	
	public static void runBulk(Config config) {

		if (config.verbose) printConfig(config);

		SpaceProvider geometries = new no.uio.ifi.qure.space.GeometryProvider(config, new DBDataProvider(config));
		geometries.populateBulk();
		SpaceToBintree gtb = new SpaceToBintree(config);

		long before = System.currentTimeMillis();

		Representation rep = gtb.constructRepresentations(geometries);

		long after = System.currentTimeMillis();

		if (rep != null && config.writeBintreesToDB) {
			try {
				writeBintreesToDB(rep, new HashSet<Block>(), config);
			} catch (SQLException e) {
				System.err.println("SQLError: " + e.getMessage());
				System.err.println(e.getNextException());
			} catch (Exception e) {
				System.out.println("Error in runBulk(): " + e);
			} finally {
				close();
			}
		} else if (rep == null) {
			System.out.println("Error occured, no solution found. Aborting...");
		}
		long after2 = System.currentTimeMillis();
		takeTime(before, after, config.rawBTTableName, "Construction time", true, true, "output.txt", false);
		takeTime(before, after2, config.rawBTTableName, "Total construction time", true, true, "output.txt", false);
	}

	public static void runManyInsertBM(Config config, int n, int iterations, boolean loc) {

		for (int i = 1; i <= iterations; i++) {
			System.out.println("======================================");
			System.out.println("Iteration: " + i + " / " + iterations + " of " + config.rawBTTableName);
			System.out.println("--------------------------------------");
			config.verbose = false;
			runInsertBM(config, n, loc);
			config.verbose = true;
		}
		finishTime("output_ins.txt", false);
	}

	public static void runInsertBM(Config config, int n, boolean loc) {
		if (loc) {
			deleteRandomBintreesLoc(n, config);
		} else {
			deleteRandomBintreesRand(n, config);
		}
		runInsert(config, loc);
	}

	public static void runInsert(Config config, boolean loc) {

		long beforeAll = System.currentTimeMillis();

		RawDataProvider<String> dataProvider = new DBDataProvider(config);
		SpaceProvider geometries = new no.uio.ifi.qure.space.GeometryProvider(config, dataProvider);
		geometries.populateUpdate();
		Map<Block, Block> oldSplits = dataProvider.getEvenSplits();
		SpaceToBintree gtb = new SpaceToBintree(config, oldSplits);

		long before = System.currentTimeMillis();

		Representation rep = gtb.constructRepresentations(geometries);
		
		long after = System.currentTimeMillis();

		if (rep != null && config.writeBintreesToDB) {
			try {
				writeBintreesToDB(rep, oldSplits.keySet(), config);
			} catch (SQLException e) {
				System.err.println("SQLError: " + e.getMessage());
				System.err.println(e.getNextException());
			} catch (Exception e) {
				System.err.println("Error in runInsert(): " + e.toString());
				e.printStackTrace();
			} finally {
				close();
			}
		} else if (rep == null) {
			System.out.println("Error occured, no solution found. Aborting...");
		}
		long afterAll = System.currentTimeMillis();
		String add = (loc) ? " (loc)" : "";
		takeTime(before, after, config.rawBTTableName, "Insert time"+add, true, false, null, false);
		takeTime(beforeAll, afterAll, config.rawBTTableName, "Total insert time"+add, true, false, null, false);
	}

	public static void writeBintreesToDB(Representation rep, Set<Block> oldSplits, Config config) throws Exception {

		Map<Integer, Bintree> bintrees = rep.getRepresentation();
		Space universe = rep.getUniverse();
		Map<Block, Block> splits = rep.getEvenSplits();

		try {
			Class.forName(config.jdbcDriver);

			if (config.verbose) {
				System.out.print("Connecting to database " + config.connectionStr + "...");
			}

			connect = DriverManager.getConnection(config.connectionStr);
			if (config.verbose) System.out.println(" Done");

			statement = connect.createStatement();

			DatabaseMetaData meta = connect.getMetaData();
			ResultSet res = meta.getTables(config.dbName, config.schemaName, config.rawBTTableName, null);
			boolean insert = res.next();

			if (insert) {
				deleteBintrees(rep, oldSplits, config);
			} else {
				createTable(statement, config);
			}
			insertBintrees(bintrees, config);

			if (!insert) {
				createIndexStructures(config);
				insertUniverse(universe, config);
			}
			if (!splits.isEmpty()) {
				insertSplits(splits, config);
			}
		} catch (ClassNotFoundException e) {
			System.err.println("Class not found: " + e.getMessage());
		} catch (Exception e) {
			throw e;
		} finally {
			close();
		}
	}

	public static void insertBintrees(Map<Integer, Bintree> bintrees, Config config) 
		throws SQLException {

		Progress prog = new Progress("Making query...", bintrees.size(), 1, "##0");
		if (config.verbose) prog.init();

		String query;

		for (Integer uri : bintrees.keySet()) {

			query = "INSERT INTO " + config.btTableName + " VALUES ";
			Set<Block> blocksArr = bintrees.get(uri).getBlocks();
			Block[] blocks = blocksArr.toArray(new Block[blocksArr.size()]);

			for (int i = 0; i < blocks.length; i++) {
				long b = blocks[i].getRepresentation();
				query += "('" + uri + "', " + b + ")" + ((i == blocks.length-1) ? ";" : ", ");
			}

			statement.addBatch(query);
			if (config.verbose) prog.update();
		}
		if (config.verbose) {
			prog.done();
			System.out.print("Executing insert batch query, writing to table " +
			                 config.btTableName + "...");
		}

		int[] ins = statement.executeBatch();
		int sum = 0;
		for (int i=0; i < ins.length; i++) sum += ins[i];

		if (config.verbose) System.out.println(" Done [Inserted " + sum + " rows]");
	}


	public static void insertUniverse(Space universe, Config config) 
		throws SQLException {

		if (config.verbose) System.out.print("Inserting universe into " + config.universeTable + "...");
		String universeStr = universe.toDBString();
		statement.executeUpdate("INSERT INTO " + config.universeTable + " VALUES ('" +
		                        config.btTableName + "', " + universeStr + ");"); 
		if (config.verbose) System.out.println(" Done.");
	}


	public static void insertSplits(Map<Block, Block> splits, Config config) 
		throws SQLException {

		if (config.verbose) System.out.print("Inserting even splits into " + config.splitTable + "...");

		DatabaseMetaData meta = connect.getMetaData();
		ResultSet res = meta.getTables(null, "split", config.rawBTTableName, null);
		boolean insert = res.next();
		if (!insert) {
			if (config.blockSize > 31) {
				statement.executeUpdate("CREATE TABLE " + config.splitTable + " (block bigint, split bigint);");
			} else {
				statement.executeUpdate("CREATE TABLE " + config.splitTable + " (block int, split int);");
			}
		}
		String splitStr = "INSERT INTO " + config.splitTable + " VALUES ";

		for (Block block : splits.keySet()) {
			splitStr += "(" + block.getRepresentation() + ", " + splits.get(block).getRepresentation() + "), ";
		}
		splitStr = splitStr.substring(0, splitStr.length()-2) + ";";

		int ins = statement.executeUpdate(splitStr);

		if (config.verbose) System.out.println(" Done. [Inserted " + ins + " rows]");
	}

	public static void createIndexStructures(Config config) 
		throws SQLException {

		if (config.verbose) System.out.print("Constructing index structures...");

		statement.executeUpdate("CREATE INDEX " + config.rawBTTableName
		                        + "_gid_index ON " + config.btTableName + "(gid);");
		statement.executeUpdate("CREATE INDEX " + config.rawBTTableName 
		                        + "_block_index ON " + config.btTableName + "(block);");
		statement.executeUpdate("CREATE INDEX " + config.rawBTTableName
		                        + "_wit_index ON " + config.btTableName + "(block,gid) WHERE block % 2 != 0;");

		if (config.verbose) System.out.println(" Done.");
	}

	private static void deleteRandomBintreesLoc(int n, Config config) {
		try {
			Class.forName(config.jdbcDriver);

			connect = DriverManager.getConnection(config.connectionStr);
			statement = connect.createStatement();
			String delQuery = "DELETE FROM " + config.btTableName + " WHERE " + config.uriColumn + " IN ";
			delQuery += "(SELECT G." + config.uriColumn + " FROM " + config.geoTableName + " G, (SELECT * FROM " + config.geoTableName + " ORDER BY random() LIMIT 1) R ORDER BY st_distance(G.geom, R.geom) LIMIT " + n + ");";
			int dels = statement.executeUpdate(delQuery);
			System.out.println("Deleted randomly " + n + " bintrees (" + dels + " rows).");
		} catch (Exception ex) {
			System.err.println(ex.toString());
			ex.printStackTrace();
			System.exit(1);
		} finally {
			close();
		}
	}

	private static void deleteRandomBintreesRand(int n, Config config) {
		try {
			Class.forName(config.jdbcDriver);

			connect = DriverManager.getConnection(config.connectionStr);
			statement = connect.createStatement();
			String delQuery = "DELETE FROM " + config.btTableName + " WHERE " + config.uriColumn + " IN ";
			delQuery += "(SELECT " + config.uriColumn + " FROM (SELECT * FROM " + config.geoTableName + " ORDER BY random() LIMIT " + n + ") T);";
			int dels = statement.executeUpdate(delQuery);
			System.out.println("Deleted randomly " + n + " bintrees (" + dels + " rows).");
		} catch (Exception ex) {
			System.err.println(ex.toString());
			ex.printStackTrace();
			System.exit(1);
		} finally {
			close();
		}
	}

	public static void deleteBintrees(Representation rep, Set<Block> oldSplitBlocks, Config config) 
		throws SQLException {
		// To update the representations we will first delete the old representations for
		// the blocks were a new representation is created.
		Map<Integer, Bintree> res = rep.getRepresentation();
		Progress prog = new Progress("Making delete query...", res.size(), 1, "##0");
		if (config.verbose) prog.init();

		for (Integer uri : res.keySet()) {
			StringBuilder delQuery = new StringBuilder("DELETE FROM " + config.btTableName + " WHERE ");
			delQuery.append(config.uriColumn + " = '" + uri + "' AND (false");

			for (Block block : res.get(uri).getBlocks()) {
				Block parent = getParentInSet(block, oldSplitBlocks);
				if (parent != null) {
					String blockStr = "" + parent.getRepresentation(); 
					delQuery.append(" OR " + makePrefixQuery(blockStr, config.blockSize));
				}
			}
			delQuery.append(");");
			statement.addBatch(delQuery.toString());
			if (config.verbose) prog.update();
		}

		if (config.verbose) {
			prog.done();
			System.out.print("Executing delete query...");
		}

		int[] deleted = statement.executeBatch();
		statement.clearBatch();
		int delSum = 0;

		for (int i = 0; i < deleted.length; i++) {
			delSum += deleted[i];
		}

		if (config.verbose) System.out.println(" Done. [Deleted " + delSum + " rows]");
	}

	public static void deleteBintreesOld(Representation rep, Set<Block> oldSplitBlocks, Config config) 
		throws SQLException {
		// To update the representations we will first delete the old representations for
		// the blocks were a new representation is created.
		Map<Integer, Bintree> res = rep.getRepresentation();
		Progress prog = new Progress("Making delete query...", res.size(), 1, "##0");
		if (config.verbose) prog.init();

		for (Integer uri : res.keySet()) {
			String delQuery = "DELETE FROM " + config.btTableName + " WHERE ";
			delQuery += config.uriColumn + " = '" + uri + "' AND (false";
			for (Block block : res.get(uri).getBlocks()) {
				Block parent = getParentInSet(block, oldSplitBlocks);
				if (parent != null) {
					String blockStr = "" + parent.getRepresentation(); 
					delQuery += " OR " + makePrefixQuery(blockStr, config.blockSize);
				}
			}
			delQuery += ");";
			statement.addBatch(delQuery);
			if (config.verbose) prog.update();
		}
		if (config.verbose) {
			prog.done();
			System.out.print("Executing delete query...");
		}
		int[] deleted = statement.executeBatch();
		statement.clearBatch();
		int delSum = 0;
		for (int i = 0; i < deleted.length; i++)
			delSum += deleted[i];

		if (config.verbose) System.out.println(" Done. [Deleted " + delSum + " rows]");
	}

	public static Block getParentInSet(Block block, Set<Block> bs) {

		Block b = new Block(block.getRepresentation());
		Block prev = b;

		while (!b.isTop()) {
			if (bs.contains(b)) {
				return prev;
			}
			prev = b;
			b = b.getParent();
		}

		return null;
	}

	public static Block getParentInSetOld(Block block, Set<Block> bs) {

		Block smallest = Block.getTopBlock();

		for (Block b : bs) {
			if (block.blockPartOf(b) && b.blockPartOf(smallest)) {
				smallest = b;
			}
		}
		
		// Need to split last block, as bs only contains blocks that were split
		Block[] lr = smallest.split();

		if (block.blockPartOf(lr[0])) {
			return lr[0];
		} else if (block.blockPartOf(lr[1])) {
			return lr[1];
		} else {
			return null;
		}
	}

	public static String makePrefixQuery(String block, int blockSize) {

		String expr;

		if (blockSize > 31) {
			expr = "((((1::bigint << (63 - ((" + block + " & 127) >> 1))::int)) - 1) | " + block + ")";
		} else {
			expr = "(((1 << (31 - ((" + block + " & 63) >> 1))) - 1) | " + block + ")";
		}

		String c1 = block + " & -2 < block"; // Should be strictly contained for deletion
		String c2 = expr + " >= block";
		return "(" + c1 + " AND " + c2 + ")";
	}

	public static void createTable(Statement statement, Config config) {

		boolean tableMade = false;

		while (!tableMade) {
			try {

				String blockType = (config.blockSize > 31) ? "bigint" : "int";

				if (config.convertUriToInt) {
					statement.executeUpdate("CREATE TABLE " + config.btTableName + " (gid int, block " + blockType + ");");
				} else {
					statement.executeUpdate("CREATE TABLE " + config.btTableName + " (id text, block " + blockType + ");");
				}
				tableMade = true;

			} catch (SQLException sqlex) {

				System.out.println("Error on table creation with name " + config.btTableName + ":");
				System.out.println(sqlex.getMessage());
				System.out.print("Try to add a new table name suffix (or just hit return twice to abort): ");
				Scanner scan = new Scanner(System.in).useDelimiter("[ \n]"); // Table name is only one word
				config = new Config(config.rawGeoTableName, scan.next(), config.maxIterDepth, config.overlapsArity, config.blockMemberCount, config.maxSplits);
				System.out.println("");

				if (config.btTableName.equals("")) {
					close();
					System.exit(0);
				}
			} 
		}

		if (config.verbose) System.out.println("Table " + config.btTableName + " created.");
	}

	// Need to close the resultSet
	public static void close() {
		try {
			if (statement != null) {
				statement.close();
			}
  
			if (connect != null) {
				connect.close();
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage()); 
		}
	}

	public static void runManyQueryBM(Collection<Config> configs) {

		for (Config config : configs) {
			System.out.println("--------------------------------------");
			System.out.println("Running query benchmark for " + config.rawBTTableName + "...");
			runQueryBM(config);
			try {
				clearCache();
			} catch (Exception ex) {
				try {
					FileWriter fw = new FileWriter("errs.txt", true);
					fw.write(ex.toString() + "\n\n");
					fw.flush();
					fw.close();
				} catch (IOException ioe) {
					System.err.println(ioe.toString());
				}
			}
		}
	}

	public static void clearCache() {
		try {
			System.out.print("Clearing cache...");
			Process p1 = Runtime.getRuntime().exec("sync");
			p1.waitFor();
			Process p2 = Runtime.getRuntime().exec("service postgresql stop");
			p2.waitFor();
			Process p3 = Runtime.getRuntime().exec("echo 1 > /proc/sys/vm/drop_caches");
			p3.waitFor();
			Process p4 = Runtime.getRuntime().exec("service postgresql start");
			p4.waitFor();
			System.out.println(" Done");
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
	}

	public static void runQueryBM(Config config) {
		try {
			Class.forName(config.jdbcDriver);

			connect = DriverManager.getConnection(config.connectionStr);
			statement = connect.createStatement();

			long before, after;

			// before = System.currentTimeMillis();
			// statement.execute("SELECT bmbt_ov_int('ins." + config.rawGeoTableName + "_2000', '" + config.btTableName + "');");
			// after = System.currentTimeMillis();
			// takeTime(before, after, config.rawBTTableName, "ov time", true, true, "query.txt", true);

			// if (!config.rawGeoTableName.equals("npd") && config.overlapsArity >= 3) {
			//	 before = System.currentTimeMillis();
			//	 statement.execute("SELECT bmbt_ov2_int('ins." + config.rawGeoTableName + "_500', '" + config.btTableName + "');");
			//	 after = System.currentTimeMillis();
			//	 takeTime(before, after, config.rawBTTableName, "ov2 time", true, true, "query.txt", true);
			// }

			before = System.currentTimeMillis();
			statement.execute("SELECT bmbt_po1_iu('ins." + config.rawGeoTableName + "_2000', '" + config.btTableName + "');");
			after = System.currentTimeMillis();
			takeTime(before, after, config.rawBTTableName, "po1 time", true, true, "query.txt", true);

			before = System.currentTimeMillis();
			statement.execute("SELECT bmbt_po2_iu('ins." + config.rawGeoTableName + "_2000', '" + config.btTableName + "');");
			after = System.currentTimeMillis();
			takeTime(before, after, config.rawBTTableName, "po2 time", true, true, "query.txt", true);

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			close();
		}
	}

	public static void writeDBSizes(Collection<Config> configs) {
		for (Config config : configs) {
			try {
				writeDBSize(config);
			} catch (Exception ex) {
				try {
					FileWriter fw = new FileWriter("errs.txt", true);
					fw.write(ex.toString() + "\n\n");
					fw.flush();
					fw.close();
				} catch (IOException ioe) {
					System.err.println(ioe.toString());
				}
			}
		}
	}

	public static void writeDBSize(Config config) {
		try {
			Class.forName(config.jdbcDriver);

			connect = DriverManager.getConnection(config.connectionStr);
			statement = connect.createStatement();
			
			statement.execute("select pg_size_pretty(pg_relation_size('" + config.btTableName + "'));");
			ResultSet res1 = statement.getResultSet();
			res1.next();

			String table = res1.getString(1);
			statement.execute("select pg_size_pretty(pg_total_relation_size('" + config.btTableName + "'));");
			ResultSet res2 = statement.getResultSet();
			res2.next();
			String out = config.btTableName + " - Table: " + table + ", Total: " + res2.getString(1);

			try {
				FileWriter fw = new FileWriter("sizes.txt", true);
				fw.write(out + "\n");
				fw.flush();
				fw.close();
			} catch (IOException ioe) {
				System.err.println(ioe.toString());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			close();
		}
	}
}
