package no.uio.ifi.qure;

import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.List;

public class TimeProvider implements SpaceProvider {

    private Map<Integer, TimeSpace> times;
    private boolean updating;
    private Set<Integer> coversUniverse;
    private TimeSpace universe;
    private RawDataProvider dataProvider;
    private Config config;

    public TimeProvider(Config config, RawDataProvider dataProvider) {
        this.config = config;
        this.dataProvider = dataProvider;
        coversUniverse = new HashSet<Integer>();
    }

    private TimeProvider(Config config, RawDataProvider dataProvider,
                         TimeSpace universe, Map<Integer, TimeSpace> times, 
                         Set<Integer> coversUniverse, boolean updating) {
        this.config = config;
        this.dataProvider = dataProvider;
        this.universe = universe;
        this.times = times;
        this.coversUniverse = coversUniverse;
        this.updating = updating;
    }

    public void populateBulk() {

        updating = false;
        Map<Integer, List<String>> timeStrs = dataProvider.getSpaces();
        times = parseTimes(timeStrs, config.verbose);
        makeAndSetUniverse();
    }

    public void populateUpdate() {

        updating = true;
        Set<Integer> urisToInsert = dataProvider.getInsertURIs();
        Map<Integer, List<String>> wkbs = dataProvider.getSpaces(urisToInsert);
        times = parseTimes(wkbs, config.verbose);
        obtainUniverse();
    }

    public Map<Integer, TimeSpace> getSpaces() { return times; }

    public TimeSpace getUniverse() { return universe; }

    public Set<Integer> getCoversUniverse() { return coversUniverse; }

    public Set<Integer> keySet() { return getSpaces().keySet(); }

    public TimeSpace makeEmptySpace() { 
         return new TimeSpace(null, null);
    }

    private void obtainUniverse() {

        List<String> universeWKB = dataProvider.getUniverse();
        Map<Integer, List<String>> uwm = new HashMap<Integer, List<String>>();
        uwm.put(0, universeWKB);
        Map<Integer, TimeSpace> ugm = parseTimes(uwm, false);
        universe = ugm.get(0);
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

    public TimeSpace get(Integer uri) { return times.get(uri); }

    private TimeProvider makeSubProvider(TimeSpace uni, Set<Integer> ints) {

        Set<Integer> coversChildUniverse = new HashSet<Integer>();
        Map<Integer, TimeSpace> overlappingChildUniverse = new HashMap<Integer, TimeSpace>();
        getIntersections(uni, ints, times, overlappingChildUniverse, coversChildUniverse);

        return new TimeProvider(config, dataProvider, uni, overlappingChildUniverse, coversChildUniverse, updating);
    }

    private void getIntersections(TimeSpace uni, Set<Integer> elems,  Map<Integer, TimeSpace> tms,
                                  Map<Integer, TimeSpace> overlapping, Set<Integer> covers) {

        Map<Integer, Space> spMap = new HashMap<Integer, Space>();
        Utils.getIntersections(uni, elems, tms, config.numThreads, spMap, covers);

        for (Integer uri : spMap.keySet()) overlapping.put(uri, (TimeSpace) spMap.get(uri));
    }

    private TimeProvider[] makeSubProviders(TimeSpace childUniL, TimeSpace childUniR,
                                            Set<Integer> intL, Set<Integer> intR) {

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
        
        Map<Integer, TimeSpace> external = getExternalOverlapping(universe);
        for (Integer uri : external.keySet())
            times.put(uri, universe.intersection(external.get(uri)));
    }

   public String extTime() {
        TimeSpace uni = (TimeSpace) getUniverse();
        String start = uni.getStart().toString();
        String stop = uni.getEnd().toString();
        String whereClause = "('" + start + "', '" + stop + "') OVERLAPS (starttime, stoptime) AND ";
        whereClause += "NOT (BEFORE(starttime, '" + start + "') AND BEFORE('" + stop + "', stoptime))";
        return whereClause;
    }
   
    public Map<Integer, TimeSpace> getExternalOverlapping(Space s) {

        Map<Integer, List<String>> timeStrs = dataProvider.getExternalOverlapping(extTime());
        Map<Integer, TimeSpace> res = parseTimes(timeStrs, false);
        return res;
    }

    private Map<Integer, TimeSpace> parseTimes(Map<Integer, List<String>> timeStrs, boolean verbose) {

        Progress prog = new Progress("Parsing timestamp pairs...", timeStrs.keySet().size(), 1, "##0");  
        prog.setConvertToLong(true);

        Map<Integer, TimeSpace> result = new HashMap<Integer,TimeSpace>(timeStrs.keySet().size());
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        if (verbose) prog.init();

        for (Integer uri : timeStrs.keySet()) {

            String start = timeStrs.get(uri).get(0);
            String stop = timeStrs.get(uri).get(1);
            TimeSpace newTime;

            try {
                newTime = new TimeSpace(LocalDateTime.parse(start,format), LocalDateTime.parse(stop,format));
            } catch (DateTimeParseException e) {
                System.err.println(e.toString());
                System.exit(1);
                continue;
            }
            
            if (!newTime.isEmpty()) result.put(uri, newTime);
            if (verbose) prog.update();
        }
        if (verbose) {
            prog.done();
            int errors = timeStrs.keySet().size() - result.values().size();
            if (errors > 0) System.out.println("Unable to parse " + errors + " timestamp pairs.");
            System.out.println("Parsed " + result.values().size() + " timestamp pairs.");
        }

        return result;
    }

}
