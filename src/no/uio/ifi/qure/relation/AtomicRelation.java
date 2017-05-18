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

	public Set<AtomicRelation> getImpliedByWithOnlyVisitedChildren(Set<AtomicRelation> visited) {
		Set<AtomicRelation> rels = new HashSet<AtomicRelation>();
		for (AtomicRelation rel : impliedBy) {
			if (visited.containsAll(rel.implies)) {
				rels.add(rel);
			}
		}
		return rels;
	}

	// Rest of methods

	/**
	 * Returns true iff this relation implies r for anny instantiation of the arguments with non-empty spaces.
	 */
	public abstract boolean impliesNonEmpty(AtomicRelation r);

	/**
	 * Returns true iff this relation is implied by r for any instantiation of the arguments with non-empty spaces.
	 */
	public boolean impliedByNonEmpty(AtomicRelation r) {
		return r.impliesNonEmpty(this);
	}

	/**
	 * Returns true iff this relation holds for any instantiation of the arguments with non-empty spaces.
	 */
	public abstract boolean isValid();

	public abstract int getArity();

	public abstract Set<Pair<List<SID>, Space>> evalAll(Map<SID, ? extends Space> spaces, Map<Integer, Set<SID>> roleToSID);

	public abstract Set<Pair<List<SID>, Space>> evalAll(Map<SID, ? extends Space> spaces,
	                                                    Set<Pair<List<SID>, Space>> tuples,
	                                                    Map<Integer, Set<SID>> roleToSID);

}

