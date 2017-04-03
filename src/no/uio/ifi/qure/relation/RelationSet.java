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
	Map<Integer, Set<Relation>> atomicRels;

	public RelationSet(Set<Relation> relations) {
		this.relations = relations;
		
		roles = new HashSet<Integer>();
		atomicRels = new HashMap<Integer, Set<Relation>>();

		for (Relation r : relations) {
			for (Relation a : r.getAtomicRels()) {
				for (Integer role : a.getRoles()) {

					roles.add(role);

					if (!atomicRels.containsKey(role)) {
						atomicRels.put(role, new HashSet<Relation>());
					}
    				atomicRels.get(role).add(a);
				}
			}
		}
	}

	public Set<Relation> getRelations() { return relations; }

	public Set<Integer> getRoles() { return roles; }

	public Map<Integer, Set<Relation>> getAtomicRels() { return atomicRels; }

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
}
