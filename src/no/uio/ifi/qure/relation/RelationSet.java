package no.uio.ifi.qure.relation;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.space.*;

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
}
