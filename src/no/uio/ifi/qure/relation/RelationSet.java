package no.uio.ifi.qure.relation;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.space.*;

import static no.uio.ifi.qure.relation.Relation.*;

public class RelationSet {

	private Set<Relation> relations;
	private Set<Integer> roles;
	private Set<AtomicRelation> atomicRels;
	private String name;

	private Set<AtomicRelation> roots; // Used for implication graph

	public RelationSet(Set<Relation> relations) {
		this.relations = relations;
		initRolesAndAtomic();
	}

	public RelationSet(Set<Relation> relations, String name) {
		this.relations = relations;
		this.name = name;
		initRolesAndAtomic();
	}

	private void initRolesAndAtomic() {
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

		computeImplicationGraph();
	}

	private void computeImplicationGraph() {

		roots = new HashSet<AtomicRelation>();

		// Naive computation of all implications, with transitive closure
		// As the set of atomic relations has a low cardinality, a naive solution suffices
		for (AtomicRelation rel1 : atomicRels) {
			for (AtomicRelation rel2 : atomicRels) {
				if (rel1.equals(rel2)) continue;
			
				if (rel1.impliesNonEmpty(rel2)) {
					rel1.addImplies(rel2);
					rel2.addImpliedBy(rel1);
				}
			}
		}

		// Remove transitive closure to obtain minimal implication graph and find all roots
		for (AtomicRelation rel : atomicRels) {
			if (rel.getImpliedByRelations().isEmpty()) {
				roots.add(rel);
				removeTransitiveClosure(rel);
			}
		}
	}

	private void removeTransitiveClosure(AtomicRelation rel) {
		
		for (AtomicRelation child : new HashSet<AtomicRelation>(rel.getImpliedRelations())) {
			rel.getImpliedRelations().removeAll(child.getImpliedByRelations());
			removeTransitiveClosure(child);
		}
	}	

	public String getName() { return name; }

	public Set<Relation> getRelations() { return relations; }

	public Set<Integer> getRoles() { return roles; }

	/**
	 * Returns the number of atmoic roles, that is, the number of bits needed to represnt all roles.
	 */
	public Set<Integer> getAtomicRoles() {

    	Set<Integer> atomicRoles = new HashSet<Integer>();

    	for (Integer role : getRoles()) {
        	for (int i = 1; i <= role; i = i << 1) {
            	if ((role & i) != 0) {
                	atomicRoles.add(i);
            	}
        	}
    	}
    	return atomicRoles;
    } 

	public Set<AtomicRelation> getAtomicRelations() { return atomicRels; }

	public static RelationSet getRCC8(int i, int b) {
		
		Set<Relation> rcc8 = new HashSet<Relation>();

		rcc8.add(not(overlaps(0, 0, 0, 1)));                                                      // DJ

		rcc8.add(overlaps(0, 0, 0, 1).and(not(overlaps(i, i, 0, 1))));                            // EC

		rcc8.add(overlaps(i, i, 0, 1).and(not(partOf(0, 0, 0, 1))).and(not(partOf(0, 0, 1, 0)))); // PO

		rcc8.add(partOf(0, 0, 0, 1).and(partOf(0, 0, 1, 0)));                                     // EQ

		rcc8.add(partOf(0, 0, 0, 1).and(overlaps(b, b, 0, 1)).and(not(partOf(0, 0, 1, 0))));      // TPP

		rcc8.add(partOf(0, 0, 0, 1).and(not(overlaps(b, b, 0, 1))));                              // NTPP
		
		return new RelationSet(rcc8, "RCC8");
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
    	return new RelationSet(simple, "simple-" + arity);
	}
}
