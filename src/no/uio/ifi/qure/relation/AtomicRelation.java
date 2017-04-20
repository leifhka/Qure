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


public abstract class AtomicRelation extends Relation {

	// Used for implication graph
	private Set<AtomicRelation> implies = new HashSet<AtomicRelation>();
	private Set<AtomicRelation> impliedBy = new HashSet<AtomicRelation>();	

	public void addImplies(AtomicRelation rel) { implies.add(rel); }
	public void addImpliedBy(AtomicRelation rel) { impliedBy.add(rel); }
	public Set<AtomicRelation> getImpliedRelations() { return implies; }
	public Set<AtomicRelation> getImpliedByRelations() { return impliedBy; }


	// Rest of methods
	public abstract boolean impliesNonEmpty(AtomicRelation r);

	public boolean impliedByNonEmpty(AtomicRelation r) {
		return r.impliesNonEmpty(this);
	}

	public abstract boolean isValid();

	public abstract int getArity();

	public abstract Set<List<SID>> evalAll(Map<SID, ? extends Space> spaces, Map<Integer, Set<SID>> roleToSID);

}

