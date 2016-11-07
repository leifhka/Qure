package no.uio.ifi.qure;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.precision.EnhancedPrecisionOp;
import com.vividsolutions.jts.precision.GeometryPrecisionReducer;
import com.vividsolutions.jts.geom.IntersectionMatrix;
import com.vividsolutions.jts.geom.Envelope;

public class GeometryProvider implements SpaceProvider {

    private Map<Integer, GeometrySpace> geometries;
    private boolean updating;
    private Set<Integer> coversUniverse;
    private GeometrySpace universe;
    private GeometryFactory geometryFactory;
    private RawDataProvider dataProvider;
    private Config config;

    public GeometryProvider(Config config, RawDataProvider dataProvider) {
        this.config = config;
        this.dataProvider = dataProvider;
        geometryFactory = new GeometryFactory(config.geometryFactoryPrecision);
        coversUniverse = new HashSet<Integer>();
    }

    private GeometryProvider(Config config, RawDataProvider dataProvider,
                             GeometrySpace universe, Map<Integer, GeometrySpace> geometries, 
                             Set<Integer> coversUniverse, GeometryFactory geometryFactory,
                             boolean updating) {
        this.config = config;
        this.dataProvider = dataProvider;
        this.universe = universe;
        this.geometries = geometries;
        this.coversUniverse = coversUniverse;
        this.geometryFactory = geometryFactory;
        this.updating = updating;
    }

    public void populateBulk() {

        updating = false;
        Map<Integer, String> wkbs = dataProvider.getSpaces();
        geometries = parseGeometries(wkbs, config.verbose);
        makeAndSetUniverse();
    }

    public void populateUpdate() {

        updating = true;
        Set<Integer> urisToInsert = dataProvider.getInsertURIs();
        Map<Integer, String> wkbs = dataProvider.getSpaces(urisToInsert);
        geometries = parseGeometries(wkbs, config.verbose);
        obtainUniverse();
    }

    public Map<Integer, GeometrySpace> getSpaces() { return geometries; }

    public GeometryFactory getGeometryFactory() { return geometryFactory; }

    public GeometrySpace getUniverse() { return universe; }

    public Set<Integer> getCoversUniverse() { return coversUniverse; }

    public Geometry toGeometry(Envelope envelope) { return getGeometryFactory().toGeometry(envelope); }

    public Set<Integer> keySet() { return getSpaces().keySet(); }

    public GeometrySpace makeEmptySpace() { 
         return new GeometrySpace(geometryFactory.createPoint((CoordinateSequence) null));
    }

    private void obtainUniverse() {

        String universeWKB = dataProvider.getUniverse();
        Map<Integer, String> uwm = new HashMap<Integer, String>();
        uwm.put(0, universeWKB);
        Map<Integer, GeometrySpace> ugm = parseGeometries(uwm, false);
        universe = ugm.get(0);
    }

    private GeometrySpace constructUniverse(boolean verbose) {

        Envelope universeEnv = new Envelope();

        Progress prog = new Progress("Making universe...", geometries.keySet().size(), 1, "##0");
        prog.setConvertToLong(true);
        if (verbose) prog.init();
        
        for (GeometrySpace gs : geometries.values()) {
            universeEnv.expandToInclude(gs.getGeometry().getEnvelopeInternal());
            if (verbose) prog.update();
        }
        if (verbose) prog.done();

        return new GeometrySpace(geometryFactory.toGeometry(universeEnv));
    }

    private void makeAndSetUniverse() {
        universe = constructUniverse(config.verbose);
        if (config.verbose) System.out.println("Universe set to: " + universe.toString());
    }

    public GeometrySpace get(Integer uri) {

        if (geometries.containsKey(uri)) {
            return geometries.get(uri);
        } else {
            Set<Integer> s = new HashSet<Integer>();
            s.add(uri);
            Map<Integer, String> m = dataProvider.getSpaces(s);
            Map<Integer, GeometrySpace> g = parseGeometries(m, false);
            return g.get(uri);
        }
    }

    private GeometryProvider makeSubProvider(GeometrySpace uni, Set<Integer> ints) {

        Set<Integer> coversChildUniverse = new HashSet<Integer>();
        Map<Integer, GeometrySpace> overlappingChildUniverse = new HashMap<Integer, GeometrySpace>();
        getIntersections(uni, ints, geometries, overlappingChildUniverse, coversChildUniverse);

        return new GeometryProvider(config, dataProvider, uni, overlappingChildUniverse, coversChildUniverse,
                                    geometryFactory, updating);
    }

    private void getIntersections(GeometrySpace uni, Set<Integer> elems,  Map<Integer, GeometrySpace> geos,
                                  Map<Integer, GeometrySpace> overlapping, Set<Integer> covers) {

        Map<Integer, Space> spMap = new HashMap<Integer, Space>();
        Utils.getIntersections(uni, elems, geos, config.numThreads, spMap, covers);

        for (Integer uri : spMap.keySet()) overlapping.put(uri, (GeometrySpace) spMap.get(uri));
    }

    private GeometryProvider[] makeSubProviders(GeometrySpace childUniL, GeometrySpace childUniR,
                                                Set<Integer> intL, Set<Integer> intR) {

        GeometryProvider subPL = makeSubProvider(childUniL, intL);
        GeometryProvider subPR = makeSubProvider(childUniR, intR);

        return new GeometryProvider[]{subPL, subPR};
    }

    public GeometryProvider[] splitProvider(int split) {

        GeometrySpace[] childUniverseres = universe.split(split);
        return makeSubProviders(childUniverseres[0], childUniverseres[1], keySet(), keySet());
    }

    public GeometryProvider[] splitProvider(int split, EvenSplit evenSplit) {

        Block splitBlock = evenSplit.splitBlock;
        GeometrySpace spL = makeEmptySpace(), spR = makeEmptySpace();
        GeometrySpace[] splitLR = getUniverse().split(split);
        GeometrySpace splitL = splitLR[0], splitR = splitLR[1];

        for (int i = 0; i < splitBlock.depth(); i++) {

            if (splitBlock.getBit(i) == 1L) {
                spL = spL.union(splitL);
                splitLR = splitR.split(split);
                splitL = splitLR[0];
                splitR = splitLR[1];
            } else {
                spR = spR.union(splitR);
                splitLR = splitL.split(split);
                splitL = splitLR[0];
                splitR = splitLR[1];
            }
        }

        return makeSubProviders(spL.union(splitL), spR.union(splitR),
                                evenSplit.intL, evenSplit.intR);
    }

    public void populateWithExternalOverlapping() {

        if (!updating) return; // Do not get external if not in insert mode
        
        Map<Integer, GeometrySpace> external = getExternalOverlapping(universe);
        for (Integer uri : external.keySet())
            geometries.put(uri, universe.intersection(external.get(uri)));
    }

   public String extGeo() {
        GeometrySpace geo = (GeometrySpace) getUniverse();
        String gs = geo.getGeometry().toString();
        String whereClause = "ST_intersects(geom, ST_GeomFromText('" + gs + "')) AND ";
        whereClause += "NOT ST_contains(geom, ST_GeomFromText('" + gs + "'))";
        return whereClause;
    }
   
    public Map<Integer, GeometrySpace> getExternalOverlapping(Space s) {

        GeometrySpace geo = (GeometrySpace) s;
        String gs = geo.getGeometry().toString();
        String whereClause = "ST_intersects(geom, ST_GeomFromText('" + gs + "')) AND ";
        whereClause += "NOT ST_contains(geom, ST_GeomFromText('" + gs + "'))";
        Map<Integer, String> wkbs = dataProvider.getExternalOverlapping(whereClause);
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
}
