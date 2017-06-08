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

	public abstract boolean isIntrinsic(SID[] tuple);

	public Space[] toSpaces(SID[] args, SpaceProvider spaces) {
		Space[] sps = new Space[args.length];
		for (int i = 0; i < sps.length; i++) {
			sps[i] = spaces.get(args[i]);
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
}

