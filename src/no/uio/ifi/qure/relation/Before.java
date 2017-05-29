
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

public class Before extends AtomicRelation {

	private final int a1, a2, r1, r2;

	public Before(int r1, int r2, int a1, int a2) {
		this.a1 = a1;
		this.a2 = a2;
		this.r1 = r1;
		this.r2 = r2;
	}

	public int getArity() { return 2; }

	public String toString() {
		return "bf(<" + r1 + "," + a1 + ">, <" + r2 + "," + a2 + ">)";
	}

	public String toSQL() { //TODO
		return "";
	}

	public boolean isValid() {
		return false;
	}

	public Set<Map<Integer, Integer>> impliesNonEmpty(AtomicRelation r) {

		if (r.isValid()) {
			return new HashSet<Map<Integer, Integer>>();
		} else if (!isValid() && (r instanceof Before)) {
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

	public boolean evalRoled(List<Space> spaceArgs) {
		return spaceArgs.get(a1).getPart(r1).before(spaceArgs.get(a2).getPart(r2));
	}

	public boolean eval(List<Space> spaceArgs) {
    	return spaceArgs.get(a1).before(spaceArgs.get(a2));
	}

	public Set<AtomicRelation> getAtomicRelations() {

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
	
	public List<SID> toSIDs(List<Integer> tuple) {
		List<SID> sids = new ArrayList<SID>(2);
		sids.add(new SID(tuple.get(0), r1));
		sids.add(new SID(tuple.get(1), r2));
		return sids;
	}
	
	public Set<List<Integer>> evalAll(SpaceProvider spaces, Map<Integer, Set<SID>> roleToSID) {

		Set<List<Integer>> tuples = new HashSet<List<Integer>>();

    	for (SID sid1 : roleToSID.get(r1)) {
        	for (SID sid2 : roleToSID.get(r2)) {
            	if (sid1.equals(sid2)) {
	            	continue;
            	}

            	List<Integer> newTuple = new ArrayList<Integer>(2);
            	newTuple.add(sid1.getID());
            	newTuple.add(sid2.getID());
				if (eval(spaces.toSpaces(toSIDs(newTuple)))) {
    				tuples.add(newTuple);
				}
        	}
    	}
    	return tuples;
	}
	
	public Set<List<Integer>> evalAll(SpaceProvider spaces, List<Integer> possible, Map<Integer, Set<SID>> roleToSID) {

		Set<List<Integer>> tuples = new HashSet<List<Integer>>();

    	for (List<Integer> pos : tupleToLists(possible)) {

        	Integer id1 = pos.get(0);

	    	if (pos.size() < 2) {
	        	for (SID sid2 : roleToSID.get(r2)) {
		        	Integer id2 = sid2.getID();
	            	if (id1.equals(id2)) {
		            	continue;
	            	}
            	
            		List<Integer> newTuple = new ArrayList<Integer>(2);
            		newTuple.add(id1);
            		newTuple.add(id2);
					if (eval(spaces.toSpaces(toSIDs(newTuple)))) {
    					tuples.add(newTuple);
					}
	        	}
	    	} else {
		    	Integer id2 = pos.get(1);
		    	if (id1.equals(id2)) {
    		    	continue;
		    	}
		    	
        		List<Integer> newTuple = new ArrayList<Integer>(2);
        		newTuple.add(id1);
        		newTuple.add(id2);
				if (eval(spaces.toSpaces(toSIDs(newTuple)))) {
					tuples.add(newTuple);
				}
	    	}
    	}
    	return tuples;
	}

	public boolean isConjunctive(boolean insideNgeation) { return true; }
}

