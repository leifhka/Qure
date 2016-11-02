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

        ArrayList<Config> configs = new ArrayList<Config>();

        //configs.add(new Config("dallas", "t2", 15, 3, 30));
        configs.add(new Config("osm_no", "t2", 15, 3, 30));
        configs.add(new Config("osm_dk", "t2", 15, 3, 30));

        configs.add(new Config("dallas", "t2", 13, 3, 30));
        configs.add(new Config("osm_no", "t2", 13, 3, 30));
        configs.add(new Config("osm_dk", "t2", 13, 3, 30));

        configs.add(new Config("dallas", "t2", 15, 2, 30));
        configs.add(new Config("osm_no", "t2", 15, 2, 30));
        configs.add(new Config("osm_dk", "t2", 15, 2, 30));

        configs.add(new Config("dallas", "t2", 15, 3, 20));
        configs.add(new Config("osm_no", "t2", 15, 3, 20));
        configs.add(new Config("osm_dk", "t2", 15, 3, 20));

        configs.add(new Config("npd", "t2", 13, 3, 30));
        configs.add(new Config("npd", "t2", 10, 3, 30));
        configs.add(new Config("npd", "t2", 13, 3, 20));
        configs.add(new Config("npd", "t2", 13, 2, 30));

        runAllInsertBM(configs, 100, 20);
        //runBulk(configs.get(0));
        //runMany(configs);
    }

    private static void runMany(Collection<Config> configs) {
        for (Config config : configs) {
            runBulk(config);
            try {
                Thread.sleep(1000*60*5);
            } catch (InterruptedException ex) {
                continue;
            }
        }
    }

    private static void runAllInsertBM(Collection<Config> configs, int n, int iterations) {
        for (Config config : configs) {
            runManyInsertBM(config, n, iterations);
            try {
                Thread.sleep(1000*60*1);
            } catch (InterruptedException ex) {
                continue;
            }
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
                FileWriter fw = new FileWriter("output_ins.txt", true); //TODO: change back to output.txt
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

        SpaceProvider geometries = new GeometryProvider(config, new DBDataProvider(config));
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
        takeTime(before, after, config.rawBTTableName, "Construction time", true, true);
        takeTime(before, after2, config.rawBTTableName, "Total time", true, true);
    }

    public static void runManyInsertBM(Config config, int n, int iterations) {
        for (int i = 1; i <= iterations; i++) {
            System.out.println("======================================");
            System.out.println("Iteration: " + i + " / " + iterations + " of " + config.rawBTTableName);
            System.out.println("--------------------------------------");
            runInsertBM(config, n);
        }
        finishTime();
    }

    public static void runInsertBM(Config config, int n) {
        deleteRandomBintrees(n, config);
        runInsert(config);
    }

    public static void runInsert(Config config) {

        long beforeAll = System.currentTimeMillis();

        RawDataProvider dataProvider = new DBDataProvider(config);
        SpaceProvider geometries = new GeometryProvider(config, dataProvider);
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
        takeTime(before, after, config.rawBTTableName, "Construct time", true, false);
        takeTime(beforeAll, afterAll, config.rawBTTableName, "Total insert time", true, false);
    }

    public static void writeBintreesToDB(Representation rep, Set<Block> oldSplits, Config config) throws Exception {

        Map<Integer, Bintree> bintrees = rep.getRepresentation();
        Space universe = rep.getUniverse();
        Map<Block, Block> splits = rep.getEvenSplits();

        try {
            Class.forName("org.postgresql.Driver");

            if (config.verbose) {
                System.out.print("Connecting to database " + config.dbName +
                                 " as user " + config.dbUsername + "...");
            }

            connect = DriverManager.getConnection("jdbc:postgresql://localhost/" + config.dbName +
                                                  "?user=" + config.dbUsername
                      + "&password=" + config.dbPWD);
            if (config.verbose) System.out.println(" Done");

            statement = connect.createStatement();

            DatabaseMetaData meta = connect.getMetaData();
            ResultSet res = meta.getTables(config.dbName, config.schemaName, config.rawBTTableName, null);
            boolean insert = res.next();
            if (insert)
                deleteBintrees(rep, oldSplits, config);
            else
                createTable(statement, config);
            
            Set<Integer> notNeedingUP = (insert) ? getURIsNotNeedingUniquePart(config, bintrees.keySet()) : new HashSet<Integer>();            
            insertBintrees(bintrees, config, notNeedingUP);

            if (!insert) {
                insertUniverse(universe, config);
                createIndexStructures(config);
            }
            if (!splits.isEmpty()) insertSplits(splits, config);

        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + e.getMessage());
        } catch (Exception e) {
            throw e;
        } finally {
            close();
        }
    }

    private static Set<Integer> getURIsNotNeedingUniquePart(Config config, Set<Integer> elems) {

        Set<Integer> result = new HashSet<Integer>();
        
        try {

            statement.execute("SELECT gid FROM " + config.btTableName + " WHERE block % 2 != 0;");
            ResultSet hasUniquePart = statement.getResultSet();
            
            while (hasUniquePart.next()) {
                Integer uri = hasUniquePart.getInt(1);
                if (elems.contains(uri)) result.add(uri);
            }
        } catch (Exception e) {
            System.out.println("Error in getURIsNotNeedingUniquePart(): " + e.toString());
            e.printStackTrace();
            System.exit(1);
        }
        return result;
    }

    public static void insertBintrees(Map<Integer, Bintree> bintrees, Config config, Set<Integer> notNeedingUP) 
        throws SQLException {

        Progress prog = new Progress("Making query...", bintrees.size(), 1, "##0");
        if (config.verbose) prog.init();

        String query;

        for (Integer uri : bintrees.keySet()) {

            query = "INSERT INTO " + config.btTableName + " VALUES ";
            Set<Block> blocksArr = bintrees.get(uri).getBlocks();
            Block[] blocks = blocksArr.toArray(new Block[blocksArr.size()]);

            for (int i = 0; i < blocks.length; i++) {
                long b = blocks[i].getRepresentation() & ((notNeedingUP.contains(uri)) ? -2L : -1L);
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
                                config.btTableName + "', '" + universeStr + "');");
        if (config.verbose) System.out.println(" Done.");
    }


    public static void insertSplits(Map<Block, Block> splits, Config config) 
        throws SQLException {

        if (config.verbose) System.out.print("Inserting even splits into " + config.splitTable + "...");

        DatabaseMetaData meta = connect.getMetaData();
        ResultSet res = meta.getTables(null, "split", config.rawBTTableName, null);
        boolean insert = res.next();
        if (!insert) {
            if (config.blockSize > 31)
                statement.executeUpdate("CREATE TABLE " + config.splitTable + " (block bigint, split bigint);");
            else
                statement.executeUpdate("CREATE TABLE " + config.splitTable + " (block int, split int);");
        }
        String splitStr = "INSERT INTO " + config.splitTable + " VALUES ";
        for (Block block : splits.keySet())
            splitStr += "(" + block.getRepresentation() + ", " + splits.get(block).getRepresentation() + "), ";
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
                                + "_wit_index ON " + config.btTableName + "(block) WHERE block % 2 != 0;");

        if (config.verbose) System.out.println(" Done.");
    }

    private static void deleteRandomBintrees(int n, Config config) {
        try {
            Class.forName("org.postgresql.Driver");

            connect = DriverManager.getConnection("jdbc:postgresql://localhost/" + config.dbName + "?user=" +
                                                  config.dbUsername + "&password=" + config.dbPWD);
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
        for (int i = 0; i < deleted.length; i++)
            delSum += deleted[i];

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
            if (bs.contains(b)) return prev;
            prev = b;
            b = b.getParent();
        }

        return null;
    }

    public static Block getParentInSetOld(Block block, Set<Block> bs) {

        Block smallest = Block.TOPBLOCK;

        for (Block b : bs) {
            if (block.blockPartOf(b) && b.blockPartOf(smallest))
                smallest = b;
        }
        
        // Need to split last block, as bs only contains blocks that were split
        Block[] lr = smallest.split();
        if (block.blockPartOf(lr[0])) 
            return lr[0];
        else if (block.blockPartOf(lr[1])) 
            return lr[1];
        else 
            return null;
    }

    public static String makePrefixQuery(String block, int blockSize) {
        String expr;
        if (blockSize > 31)
            expr = "(((1::bigint << (63 - ((" + block + " & 127) >> 1))::int)) - 1) | " + block + ")";
        else
            expr = "(((1 << (31 - ((" + block + " & 63) >> 1))) - 1) | " + block + ")";
        String c1 = block + " & -2 <= block";
        String c2 = expr + " >= block";
        return "(" + c1 + " AND " + c2 + ")";
    }

    public static void createTable(Statement statement, Config config) {

        boolean tableMade = false;

        while (!tableMade) {
            try {
                String blockType = (config.blockSize > 31) ? "bigint" : "int";
                if (config.convertUriToInt)
                    statement.executeUpdate("CREATE TABLE " + config.btTableName + " (gid int, block " + blockType + ");");
                else
                    statement.executeUpdate("CREATE TABLE " + config.btTableName + " (id text, block " + blockType + ");");
                tableMade = true;
            } catch (SQLException sqlex) {
                System.out.println("Error on table creation with name " + config.btTableName + ":");
                System.out.println(sqlex.getMessage());
                System.out.print("Try to add a new table name suffix (or just hit return twice to abort): ");
                Scanner scan = new Scanner(System.in).useDelimiter("[ \n]"); // Table name is only one word
                config = new Config(config.rawGeoTableName, scan.next(), config.maxIterDepth, config.overlapsArity, config.blockMemberCount);
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
