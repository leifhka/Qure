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

	// Rest of methods

	/**
	 * Returns true iff this relation implies r for anny instantiation of the arguments with non-empty spaces.
	 */
	public abstract Set<Map<Integer, Integer>> impliesNonEmpty(AtomicRelation r);

	/**
	 * Returns true iff this relation holds for any instantiation of the arguments with non-empty spaces.
	 */
	public abstract boolean isValid();

	public abstract int getArity();

	public abstract Set<List<Integer>> evalAll(SpaceProvider spaces, Map<Integer, Set<SID>> roleToSID);

	public abstract Set<List<Integer>> evalAll(SpaceProvider spaces, List<Integer> possible, Map<Integer, Set<SID>> roleToSID);

	public abstract List<SID> toSIDs(List<Integer> tuple);

	/**
	 * Returns the set of all lists constructed from the elements of argument tuple,
	 * assumed to have a max length of 2. Used by PartOf and Before to obtain
	 * ordered tuples from unordered tuples from Overlaps.
	 */
	public Set<List<Integer>> tupleToLists(List<Integer> tuple) {

		Set<List<Integer>> lists = new HashSet<List<Integer>>();
		lists.add(new ArrayList<Integer>(tuple));
		
		if (tuple.size() > 1) {
			List<Integer> l = new ArrayList<Integer>();
			l.add(tuple.get(1));
			l.add(tuple.get(0));
			lists.add(l);
		}
		return lists;
	}
}

