package no.uio.ifi.qure.relation;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.space.*;

import static no.uio.ifi.qure.relation.Relation.*;

public class RelationSet {

	Set<Relation> relations;
	Set<Integer> roles;
	Set<AtomicRelation> atomicRels;

	public RelationSet(Set<Relation> relations) {
		this.relations = relations;
		
		roles = new HashSet<Integer>();
		atomicRels = new HashSet<AtomicRelation>();

		for (Relation r : relations) {
			for (AtomicRelation a : r.getAtomicRelations()) {
				atomicRels.add(a);
				for (Integer role : a.getRoles()) {
					roles.add(role);
				}
			}
		}
		// If roles is empty, we add the universal role 0
		if (roles.isEmpty()) {
    		roles.add(0);
		}
	}

	public Set<Relation> getRelations() { return relations; }

	public Set<Integer> getRoles() { return roles; }

	public Set<AtomicRelation> getAtomicRelations() { return atomicRels; }

	public static RelationSet getRCC8(int i, int b) {
		
		Set<Relation> rcc8 = new HashSet<Relation>();

		rcc8.add(not(overlaps(0, 0, 0, 1)));                                                      // DJ

		rcc8.add(overlaps(0, 0, 0, 1).and(not(overlaps(i, i, 0, 1))));                            // EC

		rcc8.add(overlaps(i, i, 0, 1).and(not(partOf(0, 0, 0, 1))).and(not(partOf(0, 0, 1, 0)))); // PO

		rcc8.add(partOf(0, 0, 0, 1).and(partOf(0, 0, 1, 0)));                                     // EQ

		rcc8.add(partOf(0, 0, 0, 1).and(overlaps(b, b, 0, 1)).and(not(partOf(0, 0, 1, 0))));      // TPP

		rcc8.add(partOf(0, 0, 0, 1).and(not(overlaps(b, b, 0, 1))));                              // NTPP
		
		return new RelationSet(rcc8);
	}

	public static RelationSet getSimple(int arity) {

    	Set<Relation> simple = new HashSet<Relation>();
    	simple.add(partOf(0, 0, 0, 1));

    	for (int i = 2; i <= arity; i++) {

        	int[] noRoles = new int[i];
        	int[] args = new int[i];
        	for (int j = 0; j < i; j++) args[j] = j;
        	simple.add(overlaps(noRoles, args));
    	}
    	return new RelationSet(simple);
	}
}
