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

	public String toString() {
		return "po(<" + r1 + "," + a1 + ">, <" + r2 + "," + a2 + ">)";
	}

	public String toSQL() { //TODO
		return "";
	}

	public boolean isValid() {
		return a1 == a2 && stricterRole(r1, r2);
	}

	public boolean impliesNonEmpty(AtomicRelation r) {

		if (r.isValid()) {
			return true;
		} else if (isValid() || (r instanceof Before)) {
			return false;
		} else if (r instanceof Overlaps) {
			Overlaps ovr = (Overlaps) r;
			if (r.getArity() > 2) { // The case of arity=1 is handled by r.isvalid()
				return false;
			} else {
				// First argument must overlap one of the arguments, and
				// second argument must contain the other argument.
    			return (strictnessRelated(r1, ovr.getArgRole(a1)) &&
				        stricterRole(r2, ovr.getArgRole(a2))) ||
				       (strictnessRelated(r1, ovr.getArgRole(a2)) &&
				        stricterRole(r2, ovr.getArgRole(a1)));
			}
		} else {
			PartOf pr = (PartOf) r;
			// First argument must overlap one of the arguments, and
			// second argument must contain the other argument.
			return a1 == pr.a1 && stricterRole(pr.r1, r1) &&
			       a2 == pr.a2 && stricterRole(r2, pr.r2);
		}
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

	public boolean evalRoled(Space[] spaceArgs) {
		return spaceArgs[a1].getPart(r1).partOf(spaceArgs[a2].getPart(r2));
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

	public Set<List<SID>> evalAll(Map<SID, ? extends Space> spaces, Map<Integer, Set<SID>> roleToSID) {

		Set<List<SID>> tuples = new HashSet<List<SID>>();

    	for (SID sid1 : roleToSID.get(r1)) {
        	for (SID sid2 : roleToSID.get(r2)) {
            	if (sid1.equals(sid2)) {
	            	continue;
            	}
            	
            	Space[] spaceTuple = new Space[]{spaces.get(sid1), spaces.get(sid2)};
				if (eval(spaceTuple)) {
    				tuples.add(Arrays.asList(new SID[]{sid1, sid2}));
				}
        	}
    	}
    	return tuples;
	}
	
	public Set<List<SID>> evalAll(Map<SID, ? extends Space> spaces, Set<List<SID>> possible, Map<Integer, Set<SID>> roleToSID) {

		Set<List<SID>> tuples = new HashSet<List<SID>>();

    	for (List<SID> posTup : possible) {
	    	SID sid1 = posTup.get(0);
	    	if (posTup.size() < 2) {
	        	for (SID sid2 : roleToSID.get(r2)) {
	            	if (sid1.equals(sid2)) {
		            	continue;
	            	}
            	
	            	Space[] spaceTuple = new Space[]{spaces.get(sid1), spaces.get(sid2)};
					if (eval(spaceTuple)) {
						tuples.add(Arrays.asList(new SID[]{sid1, sid2}));
					}
	        	}
	    	} else {
		    	SID sid2 = posTup.get(1);
            	Space[] spaceTuple = new Space[]{spaces.get(sid1), spaces.get(sid2)};
				if (eval(spaceTuple)) {
					tuples.add(Arrays.asList(new SID[]{sid1, sid2}));
				}
	    	}
    	}
    	return tuples;
	}

	public boolean isConjunctive(boolean insideNgeation) { return true; }
}
