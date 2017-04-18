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

	private TimeProvider makeSubProvider(TimeSpace uni, Set<SID> ints) {

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

	private TimeProvider[] makeSubProviders(TimeSpace childUniL, TimeSpace childUniR,
	                                        Set<SID> intL, Set<SID> intR) {

		TimeProvider subPL = makeSubProvider(childUniL, intL);
		TimeProvider subPR = makeSubProvider(childUniR, intR);

		return new TimeProvider[]{subPL, subPR};
	}

	public TimeProvider[] splitProvider(int split) {

		TimeSpace[] childUniverseres = universe.split(split);
		return makeSubProviders(childUniverseres[0], childUniverseres[1], keySet(), keySet());
	}

	public TimeProvider[] splitProvider(int split, EvenSplit evenSplit) {

		Block splitBlock = evenSplit.splitBlock;
		TimeSpace spL = makeEmptySpace(), spR = makeEmptySpace();
		TimeSpace[] splitLR = getUniverse().split(split);
		TimeSpace splitL = splitLR[0], splitR = splitLR[1];

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
}
