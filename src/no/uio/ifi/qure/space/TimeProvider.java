package no.uio.ifi.qure.space;

import no.uio.ifi.qure.*;
import no.uio.ifi.qure.relation.*;
import no.uio.ifi.qure.traversal.*;
import no.uio.ifi.qure.dataprovider.*;
import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.bintree.Block;

import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;

public class TimeProvider implements SpaceProvider {

	private Map<SID, TimeSpace> times;
	private boolean updating;
	private Set<SID> coversUniverse;
	private TimeSpace universe;
	private DateTimeFormatter format;
	private RawDataProvider<String> dataProvider;
	private Config config;

	public TimeProvider(Config config, RawDataProvider<String> dataProvider) {
		this.config = config;
		this.dataProvider = dataProvider;
		coversUniverse = new HashSet<SID>();
		format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	}

	private TimeProvider(Config config, RawDataProvider<String> dataProvider,
	                     TimeSpace universe, Map<SID, TimeSpace> times, 
	                     Set<SID> coversUniverse, boolean updating) {
		this.config = config;
		this.dataProvider = dataProvider;
		this.universe = universe;
		this.times = times;
		this.coversUniverse = coversUniverse;
		this.updating = updating;
	}

	public void clear() {
    	times.clear();
    	times = null;
    	coversUniverse.clear();
    	coversUniverse = null;
    	dataProvider = null;
	}

	public void populateBulk() {

		updating = false;
		UnparsedIterator<String> timeStrs = dataProvider.getSpaces();
		times = parseTimes(timeStrs, config.relationSet.getRoles(), config.verbose);
		makeAndSetUniverse();
	}

	public void populateUpdate() {

		updating = true;
		Set<Integer> urisToInsert = dataProvider.getInsertURIs();
		UnparsedIterator<String> wkbs = dataProvider.getSpaces(urisToInsert);
		times = parseTimes(wkbs, config.relationSet.getRoles(), config.verbose);
		obtainUniverse();
	}

	public Map<SID, TimeSpace> getSpaces() { return times; }

	public void put(SID uri, Space space) {
		times.put(uri, (TimeSpace) space);
	}

	public TimeSpace getUniverse() { return universe; }

	public Set<SID> getCoversUniverse() { return coversUniverse; }

	public Set<SID> keySet() { return getSpaces().keySet(); }

	public TimeSpace makeEmptySpace() { return new TimeSpace(null, null); }

	private void obtainUniverse() {

		UnparsedSpace<String> universeTime = dataProvider.getUniverse();
		universe = parseTime(universeTime.unparsedSpace);
	}

	private TimeSpace constructUniverse(boolean verbose) {

		Progress prog = new Progress("Making universe...", times.keySet().size(), 1, "##0");
		prog.setConvertToLong(true);
		if (verbose) prog.init();

		TimeSpace univ = new TimeSpace(null, null);
		
		for (TimeSpace ts : times.values()) {
			univ = univ.union(ts);
			if (verbose) prog.update();
		}
		if (verbose) prog.done();

		return univ;
	}

	private void makeAndSetUniverse() {
		universe = constructUniverse(config.verbose);
		if (config.verbose) System.out.println("Universe set to: " + universe.toString());
	}

	public TimeSpace get(SID uri) { return times.get(uri); }

	public TimeProvider makeSubProvider(Space spUni, Set<SID> ints) {

		TimeSpace uni = (TimeSpace) spUni;
		Set<SID> coversChildUniverse = new HashSet<SID>();
		Map<SID, TimeSpace> overlappingChildUniverse = new HashMap<SID, TimeSpace>();
		getIntersections(uni, ints, overlappingChildUniverse, coversChildUniverse);

		return new TimeProvider(config, dataProvider, uni, overlappingChildUniverse,
		                        coversChildUniverse, updating);
	}

	private void getIntersections(TimeSpace uni, Set<SID> elems, Map<SID, TimeSpace> overlapping, Set<SID> covers) {

		Map<SID, Space> spMap = new HashMap<SID, Space>();
		getIntersections(uni, elems, config.numThreads, spMap, covers);

		for (SID uri : spMap.keySet()) overlapping.put(uri, (TimeSpace) spMap.get(uri));
	}

	public void populateWithExternalOverlapping() {

		if (!updating) return; // Do not get external if not in insert mode
		
		Map<SID, TimeSpace> external = getExternalOverlapping(universe);
		for (SID uri : external.keySet()) {
			times.put(uri, universe.intersection(external.get(uri)));
		}
	}

   public String extTime() {

		TimeSpace uni = getUniverse();
		String start = uni.getStart().toString();
		String stop = uni.getEnd().toString();
		String whereClause = "('" + start + "', '" + stop + "') OVERLAPS (starttime, stoptime) AND ";
		whereClause += "NOT (BEFORE(starttime, '" + start + "') AND BEFORE('" + stop + "', stoptime))";

		return whereClause;
	}
   
	public Map<SID, TimeSpace> getExternalOverlapping(Space s) {

		UnparsedIterator<String> timeStrs = dataProvider.getExternalOverlapping(extTime());
		Map<SID, TimeSpace> res = parseTimes(timeStrs, config.relationSet.getRoles(), false);
		return res;
	}

	private TimeSpace parseTime(List<String> timeStr) {

		String start = timeStr.get(0);
		String stop = timeStr.get(1);
		TimeSpace newTime = null;

		try {
			newTime = new TimeSpace(LocalDateTime.parse(start,format), LocalDateTime.parse(stop,format));
		} catch (DateTimeParseException e) {
			System.err.println(e.toString());
			System.exit(1);
		}
		return newTime;
	}	

	private void extractAndPutRoledTimes(int id, TimeSpace ts, Set<Integer> roles, Map<SID, TimeSpace> result) {

		if (roles == null || roles.isEmpty()) { 
			result.put(new SID(id), ts);
		} else {
			for (Integer role : roles) {
				TimeSpace roledTS = ts.getPart(role);
				if (!roledTS.isEmpty()) {
					result.put(new SID(id, role), roledTS);
				}
			}
		}
	}

	private Map<SID, TimeSpace> parseTimes(UnparsedIterator<String> timeStrs, Set<Integer> roles, boolean verbose) {

		int total = timeStrs.size();
		Progress prog = new Progress("Parsing timestamp pairs...", total, 1, "##0");  
		prog.setConvertToLong(true);

		Map<SID, TimeSpace> result = new HashMap<SID,TimeSpace>(total);
		int totalParsed = 0;

		if (verbose) prog.init();

		while (timeStrs.hasNext()) {

			UnparsedSpace<String> ups = timeStrs.next();

			TimeSpace newTime = parseTime(ups.unparsedSpace);

			if (newTime != null && !newTime.isEmpty()) {
				extractAndPutRoledTimes(ups.uri, newTime, roles, result);
    			totalParsed++;
			}
		   
			if (verbose) prog.update();
		}
		if (verbose) {
			prog.done();
			int errors = total - totalParsed;
			if (errors > 0) System.out.println("Unable to parse " + errors + " timestamp pairs.");
			System.out.println("Parsed " + result.values().size() + " timestamp pairs.");
		}

		return result;
	}

	private String getOverlapsWhere(Overlaps rel, int i, int j) {
		String where = "";
		if (rel.getArgRole(i) == 0 && rel.getArgRole(j) == 0) { // 0, 0
			where += "((T" + i + ".starttime <= T" + j + ".stoptime AND\n";
			where += "  T" + j + ".stoptime <= T" + i + ".stoptime) OR ";
			where += " (T" + j + ".starttime <= T" + i + ".stoptime AND\n";
			where += "  T" + i + ".stoptime <= T" + j + ".stoptime))";
		} else if ((rel.getArgRole(i) | TimeSpace.INTERIOR) == TimeSpace.INTERIOR && (rel.getArgRole(j) | TimeSpace.INTERIOR) == TimeSpace.INTERIOR) { // (0 OR INTERIOR), (0 OR INTERIOR), but not 0,0
			where += "((T" + i + ".starttime < T" + j + ".stoptime AND\n";
			where += "  T" + j + ".stoptime <= T" + i + ".stoptime) OR ";
			where += " (T" + j + ".starttime < T" + i + ".stoptime AND\n";
			where += "  T" + i + ".stoptime <= T" + j + ".stoptime))";
		} else if (rel.getArgRole(i) == 0 && rel.getArgRole(j) == TimeSpace.FIRST) { // 0, FIRST
			where += "(T" + i + ".starttime <= T" + j + ".starttime AND\n";
			where += " T" + j + ".starttime <= T" + i + ".stoptime)";
		} else if (rel.getArgRole(i) == 0 && rel.getArgRole(j) == TimeSpace.LAST) { // 0, LAST
			where += "(T" + i + ".starttime <= T" + j + ".stoptime AND\n";
			where += " T" + j + ".stoptime <= T" + i + ".stoptime)";
		} else if (rel.getArgRole(i) == TimeSpace.INTERIOR && rel.getArgRole(j) == TimeSpace.FIRST) { // INTERIOR, FIRST
			where += "(T" + i + ".starttime < T" + j + ".starttime AND\n";
			where += " T" + j + ".starttime < T" + i + ".stoptime)";
		} else if (rel.getArgRole(i) == TimeSpace.INTERIOR && rel.getArgRole(j) == TimeSpace.LAST) { // INTERIOR, LAST
			where += "(T" + i + ".starttime < T" + j + ".stoptime AND\n";
			where += " T" + j + ".stoptime < T" + i + ".stoptime)";
		} else if (rel.getArgRole(i) == TimeSpace.FIRST && rel.getArgRole(j) == TimeSpace.FIRST) { // FIRST, FIRST
			where += "(T" + i + ".starttime = T" + j + ".starttime)";
		} else if (rel.getArgRole(i) == TimeSpace.FIRST && rel.getArgRole(j) == TimeSpace.LAST) { // FIRST, LAST
			where += "(T" + i + ".starttime = T" + j + ".stoptime)";
		} else if (rel.getArgRole(i) == TimeSpace.LAST && rel.getArgRole(j) == TimeSpace.LAST) { // LAST, LAST
			where += "(T" + i + ".stoptime = T" + j + ".stoptime)";
		} else {
			System.err.println("Cannot make query for roles " + rel.toString());
			System.exit(1);
		}
		return where;
	}

	private String getOverlapsWhere(Overlaps rel) {
		String where = getOverlapsWhere(rel, rel.getArg(0), rel.getArg(1));
		if (!where.equals("")) return where;
		return getOverlapsWhere(rel, rel.getArg(1), rel.getArg(0));
	}

	private String getPartOfWhere(PartOf rel) {
		int a0 = rel.getArg(0);
		int a1 = rel.getArg(1);
		String where = "";
		if (rel.getArgRole(a0) == 0 && rel.getArgRole(a1) == 0) { // 0, 0
			where += "(T" + a1 + ".starttime <= T" + a0 + ".starttime AND\n";
			where += " T" + a0 + ".stoptime <= T" + a1 + ".stoptime)";
		} else if (rel.getArgRole(a0) == TimeSpace.FIRST && rel.getArgRole(a1) == 0) { // FIRST, 0
			where += "(T" + a1 + ".starttime <= T" + a0 + ".starttime AND\n";
			where += " T" + a0 + ".starttime <= T" + a1 + ".stoptime)";
		} else if (rel.getArgRole(a0) == TimeSpace.LAST && rel.getArgRole(a1) == 0) { // LAST, 0
			where += "(T" + a1 + ".starttime <= T" + a0 + ".stoptime AND\n";
			where += " T" + a0 + ".stoptime <= T" + a1 + ".stoptime)";
		} else if (rel.getArgRole(a0) == TimeSpace.FIRST && rel.getArgRole(a1) == TimeSpace.INTERIOR) { // FIRST, INTERIOR
			where += "(T" + a1 + ".starttime < T" + a0 + ".starttime AND\n";
			where += " T" + a0 + ".starttime < T" + a1 + ".stoptime)";
		} else if (rel.getArgRole(a0) == TimeSpace.LAST && rel.getArgRole(a1) == TimeSpace.INTERIOR) { // LAST, INTERIOR
			where += "(T" + a1 + ".starttime < T" + a0 + ".stoptime AND\n";
			where += " T" + a0 + ".stoptime < T" + a1 + ".stoptime)";
		} else if (rel.getArgRole(a0) == TimeSpace.FIRST && rel.getArgRole(a1) == TimeSpace.FIRST) { // FIRST, FIRST
			where += "(T" + a0 + ".starttime = T" + a1 + ".starttime)";
		} else if (rel.getArgRole(a0) == TimeSpace.FIRST && rel.getArgRole(a1) == TimeSpace.LAST) { // FIRST, LAST
			where += "(T" + a0 + ".starttime = T" + a1 + ".stoptime)";
		} else if (rel.getArgRole(a0) == TimeSpace.LAST && rel.getArgRole(a1) == TimeSpace.FIRST) { // LAST, FIRST
			where += "(T" + a0 + ".stoptime = T" + a1 + ".starttime)";
		} else if (rel.getArgRole(a0) == TimeSpace.LAST && rel.getArgRole(a1) == TimeSpace.LAST) { // LAST, LAST
			where += "(T" + a0 + ".stoptime = T" + a1 + ".stoptime)";
		} else {
			System.err.println("Cannot make query for roles " + rel.toString());
			System.exit(1);
		}
		return where;
	}

	private String getBeforeWhere(Before rel) {
		int a0 = rel.getArg(0);
		int a1 = rel.getArg(1);
		String where = "";
		if ((rel.getArgRole(a0) | TimeSpace.LAST) == TimeSpace.LAST && (rel.getArgRole(a1) | TimeSpace.FIRST) == TimeSpace.FIRST) {
			where += "(T" + a0 + ".stoptime < T" + a1 + ".starttime)";
		} else if ((rel.getArgRole(a0) | TimeSpace.FIRST) == TimeSpace.FIRST && (rel.getArgRole(a1) | TimeSpace.LAST) == TimeSpace.LAST) {
			where += "(T" + a0 + ".starttime < T" + a1 + ".stoptime)";
		} else if ((rel.getArgRole(a0) | TimeSpace.FIRST) == TimeSpace.FIRST && (rel.getArgRole(a1) | TimeSpace.FIRST) == TimeSpace.FIRST) {
			where += "(T" + a0 + ".starttime < T" + a1 + ".starttime)";
		} else if ((rel.getArgRole(a0) | TimeSpace.LAST) == TimeSpace.LAST && (rel.getArgRole(a1) | TimeSpace.LAST) == TimeSpace.LAST) {
			where += "(T" + a0 + ".stoptime < T" + a1 + ".stoptime)";
		}
		return where;
	}

	public String toSQL(AtomicRelation rel, String[] vals, Config config) {

		String[] selFroWhe = rel.makeSelectFromWhereParts(config.geoTableName, config.uriColumn, vals);
		String select = "SELECT " + selFroWhe[0] + "\n";
		String from = "FROM " + selFroWhe[1] + "\n";

		String where = "WHERE ";
		if (!selFroWhe[2].equals("")) where += selFroWhe[2] + " AND\n";
		if (rel instanceof Overlaps) {
			where += getOverlapsWhere((Overlaps) rel);
		} else if (rel instanceof PartOf) {
			where += getPartOfWhere((PartOf) rel);
		} else if (rel instanceof Before) {
			where += getBeforeWhere((Before) rel);
		} else {
			return null;
		}
		return select + from + where;
	}
}
