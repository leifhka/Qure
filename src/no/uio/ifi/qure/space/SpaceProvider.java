package no.uio.ifi.qure.space;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import no.uio.ifi.qure.*;
import no.uio.ifi.qure.relation.*;
import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.bintree.*;
import no.uio.ifi.qure.traversal.*;

public interface SpaceProvider {

	public void clear();

	public void populateBulk();

	public void populateUpdate();

	public Map<SID, ? extends Space> getSpaces();

	public void put(SID uri, Space space);

	public Space getUniverse();

	public Space makeEmptySpace();

	public Set<SID> getCoversUniverse();

	public default Set<SID> keySet() {
		return getSpaces().keySet();
	}

	public default boolean isEmpty() {
		return keySet().isEmpty();
	}

	public default int size() {
		return keySet().size();
	}

	public default void expandWithRoles(Set<Integer> roles) {
		for (Integer role : roles) {
			if (role == 0) continue;
			for (SID sid : keySet()) {
				Space s = get(sid);
				Space rs = s.getPart(role).intersection(getUniverse());
				if (!rs.isEmpty()) put(new SID(sid.getID(), role), rs);
			}
		}
	}

	public Space get(SID uri);

	public void populateWithExternalOverlapping();

	public String toSQL(AtomicRelation rel, String[] vals, Config config);

	public default List<Space> toSpaces(List<SID> tuple) {
		List<Space> sps = new ArrayList<Space>(tuple.size());
		for (SID s : tuple) {
			sps.add(get(s));
		}
		return sps;
	}

	public SpaceProvider makeSubProvider(Space uni, Set<SID> sids);

	public default Pair<SpaceProvider, SpaceProvider> makeSubProviders(Space childUniL, Space childUniR,
	                                                                   Set<SID> intL, Set<SID> intR) {
		SpaceProvider subPL = makeSubProvider(childUniL, intL);
		SpaceProvider subPR = makeSubProvider(childUniR, intR);
		return new Pair<SpaceProvider, SpaceProvider>(subPL, subPR);
	}

	public default Pair<SpaceProvider, SpaceProvider> splitProvider(int split) {
		Pair<? extends Space, ? extends Space> childUniverseres = getUniverse().split(split);
		return makeSubProviders(childUniverseres.fst, childUniverseres.snd, keySet(), keySet());
	}

	public default Pair<SpaceProvider, SpaceProvider> splitProvider(int split, Block splitBlock, Set<SID> sids) {
		Pair<? extends Space, ? extends Space> splitSpaces = computeSpacesFromSplit(splitBlock, split);
		return makeSubProviders(splitSpaces.fst, splitSpaces.snd, sids, sids);
	}

	public default Pair<SpaceProvider, SpaceProvider> splitProvider(int split, EvenSplit evenSplit) {
		return makeSubProviders(evenSplit.uniL, evenSplit.uniR, evenSplit.intL, evenSplit.intR);
	}

	public default Pair<? extends Space, ? extends Space> computeSpacesFromSplit(Block splitBlock, int split) {

		Space spL = makeEmptySpace(), spR = makeEmptySpace();
		Pair<? extends Space, ? extends Space> splitLR = getUniverse().split(split);
		Space splitL = splitLR.fst, splitR = splitLR.snd;

		for (int i = 0; i < splitBlock.depth(); i++) {

			if (splitBlock.getBit(i) == 1L) {
				spL = spL.union(splitL);
				splitLR = splitR.split(split);
				splitL = splitLR.fst;
				splitR = splitLR.snd;
			} else {
				spR = spR.union(splitR);
				splitLR = splitL.split(split);
				splitL = splitLR.fst;
				splitR = splitLR.snd;
			}
		}

		return new Pair<Space, Space>(spL.union(splitL), spR.union(splitR));
	}

	// TODO: Long method, split into smaller 
	public default EvenSplit getEvenSplit(int split, int maxSplits, double minRatio, int numThreads) {

		Space spL = makeEmptySpace(), spR = makeEmptySpace();
		Pair<? extends Space, ? extends Space> splitLR = getUniverse().split(split);
		Space splitL = splitLR.fst.toUniverse(), splitR = splitLR.snd.toUniverse();

		Set<SID> undecided = new HashSet<SID>(keySet());
		Set<SID> intL = new HashSet<SID>(), intR = new HashSet<SID>();
		Set<SID> intSpL = getIntersecting(splitL, undecided, numThreads);
		Set<SID> intSpR = getIntersecting(splitR, undecided, numThreads);

		int diff = Math.abs(intSpL.size() - intSpR.size());
		int bestDiff = diff;

		Block splitBlock = Block.getTopBlock();
		Set<SID> intAllL = intSpL, intAllR = intSpR;
		EvenSplit bestSplit = new EvenSplit(splitBlock, intSpL, intSpR, splitL, splitR);

		int i = 0;
		double ratio = ((double) Math.min(intSpR.size(), intSpL.size())) / Math.max(intSpR.size(), intSpL.size());

		while (i++ < maxSplits && ratio < minRatio) {

			if (intAllR.size() > intAllL.size()) {

				spL = spL.union(splitL).toUniverse();
				intL.addAll(intSpL);
				undecided = new HashSet<SID>(intSpR);

				splitLR = splitR.split(split);
				splitBlock = splitBlock.addOne();

			} else {

				spR = spR.union(splitR).toUniverse();
				intR.addAll(intSpR);
				undecided = new HashSet<SID>(intSpL);

				splitLR = splitL.split(split);
				splitBlock = splitBlock.addZero();
			}

			splitL = splitLR.fst.toUniverse();
			splitR = splitLR.snd.toUniverse();

			intSpL = getIntersecting(splitL, undecided, numThreads);
			intSpR = getIntersecting(splitR, undecided, numThreads);

			intAllL = Utils.union(intL, intSpL);
			intAllR = Utils.union(intR, intSpR);
			diff = Math.abs(intAllL.size() - intAllR.size());

			if (diff < bestDiff) {
				bestSplit = new EvenSplit(new Block(splitBlock.getRepresentation()), 
				                          new HashSet<SID>(intAllL), 
				                          new HashSet<SID>(intAllR),
				                          spL.union(splitL).toUniverse(),
				                          spR.union(splitR).toUniverse());
				bestDiff = diff;
			}

			ratio = ((double) Math.min(intAllR.size(), intAllL.size())) / Math.max(intAllR.size(), intAllL.size());
		}
		return bestSplit;
	}

	public default void getIntersectionsSingle(Space uni, Set<SID> elems,
	                                           Map<SID, Space> intersections,
	                                           Set<SID> covering) {
		for (SID uri : elems) {

			Space gs = get(uri);
			Space ngs = gs.intersection(uni);

			if (!ngs.isEmpty())  {
				if (ngs.hasPart(uni)) {
					covering.add(uri);
				} else {
					intersections.put(uri, ngs);
				}
			}
		}
	}

	// TODO: Handle exceptions when intersecting
	public default void getIntersections(Space uni, Set<SID> elems,
	                                     int numThreads,
 	                                     Map<SID, Space> intersections,
	                                     Set<SID> covering) {
		
		if (elems.size() < 1000) {
			getIntersectionsSingle(uni, elems, intersections, covering);
			return;
		}

		int partitionSize = (int) Math.ceil( ((float) elems.size()) / numThreads );

		SID[] elemsArr = elems.toArray(new SID[elems.size()]);

		List<Thread> threads = new ArrayList<Thread>();
		List<Map<SID, Space>> intersectionMaps = new ArrayList<Map<SID, Space>>();
		List<Set<SID>> coversSets = new ArrayList<Set<SID>>();
		int cur = 0;

		for (int i = 0; i < numThreads; i++) {

			Map<SID, Space> intMap = new HashMap<SID, Space>();
			Set<SID> covSet = new HashSet<SID>();
			final int maxInd = Math.min(cur + partitionSize, elemsArr.length);
			final int minInd = cur;

			Thread is = new Thread() {
				public void run() {
					for (int j = minInd; j < maxInd; j++) {

						SID uri = elemsArr[j];
						Space gs = get(uri);
						Space ngs = gs.intersection(uni);

						if (!ngs.isEmpty())  {
							if (ngs.hasPart(uni)) {
								covSet.add(uri);
							} else {
								intMap.put(uri, ngs);
							}
						}
					}
				}
			};
			intersectionMaps.add(intMap);
			coversSets.add(covSet);
			threads.add(is);
			is.start();
			cur += partitionSize;
		}

		try {
			for (Thread thread : threads) {
				thread.join();
			}
		} catch (InterruptedException ex) {
			System.err.println(ex.toString());
			System.exit(1);
		}

		for (Map<SID, Space> res : intersectionMaps) {
			intersections.putAll(res);
		}
		for (Set<SID> res : coversSets) {
			covering.addAll(res);
		}
	}

	// TODO: Handle exceptions when intersecting
	public default Set<SID> getIntersecting(Space space, Set<SID> elems, int numThreads) {
		
		if (elems.size() < 1000) {
			return getIntersectingSingle(space, elems);
		}
		int partitionSize = (int) Math.ceil( ((float) elems.size()) / numThreads );

		SID[] elemsArr = elems.toArray(new SID[elems.size()]);

		List<Thread> threads = new ArrayList<Thread>();
		List<Set<SID>> results = new ArrayList<Set<SID>>();
		int cur = 0;

		for (int i = 0; i < numThreads; i++) {

			Set<SID> res = new HashSet<SID>();
			final int maxInd = Math.min(cur + partitionSize, elemsArr.length);
			final int minInd = cur;

			Thread is = new Thread() {
				public void run() {
					Set<SID> intersecting = new HashSet<SID>();
					for (int j = minInd; j < maxInd; j++) {
						SID uri = elemsArr[j];
						if (get(uri).overlaps(space)) {
							res.add(uri);
						}
					}
				}
			};
			results.add(res);
			threads.add(is);
			is.start();
			cur += partitionSize;
		}

		try {
			for (Thread thread : threads) {
				thread.join();
			}
		} catch (InterruptedException ex) {
			System.err.println(ex.toString());
			System.exit(1);
		}

		Set<SID> intersecting = new HashSet<SID>();
		for (Set<SID> res : results) {
			intersecting.addAll(res);
		}
		return intersecting;
	}

	public default Set<SID> getIntersectingSingle(Space space, Set<SID> elems) {

		Set<SID> intersecting = new HashSet<SID>();

		for (SID uri : elems) {
			if (get(uri).overlaps(space)) {
				intersecting.add(uri);
			}
		}

		return intersecting;
	}
}
