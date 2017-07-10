
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

	private final int a0, a1, r0, r1;

	public Before(int r0, int r1, int a0, int a1) {
		this.a0 = a0;
		this.a1 = a1;
		this.r0 = r0;
		this.r1 = r1;
	}

	public int getArity() { return 2; }

	public Integer getArgRole(Integer pos) { return (pos.equals(a0)) ? r0 : r1; }
	
	public Set<Integer> getArguments() {
		Set<Integer> res = new HashSet<Integer>();
		res.add(a0);
		res.add(a1);
		return res;
	}

	public int getArg(int i) {
		if (i == 0) return a0;
		else if (i == 1) return a1;
		else return -1;
	}

	public boolean relatesArg(int arg) {
		return a0 == arg || a1 == arg;
	}

	public String toString() {
		return "bf(<" + r0 + "," + a0 + ">, <" + r1 + "," + a1 + ">)";
	}

	public String toBTSQL(String[] vals, Config config) {
		String[] selFroWhe = makeSelectFromWhereParts(config.btTableName, config.uriColumn, vals);
		String query = "SELECT DISTINCT " + selFroWhe[0] + "\n";
		query += "  FROM " + selFroWhe[1] + "\n";
		query += "  WHERE ";
		if (!selFroWhe[2].equals("")) query += selFroWhe[2] + " AND\n";
		if (r0 != 0) query += "  T" + a0 + ".role & " + (1 | (r0 << 1)) + " = " + (1 | (r0 << 1)) + " AND\n";
		if (r1 != 0) query += "  T" + a1 + ".role & " + (1 | (r1 << 1)) + " = " + (1 | (r1 << 1)) + " AND\n";
		query += "  T" + a0 + ".block < T" + a1 + ".block";
		return query;
	}

	public String toGeoSQL(String[] vals, Config config, SpaceProvider sp) {
		return sp.toSQL(this, vals, config);
	}

	public boolean isValid() {
		return false;
	}

	public boolean isIntrinsic(Integer[] tuple) {
		return tuple[a0] == tuple[a1] && strictnessRelated(r0, r1);
	}

	public Set<Map<Integer, Integer>> impliesNonEmpty(AtomicRelation r) {

		Set<Map<Integer, Integer>> unifiers = new HashSet<Map<Integer, Integer>>();

		if (r instanceof Before) {
			Before bfr = (Before) r;
			if (a0 == bfr.a0 && stricterRole(bfr.r0, r0) &&
			    a1 == bfr.a1 && stricterRole(bfr.r1, r1)) {

				Map<Integer, Integer> unifier = new HashMap<Integer, Integer>();
				unifier.put(new Integer(r0), new Integer(bfr.r0));
				unifier.put(new Integer(r1), new Integer(bfr.r1));
				unifiers.add(unifier);
			}
		} else if (r instanceof Overlaps && r.getArity() == 1) {
			if (stricterRole(r0, r.getArgRole(0))) {
				Map<Integer, Integer> unifier = new HashMap<Integer, Integer>();
				unifier.put(0, 0);
				unifiers.add(unifier);
			}
			if (stricterRole(r1, r.getArgRole(0))) {
				Map<Integer, Integer> unifier = new HashMap<Integer, Integer>();
				unifier.put(1, 0);
				unifiers.add(unifier);
			} 
		}
		return unifiers.isEmpty() ? null : unifiers;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Before)) return false;

		Before obf = (Before) o;
		return a0 == obf.a0 && a1 == obf.a1 && r0 == obf.r0 && r1 == obf.r1;
	}

	@Override
	public int hashCode() {
		return 10*((r0+a0) + 2*(r1+a1));
	}

	public boolean eval(Space[] spaceArgs) {
    	return spaceArgs[a0].before(spaceArgs[a1]);
	}

	public Set<AtomicRelation> getNormalizedAtomicRelations() {

		Set<AtomicRelation> rels = new HashSet<AtomicRelation>();
		if (a0 == a1) {
			rels.add(new Before(r0, r1, 0, 0));
		} else {
			rels.add(new Before(r0, r1, 0, 1));
		}
		return rels;
	}

	public Set<Integer> getRoles() {
		Set<Integer> rs = new HashSet<Integer>();
		rs.add(r0);
		rs.add(r1);
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

