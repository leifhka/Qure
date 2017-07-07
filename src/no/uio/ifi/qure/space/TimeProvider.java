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
		times = parseTimes(timeStrs, config.verbose);
		makeAndSetUniverse();
	}

	public void populateUpdate() {

		updating = true;
		Set<Integer> urisToInsert = dataProvider.getInsertURIs();
		UnparsedIterator<String> wkbs = dataProvider.getSpaces(urisToInsert);
		times = parseTimes(wkbs, config.verbose);
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
		getIntersections(uni, ints, times, overlappingChildUniverse, coversChildUniverse);

		return new TimeProvider(config, dataProvider, uni, overlappingChildUniverse,
		                        coversChildUniverse, updating);
	}

	private void getIntersections(TimeSpace uni, Set<SID> elems,  Map<SID, TimeSpace> tms,
	                              Map<SID, TimeSpace> overlapping, Set<SID> covers) {

		Map<SID, Space> spMap = new HashMap<SID, Space>();
		Utils.getIntersections(uni, elems, tms, config.numThreads, spMap, covers);

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
		Map<SID, TimeSpace> res = parseTimes(timeStrs, false);
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

	private Map<SID, TimeSpace> parseTimes(UnparsedIterator<String> timeStrs, boolean verbose) {

		int total = timeStrs.size();
		Progress prog = new Progress("Parsing timestamp pairs...", total, 1, "##0");  
		prog.setConvertToLong(true);

		Map<SID, TimeSpace> result = new HashMap<SID,TimeSpace>(total);

		if (verbose) prog.init();

		while (timeStrs.hasNext()) {

			UnparsedSpace<String> ups = timeStrs.next();

			TimeSpace newTime = parseTime(ups.unparsedSpace);
		   
			if (!newTime.isEmpty()) {
				result.put(new SID(ups.uri), newTime);
			}
			if (verbose) prog.update();
		}
		if (verbose) {
			prog.done();
			int errors = total - result.values().size();
			if (errors > 0) System.out.println("Unable to parse " + errors + " timestamp pairs.");
			System.out.println("Parsed " + result.values().size() + " timestamp pairs.");
		}

		return result;
	}

	private String[] getRolePart(int role, String table) {
		return null; //TODO
	}

	private String[][] getRoleParts(AtomicRelation rel, int args) {
		String[][] argRoleParts = new String[args][];
		for (int i = 0; i < args; i++) {
			argRoleParts[i] = getRolePart(rel.getArgRole(i), "T." + i);
		}
		return argRoleParts;
	}

	public String toSQL(AtomicRelation rel, String[] vals, Config config) {
		String[] selFroWhe = rel.makeSelectFromWhereParts(config.geoTableName, config.uriColumn, vals);
		String[][] roleParts = getRoleParts(rel, vals.length);
		String select = "SELECT " + selFroWhe[0] + "\n";
		String from = "FROM " + selFroWhe[1] + "\n";
		String where = "WHERE ";
		if (!selFroWhe[2].equals("")) where += selFroWhe[2] + " AND\n";
		if (rel instanceof Overlaps) {
			where += "((T" + rel.getArg(0) + ".starttime <= T" + rel.getArg(1) + ".stoptime AND T" + rel.getArg(1) + ".stoptime <= T" + rel.getArg(0) + ".stoptime) OR ";
			where += "(T" + rel.getArg(1) + ".starttime <= T" + rel.getArg(0) + ".stoptime AND T" + rel.getArg(0) + ".stoptime <= T" + rel.getArg(1) + ".stoptime))";
		} else if (rel instanceof PartOf) {
			where += "T" + rel.getArg(1) + ".starttime <= T" + rel.getArg(0) + ".starttime AND T" + rel.getArg(0) + ".stoptime <= T" + rel.getArg(1) + ".stoptime";
		} else if (rel instanceof Before) {
			where += "BEFORE(T" + rel.getArg(0) + ".stoptime, T" + rel.getArg(1) + ".starttime)";
		} else {
			return null;
		}
		return select + from + where;
	}
}
