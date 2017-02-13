package no.uio.ifi.qure;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet; 
import java.sql.SQLException;
import java.sql.Statement;


public class DBDataProvider implements RawDataProvider {

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
            Class.forName("org.postgresql.Driver");
            if (config.verbose) {
                System.out.print("Connecting to database " + config.dbName +
                                 " as user " + config.dbUsername + "...");
            }

            connect = DriverManager.getConnection("jdbc:postgresql://localhost/" + config.dbName + "?user=" +
                                                  config.dbUsername + "&password=" + config.dbPWD);
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
            System.out.println("Error in queryForInsert(): " + e.toString());
            e.printStackTrace();
            System.exit(1);
        } finally {
            close();
        }

        return insertUris;
    }

    public Map<Integer, String> getExternalOverlapping(String whereClause) {

        Map<Integer, String> wkbs = new HashMap<Integer, String>();

        try {
            Class.forName("org.postgresql.Driver");

            connect = DriverManager.getConnection(config.connectionStr);

            statement = connect.createStatement();
            String query = config.geoQuerySelectFromStr;
            query += " WHERE " + whereClause + ";";
            statement.execute(query);
            resultSet = statement.getResultSet();
 
            while (resultSet.next()) {
                Integer uri = Integer.parseInt(resultSet.getString(1));
                String wkb = resultSet.getString(2);
                wkbs.put(uri,wkb);
            }
        } catch (Exception e) {
            System.err.println("Error in query process: " + e.toString());
            e.printStackTrace();
            System.exit(1);
        } finally {
    	    close();
        }

        return wkbs;
    }

    public String getUniverse() {

        String universeStr = null;

        try {
            Class.forName("org.postgresql.Driver");
            connect = DriverManager.getConnection(config.connectionStr);

            statement = connect.createStatement();
            String query = "SELECT " + config.geoColumn + " FROM " + config.universeTable + 
                           " WHERE table_name = '" + config.btTableName + "';";
            
            statement.execute(query);
            resultSet = statement.getResultSet();
            resultSet.next(); 
            universeStr = resultSet.getString(1);
        } catch (Exception e) {
            System.err.println("Error querying for universe: " + e.toString());
            e.printStackTrace();
            System.exit(1);
        } finally {
    	    close();
        }

        return universeStr;
    }

    public Map<Integer,String> getSpaces() {

        Map<Integer, String> res = new HashMap<Integer,String>();

        try {
            Class.forName("org.postgresql.Driver");

            if (config.verbose) {
                System.out.print("Connecting to database " + config.dbName +
                                 " as user " + config.dbUsername + "...");
            }

            connect = DriverManager.getConnection(config.connectionStr);

            if (config.verbose) {
                System.out.println(" Done");
                System.out.print("Retriving spaces from table " + config.geoTableName + "...");
            }

            statement = connect.createStatement();
            statement.execute(config.geoQueryStr);
            resultSet = statement.getResultSet();
 
            while (resultSet.next()) {
                Integer uri = Integer.parseInt(resultSet.getString(1));
                String wkb = resultSet.getString(2);
                res.put(uri,wkb);
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

    public Map<Integer,String> getSpaces(Set<Integer> uris) {

        if (uris.isEmpty()) return new HashMap<Integer, String>();

        Map<Integer, String> res = new HashMap<Integer,String>();

        try {
            Class.forName("org.postgresql.Driver");

            if (config.verbose) {
                System.out.print("Connecting to database " + config.dbName +
                                 " as user " + config.dbUsername + "...");
            }

            connect = DriverManager.getConnection(config.connectionStr);

            if (config.verbose) {
                System.out.println(" Done");
                System.out.print("Retriving spaces from table " + config.geoTableName + "...");
            }

            statement = connect.createStatement();
            statement.execute(makeValuesQuery(uris));
            resultSet = statement.getResultSet();
 
            while (resultSet.next()) {
                Integer uri = Integer.parseInt(resultSet.getString(1));
                String wkb = resultSet.getString(2);
                res.put(uri, wkb);
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

    private String makeValuesQuery(Set<Integer> uris) {

        String query = config.geoQuerySelectFromStr + ", (VALUES ";

        Integer[] urisArr = uris.toArray(new Integer[uris.size()]);

        for (int i = 0; i < urisArr.length - 1; i++)
            query += "(" + urisArr[i] + "), ";

        query += "(" + urisArr[urisArr.length-1] + ")"; // Last element, no need for comma after.
        query += ") AS V(uri) WHERE V.uri = " + config.uriColumn + ";";
        
        return query;
    }

    public Map<Block, Block> getEvenSplits() {

        Map<Block, Block> res = new HashMap<Block, Block>();

        try {
            Class.forName("org.postgresql.Driver");

            if (config.verbose)
                System.out.print("Retrieving splitting blocks...");

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
}
