package no.uio.ifi.qure.space;

import no.uio.ifi.qure.*;
import no.uio.ifi.qure.relation.*;
import no.uio.ifi.qure.traversal.*;
import no.uio.ifi.qure.dataprovider.*;
import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.bintree.Block;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

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

	private Map<SID, GeometrySpace> geometries;
	private boolean updating;
	private Set<SID> coversUniverse;
	private GeometrySpace universe;
	private GeometryFactory geometryFactory;
	private WKBReader reader;
	private GeometryPrecisionReducer geoRed;
	private RawDataProvider<String> dataProvider;
	private Config config;

	public GeometryProvider(Config config, RawDataProvider<String> dataProvider) {
		this.config = config;
		this.dataProvider = dataProvider;
		geometryFactory = new GeometryFactory(config.geometryFactoryPrecision);
		coversUniverse = new HashSet<SID>();
		reader = new WKBReader(geometryFactory);
		geoRed = new GeometryPrecisionReducer(config.geometryPrecision);

	}

	private GeometryProvider(Config config, RawDataProvider<String> dataProvider,
	                         GeometrySpace universe, Map<SID, GeometrySpace> geometries,
                             Set<SID> coversUniverse, GeometryFactory geometryFactory,
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
		UnparsedIterator<String> wkbs = dataProvider.getSpaces();
		geometries = parseGeometries(wkbs, config.relationSet.getRoles(), config.verbose);
		makeAndSetUniverse();
	}

	public void populateUpdate() {

		updating = true;
		Set<Integer> urisToInsert = dataProvider.getInsertURIs();
		UnparsedIterator<String> wkbs = dataProvider.getSpaces(urisToInsert);
		geometries = parseGeometries(wkbs, config.relationSet.getRoles(), config.verbose);
		obtainUniverse();
	}

	public Map<SID, GeometrySpace> getSpaces() { return geometries; }

	public GeometryFactory getGeometryFactory() { return geometryFactory; }

	public GeometrySpace getUniverse() { return universe; }

	public Set<SID> getCoversUniverse() { return coversUniverse; }

	public Geometry toGeometry(Envelope envelope) { return getGeometryFactory().toGeometry(envelope); }

	public Set<SID> keySet() { return getSpaces().keySet(); }

	public GeometrySpace makeEmptySpace() { 
		return new GeometrySpace(geometryFactory.createPoint((CoordinateSequence) null),
		                         config.geometryPrecision);
	}

	private void obtainUniverse() {

		UnparsedSpace<String> universeWKB = dataProvider.getUniverse();
		universe = new GeometrySpace(parseGeometry(universeWKB.unparsedSpace), config.geometryPrecision);
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

		return new GeometrySpace(geometryFactory.toGeometry(universeEnv), config.geometryPrecision);
	}

	private void makeAndSetUniverse() {
		universe = constructUniverse(config.verbose);
		if (config.verbose) System.out.println("Universe set to: " + universe.toString());
	}

	public GeometrySpace get(SID uri) {

		return geometries.get(uri);
	}

	public GeometryProvider makeSubProvider(Space spUni, Set<SID> ints) {

		GeometrySpace uni = (GeometrySpace) spUni;
		Set<SID> coversChildUniverse = new HashSet<SID>();
		Map<SID, GeometrySpace> overlappingChildUniverse = new HashMap<SID, GeometrySpace>();
		getIntersections(uni, ints, geometries, overlappingChildUniverse, coversChildUniverse);

		return new GeometryProvider(config, dataProvider, uni, overlappingChildUniverse, coversChildUniverse,
		                            geometryFactory, updating);
	}

	private void getIntersections(GeometrySpace uni, Set<SID> elems,  Map<SID, GeometrySpace> geos,
	                              Map<SID, GeometrySpace> overlapping, Set<SID> covers) {

		Map<SID, Space> spMap = new HashMap<SID, Space>();
		Utils.getIntersections(uni, elems, geos, config.numThreads, spMap, covers);

		for (SID uri : spMap.keySet()) {
			overlapping.put(uri, (GeometrySpace) spMap.get(uri));
		}
	}

	public void populateWithExternalOverlapping() {

		if (!updating) return; // Do not get external if not in insert mode
		
		Map<SID, GeometrySpace> external = getExternalOverlapping(universe);
		for (SID uri : external.keySet()) {
			geometries.put(uri, universe.intersection(external.get(uri)));
		}
	}

   public String extGeo() {

		GeometrySpace geo = getUniverse();
		String gs = geo.getGeometry().toString();
		String whereClause = "ST_intersects(geom, ST_GeomFromText('" + gs + "')) AND ";
		whereClause += "NOT ST_contains(geom, ST_GeomFromText('" + gs + "'))";

		return whereClause;
	}
   
	public Map<SID, GeometrySpace> getExternalOverlapping(Space s) {

		GeometrySpace geo = (GeometrySpace) s;
		String gs = geo.getGeometry().toString();
		String whereClause = "ST_intersects(geom, ST_GeomFromText('" + gs + "')) AND ";
		whereClause += "NOT ST_contains(geom, ST_GeomFromText('" + gs + "'))";
		UnparsedIterator<String> wkbs = dataProvider.getExternalOverlapping(whereClause);
		Map<SID, GeometrySpace> res = parseGeometries(wkbs, false);

		return res;
	}

	private Geometry parseGeometry(List<String> wkb) {

		Geometry geo;

		try {
			geo = reader.read(WKBReader.hexToBytes(wkb.get(0))); // wkbs consist of only one string
			geo = geoRed.reduce(geo);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return geo;
	} 

	private void extractAndPutRoledGeos(int id, GeometrySpace gs, Set<Integer> roles, Map<SID, GeometrySpace> result) {

		if (roles == null || roles.isEmpty()) { 
			result.put(new SID(id), gs);
		} else {
			for (Integer role : roles) {
				GeometrySpace roledGS = gs.getPart(role);
				if (!roledGS.isEmpty()) {
					result.put(new SID(id, role), roledGS);
				}
			}
		}
	}

	private Map<SID, GeometrySpace> parseGeometries(UnparsedIterator<String> wkbs, boolean verbose) {
		return parseGeometries(wkbs, null, verbose);
	}

	private Map<SID, GeometrySpace> parseGeometries(UnparsedIterator<String> wkbs,
	                                                Set<Integer> roles, boolean verbose) {

		int total = wkbs.size();
		Progress prog = new Progress("Parsing geometries...", total, 1, "##0");  
		prog.setConvertToLong(true);

		Map<SID, GeometrySpace> result = new HashMap<SID,GeometrySpace>(total);
		int totalParsed = 0;

		if (verbose) prog.init();

		while (wkbs.hasNext()) {

			UnparsedSpace<String> ups = wkbs.next();
			Geometry geo = parseGeometry(ups.unparsedSpace);
			
			if (geo != null && geo.isValid() && !geo.isEmpty()) {
				extractAndPutRoledGeos(ups.uri, new GeometrySpace(geo, config.geometryPrecision), roles, result);
    			totalParsed++;
			}
			if (verbose) prog.update();
		}

		if (verbose) {
			prog.done();
			int errors = total - totalParsed;
			if (errors > 0) System.out.println("Unable to parse " + errors + " geometries.");
			System.out.println("Parsed " + totalParsed + " geometries.");
		}
		return result;
	}
}
