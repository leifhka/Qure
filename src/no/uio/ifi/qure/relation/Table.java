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

	private Set<Set<Integer>> checked; // Used if rel is Overlaps to remove dedundant tuples

	public Table(AtomicRelation rel) {
		this.rel = rel;
		tuples = new HashSet<Integer[]>();
		indecies = new ArrayList<Map<Integer, Set<Integer[]>>>();
		checked = new HashSet<Set<Integer>>();

		for (int i = 0; i < rel.getArity(); i++) {
			indecies.add(new HashMap<Integer, Set<Integer[]>>());
		}
	}
	
	public static Table getUniversalTable(AtomicRelation rel) {
		Table res = new Table(rel);
		res.addTuple(new Integer[rel.getArity()]);
		return res;
	}

	public Set<Integer[]> getTuples() { return tuples; }

	public AtomicRelation getRelation() { return rel; }

	public String toString() {
		String res = "";
		for (Integer[] t : getTuples()) {
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
		for (Integer[] tuple : other.getTuples()) {
			addTuple(tuple);
		}
	}
	
	public int size() { return tuples.size(); }

	public void addAllTuples(Set<Integer[]> tuples) {
		for (Integer[] tuple : tuples) {
			addTuple(tuple);
		}
	}

	public void addTuple(Integer[] tuple) {
		tuples.add(tuple);
		for (int i = 0; i < tuple.length; i++) {
			if (tuple[i] != null) {
				indecies.get(i).putIfAbsent(tuple[i], new HashSet<Integer[]>());
				indecies.get(i).get(tuple[i]).add(tuple);
			}
		}
	}

	/**
	 * Returns the tuple-join of t1 and t2 with null being a wild-card
	 */
	public static Integer[] joinWUni(Integer[] t1, Integer[] t2, Map<Integer, Integer> uni) {
		Integer[] res = new Integer[t1.length];
		for (int i = 0; i < t1.length; i++) {
			if (uni.containsKey(i)) {
				res[i] = t2[uni.get(i)]; // Equal if t2 has i, as gathered from getJoinableWUni
			} else {
				res[i] = t1[i];
			}
		}
		return res;
	}

	public Set<Integer[]> getJoinableWUni(Integer[] tuple, Map<Integer, Integer> uni) {

		Pair<Integer, Set<Integer>> somePos = Utils.getSome(uni.keySet());
		Integer someInteger = tuple[uni.get(somePos.fst)];

		Set<Integer[]> res;
		if (!indecies.get(somePos.fst).containsKey(someInteger)) {
			res = new HashSet<Integer[]>(getTuples());
		} else { 
			res = new HashSet<Integer[]>(indecies.get(somePos.fst).get(someInteger));
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
		for (Integer[] tuple : other.getTuples()) { // TODO: Equals not correct
			for (Integer[] joinable : getJoinableWUni(tuple, uni)) {
				Integer[] joined = joinWUni(joinable, tuple, uni);
				Set<Integer> sidSet = Utils.asSet(joined);
				if (!rel.isIntrinsic(joined)) { // && !checked.contains(sidSet)) {
					//if (newRel instanceof PartOf) System.out.println(newRel.toString() + ": " + Arrays.toString(joined));
					res.addTuple(joined);
					//if (rel instanceof Overlaps) {
					//	checked.add(sidSet);
					//}
				}
			}
		}
		return res;
	}
}
