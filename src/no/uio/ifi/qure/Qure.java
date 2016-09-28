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


public class Qure {

    private static Connection connect = null;
    private static Statement statement = null;
    private static PreparedStatement preparedStatement = null;
    private static ResultSet resultSet = null;

    private static Map<String, Long> times = new HashMap<String, Long>();

    public static void main(String[] args) {

        Config[] configs = new Config[1];
        int i = 0;

        Config o2 = new Config("npd", "es_bc50", 15, 3);
        configs[i++] = o2;

        // Config o3 = new Config("dallas", "es_bc40", 20, 3);
        // o3.blockMemberCount = 40;
        // configs[i++] = o3;

        // Config o4 = new Config("osm_no", "dd_bc50", 20, 3);
        // configs[i++] = o4;

        // Config o5 = new Config("osm_no", "dd_bc70", 20, 3);
        // o5.blockMemberCount = 70;
        // configs[i++] = o5;

        // Config o6 = new Config("osm_dk", "dd_bc50", 20, 3);
        // configs[i++] = o6;

        runMany(configs);
    }

    private static void runMany(Config[] configs) {

        for (int i = 0; i < configs.length; i++) {
            Config config = configs[i];
            runBulk(config);
        }
    }


    public static void takeTime(long before, long after, String name,
                                 String what, boolean print, boolean writeToFile) {

        long totalSec = (long) Math.round(((after - before) / 1000.0));

        String mapStr = name + " - " + what;
        if (!times.containsKey(mapStr)) times.put(mapStr, 0L);
        times.put(mapStr, times.get(mapStr)+totalSec);

        long mins = (long) Math.floor(totalSec / 60.0);
        long restSec = totalSec - (mins*60);
        String timeStr = what + ": " + totalSec + "s" + " = " + mins + "m" + restSec + "s";

        if (print) {
            System.out.println(timeStr);
        }

        if (writeToFile) {
            try {
                FileWriter fw = new FileWriter("output.txt", true);
                fw.write(name + " - " + timeStr + "\n");
                fw.flush();
                fw.close();
            } catch (IOException ioe) {
                System.err.println(ioe.toString());
            } 
        }
    }

    public static void finishTime() {
        for (String what : times.keySet()) {
            long total = times.get(what);
            long mins = (long) Math.floor(total / 60.0);
            long restSec = total - (mins*60);
            try {
                FileWriter fw = new FileWriter("output.txt", true);
                fw.write("Finish:: " + what + ": " + total + "s = " + mins + "m" + restSec + "s\n");
                fw.flush();
                fw.close();
            } catch (IOException ioe) {
                System.err.println(ioe.toString());
            } 
        }
        times = new HashMap<String, Long>();
    }
            
    public static void printConfig(Config config) {
        System.out.println("Config:");
        System.out.println("--------------------------------------");
        System.out.println("* Geo. table: " + config.geoTableName);
        System.out.println("* Max depth: " + config.maxIterDepth);
        System.out.println("* Block member count: " + config.blockMemberCount);
        System.out.println("* Overlaps arity: " + config.overlapsArity);
        System.out.println("* Write to: " + config.btTableName);
        System.out.println("--------------------------------------");
    }
    
    public static void runBulk(Config config) {

        if (config.verbose) printConfig(config);

        SpaceProvider geometries = new GeometryProvider(config, new DBDataProvider(config));
        geometries.populateBulk();
        SpaceToBintree gtb = new SpaceToBintree(config);

        long before = System.currentTimeMillis();

        Representation rep = gtb.constructRepresentations(geometries);

        long after = System.currentTimeMillis();

        if (rep != null && config.writeBintreesToDB) {
             try {
                 writeBintreesToDB(rep, config);
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
        takeTime(before, after, config.rawBTTableName, "Construction time", true, true);
        takeTime(before, after2, config.rawBTTableName, "Total time", true, true);
    }

    public static void runInsert(Config config) {

        long beforeAll = System.currentTimeMillis();

        SpaceProvider geometries = new GeometryProvider(config, new DBDataProvider(config));
        geometries.populateUpdate();
        SpaceToBintree gtb = new SpaceToBintree(config);

        long before = System.currentTimeMillis();

        Representation rep = gtb.constructRepresentations(geometries);
        
        long after = System.currentTimeMillis();

        if (rep != null && config.writeBintreesToDB) {
             try {
                 updateBintreesInDB(rep, config);
             } catch (SQLException e) {
                 System.err.println("SQLError: " + e.getMessage());
                 System.err.println(e.getNextException());
             } catch (Exception e) {
                 System.out.println("Error in runInsert(): " + e.toString());
             } finally {
                 close();
             }
         } else if (rep == null) {
             System.out.println("Error occured, no solution found. Aborting...");
         }
        long afterAll = System.currentTimeMillis();
        takeTime(before, after, config.rawBTTableName, "Construct time", true, false);
        takeTime(beforeAll, afterAll, config.rawBTTableName, "Total insert time", true, false);
    }

    public static void writeBintreesToDB(Representation rep, Config config) throws Exception {

        Map<Integer, Bintree> bintrees = rep.getRepresentation();
        Space universe = rep.getUniverse();
        Map<Block, Block> splits = rep.getEvenSplits();

        try {
            Class.forName("org.postgresql.Driver");

            if (config.verbose) {
                System.out.println("--------------------------------------");
                System.out.print("Connecting to database " + config.dbName +
                                 " as user " + config.dbUsername + "...");
            }

            connect = DriverManager.getConnection("jdbc:postgresql://localhost/" + config.dbName +
                                                  "?user=" + config.dbUsername
                      + "&password=" + config.dbPWD);
            if (config.verbose) System.out.println(" Done");

            statement = connect.createStatement();

            DatabaseMetaData meta = connect.getMetaData();
            ResultSet res = meta.getTables(null, "qure", "posm_no_d15_k3_es", null);
            if (res.next())
                deleteBintrees(rep, config);
            else
                createTable(statement, config);
            insertBintrees(bintrees, config);
            insertUniverse(universe, config);
            insertSplits(splits, config);
            createIndexStructures(config);

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

            for (int i = 0; i < blocks.length-1; i++)
                query += "('" + uri + "', " + blocks[i].getRepresentation() + "), ";

            query += "('" + uri + "', " + blocks[blocks.length-1].getRepresentation() + ");";

            statement.addBatch(query);
            if (config.verbose) prog.update();
        }
        if (config.verbose) {
            prog.done();
            System.out.print("Executing insert batch query, writing to table " +
                             config.btTableName + "...");
        }

        statement.executeBatch();
        if (config.verbose) System.out.println(" Done");
    }


    public static void insertUniverse(Space universe, Config config) 
        throws SQLException {

        if (config.verbose) System.out.print("Inserting universe into " + config.universeTable + "...");
        String universeStr = universe.toDBString();
        statement.executeUpdate("INSERT INTO " + config.universeTable + " VALUES ('" +
                                config.btTableName + "', '" + universeStr + "');");
        if (config.verbose) System.out.println(" Done.");
    }


    public static void insertSplits(Map<Block, Block> splits, Config config) 
        throws SQLException {

        if (config.verbose) System.out.print("Inserting even splits into " + config.splitTable + "...");
        statement.executeUpdate("CREATE TABLE " + config.splitTable + " (block bigint, split bigint);");

        String splitStr = "INSERT INTO " + config.splitTable + " VALUES ";
        for (Block block : splits.keySet())
            splitStr += "(" + block.getRepresentation() + ", " + splits.get(block).getRepresentation() + "), ";
        splitStr = splitStr.substring(0, splitStr.length()-2) + ";";

        statement.executeUpdate(splitStr);

        if (config.verbose) System.out.println(" Done.");
    }

    public static void createIndexStructures(Config config) 
        throws SQLException {

        if (config.verbose) System.out.print("Constructing index structures...");

        statement.executeUpdate("CREATE INDEX " + config.rawBTTableName
                                + "_gid_index ON " + config.btTableName + "(gid);");
        statement.executeUpdate("CREATE INDEX " + config.rawBTTableName 
                                + "_block_index ON " + config.btTableName + "(block);");
        statement.executeUpdate("CREATE INDEX " + config.rawBTTableName
                                + "_wit_index ON " + config.btTableName + "(block) WHERE block % 2 != 0;");

        if (config.verbose) System.out.println(" Done.");
    }

    public static void deleteBintrees(Representation rep, Config config) 
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
                Block parent = getParentInSet(block, rep.getEvenSplits().keySet());
                String blockStr = "" + parent.getRepresentation(); 
                delQuery += " OR " + makePrefixQuery(blockStr);
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

        if (config.verbose) System.out.println(" Done. [Deleted " + delSum + " rows.]");
    }

    public static Block getParentInSet(Block block, Set<Block> bs) {
        for (Block b : bs) {
            if (block.blockPartOf(b))
                return b;
        }
        return null;
    }

    public static String makePrefixQuery(String block) {
        String expr = "(((1::bigint << (63 - (" + block + " & ((1 << 7)-1))::int)) - 1) | " + block + ")";
        String c1 = block + " & -2 <= block";
        String c2 = expr + " >= block";
        return "(" + c1 + " AND " + c2 + ")";
    }

    public static void updateBintreesInDB(Representation rep, Config config) throws Exception {

        Map<Integer, Bintree> res = rep.getRepresentation();

        try {
            Class.forName("org.postgresql.Driver");

            if (config.verbose) {
                System.out.println("--------------------------------------");
                System.out.print("Connecting to database " + config.dbName +
                                 " as user " + config.dbUsername + "...");
            }

            connect = DriverManager.getConnection("jdbc:postgresql://localhost/" + config.dbName + "?user=" +
                                                  config.dbUsername + "&password=" + config.dbPWD);
            if (config.verbose) System.out.println(" Done");

            statement = connect.createStatement();

            deleteBintrees(rep, config);

            Progress prog = new Progress("Making insert query...", res.size(), 1, "##0");
            if (config.verbose) prog.init();

            String insQuery;

            for (Integer uri : res.keySet()) {
                insQuery = "INSERT INTO " + config.btTableName + " VALUES ";
                Set<Block> blocksSet = res.get(uri).getBlocks();
                Block[] blocks = blocksSet.toArray(new Block[blocksSet.size()]);
                for (int i = 0; i < blocks.length; i++) {
                    insQuery += "('" + uri + "', " + blocks[i].getRepresentation() + "), ";
                }
                insQuery += "('" + uri + "', " + blocks[blocks.length-1].getRepresentation() + ");";
                statement.addBatch(insQuery);
                if (config.verbose) prog.update();
            }
            if (config.verbose) {
                prog.done();
                System.out.print("Executing update batch query, writing to table " +
                                 config.btTableName + "...");
            }

            int[] r = statement.executeBatch();
            if (config.verbose) System.out.println(" Done");
            int sum = 0;
            for (int ins : r) sum += ins;
            System.out.println("Inserted " + sum + " rows.");

        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + e.getMessage());
        } catch (Exception e) {
            throw e;
        } finally {
            close();
        }
    }

    public static void createTable(Statement statement, Config config) {

        boolean tableMade = false;

        while (!tableMade) {
            try {
                if (config.convertUriToInt) {
                    statement.executeUpdate("CREATE TABLE " + config.btTableName +
                                            " (gid int, block bigint);");
                    tableMade = true;
                } else {
                    statement.executeUpdate("CREATE TABLE " + config.btTableName +
                                            " (id text, block bigint);");
                    tableMade = true;
                }
            } catch (SQLException sqlex) {
                System.out.println("Error on table creation with name " + config.btTableName + ":");
                System.out.println(sqlex.getMessage());
                System.out.print("Try to add a new table name suffix (or just hit return twice to abort): ");
                Scanner scan = new Scanner(System.in).useDelimiter("[ \n]"); // Table name is only one word
                config = new Config(config.rawGeoTableName, scan.next(), config.representationDepth, config.overlapsArity);
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
}
