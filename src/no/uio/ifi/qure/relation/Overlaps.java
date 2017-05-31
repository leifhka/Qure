package no.uio.ifi.qure.relation;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.traversal.*;
import no.uio.ifi.qure.space.*;

public class Overlaps extends AtomicRelation {

	private Map<Integer, Integer> argRole;

	public Overlaps(int r, int a) {
		argRole= new HashMap<Integer, Integer>();
		argRole.put(a, r);
	}

	public Overlaps(int r1, int r2, int a1, int a2) {

		argRole= new HashMap<Integer, Integer>();
		if (a1 == a2) {
    		argRole.put(a1, (r1 | r2));
		} else {
        	argRole.put(a1, r1);
    		argRole.put(a2, r2);
    	}		
	}

	public Overlaps(int[] rs, int[] as) {

		argRole = new HashMap<Integer, Integer>();
		for (int i = 0; i < as.length; i++) {
			argRole.put(as[i], 0);
		}
		for (int i = 0; i < as.length; i++) {
			argRole.put(as[i], argRole.get(as[i]) | rs[i]);
		}
	}

	public Overlaps(Map<Integer, Integer> argRole) {
		this.argRole = argRole;
	}

	public String toString() {
		String res = "ov(";
		String delim = "";
		for (int arg : argRole.keySet()) {
			res += delim + "<" + argRole.get(arg) + "," + arg + ">";
			if (delim.equals("")) delim = ", ";
		}
		return res + ")";
	}

	public Integer getArgRole(Integer arg) { return argRole.get(arg); }

	public int getArity() {
		return argRole.keySet().size();
	}

	public Set<Map<Integer, Integer>> impliesNonEmpty(AtomicRelation r) {

		if (!(r instanceof Overlaps)) {
			return null;
		} else {
    		if (getArity() < r.getArity()) return null;

			Overlaps ovr = (Overlaps) r;
			Set<Map<Integer, Integer>> unifiers = new HashSet<Map<Integer, Integer>>();
			Map<Integer, Integer> unifier = new HashMap<Integer, Integer>();
			Set<Integer> rArgs = new HashSet<Integer>(ovr.argRole.keySet());
			Set<Integer> tArgs = new HashSet<Integer>(argRole.keySet());
			unifyOverlaps(unifiers, unifier, tArgs, rArgs, ovr);
			return unifiers;
		}
	}

	private void unifyOverlaps(Set<Map<Integer, Integer>> unifiers, Map<Integer, Integer> unifier,
	                           Set<Integer> tArgs, Set<Integer> rArgs, Overlaps r) {
    	if (rArgs.isEmpty()) { // Unifier found for all variables
	    	unifiers.add(unifier);
    		return;
    	}

		// Pick some argument which we try to unify with next
    	Pair<Integer, Set<Integer>> oneArg = Utils.getSome(rArgs);
    	Integer rArg = oneArg.fst;

		// Generate a set of all possible unifications for rArg
    	Set<Integer> possible = new HashSet<Integer>();
    	for (Integer tArg : tArgs) {
        	if (stricterRole(argRole.get(tArg), r.argRole.get(rArg))) {
            	possible.add(tArg);
        	}
    	}
    	// Then recusively try to unify rest of variables with each unification
    	for (Integer tArg : possible) {
        	Set<Integer> tArgsNew = new HashSet<Integer>(tArgs);
        	tArgsNew.remove(tArg);
        	Map<Integer, Integer> newUnifier = new HashMap<Integer, Integer>(unifier);
        	newUnifier.put(tArg, rArg);
        	unifyOverlaps(unifiers, newUnifier, tArgsNew, oneArg.snd, r);
    	}
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Overlaps)) return false;

		Overlaps oov = (Overlaps) o;
		return argRole.equals(oov.argRole);
	}

	@Override
	public int hashCode() {
		int hc = 0;
		for (Integer r : argRole.keySet()) {
			hc += r + argRole.get(r).hashCode();
		}
		return hc;
	}

	public boolean eval(Space[] spaceArgs) {

		Pair<Space, Set<Space>> sm = Utils.getSome(Utils.asSet(spaceArgs));
		return sm.fst.overlaps(sm.snd);
	}

	public Set<AtomicRelation> getAtomicRelations() {

		Map<Integer, Integer> argNormMap = new HashMap<Integer, Integer>();
		int i = 0;
		for (Integer arg : argRole.keySet()) {
			if (!argNormMap.containsKey(arg)) {
				argNormMap.put(arg, i);
				i++;
			}
		}
		Map<Integer, Integer> normalizedArgRole = new HashMap<Integer, Integer>();
		for (Integer arg : argRole.keySet()) {
			normalizedArgRole.put(argNormMap.get(arg), argRole.get(arg));
		}
		
		Set<AtomicRelation> rels = new HashSet<AtomicRelation>();
		rels.add(new Overlaps(normalizedArgRole));
		return rels;
	}

	public Set<Integer> getRoles() {
		return new HashSet<Integer>(argRole.values());
	}

	public Table evalAll(SpaceProvider spaces) {
		// Must be a unary role-relation
		Table table = new Table(this);
		Integer role = argRole.get(Utils.getOnlySome(argRole.keySet()));

		for (SID sid : spaces.keySet()) {
			if (role.equals(sid.getRole())) {
				Integer[] tuple = new Integer[1];
				tuple[0] = sid.getID();
				table.addTuple(tuple);
			}
		}
    	return table;
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

	public String toSQL() { //TODO
		return "";
	}

	public boolean isConjunctive(boolean insideNgeation) { return true; }
}

