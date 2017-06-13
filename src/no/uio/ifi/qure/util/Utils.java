package no.uio.ifi.qure.util;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

import no.uio.ifi.qure.space.Space;
import no.uio.ifi.qure.traversal.SID;

public class Utils {

	public static void getIntersectionsSingle(Space uni, Set<SID> elems,
	                                          Map<SID, ? extends Space> map,
	                                          Map<SID, Space> intersections,
	                                          Set<SID> covering) {
		for (SID uri : elems) {

			Space gs = map.get(uri);
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
	public static void getIntersections(Space uni, Set<SID> elems,
	                                    Map<SID, ? extends Space> map, int numThreads,
	                                    Map<SID, Space> intersections,
	                                    Set<SID> covering) {
		
		if (elems.size() < 1000) {
			getIntersectionsSingle(uni, elems, map, intersections, covering);
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
						Space gs = map.get(uri);
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
	public static Set<SID> getIntersecting(Space space, Set<SID> elems,
	                                       Map<SID, ? extends Space> map, int numThreads) {
		
		if (elems.size() < 1000) {
			return getIntersectingSingle(space, elems, map);
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
						if (map.get(uri).overlaps(space)) {
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

	public static Set<SID> getIntersectingSingle(Space space, Set<SID> elems,
	                                             Map<SID, ? extends Space> map) {

		Set<SID> intersecting = new HashSet<SID>();

		for (SID uri : elems) {
			if (map.get(uri).overlaps(space)) {
				intersecting.add(uri);
			}
		}

		return intersecting;
	}

	public static <T> Set<T> intersection(Set<T> s1, Set<T> s2) {
		Set<T> u = new HashSet<T>(s1);
		u.retainAll(s2);
		return u;
	}

	public static <T> Set<T> difference(Set<T> s1, Set<T> s2) {
		Set<T> u = new HashSet<T>(s1);
		u.removeAll(s2);
		return u;
	}

	public static <T> Set<T> union(Set<T> s1, Set<T> s2) {
		Set<T> u = new HashSet<T>(s1);
		u.addAll(s2);
		return u;
	}

	public static <T> Set<T> add(Set<T> s, T e) {
		Set<T> u = new HashSet<T>(s);
		u.add(e);
		return u;
	}

	public static <T> Set<T> remove(Set<T> s, T e) {
		Set<T> u = new HashSet<T>(s);
		u.remove(e);
		return u;
	}

	public static <T> Pair<T,Set<T>> getSome(Set<T> s) {
		//Set<T> ns = cloneByClass(s);
		Set<T> ns = new HashSet<T>();
		Iterator<T> iter = s.iterator();

		if (!iter.hasNext()) {
    		return null;
		}

		T some = iter.next();

		while (iter.hasNext()) {	
			ns.add(iter.next());
		}
		return new Pair<T, Set<T>>(some, ns);
	}

	public static <T> T getOnlySome(Set<T> s) {
		Set<T> ns = new HashSet<T>();
		Iterator<T> iter = s.iterator();

		if (!iter.hasNext()) {
    		return null;
		}

		return iter.next();
	}

	public static <T> Set<T> asSet(T[] ts) {
		return new HashSet<T>(Arrays.asList(ts));
	}
	
	public static <T> Set<Set<T>> getSubsets(Set<T> set, int minLen, int maxLen) {
		Set<Set<T>> subsets = new HashSet<Set<T>>();
		generateAllSubsets(new HashSet<T>(), set, minLen, maxLen, subsets);
		return subsets;
	}

	public static <T> Set<Set<T>> getSubsets(Set<T> set, int minLen) {
		Set<Set<T>> subsets = new HashSet<Set<T>>();
		generateAllSubsets(new HashSet<T>(), set, minLen, set.size(), subsets);
		return subsets;
	}

	private static <T> void generateAllSubsets(Set<T> generated, Set<T> remaining, int minLen, int maxLen, Set<Set<T>> res) {
		if (generated.size() > maxLen) return;
		
		if (generated.size() >= minLen) {
			res.add(generated);
		}
		for (T t : remaining) {
			generateAllSubsets(add(generated, t), remove(remaining, t), minLen, maxLen, res);
		}
	}

// Does not work:
//	public static <T> T cloneByClass(T i) {
//		T ni;
//		try {
//			Class<T> cls = i.getClass();
//			ni = cls.newInstance();
//		} catch (InstantiationException e) {
//			e.printStackTrace();
//			System.exit(1);
//		}
//		return ni;
//	}
}
