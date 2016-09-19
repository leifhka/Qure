package no.uio.ifi.qure;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.precision.EnhancedPrecisionOp;
import com.vividsolutions.jts.precision.GeometryPrecisionReducer;
import com.vividsolutions.jts.geom.IntersectionMatrix;
import com.vividsolutions.jts.geom.Envelope;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet; 
import java.sql.SQLException;
import java.sql.Statement;

public class GeometryProvider implements SpaceProvider {

    private Map<Integer, GeometrySpace> geometries;
    private Set<Integer> coversUniverse;
    private Set<Integer> urisToInsert;
    private GeometrySpace universe;
    private GeometryFactory geometryFactory;
    private Config config;

    private Connection connect = null;
    private Statement statement = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;

    public GeometryProvider(Config config, Set<Integer> urisToInsert) {
        this.config = config;
        this.urisToInsert = urisToInsert;
        geometryFactory = new GeometryFactory(config.geometryFactoryPrecision);
        coversUniverse = new HashSet<Integer>();
    }

    public GeometryProvider(Config config) {
        this.config = config;
        geometryFactory = new GeometryFactory(config.geometryFactoryPrecision);
        coversUniverse = new HashSet<Integer>();
    }

    private GeometryProvider(Config config, GeometrySpace universe, Map<Integer, GeometrySpace> geometries,
                            Set<Integer> coversUniverse, GeometryFactory geometryFactory) {
        this.config = config;
        this.universe = universe;
        this.geometries = geometries;
        this.coversUniverse = coversUniverse;
        this.geometryFactory = geometryFactory;
    }

    private GeometryProvider(Config config, GeometrySpace universe, Map<Integer, GeometrySpace> geometries, 
                            Set<Integer> coversUniverse, GeometryFactory geometryFactory,
                            Set<Integer> urisToInsert) {
        this.config = config;
        this.universe = universe;
        this.geometries = geometries;
        this.coversUniverse = coversUniverse;
        this.geometryFactory = geometryFactory;
        this.urisToInsert = urisToInsert;
    }

    public void populate() {

        Progress prog;

        if (urisToInsert == null) {
            Map<Integer, String> wkbs = queryDBForGeometries();
            geometries = parseGeometries(wkbs, config.verbose);
            makeUniverse();
        } else {
            Map<Integer, String> wkbs = queryDBForGeometries(urisToInsert);
            geometries = parseGeometries(wkbs, config.verbose);
            obtainUniverse();
        }
    }

    public Map<Integer, GeometrySpace> getSpaces() {
        return geometries;
    }

    public GeometryFactory getGeometryFactory() {
        return geometryFactory;
    }

    private void obtainUniverse() {

        String universeWKB = queryDBForUniverse();
        Map<Integer, String> uwm = new HashMap<Integer, String>();
        uwm.put(0, universeWKB);
        Map<Integer, GeometrySpace> ugm = parseGeometries(uwm, false);
        universe = ugm.get(0);
    }

    private void makeUniverse() {

        Envelope universeEnv = new Envelope();

        Progress prog = new Progress("Making universe...", geometries.keySet().size(), 1, "##0");
        prog.setConvertToLong(true);
        if (config.verbose) prog.init();
        
        for (GeometrySpace gs : geometries.values()) {
            universeEnv.expandToInclude(gs.getGeometry().getEnvelopeInternal());
            if (config.verbose) prog.update();
        }
        universe = new GeometrySpace(geometryFactory.toGeometry(universeEnv));
        if (config.verbose) prog.done();
        if (config.verbose) System.out.println("Universe set to: " + universe.toString());
    }

    public GeometrySpace getUniverse() {
        return universe;
    }

    public Set<Integer> getCoversUniverse() {
        return coversUniverse;
    }

    public Geometry toGeometry(Envelope envelope) {
        return getGeometryFactory().toGeometry(envelope);
    }

    public Set<Integer> keySet() {
        return getSpaces().keySet();
    }

    public GeometrySpace get(Integer uri) {

        if (geometries.containsKey(uri)) {
            return geometries.get(uri);
        } else {
            Set<Integer> s = new HashSet<Integer>();
            s.add(uri);
            Map<Integer, String> m = queryDBForGeometries(s);
            Map<Integer, GeometrySpace> g = parseGeometries(m, false);
            return g.get(uri);
        }
    }

    public GeometryProvider[] splitProvider(int split, int depth) {
        return splitProvider(split == 0, depth);
    }

    public GeometryProvider[] splitProvider(boolean xSplit, int depth) {

        GeometrySpace[] childUniverseres = universe.split(xSplit);
        GeometryProvider[] res = new GeometryProvider[childUniverseres.length];

        for (int i = 0; i < childUniverseres.length; i++) {

            Map<Integer, GeometrySpace> geoms = new HashMap<Integer, GeometrySpace>();
            Set<Integer> coversChildUniverse = new HashSet<Integer>();

            for (Integer uri : keySet()) {

                GeometrySpace gs = get(uri);
                GeometrySpace ngs = gs.intersection(childUniverseres[i]);

                if (!ngs.isEmpty())  {
                    if (ngs.covers(childUniverseres[i]))
                        coversChildUniverse.add(uri);
                    else
                        geoms.put(uri, ngs);
                }
            }

            if (urisToInsert == null)
                res[i] = new GeometryProvider(config, childUniverseres[i], geoms, coversChildUniverse,
                                              geometryFactory);
            else
                res[i] = new GeometryProvider(config, childUniverseres[i], geoms, coversChildUniverse,
                                              geometryFactory, urisToInsert);
        }

        return res;
    }

    public void populateWithExternalOverlapping() {

        if (urisToInsert == null) return; // Do not get external if not in insert mode
        
        Map<Integer, GeometrySpace> extGeos = getExternalOverlapping(universe);
        Set<Integer> extGeoKeys = new HashSet<Integer>(extGeos.keySet());

        for (Integer uri : extGeoKeys) {
            if (extGeos.get(uri).covers(universe))
                extGeos.remove(uri);
        }

        geometries.putAll(extGeos);
    }
    
    private Map<Integer, GeometrySpace> getExternalOverlapping(Space s) {

        GeometrySpace geo = (GeometrySpace) s;

        Map<Integer, String> wkbs = new HashMap<Integer, String>();

        try {
            Class.forName("org.postgresql.Driver");

            connect = DriverManager.getConnection(config.connectionStr);

            statement = connect.createStatement();
            String query = config.geoQuerySelectFromStr;
            query += " WHERE ST_intersects(geom, ST_GeomFromText('" + geo.getGeometry().toString() + "'));";
            statement.execute(query);
            resultSet = statement.getResultSet();
 
            while (resultSet.next()) {
                Integer uri = Integer.parseInt(resultSet.getString(1));
                String wkb = resultSet.getString(2);
                wkbs.put(uri,wkb);
            }
        } catch (Exception e) {
            System.err.println("Error in query process: " + e.toString());
            System.exit(1);
        } finally {
    	    close();
        }

        Map<Integer, GeometrySpace> res = parseGeometries(wkbs, false);
        return res;
    }

    private Map<Integer, GeometrySpace> parseGeometries(Map<Integer,String> wkbs, boolean verbose) {

        Progress prog = new Progress("Parsing geometries...", wkbs.keySet().size(), 1, "##0");  
        prog.setConvertToLong(true);

        WKBReader reader = new WKBReader(geometryFactory);
        GeometryPrecisionReducer geoRed = new GeometryPrecisionReducer(config.geometryPrecision);

        Map<Integer, GeometrySpace> result = new HashMap<Integer,GeometrySpace>(wkbs.keySet().size());

        if (verbose) prog.init();

        for (Integer uri : wkbs.keySet()) {

            String wkb = wkbs.get(uri);
            Geometry geo;

            try {
                geo = reader.read(WKBReader.hexToBytes(wkb));
                geo = geoRed.reduce(geo);
            } catch (Exception e) {
                continue;
            }
            
            if (geo.isValid() && !geo.isEmpty())
                result.put(uri, new GeometrySpace(geo));

            if (verbose) prog.update();
        }
        if (verbose) {
            prog.done();
            int errors = wkbs.keySet().size() - result.values().size();
            if (errors > 0) System.out.println("Unable to parse " + errors + " geometries.");
            System.out.println("Parsed " + result.values().size() + " geometries.");
        }

        return result;
    }

    private String queryDBForUniverse() {

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
            System.exit(1);
        } finally {
    	    close();
        }

        return universeStr;
    }

    private Map<Integer,String> queryDBForGeometries() {

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
                System.out.print("Retriving geometries from table " + config.geoTableName + "...");
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
            System.exit(1);
        } finally {
    	    close();
        }

        return res;
    }

    private Map<Integer,String> queryDBForGeometries(Set<Integer> uris) {

        Map<Integer, String> res = new HashMap<Integer,String>();

        try {
            Class.forName("org.postgresql.Driver");

            if (config.verbose) {
                System.out.println("--------------------------------------");
                System.out.print("Connecting to database " + config.dbName +
                                 " as user " + config.dbUsername + "...");
            }

            connect = DriverManager.getConnection(config.connectionStr);

            if (config.verbose) {
                System.out.println(" Done");
                System.out.print("Retriving geometries from table " + config.geoTableName + "...");
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
