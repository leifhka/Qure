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

        runBulk(new Config("dallas", 15, 3));
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
        System.out.println("* Max iter. depth: " + config.maxIterDepth);
        System.out.println("* Block member count: " + config.blockMemberCount);
        System.out.println("* Representation depth: " + config.representationDepth);
        System.out.println("* Overlaps arity: " + config.overlapsArity);
        System.out.println("* Write to: " + config.btTableName);
        System.out.println("--------------------------------------");
    }
    
    public static void runBulk(Config config) {

        if (config.verbose) printConfig(config);

        SpaceProvider geometries = new GeometryProvider(config);
        geometries.populate();
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

    public static void runInsert(Config config, Set<Integer> urisToInsert) {

        long beforeAll = System.currentTimeMillis();

        SpaceProvider geometries = new GeometryProvider(config, urisToInsert);
        geometries.populate();
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

    public static void runBenchmark(Config config, String bmName, String btName, String geoName) {

        try {
            Class.forName("org.postgresql.Driver");
            connect = DriverManager.getConnection("jdbc:postgresql://localhost/" + config.dbName + "?user=" +
                                                  config.dbUsername + "&password=" + config.dbPWD);
            statement = connect.createStatement();

            statement.execute("SELECT gid FROM " + bmName + ";");
            resultSet = statement.getResultSet();
            Set<Integer> gidsSet = new HashSet<Integer>();
            while (resultSet.next()) 
                gidsSet.add(Integer.parseInt(resultSet.getString(1)));

            Integer[] gids = gidsSet.toArray(new Integer[gidsSet.size()]); 
            String[] btQueries = new String[gids.length];
            String[] geoQueries = new String[gids.length];

            for (int i = 0; i < gids.length; i++) {
                btQueries[i] = makeBTOverlapsQuery(gids[i].toString(), btName);
                geoQueries[i] = makeGeoOverlapsQuery(gids[i].toString(), geoName);
            }
            System.out.println("Done with gids!");

            long geoBefore = System.currentTimeMillis();
            for (String query : geoQueries) {
                statement.execute(query);
                resultSet = statement.getResultSet();
            }
            long geoAfter = System.currentTimeMillis();
            System.out.println("Done with GEO!");

            long btBefore = System.currentTimeMillis();
            for (String query : btQueries) {
                statement.execute(query);
                resultSet = statement.getResultSet();
            }
            long btAfter = System.currentTimeMillis();

            long geoT = 0;
            long btT = 0;
            for (int i = 0; i < gids.length; i++) {
                long gB = System.currentTimeMillis();
                statement.execute(geoQueries[i]);
                resultSet = statement.getResultSet();
                long gA = System.currentTimeMillis();
                geoT += (gA - gB);

                long btB = System.currentTimeMillis();
                statement.execute(btQueries[i]);
                resultSet = statement.getResultSet();
                long btA = System.currentTimeMillis();
                btT += (btA - btB);
            }

            System.out.println("BT: " + (btAfter - btBefore));
            System.out.println("Geo: " + (geoAfter - geoBefore));
            System.out.println("Both: " + btT + " "  + geoT);

        } catch (Exception e) {
            System.err.println("Error in runBenchmark(): " + e.toString());
        }
    }

    public static String makeBTOverlapsQuery(String gid, String btName) {

        String q;
        q =  "SELECT DISTINCT T2.gid ";
        q += "FROM " + btName + " AS T1, " + btName + " AS T2, ";
        q +=   "(VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13), (14), (15), (16), (17), (18), (19), (20), (21), (22), (23), (24), (25), (26), (27), (28), (29), (30), (31), (32), (33), (34), (35), (36), (37), (38), (39), (40), (41), (42), (43), (44), (45), (46), (47), (48), (49), (50), (51), (52), (53), (54), (55), (56)) AS V(n) ";
        q += "WHERE T1.gid = " + gid + " AND ";
        q +=       "((T1.block <= T2.block AND ";
        q +=        "(((1::bigint << ((64 - 1) - (T1.block & ((1 << 6)-1))::int)) - 1) | T1.block) >= T2.block) OR ";
        q +=       "((T1.block & ((1 << 6)-1)) >= V.n AND T2.block = ((T1.block & ~((1::bigint << ((64 - V.n) - 1)) - 1)) | V.n)) OR ";
        q +=       "T2.block = 0);";
        return q;
    }

    public static String makeGeoOverlapsQuery(String gid, String geoName) {

        String q = "SELECT DISTINCT T2.gid FROM " + geoName + " AS T1, " + geoName + " AS T2 WHERE T1.gid = " + gid + " AND ST_intersects(T1.geom, T2.geom);";
        return q;
    }


    public static Set<Integer> queryForInsertURIs(Config config) {

        Set<Integer> res = new HashSet<Integer>();

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

            String query = "SELECT * FROM " + config.btTableName + ";";
            statement.execute(query);
            resultSet = statement.getResultSet();
 
            while (resultSet.next()) {
                Integer uri = Integer.parseInt(resultSet.getString(1));
                res.add(uri);
            }
        } catch (SQLException e) {
            System.err.println("SQLError: " + e.toString());
            System.err.println(e.getNextException());
        } catch (Exception e) {
            System.out.println("Error in queryForInsert(): " + e.toString());
        } finally {
            close();
        }

        return res;
    }

    public static void writeBintreesToDB(Representation rep, Config config) throws Exception {

        Map<Integer, Bintree> res = rep.getRepresentation();
        Space universe = rep.getUniverse();

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

            createTable(statement, config);

            if (config.verbose) System.out.println("Table " + config.btTableName + " created.");
            Progress prog = new Progress("Making query...", res.size(), 1, "##0");
            if (config.verbose) prog.init();

            String query;

            for (Integer uri : res.keySet()) {

                query = "INSERT INTO " + config.btTableName + " VALUES ";
                Set<Block> blocksArr = res.get(uri).getBlocks();
                Block[] blocks = blocksArr.toArray(new Block[blocksArr.size()]);

                boolean witDone = false;

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

            if (config.verbose) System.out.print("Inserting universe into " + config.universeTable + "...");
            String universeStr = universe.toDBString();
            statement.executeUpdate("INSERT INTO " + config.universeTable + " VALUES ('" +
                                    config.btTableName + "', '" + universeStr + "');");
            if (config.verbose) System.out.println(" Done.");

            if (config.verbose) System.out.print("Constructing index structures...");
            statement.executeUpdate("CREATE INDEX " + config.rawBTTableName + "_gid_index ON " + config.btTableName + "(gid);");
            statement.executeUpdate("CREATE INDEX " + config.rawBTTableName + "_block_index ON " + config.btTableName + "(block);");
            statement.executeUpdate("CREATE INDEX " + config.rawBTTableName + "_wit_index ON " + config.btTableName + "(block) WHERE block % 2 != 0;");
            if (config.verbose) System.out.println(" Done.");

        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + e.getMessage());
        } catch (Exception e) {
            throw e;
        } finally {
            close();
        }
    }

    public static String makePrefixQuery(String block) {
        String expr = "(((1::bigint << (63 - (" + block + " & ((1 << 6)-1))::int)) - 1) | " + block + ")";
        String c1 = block + " <= block";
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

            // To update the representations we will first delete the old representations for
            // the blocks were a new representation is created.
            Progress prog = new Progress("Making delete query...", res.size(), 1, "##0");
            if (config.verbose) prog.init();

            for (Integer uri : res.keySet()) {
                String delQuery = "DELETE FROM " + config.btTableName + " WHERE ";
                delQuery += config.uriColumn + " = '" + uri + "' AND (false";
                for (Block block : res.get(uri).getBlocks()) {
                    String blockStr = block.toString();
                    if (block.depth() <= config.representationDepth)
                        delQuery += " OR block = " + blockStr;
                    else
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
            prog = new Progress("Making insert query...", res.size(), 1, "##0");
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
                System.out.print("Try a new table name (or just hit return twice to abort): ");
                Scanner scan = new Scanner(System.in).useDelimiter("[ \n]"); // Table name is only one word
                config.btTableName = scan.next();
                System.out.println("");
                if (config.btTableName.equals("")) {
                    close();
                    System.exit(0);
                }
            } 
        }
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
