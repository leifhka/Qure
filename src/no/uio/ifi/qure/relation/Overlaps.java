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

	private Map<Integer, Set<Integer>> argRoles;

	public Overlaps(int r1, int r2, int a1, int a2) {
		argRoles= new HashMap<Integer, Set<Integer>>();
		argRoles.put(a1, new HashSet<Integer>());
		argRoles.put(a2, new HashSet<Integer>());
		argRoles.get(a1).add(r1);
		argRoles.get(a2).add(r2);
	}

	public Overlaps(int[] rs, int[] as) {

		argRoles = new HashMap<Integer, Set<Integer>>();
		for (int i = 0; i < rs.length; i++) {
			argRoles.put(as[i], new HashSet<Integer>());
		}
		for (int i = 0; i < rs.length; i++) {
			argRoles.get(as[i]).add(rs[i]);
		}
	}

	public Overlaps(Map<Integer, Set<Integer>> argRoles) {
		this.argRoles = argRoles;
	}

	public int getArity() { return argRoles.keySet().size(); }

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Overlaps)) return false;

		Overlaps oov = (Overlaps) o;
		return argRoles.equals(oov.argRoles);
	}

	@Override
	public int hashCode() {
		int hc = 0;
		for (Integer r : argRoles.keySet()) {
			hc += r + argRoles.get(r).hashCode();
		}
		return hc;
	}

	public boolean evalRoled(Space[] spaceArgs) {

		Set<Space> sps = new HashSet<Space>();
		for (Integer arg : argRoles.keySet()) {	
			for (Integer role : argRoles.get(arg)) {
    			sps.add(spaceArgs[arg].getPart(role));
			}
		}
		Pair<Space, Set<Space>> sm = Utils.getSome(sps);
		return sm.fst.overlaps(sm.snd);
	}

	public boolean eval(Space[] spaceArgs) {

		Set<Space> sps = new HashSet<Space>();
		for (Integer arg : argRoles.keySet()) {	
			for (Integer role : argRoles.get(arg)) {
    			sps.add(spaceArgs[arg]);
			}
		}
		Pair<Space, Set<Space>> sm = Utils.getSome(sps);
		return sm.fst.overlaps(sm.snd);
	}

	public Set<AtomicRelation> getAtomicRelations() {

		Map<Integer, Integer> argNormMap = new HashMap<Integer, Integer>();
		int i = 0;
		for (Integer arg : argRoles.keySet()) {
			if (!argNormMap.containsKey(arg)) {
				argNormMap.put(arg, i);
				i++;
			}
		}
		Map<Integer, Set<Integer>> normalizedArgRoles = new HashMap<Integer, Set<Integer>>();
		for (Integer arg : argRoles.keySet()) {
			normalizedArgRoles.put(argNormMap.get(arg), argRoles.get(arg));
		}
		
		Set<AtomicRelation> rels = new HashSet<AtomicRelation>();
		rels.add(new Overlaps(normalizedArgRoles));
		return rels;
	}

	public Set<Integer> getRoles() {

		Set<Integer> rs = new HashSet<Integer>();
		for (Set<Integer> vrs : argRoles.values()) {
			for (Integer role : vrs) {
    			rs.add(role);
			}
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

	private void evalAll(Map<SID, ? extends Space> spaces, Set<Set<SID>> checked, Map<Integer, Set<SID>> roleToSID, Set<List<SID>> tuples, List<SID> tuple, int i) {

    	if (i == getArity()) {
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
        	Pair<Integer, Set<Integer>> someRole = Utils.getSome(argRoles.get(i));
        	Set<SID> candidates = new HashSet<SID>(roleToSID.get(someRole.fst));
        	for (Integer role : someRole.snd) {
            	candidates.retainAll(roleToSID.get(role));
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

	public boolean isOverlaps() { return true; }
}

