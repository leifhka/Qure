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

public class PartOf extends AtomicRelation {

	private final int a1, a2, r1, r2;

	public PartOf(int r1, int r2, int a1, int a2) {
		this.a1 = a1;
		this.a2 = a2;
		this.r1 = r1;
		this.r2 = r2;
	}

	public int getArity() { return 2; }

	public Integer getArgRole(Integer pos) { return (pos.equals(a1)) ? r1 : r2; }

	public String toString() {
		return "po(<" + r1 + "," + a1 + ">, <" + r2 + "," + a2 + ">)";
	}

	public String toSQL() { //TODO
		return "";
	}

	public boolean isIntrinsic(Integer[] tuple) {
		return tuple[a1] == tuple[a2] && stricterRole(r1, r2);
	}

	public Set<Map<Integer, Integer>> impliesNonEmpty(AtomicRelation r) {

		if (r.getArity() > 2 || (r instanceof Before)) {
			return null;
		}

		Set<Map<Integer, Integer>> unifiers = new HashSet<Map<Integer, Integer>>();

		if (r instanceof Overlaps) {
			Overlaps ovr = (Overlaps) r;
			if (ovr.getArity() == 1) {
				Integer oRole = Utils.getOnlySome(ovr.getRoles());
				if (stricterRole(r1, oRole)) {
					Map<Integer, Integer> unifier = new HashMap<Integer, Integer>();
					unifier.put(a1, 0);
					unifiers.add(unifier);
				}
				if (stricterRole(r2, oRole)) {
					Map<Integer, Integer> unifier = new HashMap<Integer, Integer>();
					unifier.put(a2, 0);
					unifiers.add(unifier);
				}
			} else {

				// First argument must overlap one of the arguments, and
				// second argument must contain the other argument.
				if (strictnessRelated(r1, ovr.getArgRole(a1)) &&
				    stricterRole(r2, ovr.getArgRole(a2))) {

					Map<Integer, Integer> unifier = new HashMap<Integer, Integer>();
					unifier.put(new Integer(a1), new Integer(a1));
					unifier.put(new Integer(a2), new Integer(a2));
					unifiers.add(unifier);
				}
				if (strictnessRelated(r1, ovr.getArgRole(a2)) &&
				    stricterRole(r2, ovr.getArgRole(a1))) {

					Map<Integer, Integer> unifier = new HashMap<Integer, Integer>();
					unifier.put(new Integer(a1), new Integer(a1));
					unifier.put(new Integer(a2), new Integer(a2));
					unifiers.add(unifier);
				}
			}
		} else {
			PartOf pr = (PartOf) r;
			// First argument must be less strict than r's first argument, and
			// second argument must be stricter than  r's other argument.
			if (stricterRole(pr.r1, r1) && stricterRole(r2, pr.r2)) {
				Map<Integer, Integer> unifier = new HashMap<Integer, Integer>();
				unifier.put(new Integer(a1), new Integer(pr.a1));
				unifier.put(new Integer(a2), new Integer(pr.a2));
			}
		}
		return (unifiers.isEmpty()) ? null : unifiers;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PartOf)) return false;

		PartOf opo = (PartOf) o;
		return a1 == opo.a1 && a2 == opo.a2 && r1 == opo.r1 && r2 == opo.r2;
	}

	@Override
	public int hashCode() {
		return (r1+a1) + 2*(r2+a2);
	}

	public boolean eval(Space[] spaceArgs) {
    	return spaceArgs[a1].partOf(spaceArgs[a2]);
	}

	public Set<AtomicRelation> getAtomicRelations() {

		Set<AtomicRelation> rels = new HashSet<AtomicRelation>();
		if (a1 == a2) {
			rels.add(new PartOf(r1, r2, 0, 0));
		} else {
			rels.add(new PartOf(r1, r2, 0, 1));
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

			if (eval(toSpaces(tuple, spaces))) {
				table.addTuple(tuple);
	    	}
    	}
    	return table;
	}

	public boolean isConjunctive(boolean insideNgeation) { return true; }
}
