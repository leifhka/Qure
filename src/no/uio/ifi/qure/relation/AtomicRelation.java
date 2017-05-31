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

	/**
	 * Returns true iff this relation implies r for anny instantiation of the arguments with non-empty spaces.
	 */
	public abstract Set<Map<Integer, Integer>> impliesNonEmpty(AtomicRelation r);

	public abstract Integer getArgRole(Integer pos);

	public abstract int getArity();

	public abstract Table evalAll(SpaceProvider spaces);

	public abstract Table evalAll(SpaceProvider spaces, Table possible);

	public abstract boolean isIntrinsic(Integer[] tuple);

	public Space[] toSpaces(Integer[] args, SpaceProvider spaces) {
		Space[] sps = new Space[args.length];
		for (int i = 0; i < sps.length; i++) {
			sps[i] = spaces.get(new SID(args[i], getArgRole(i)));
		}
		return sps;
	}

	public SID[] toSIDs(Integer[] tuple) {
		SID[] sids = new SID[tuple.length];
		for (int i = 0; i < tuple.length; i++) {
			sids[i] = new SID(tuple[i], getArgRole(i));
		}
		return sids;
	}

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

