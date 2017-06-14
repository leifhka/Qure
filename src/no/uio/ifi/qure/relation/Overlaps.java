package no.uio.ifi.qure.relation;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import no.uio.ifi.qure.Config;
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

	public boolean isIntrinsic(SID[] tuple) {
		if (getArity() < 2) return false;

		Set<SID> sids = new HashSet<SID>();
		for (int i = 0; i < tuple.length; i++) {
			if (tuple[i] == null) continue;
			for (int role : getStricter(new HashSet<Integer>(argRole.values()), tuple[i].getRole())) {
				SID s = new SID(tuple[i].getID(), role);
				if (sids.contains(s)) {
					return true;
				}
				sids.add(s);
			}
		}
		return false;
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
			return (unifiers.isEmpty()) ? null : unifiers;
		}
	}

	private boolean redundantUnifier(Map<Integer, Integer> unifier, Set<Map<Integer, Integer>> unifiers, AtomicRelation or) {

		if (or.getArity() == 1) return false;

		Set<SID> sids = new HashSet<SID>();
		for (Integer val : unifier.values()) {
			sids.add(new SID(val, or.getArgRole(val)));
		}

		Set<Integer> keys = unifier.keySet();
		for (Map<Integer, Integer> other : unifiers) {
			Set<Integer> oKeys = other.keySet();
			if (keys.equals(oKeys)) {
				Set<SID> oSids = new HashSet<SID>();
				for (Integer oVal : other.values()) {
					oSids.add(new SID(oVal, or.getArgRole(oVal)));
				}
				if (sids.equals(oSids)) return true;
			}
		}
		return false;
	}


	private void unifyOverlaps(Set<Map<Integer, Integer>> unifiers, Map<Integer, Integer> unifier,
	                           Set<Integer> tArgs, Set<Integer> rArgs, Overlaps r) {
    	if (rArgs.isEmpty()) { // Unifier found for all variables
    		if (!redundantUnifier(unifier, unifiers, r)) {
		    	unifiers.add(unifier);
    		}
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

	private Map<Integer, Set<SID>> getRoleToSID(Set<SID> tuple) {
		Map<Integer, Set<SID>> roleToSID = new HashMap<Integer, Set<SID>>();
		for (Integer role : getRoles()) {
			Set<SID> sids = new HashSet<SID>();
			for (SID s : tuple) {
				if (s != null && strictnessRelated(s.getRole(), role)) {
					sids.add(s);
				}
			}
			roleToSID.put(role, sids);
		}
		return roleToSID;
	}

	public boolean compatible(Set<SID> tuple) {
		if (tuple.size() != getArity()) return false;
		
		Map<Integer, Set<SID>> roleToSID = getRoleToSID(tuple);
		for (Integer pos : argRole.keySet()) {
			Set<SID> possible = roleToSID.get(argRole.get(pos));
			if (possible == null || possible.isEmpty()) {
				return false;
			} else {
				possible = Utils.getSome(possible).snd; // Remove abitrary element from possible
			}
		}
		return true;
	}
	
	private void generateAllOrderedTuples(Set<SID[]> tuples, Map<Integer, Set<SID>> roleToSID,
	                                      Set<Integer> remPos, SID[] tuple) {
		if (remPos.isEmpty()) {
			tuples.add(Arrays.copyOf(tuple, tuple.length));
		} else {
			Pair<Integer, Set<Integer>> somePos = Utils.getSome(remPos);
			for (SID s : roleToSID.get(argRole.get(somePos.fst))) {
				 tuple[somePos.fst] = s;
				 generateAllOrderedTuples(tuples, roleToSID, somePos.snd, tuple);
			}
		}
	}

	public Set<SID[]> generateAllOrderedTuples(Set<SID> tuple) {

		Map<Integer, Set<SID>> roleToSID = getRoleToSID(tuple);
		Set<SID[]> res = new HashSet<SID[]>();
		generateAllOrderedTuples(res, roleToSID, argRole.keySet(), new SID[getArity()]);
		return res;
	}

	public Table evalAll(SpaceProvider spaces) {
		// Must be a unary role-relation
		Table table = new Table(this);
		Integer role = argRole.get(Utils.getOnlySome(argRole.keySet()));

		for (SID sid : spaces.keySet()) {
			if (role.equals(sid.getRole())) {
				SID[] tuple = new SID[1];
				tuple[0] = sid;
				table.addTuple(tuple);
			}
		}
    	return table;
	}

	public Table evalAll(SpaceProvider spaces, Table possible) {
		Table table = new Table(this);

		for (SID[] tuple : possible.getTuples()) {
			if (getArity() == 1) {
				if (spaces.get(tuple[0]) != null) {
					table.addTuple(tuple);
				}
			} else if (eval(toSpaces(tuple, spaces))) {
				table.addTuple(tuple);
			}
		}
    	return table;
	}

	public String toSQL(Integer[] agrs, Config config) { //TODO
		return "";
	}

	public boolean isConjunctive(boolean insideNgeation) { return true; }
}

