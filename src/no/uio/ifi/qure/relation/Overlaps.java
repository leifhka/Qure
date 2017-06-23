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

	private final Map<Integer, Integer> argRole;
	private final int[] args;

	public Overlaps(int r, int a) {
		argRole= new HashMap<Integer, Integer>();
		argRole.put(a, r);
		args = new int[]{a};
	}

	public Overlaps(int r0, int r1, int a0, int a1) {

		argRole= new HashMap<Integer, Integer>();
		if (a0 == a1) {
    		argRole.put(a0, (r0 | r1));
		} else {
        	argRole.put(a0, r0);
    		argRole.put(a1, r1);
    	}		
		args = new int[]{a0, a1};
	}

	public Overlaps(int[] rs, int[] as) {

		argRole = new HashMap<Integer, Integer>();
		for (int i = 0; i < as.length; i++) {
			argRole.put(as[i], 0);
		}
		for (int i = 0; i < as.length; i++) {
			argRole.put(as[i], argRole.get(as[i]) | rs[i]);
		}
		args = as;
	}

	public Overlaps(Map<Integer, Integer> argRole) {
		this.argRole = argRole;
		args = new int[argRole.keySet().size()];
		int i = 0;
		for (Integer k : argRole.keySet()) args[i++] = k;
	}

	public boolean relatesArg(int arg) {
		return argRole.containsKey(arg);
	}
		
	public String toBTSQL(Integer[] vals, Config config) { //TODO
		if (getArity() == 2) {
			return toBTSQL2(vals, config);
		} else {
			return null; // Base query on implied Overlaps of lower arity
		}
	}

	private String makeBlockOverlapsWhere(String table0, String table1) {
		String query = "";
		query += " ((" + table1 + ".block >= (" + table0 + ".block & (" + table0 + ".block-1)) AND \n";
        query += "   " + table1 + ".block <= (" + table0 + ".block | (" + table0 + ".block-1))) OR \n";
		query += "  " + table1 + ".block = ((" + table0 + ".block & ~(V.n-1)) | V.n))";
		return query;
	}

	private String toBTSQL2(Integer[] vals, Config config) {
		String[] sfw = makeSelectFromWhereParts(config.btTableName, config.uriColumn, vals);
		
		String from = sfw[1] + ",\n";
	    from += "(" + makeValuesFrom(config) + ") AS V(n)";
	    
		String query = "SELECT DISTINCT " + sfw[0] + "\n";
		query += " FROM " + from + "\n";
		query += " WHERE ";
		if (!sfw[2].equals("")) query += sfw[2] + " AND \n";
		for (int i = 0; i < args.length; i++) {
			if (argRole.get(args[i]) != 0) {
				query += "T" + args[i] + ".role & " + argRole.get(args[i]) + " != 0 AND\n";
			}
		}
		if (vals[0] != null) {
			query += makeBlockOverlapsWhere("T" + args[1], "T" + args[0]);
		} else {
			query += makeBlockOverlapsWhere("T" + args[0], "T" + args[1]);
		}
		return query;
	}

	public String toGeoSQL(Integer[] vals, Config config, SpaceProvider sp) {
		return sp.toSQL(this, vals, config);
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

	public Set<Integer> getArguments() { return argRole.keySet(); }

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

	public boolean isConjunctive(boolean insideNgeation) { return true; }
}

