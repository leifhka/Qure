package no.uio.ifi.qure.relation;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.traversal.*;
import no.uio.ifi.qure.space.*;


public abstract class AtomicRelation extends Relation {

	// Used for implication graph
	private Set<AtomicRelation> implies = new HashSet<AtomicRelation>();
	private Set<AtomicRelation> impliedBy = new HashSet<AtomicRelation>();	

	public void addImplies(AtomicRelation rel) { implies.add(rel); }
	public void addImpliedBy(AtomicRelation rel) { impliedBy.add(rel); }
	public Set<AtomicRelation> getImpliesRelations() { return implies; }
	public Set<AtomicRelation> getImpliedByRelations() { return impliedBy; }

	public Set<AtomicRelation> getImpliedByWithOnlyVisitedChildren(Set<AtomicRelation> visited) {
		Set<AtomicRelation> rels = new HashSet<AtomicRelation>();
		for (AtomicRelation rel : impliedBy) {
			if (visited.containsAll(rel.implies)) {
				rels.add(rel);
			}
		}
		return rels;
	}

	// Rest of methods

	/**
	 * Returns true iff this relation implies r for anny instantiation of the arguments with non-empty spaces.
	 */
	public abstract boolean impliesNonEmpty(AtomicRelation r);

	/**
	 * Returns true iff this relation is implied by r for any instantiation of the arguments with non-empty spaces.
	 */
	public boolean impliedByNonEmpty(AtomicRelation r) {
		return r.impliesNonEmpty(this);
	}

	/**
	 * Returns true iff this relation holds for any instantiation of the arguments with non-empty spaces.
	 */
	public abstract boolean isValid();

	public abstract int getArity();

	public abstract Set<Tuple> evalAll(SpaceProvider spaces, Map<Integer, Set<SID>> roleToSID);

	public abstract Set<Tuple> evalAll(SpaceProvider spaces, Tuple possible, Map<Integer, Set<SID>> roleToSID);

	/**
	 * Returns the set of all lists constructed from the elements of argument tuple,
	 * assumed to have a max length of 2. Used by PartOf and Before to obtain
	 * ordered tuples from unordered tuples from Overlaps.
	 */
	public Set<List<SID>> tupleToLists(Tuple tuple) {

		Set<List<SID>> lists = new HashSet<List<SID>>();

		if (tuple.getElements() instanceof List) {

			lists.add((List<SID>) tuple.getElements());

		} else {
			Set<SID> elems = (Set<SID>) tuple.getElements();

			if (elems.size() <= 1) {
				lists.add(new ArrayList<SID>(elems));
			} else {
				SID[] arr = elems.toArray(new SID[2]);

				List<SID> l1 = new ArrayList<SID>();
				l1.add(arr[0]);
				l1.add(arr[1]);
				lists.add(l1);

				List<SID> l2 = new ArrayList<SID>();
				l2.add(arr[1]);
				l2.add(arr[0]);
				lists.add(l2);
			}
		}
		return lists;
	}
}

