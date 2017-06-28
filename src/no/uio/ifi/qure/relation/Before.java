
package no.uio.ifi.qure.relation;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import no.uio.ifi.qure.Config;
import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.traversal.*;
import no.uio.ifi.qure.space.*;

public class Before extends AtomicRelation {

	private final int a1, a2, r1, r2;

	public Before(int r1, int r2, int a1, int a2) {
		this.a1 = a1;
		this.a2 = a2;
		this.r1 = r1;
		this.r2 = r2;
	}

	public int getArity() { return 2; }

	public Integer getArgRole(Integer pos) { return (pos.equals(a1)) ? r1 : r2; }
	
	public Set<Integer> getArguments() {
		Set<Integer> res = new HashSet<Integer>();
		res.add(a1);
		res.add(a2);
		return res;
	}

	public boolean relatesArg(int arg) {
		return a1 == arg || a2 == arg;
	}

	public String toString() {
		return "bf(<" + r1 + "," + a1 + ">, <" + r2 + "," + a2 + ">)";
	}

	public String toBTSQL(Integer[] vals, Config config) { //TODO
		return null;
	}

	public String toGeoSQL(Integer[] vals, Config config, SpaceProvider sp) {
		return sp.toSQL(this, vals, config);
	}

	public boolean isValid() {
		return false;
	}

	public boolean isIntrinsic(Integer[] tuple) {
		return tuple[a1] == tuple[a2] && strictnessRelated(r1, r2);
	}

	public Set<Map<Integer, Integer>> impliesNonEmpty(AtomicRelation r) {

		if (r instanceof Before) {
			Before bfr = (Before) r;
			if (a1 == bfr.a1 && stricterRole(bfr.r1, r1) &&
			    a2 == bfr.a2 && stricterRole(bfr.r2, r2)) {

				Set<Map<Integer, Integer>> unifiers = new HashSet<Map<Integer, Integer>>();
				Map<Integer, Integer> unifier = new HashMap<Integer, Integer>();
				unifier.put(new Integer(r1), new Integer(bfr.r1));
				unifier.put(new Integer(r2), new Integer(bfr.r2));
				unifiers.add(unifier);
				return unifiers;
			}
		}
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Before)) return false;

		Before obf = (Before) o;
		return a1 == obf.a1 && a2 == obf.a2 && r1 == obf.r1 && r2 == obf.r2;
	}

	@Override
	public int hashCode() {
		return 10*((r1+a1) + 2*(r2+a2));
	}

	public boolean eval(Space[] spaceArgs) {
    	return spaceArgs[a1].before(spaceArgs[a2]);
	}

	public Set<AtomicRelation> getNormalizedAtomicRelations() {

		Set<AtomicRelation> rels = new HashSet<AtomicRelation>();
		if (a1 == a2) {
			rels.add(new Before(r1, r2, 0, 0));
		} else {
			rels.add(new Before(r1, r2, 0, 1));
		}
		return rels;
	}

	public Set<Integer> getRoles() {
		Set<Integer> rs = new HashSet<Integer>();
		rs.add(r1);
		rs.add(r2);
		return rs;
	}
	
	public Table evalAll(SpaceProvider spaces) {

    	return null; // Should never occur
	}
	
	public Table evalAll(SpaceProvider spaces, Table possible) {

		Table table = new Table(this);

    	for (Integer[] tuple : possible.getTuples()) {
			if (eval(toSpaces(toSIDs(tuple), spaces))) {
				table.addTuple(tuple);
	    	}
    	}
    	return table;
	}
}

