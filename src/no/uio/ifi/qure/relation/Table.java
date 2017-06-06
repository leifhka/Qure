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

	private final Set<SID[]> tuples;
	private final List<Map<SID, Set<SID[]>>> indecies;
	private final AtomicRelation rel;
	private boolean hasOrderedTuples;
	private boolean shouldHaveOrderedTuples;

	private Set<Set<SID>> checked; // Used if rel is Overlaps to remove dedundant tuples

	public Table(AtomicRelation rel) {
		this.rel = rel;
		tuples = new HashSet<SID[]>();
		indecies = new ArrayList<Map<SID, Set<SID[]>>>();
		checked = new HashSet<Set<SID>>();
		hasOrderedTuples = !(rel instanceof Overlaps);
		shouldHaveOrderedTuples = !(rel instanceof Overlaps);

		for (int i = 0; i < rel.getArity(); i++) {
			indecies.add(new HashMap<SID, Set<SID[]>>());
		}
	}

	public Table(AtomicRelation rel, boolean hasOrderedTuples) {
		this.rel = rel;
		tuples = new HashSet<SID[]>();
		indecies = new ArrayList<Map<SID, Set<SID[]>>>();
		checked = new HashSet<Set<SID>>();
		this.hasOrderedTuples = hasOrderedTuples;
		shouldHaveOrderedTuples = !(rel instanceof Overlaps);

		for (int i = 0; i < rel.getArity(); i++) {
			indecies.add(new HashMap<SID, Set<SID[]>>());
		}
	}

	public Set<SID[]> getTuples() { return tuples; }

	@Override
	public boolean equals(Object other) {
		return (other instanceof Table) && ((Table) other).tuples.equals(tuples);
	}

	@Override
	public int hashCode() {
		return tuples.hashCode();
	}

	public boolean relatesSIDs(Set<SID> tuple) {
		return containsAllSubTuples(new HashSet<SID>(), new HashSet<SID>(tuple), 0);
	}

	private boolean containsAllSubTuples(Set<SID> subTuple, Set<SID> remaining, int i) {
		if (i == rel.getArity()) {
			Pair<SID, Set<SID>> some = Utils.getSome(subTuple);
			Set<SID[]> tuples = index.get(some.fst);
			for (SID rest : some.snd) {
				tuples.retainAll(index.get(rest));
			}
			return tuples.isEmpty();
		} else {
			for (SID sid : remaining) {
				if (Relation.stricterRole(rel.getArgRole(i), sid.getRole())) {
					Set<SID> newSubTuple = Utils.add(subTuple, sid);
					Set<SID> newRemaining = Utils.remove(remaining, sid);
					if (!containsAllSubTuples(newSubTuple, newRemaining, i+1)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Returns the tuple that results from applying the reverse of unifier to tuple, putting null/wild-card
	 * for non-matched positions
	 */
	private SID[] fromUnifier(SID[] tuple, Map<Integer, Integer> unifier) {
		SID[] res = new SID[rel.getArity()];
		for (int i = 0; i < res.length; i++) {
			if (unifier.get(i) != null) {
				res[i] = tuple[unifier.get(i)];
			}
		}
		return res;
	}
	
	public static Table fromTable(Table other, Map<Integer, Integer> unifier, AtomicRelation rel) {

		Table res = new Table(rel, other.hasOrderedTuples);
		for (SID[] tuple : other.tuples) {
			res.addTuple(res.fromUnifier(tuple, unifier));
		}
		return res;
	}

	public int size() { return tuples.size(); }

	public void addTuple(SID[] tuple) {
		tuples.add(tuple);
		for (int i = 0; i < tuple.length; i++) {
			if (tuple[i] != null) {
				indecies.get(i).putIfAbsent(tuple[i], new HashSet<SID[]>());
				indecies.get(i).get(tuple[i]).add(tuple);
			}
		}
	}

	private SID[] reverse(SID[] tuple) {
		SID[] rev = new SID[tuple.length];
		for (int i = 0; i < rev.length; i++) {
			rev[i] = tuple[tuple.length-(i+1)];
		}
		return rev;
	}

	/**
	 * Returns the tuple-join of t1 and t2 with null being a wild-card
	 */
	public static SID[] join(SID[] t1, SID[] t2) {
		SID[] res = new SID[t1.length];
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

	// TODO: Fix, join now dependent on the random order of tuples. Can have both [a,b] and [b,a] if same roles,
	// however, only one of them joins with [a,c].

	/**
	 * Returns the set of tuples from this table that joins on all fields with argument,
	 * null is a wild-card
	 */
	public Set<SID[]> getJoinable(SID[] tuple) {

		Set<SID[]> res;

		if (tuple[0] == null || indecies.get(0).keySet().isEmpty()) {
			res = new HashSet<SID[]>(tuples);
		} else {
			Set<SID[]> pos = indecies.get(0).get(tuple[0]);
			if (pos == null) {
				return new HashSet<SID[]>();
			} else {
				res = new HashSet<SID[]>(pos);
			}
		}

		for (int i = 1; i < tuple.length; i++) {
			if (tuple[i] != null && !indecies.get(i).keySet().isEmpty()) {
				if (!indecies.get(i).containsKey(tuple[i])) {
					return new HashSet<SID[]>();
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
		for (SID[] tuple : tuples) {
			for (SID[] joinable : other.getJoinable(tuple)) {
				SID[] joined = join(tuple, joinable);
				Set<SID> sidSet = Utils.asSet(joined);
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
