
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

	public boolean impliesNonEmpty(AtomicRelation r) {

		if (r.isValid()) {
			return true;
		} else if (isValid() || r instanceof Overlaps || r instanceof PartOf) {
			return false;
		} else {
			Before bfr = (Before) r;
			return a1 == bfr.a1 && stricterRole(bfr.r1, r1) &&
			       a2 == bfr.a2 && stricterRole(bfr.r2, r2);
		}
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

	public boolean evalRoled(Space[] spaceArgs) {
		return spaceArgs[a1].getPart(r1).before(spaceArgs[a2].getPart(r2));
	}

	public boolean eval(Space[] spaceArgs) {
    	return spaceArgs[a1].before(spaceArgs[a2]);
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

	public Set<Tuple> evalAll(SpaceProvider spaces, Map<Integer, Set<SID>> roleToSID) {

		Set<Tuple> tuples = new HashSet<Tuple>();

    	for (SID sid1 : roleToSID.get(r1)) {
        	for (SID sid2 : roleToSID.get(r2)) {
            	if (sid1.equals(sid2)) {
	            	continue;
            	}
            	
            	Space[] spaceTuple = new Space[]{spaces.get(sid1), spaces.get(sid2)};
				if (eval(spaceTuple)) {
    				tuples.add(new Tuple(Arrays.asList(new SID[]{sid1, sid2})));
				}
        	}
    	}
    	return tuples;
	}
	
	public Set<Tuple> evalAll(SpaceProvider spaces, Set<Tuple> possible, Map<Integer, Set<SID>> roleToSID) {

		Set<Tuple> tuples = new HashSet<Tuple>();

    	for (List<SID> pos : tuplesToLists(possible)) {

        	SID sid1 = pos.get(0);

	    	if (pos.size() < 2) {
	        	for (SID sid2 : roleToSID.get(r2)) {
	            	if (sid1.equals(sid2)) {
		            	continue;
	            	}
            	
	            	Space[] spaceTuple = new Space[]{spaces.get(sid1), spaces.get(sid2)};
					if (eval(spaceTuple)) {
						tuples.add(new Tuple(Arrays.asList(new SID[]{sid1, sid2})));
					}
	        	}
	    	} else {
		    	SID sid2 = pos.get(1);
            	Space[] spaceTuple = new Space[]{spaces.get(sid1), spaces.get(sid2)};
				if (eval(spaceTuple)) {
					tuples.add(new Tuple(Arrays.asList(new SID[]{sid1, sid2})));
				}
	    	}
    	}
    	return tuples;
	}

	public boolean isConjunctive(boolean insideNgeation) { return true; }
}

