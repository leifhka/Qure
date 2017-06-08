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
	
	public static Table fromTable(Table other, Map<Integer, Integer> unifier, AtomicRelation rel) {

		Table res = new Table(rel);
		for (SID[] tuple : other.tuples) {
			res.addTuple(res.fromUnifier(tuple, unifier));
		}
		return res;
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

//	private Set<SID[]> toSameFormat(SID[] tuple, Set<SID[]> ordered) {
//		Set<SID[]> res = new HashSet<SID[]>();
//		for (SID[] ord : ordered) {
//			boolean add = true;
//			for (int i = 0; i < ord.length; i++) {
//				if ((ord[i] == null && tuple[i] != null) || (ord[i] != null && tuple[i] == null)) {
//					add = false;
//					break;
//				}
//			}
//			if (add) {
//				res.add(ord);
//			}
//		}
//		return res;
//	}

	/**
	 * Returns the tuple-join of t1 and t2 with null being a wild-card
	 */
	public static SID[] joinWUni(SID[] t1, SID[] t2, Map<Integer, Integer> toUni1, Map<Integer, Integer> toUni2) {
		Set<Integer> allKeys = Utils.union(toUni1.keySet(), toUni2.keySet());
		SID[] res = new SID[allKeys.size()];
		for (int i = 0; i < t1.length; i++) {
			if (toUni1.containsKey(i)) {
				res[i] = t1[toUni1.get(i)];
			} else { // Equal if both have i, as gathered from getJoinableWUni
				res[i] = t2[toUni2.get(i)];
			}
		}
		return res;
	}

	public Set<SID[]> getJoinableWUni(SID[] tuple, Map<Integer, Integer> toUni1, Map<Integer, Integer> toUni2) {

		Set<Integer> common = Utils.intersection(toUni1.keySet(), toUni2.keySet());
		Pair<Integer, Set<Integer>> somePos = Utils.getSome(common);

		if (indecies.get(toUni1.get(somePos.fst)).keySet().isEmpty()) {
			return null;
		} 
		Set<SID[]> res = new HashSet<SID[]>(indecies.get(toUni2.get(somePos.fst)).get(tuple[toUni1.get(somePos.fst)]));

		for (Integer i : somePos.snd) {
			if (!indecies.get(toUni2.get(i)).containsKey(tuple[toUni1.get(i)])) {
					return null;
			} else {
				res.retainAll(indecies.get(toUni2.get(i)).get(tuple[toUni1.get(i)]));
			}
		}
		return res;
	}

	/**
	 * Returns the join of the unifiers uni1 and uni2
	 * toUni1 and toUni2 are maps going from positions in result tuple
	 * to the positions from which that value came from in 1 and 2 tuple resp. 
	 * Thus, if uni1 and uni2 both contains i, then they are to be joined on i.
	 */
	private Map<Integer, Integer> joinUnifiers(Map<Integer, Integer> uni1, Map<Integer, Integer> uni2,
	                                           Map<Integer, Integer> toUni1, Map<Integer, Integer> toUni2) {
		                                           
		Map<Integer, Integer> res = new HashMap<Integer, Integer>();
		int i = 0;
		for (Integer pos : Utils.union(uni1.keySet(), uni2.keySet())) {
			res.put(pos, i);
			if (uni1.containsKey(pos)) {
				toUni1.put(i, uni1.get(pos));
			}
			if (uni2.containsKey(pos)) {
				toUni2.put(i, uni2.get(pos));
			}
			i++;
		}
		
		if (res.keySet().size() == uni1.keySet().size() + uni2.keySet().size()) {
			return null; // No common positions to join on
		} else {
			return res;
		}
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
						//res.addAllTuples(toSameFormat(tuple, ((Overlaps) rel).generateAllOrderedTuples(Utils.asSet(joined))));
					}
				}
			}
		}
		return res;
	}
}
