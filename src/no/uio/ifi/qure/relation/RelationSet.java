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
	private int highestArity;

	private Set<AtomicRelation> leaves; // Used for implication graph
	private Map<AtomicRelation, Set<AtomicRelation>> implies;
	private Map<AtomicRelation, Set<AtomicRelation>> impliedBy;
	private Map<Pair<AtomicRelation, AtomicRelation>, Set<Map<Integer, Integer>>> unifiers;

	public RelationSet(Set<Relation> relations) {
		this.relations = relations;
		initRolesAndAtomic();
	}

	public RelationSet(Set<Relation> relations, String name) {
		this.relations = relations;
		this.name = name;
		initRolesAndAtomic();
	}

	public RelationSet add(Relation rel) {
    	Set<Relation> newRels = new HashSet<Relation>(relations);
    	newRels.add(rel);
    	return new RelationSet(newRels);
	}

	public RelationSet union(RelationSet rels) {
		return new RelationSet(Utils.union(relations, rels.getRelations()));
	}

	private void initRolesAndAtomic() {
		roles = new HashSet<Integer>();
		atomicRels = new HashSet<AtomicRelation>();
		highestArity = 0;

		for (Relation r : relations) {
			for (AtomicRelation a : r.getAtomicRelations()) {
				atomicRels.add(a);
				for (Integer role : a.getRoles()) {
					roles.add(role);
				}
				if (a.getArity() > highestArity) {
					highestArity = a.getArity();
				}
			}
		}
		// If roles is empty, we add the universal role 0
		if (roles.isEmpty()) {
    		roles.add(0);
		}

		removeRedundantAtomicRelations();
		computeImplicationGraph();
	}

	private void removeRedundantAtomicRelations() {

    	Set<AtomicRelation> removed = new HashSet<AtomicRelation>();

    	for (AtomicRelation r1 : new HashSet<AtomicRelation>(atomicRels)) {
        	if (removed.contains(r1)) {
            	continue;
        	}
        	if (r1.isValid()) {
            	atomicRels.remove(r1);
            	continue;
        	}
        	for (AtomicRelation r2 : new HashSet<AtomicRelation>(atomicRels)) {
            	if (!r1.equals(r2) && r1.impliesNonEmpty(r2) != null && r2.impliesNonEmpty(r1) != null) {
                	atomicRels.remove(r2);
                	removed.add(r2);
            	}
        	}
    	}
	}

	private void computeImplicationGraph() {

		implies = new HashMap<AtomicRelation, Set<AtomicRelation>>();
		impliedBy = new HashMap<AtomicRelation, Set<AtomicRelation>>();
		unifiers = new HashMap<Pair<AtomicRelation, AtomicRelation>, Set<Map<Integer, Integer>>>();

		for (AtomicRelation rel : atomicRels) {
			implies.put(rel, new HashSet<AtomicRelation>());
			impliedBy.put(rel, new HashSet<AtomicRelation>());
		}

		// Naive computation of all implications, with transitive closure
		// As the set of atomic relations has a low cardinality, a naive solution suffices
		for (AtomicRelation rel1 : atomicRels) {
			for (AtomicRelation rel2 : atomicRels) {
				if (rel1.equals(rel2)) continue;

				Set<Map<Integer, Integer>> unis = rel1.impliesNonEmpty(rel2);
				if (unis != null) {
					implies.get(rel1).add(rel2);
					impliedBy.get(rel2).add(rel1);
					unifiers.put(new Pair<AtomicRelation, AtomicRelation>(rel1, rel2), unis);
				}
			}
		}

		// Remove transitive closure to obtain minimal implication graph and find all leaves
		leaves = new HashSet<AtomicRelation>();
		for (AtomicRelation rel : atomicRels) {
			if (impliedBy.get(rel).isEmpty()) {
				removeTransitiveClosure(rel);
			}
			if (implies.get(rel).isEmpty()) {
				leaves.add(rel);
			}
		}
	}

	private void removeTransitiveClosure(AtomicRelation rel) {
		
		for (AtomicRelation child : new HashSet<AtomicRelation>(implies.get(rel))) {
			for (AtomicRelation cc : implies.get(child)) {
				implies.get(rel).remove(cc);
				unifiers.remove(new Pair<AtomicRelation, AtomicRelation>(rel, cc));
			}
			removeTransitiveClosure(child);
		}
	}

	public Set<AtomicRelation> getImplies(AtomicRelation rel) {
		return implies.get(rel);
	}

	public Set<AtomicRelation> getImpliedBy(AtomicRelation rel) {
		return impliedBy.get(rel);
	}

	public Set<AtomicRelation> getImpliedByWithOnlyVisitedChildren(AtomicRelation rel, Set<AtomicRelation> visited) {
		Set<AtomicRelation> rels = new HashSet<AtomicRelation>();
		for (AtomicRelation r : impliedBy.get(rel)) {
			if (visited.containsAll(implies.get(r))) {
				rels.add(r);
			}
		}
		return rels;
	}

	public String getName() { return name; }

	public void setName(String newName) { name = newName; }

	public Set<Relation> getRelations() { return relations; }

	public Set<Integer> getRoles() { return roles; }

	public Set<AtomicRelation> getImplicationGraphLeaves() { return leaves; }

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

    public static int getHighestArity(Set<AtomicRelation> rels) {
		int maxArr = 0;
		for (AtomicRelation rel : rels) {
			if (rel.getArity() > maxArr) maxArr = rel.getArity();
		}
		return maxArr;
    }

	public static Set<AtomicRelation> getRelationsWithHighestArity(Set<AtomicRelation> rels) {
		return getRelationsWithArity(getHighestArity(rels), rels);
	}

	public static Set<AtomicRelation> getRelationsWithArity(int arr, Set<AtomicRelation> rels) {
		Set<AtomicRelation> result = new HashSet<AtomicRelation>();
		
		for (AtomicRelation rel : rels) {
			if (rel.getArity() == arr) result.add(rel);
		}
		return result;
	}

	public int getHighestArity() { return highestArity; }

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

	public static RelationSet getAllensIntervalAlgebra(int f, int i, int l) {

    	Set<Relation> allen = new HashSet<Relation>();

    	allen.add(before(f, l, 0, 1));                                                             // before

    	allen.add(partOf(f, l, 0, 1).and(partOf(l, f, 1, 0)));                                     // meets

    	allen.add(overlaps(i, i, 0, 1).and(not(partOf(0, 0, 0, 1))).and(not(partOf(0, 0, 1, 0)))); // overlaps

    	allen.add(partOf(0, 0, 0, 1).and(partOf(0, 0, 1, 0)));                                     // equal

    	allen.add(partOf(l, l, 0, 1).and(partOf(l, l, 1, 0)).and(partOf(f, i, 0, 1)));             // starts

    	allen.add(partOf(l, i, 0, 1).and(partOf(f, i, 0, 1)));                                     // during

    	allen.add(partOf(f, f, 0, 1).and(partOf(f, f, 1, 0)).and(partOf(l, i, 0, 1)));             // ends

    	allen.add(before(f, l, 1, 0));                                                             // after

		return new RelationSet(allen, "allen");
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
    	return new RelationSet(simple, "simple" + arity);
	}
}
