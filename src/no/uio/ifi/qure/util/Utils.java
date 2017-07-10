package no.uio.ifi.qure.util;

import java.util.*;

import no.uio.ifi.qure.space.Space;
import no.uio.ifi.qure.traversal.SID;

public class Utils {



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

	public static <T> T unpackSingleton(Collection<T> s) {
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
