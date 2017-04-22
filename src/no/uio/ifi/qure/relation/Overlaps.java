package no.uio.ifi.qure.relation;

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

	protected Integer getArgRole(int arg) { return argRole.get(arg); }

	public int getArity() {
		return argRole.keySet().size();
	}

	public boolean isValid() {
		return getArity() == 1; // Assuming non-empty argument for argument role
	}

	public boolean impliesNonEmpty(AtomicRelation r) {

		if (r.isValid()) {
			return true;
		} else if (isValid() || !(r instanceof Overlaps)) {
			return false;
		} else {
    		if (getArity() < r.getArity()) return false;

			Overlaps ovr = (Overlaps) r;
			Set<Integer> rArgs = new HashSet<Integer>(ovr.argRole.keySet());
			Set<Integer> tArgs = new HashSet<Integer>(argRole.keySet());
			return unifyOverlaps(tArgs, rArgs, ovr);
		}
	}

	private boolean unifyOverlaps(Set<Integer> tArgs, Set<Integer> rArgs, Overlaps r) {
    	if (rArgs.isEmpty()) return true;

    	Set<Integer> possible = new HashSet<Integer>();
    	Pair<Integer, Set<Integer>> oneArg = Utils.getSome(rArgs);
    	Integer rArg = oneArg.fst;

    	for (Integer tArg : tArgs) {
        	if (stricterRole(argRole.get(tArg), r.argRole.get(rArg))) {
            	possible.add(tArg);
        	}
    	}
    	for (Integer tArg : possible) {
        	Set<Integer> tArgsNew = new HashSet<Integer>(tArgs);
        	tArgsNew.remove(tArg);
        	if (unifyOverlaps(tArgsNew, oneArg.snd, r)) return true;
    	}
    	return false;
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

	public boolean evalRoled(Space[] spaceArgs) {

		Set<Space> sps = new HashSet<Space>();
		for (Integer arg : argRole.keySet()) {	
    			sps.add(spaceArgs[arg].getPart(argRole.get(arg)));
		}
		Pair<Space, Set<Space>> sm = Utils.getSome(sps);
		return sm.fst.overlaps(sm.snd);
	}

	public boolean eval(Space[] spaceArgs) {

		Set<Space> sps = new HashSet<Space>();
		for (Integer arg : argRole.keySet()) {	
			sps.add(spaceArgs[arg]);
		}
		Pair<Space, Set<Space>> sm = Utils.getSome(sps);
		return sm.fst.overlaps(sm.snd);
	}

	public Set<AtomicRelation> getAtomicRelations() {

		// TODO: Normalize argument-variables' order according to number of roles
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

		Set<Integer> rs = new HashSet<Integer>();
		for (Integer role : argRole.values()) {
			rs.add(role);
		}
		return rs;
	}

	public Set<List<SID>> evalAll(Map<SID, ? extends Space> spaces, Map<Integer, Set<SID>> roleToSID) {
    	List<SID> args = new ArrayList<SID>(getArity());
    	Set<List<SID>> tuples = new HashSet<List<SID>>();
    	Set<Set<SID>> checked = new HashSet<Set<SID>>();
		evalAll(spaces, checked, roleToSID, tuples, args, 0);
    	return tuples;
	}

	private void evalAll(Map<SID, ? extends Space> spaces, Set<Set<SID>> checked,
	                     Map<Integer, Set<SID>> roleToSID, Set<List<SID>> tuples, List<SID> tuple, int i) {

    	if (i == getArity()) {
			// Found one potensial tuple, so we now eval that if not checked before
			if (!checked.contains(new HashSet<SID>(tuple))) {
    			Space[] spaceTuple = new Space[getArity()];

    			for (int j = 0; j < getArity(); j++) {
        			spaceTuple[j] = spaces.get(tuple.get(j));
    			}

				if (eval(spaceTuple)) tuples.add(tuple);
				checked.add(new HashSet<SID>(tuple));
			}
    	} else {
        	// We first need to extract all candidate SIDs that has a non-empty space for each role of i'th arg
        	Set<Integer> stricterRoles = getStricter(roleToSID.keySet(), argRole.get(i));
        	Set<SID> candidates = new HashSet<SID>();

        	for (Integer role : stricterRoles) {
            	candidates.addAll(roleToSID.get(role));
        	}

        	candidates.removeAll(tuple);

        	// We then try eval-ing with each candidate as i'th argument
        	for (SID sid : candidates) {
            	List<SID> newTuple = new ArrayList<SID>(tuple);
            	newTuple.add(sid);
            	evalAll(spaces, checked, roleToSID, tuples, newTuple, i+1);
        	}
    	}
	}

	public String toSQL() { //TODO
		return "";
	}

	public boolean isConjunctive(boolean insideNgeation) { return true; }
}
