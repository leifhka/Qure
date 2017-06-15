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

	private Set<Set<SID>> checked; // Used if rel is Overlaps to remove dedundant tuples

	public Table(AtomicRelation rel) {
		this.rel = rel;
		tuples = new HashSet<SID[]>();
		indecies = new ArrayList<Map<SID, Set<SID[]>>>();
		checked = new HashSet<Set<SID>>();

		for (int i = 0; i < rel.getArity(); i++) {
			indecies.add(new HashMap<SID, Set<SID[]>>());
		}
	}
	
	public static Table getUniversalTable(AtomicRelation rel) {
		Table res = new Table(rel);
		res.addTuple(new SID[rel.getArity()]);
		return res;
	}

	public Set<SID[]> getTuples() { return tuples; }

	public AtomicRelation getRelation() { return rel; }

	public String toString() {
		String res = "";
		for (SID[] t : getTuples()) {
			res += Arrays.toString(t) + "\n";
		}
		return res;
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof Table) && ((Table) other).tuples.equals(tuples);
	}

	@Override
	public int hashCode() {
		return tuples.hashCode();
	}

	public void addAll(Table other) {
		for (SID[] tuple : other.getTuples()) {
			addTuple(tuple);
		}
	}
	
	public int size() { return tuples.size(); }

	public void addAllTuples(Set<SID[]> tuples) {
		for (SID[] tuple : tuples) {
			addTuple(tuple);
		}
	}

	public void addTuple(SID[] tuple) {
		tuples.add(tuple);
		for (int i = 0; i < tuple.length; i++) {
			if (tuple[i] != null) {
				indecies.get(i).putIfAbsent(tuple[i], new HashSet<SID[]>());
				indecies.get(i).get(tuple[i]).add(tuple);
			}
		}
	}

	/**
	 * Returns the tuple-join of t1 and t2 with null being a wild-card
	 */
	public static SID[] joinWUni(SID[] t1, SID[] t2, Map<Integer, Integer> uni) {
		SID[] res = new SID[t1.length];
		for (int i = 0; i < t1.length; i++) {
			if (uni.containsKey(i)) {
				res[i] = t2[uni.get(i)]; // Equal if t2 has i, as gathered from getJoinableWUni
			} else {
				res[i] = t1[i];
			}
		}
		return res;
	}

	public Set<SID[]> getJoinableWUni(SID[] tuple, Map<Integer, Integer> uni) {

		Pair<Integer, Set<Integer>> somePos = Utils.getSome(uni.keySet());
		SID someSID = tuple[uni.get(somePos.fst)];

		Set<SID[]> res;
		if (!indecies.get(somePos.fst).containsKey(someSID)) {
			res = new HashSet<SID[]>(getTuples());
		} else { 
			res = new HashSet<SID[]>(indecies.get(somePos.fst).get(someSID));
		}

		for (Integer i : somePos.snd) {
			if (indecies.get(i).containsKey(tuple[uni.get(i)])) {
				res.retainAll(indecies.get(i).get(tuple[uni.get(i)]));
			}
		}
		return res;
	}

	/**
	 * Returns a table containing the relational join of this and other
	 */
	public Table join(AtomicRelation newRel, Table other, Map<Integer, Integer> uni) {
		Table res = new Table(newRel);
		for (SID[] tuple : other.getTuples()) {
			for (SID[] joinable : getJoinableWUni(tuple, uni)) {
				SID[] joined = joinWUni(joinable, tuple, uni);
				Set<SID> sidSet = Utils.asSet(joined);
				if (!rel.isIntrinsic(joined) && !checked.contains(sidSet)) {
					res.addTuple(joined);
					if (rel instanceof Overlaps) {
						checked.add(sidSet);
					}
				}
			}
		}
		return res;
	}
}
