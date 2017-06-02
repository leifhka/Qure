package no.uio.ifi.qure.relation;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.space.*;
import no.uio.ifi.qure.traversal.*;

public class Table {

	private final Set<Integer[]> tuples;
	private final List<Map<Integer, Set<Integer[]>>> indecies;
	private final AtomicRelation rel;
	private boolean hasOrderedTuples;
	private boolean shouldHaveOrderedTuples;

	private Set<Set<SID>> checked; // Used if rel is Overlaps to remove dedundant tuples

	public Table(AtomicRelation rel) {
		this.rel = rel;
		tuples = new HashSet<Integer[]>();
		indecies = new ArrayList<Map<Integer, Set<Integer[]>>>();
		checked = new HashSet<Set<SID>>();
		hasOrderedTuples = !(rel instanceof Overlaps);
		shouldHaveOrderedTuples = !(rel instanceof Overlaps);

		for (int i = 0; i < rel.getArity(); i++) {
			indecies.add(new HashMap<Integer, Set<Integer[]>>());
		}
	}

	public Table(AtomicRelation rel, boolean hasOrderedTuples) {
		this.rel = rel;
		tuples = new HashSet<Integer[]>();
		indecies = new ArrayList<Map<Integer, Set<Integer[]>>>();
		checked = new HashSet<Set<SID>>();
		this.hasOrderedTuples = hasOrderedTuples;
		shouldHaveOrderedTuples = !(rel instanceof Overlaps);

		for (int i = 0; i < rel.getArity(); i++) {
			indecies.add(new HashMap<Integer, Set<Integer[]>>());
		}
	}

	public Set<Integer[]> getTuples() { return tuples; }

	@Override
	public boolean equals(Object other) {
		return (other instanceof Table) && ((Table) other).tuples.equals(tuples);
	}

	@Override
	public int hashCode() {
		return tuples.hashCode();
	}

	/**
	 * Returns the tuple that results from applying the reverse of unifier to tuple, putting null/wild-card
	 * for non-matched positions
	 */
	private Integer[] fromUnifier(Integer[] tuple, Map<Integer, Integer> unifier) {
		Integer[] res = new Integer[rel.getArity()];
		for (int i = 0; i < res.length; i++) {
			if (unifier.get(i) != null) {
				res[i] = tuple[unifier.get(i)];
			}
		}
		return res;
	}
	
	public static Table fromTable(Table other, Map<Integer, Integer> unifier, AtomicRelation rel) {

		Table res = new Table(rel, other.hasOrderedTuples);
		for (Integer[] tuple : other.tuples) {
			res.addTuple(res.fromUnifier(tuple, unifier));
		}
		return res;
	}

	public int size() { return tuples.size(); }

	public void addTuple(Integer[] tuple) {
		tuples.add(tuple);
		for (int i = 0; i < tuple.length; i++) {
			if (tuple[i] != null) {
				indecies.get(i).putIfAbsent(tuple[i], new HashSet<Integer[]>());
				indecies.get(i).get(tuple[i]).add(tuple);
			}
		}
	}

	private Integer[] reverse(Integer[] tuple) {
		Integer[] rev = new Integer[tuple.length];
		for (int i = 0; i < rev.length; i++) {
			rev[i] = tuple[tuple.length-(i+1)];
		}
		return rev;
	}

	/**
	 * Returns the tuple-join of t1 and t2 with null being a wild-card
	 */
	public static Integer[] join(Integer[] t1, Integer[] t2) {
		Integer[] res = new Integer[t1.length];
		for (int i = 0; i < t1.length; i++) {
			if (t1[i] == null) {
				res[i] = t2[i];
			} else if (t2[i] == null || t1[i].equals(t2[i])) {
				res[i] = t1[i];
			} else {
				return null;
			}
		}
		return res;
	}

	/**
	 * Returns the set of tuples from this table that joins on all fields with argument,
	 * null is a wild-card
	 */
	public Set<Integer[]> getJoinable(Integer[] tuple) {

		Set<Integer[]> res;

		if (tuple[0] == null || indecies.get(0).keySet().isEmpty()) {
			res = new HashSet<Integer[]>(tuples);
		} else {
			Set<Integer[]> pos = indecies.get(0).get(tuple[0]);
			if (pos == null) {
				return new HashSet<Integer[]>();
			} else {
				res = new HashSet<Integer[]>(pos);
			}
		}

		for (int i = 1; i < tuple.length; i++) {
			if (tuple[i] != null && !indecies.get(i).keySet().isEmpty()) {
				if (!indecies.get(i).containsKey(tuple[i])) {
					return new HashSet<Integer[]>();
				} else {
					res.retainAll(indecies.get(i).get(tuple[i]));
				}
			}
		}
		return res;
	}

	/**
	 * Returns a table containing the relational join of this and other
	 */
	public Table join(Table other) {
		Table res = new Table(rel);
		for (Integer[] tuple : tuples) {
			for (Integer[] joinable : other.getJoinable(tuple)) {
				Integer[] joined = join(tuple, joinable);
				Set<SID> sidSet = rel.toSIDSet(joined);
				if (!rel.isIntrinsic(joined) && !checked.contains(sidSet)) {
					res.addTuple(joined);
					if (rel instanceof Overlaps) {
						checked.add(sidSet);
					}
					if (!hasOrderedTuples && shouldHaveOrderedTuples) {
						res.addTuple(reverse(joined));
					}
				}
			}
		}
		return res;
	}
}
