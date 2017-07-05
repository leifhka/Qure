package no.uio.ifi.qure.relation;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.space.*;
import no.uio.ifi.qure.traversal.*;

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

	public RelationSet() {
		relations = new HashSet<Relation>();
	}

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

	private Set<Map<Integer, Integer>> getUnifiers(AtomicRelation rel1, AtomicRelation rel2) {
		return unifiers.get(new Pair<AtomicRelation, AtomicRelation>(rel1, rel2));
	}

	private void initRolesAndAtomic() {
		roles = new HashSet<Integer>();
		atomicRels = new HashSet<AtomicRelation>();
		highestArity = 0;

		for (Relation r : relations) {
			for (AtomicRelation a : r.getNormalizedAtomicRelations()) {
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

		// Add unary role-relations
		atomicRels.add(new Overlaps(0,0));
		for (Integer role : roles) {
			atomicRels.add(new Overlaps(role, 0));
		}

		computeImplicationGraph();
	}

	private void removeTransitiveUnifiers(AtomicRelation rel) {
		for (AtomicRelation imp : getImplies(rel)) {
			for (AtomicRelation impimp : getImplies(imp)) {
				for (Map<Integer, Integer> uImp : new HashSet<Map<Integer, Integer>>(getUnifiers(rel, imp))) {
					for (Map<Integer, Integer> uImpimp : new HashSet<Map<Integer, Integer>>(getUnifiers(rel, impimp))) {
						 if (uImp.keySet().containsAll(uImpimp.keySet())) {
							 getUnifiers(rel, impimp).remove(uImpimp);
						 }
					}
				}
			}
		}
	}

	private void computeAllImplications(Set<AtomicRelation> fromRels, Set<AtomicRelation> toRels, Map<AtomicRelation, Set<AtomicRelation>> implies,
	                                    Map<AtomicRelation, Set<AtomicRelation>> impliedBy,
	                                    Map<Pair<AtomicRelation, AtomicRelation>, Set<Map<Integer, Integer>>> unifiers) {

		// Naive computation of all implications, with transitive closure
		// As the set of atomic relations has a low cardinality, a naive solution suffices
		for (AtomicRelation rel1 : fromRels) {
			for (AtomicRelation rel2 : toRels) {
				if (rel1.equals(rel2)) continue;

				Set<Map<Integer, Integer>> unis = rel1.impliesNonEmpty(rel2);
				if (unis != null) {
					implies.get(rel1).add(rel2);
					impliedBy.get(rel2).add(rel1);
					unifiers.put(new Pair<AtomicRelation, AtomicRelation>(rel1, rel2), unis);
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

		computeAllImplications(atomicRels, atomicRels, implies, impliedBy, unifiers);

		// Remove transitive closure to obtain minimal implication graph and find all leaves
		leaves = new HashSet<AtomicRelation>();
		for (AtomicRelation rel : atomicRels) {
			removeTransitiveUnifiers(rel);
			if (implies.get(rel).isEmpty()) {
				leaves.add(rel);
			}
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

	public int getHighestArity() { return highestArity; }

	public Set<AtomicRelation> getAtomicRelations() { return atomicRels; }
	
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

	public static Set<AtomicRelation> getRelationsWithArity(int arr, Set<AtomicRelation> rels) {
		Set<AtomicRelation> result = new HashSet<AtomicRelation>();
		
		for (AtomicRelation rel : rels) {
			if (rel.getArity() == arr) result.add(rel);
		}
		return result;
	}

	private Table getSmallest(Set<Table> tables) {
		if (tables.size() == 0) return null;

		Iterator<Table> iter = tables.iterator();
		Table smallest = iter.next();

		while (iter.hasNext()) {
			Table n = iter.next();
			if (smallest.size() > n.size()) {
				smallest = n;
			}
		}
		return smallest;
	}

//	public Set<Table> makeTables(AtomicRelation rel, Map<AtomicRelation, Table> tables) {
//
//		Set<Table> res = new HashSet<Table>();
//		for (AtomicRelation imp : getImplies(rel)) {
//			for (Map<Integer, Integer> unifier : unifiers.get(new Pair<AtomicRelation, AtomicRelation>(rel, imp))) {
//				res.add(Table.fromTable(tables.get(imp), unifier, rel));
//			}
//		}
//		return res;
//	}

	private Set<Set<SID>> getAllOverlaps(Map<AtomicRelation, Table> tables) {
		Set<Set<SID>> ovs = new HashSet<Set<SID>>();
		for (AtomicRelation rel : getAtomicRelations()) {
			if (rel instanceof Overlaps && rel.getArity() >= 2) {
				for (Integer[] tuple : tables.get(rel).getTuples()) {
					ovs.add(Utils.asSet(rel.toSIDs(tuple)));
				}
			}
		}
		return ovs;
	}

	private Set<SID[]> getAllPartOfs(Map<AtomicRelation, Table> tables) {
		Set<SID[]> partOfs = new HashSet<SID[]>();
		for (AtomicRelation rel : getAtomicRelations()) {
			if (rel instanceof PartOf) {
				for (Integer[] tuple : tables.get(rel).getTuples()) {
					partOfs.add(rel.toSIDs(tuple));
				}
			}
		}
		return partOfs;
	}

	private Set<SID[]> getAllBefores(Map<AtomicRelation, Table> tables) {
		Set<SID[]> befores = new HashSet<SID[]>();
		for (AtomicRelation rel : getAtomicRelations()) {
			if (rel instanceof Before) {
				for (Integer[] tuple : tables.get(rel).getTuples()) {
					befores.add(rel.toSIDs(tuple));
				}
			}
		}
		return befores;
	}

	private Table getJoinedTable(AtomicRelation rel, Map<AtomicRelation, Table> tables) {
		
		Table joined = Table.getUniversalTable(rel);
		
		for (AtomicRelation imp : getImplies(rel)) {
			for (Map<Integer, Integer> unifier : getUnifiers(rel, imp)) {
				joined = joined.join(rel, tables.get(imp), unifier);
			}
		}
		return joined;
	}
	
	public Relationships computeRelationships(SpaceProvider spaces) {
		
		// tuples contains map from relation to tuples/lists (with witness space) satisfying that relation
		// The witness space is the intersection of the spaces in the list, and can be used to optimize computation
		// of other more specific relationships (e.g. higher arity overlaps or part-of)
		Map<AtomicRelation, Table> tables = new HashMap<AtomicRelation, Table>();
		// nexRels contains all relations to visit next according to implication graph. Start at leaves.
		Set<AtomicRelation> nextRels = new HashSet<AtomicRelation>(getImplicationGraphLeaves());
		// currentRels will contain the relations to visit this iteration, taken from previou's nextRels.
		Set<AtomicRelation> currentRels;
		Set<AtomicRelation> visited = new HashSet<AtomicRelation>();

		while (!nextRels.isEmpty()) {

			currentRels = new HashSet<AtomicRelation>(nextRels);
			nextRels.clear();
			
			for (AtomicRelation rel : currentRels) {

				if (!getImplies(rel).isEmpty()) {
					// We first join the tables of the actual tuples in the implied relations
					Table joined = getJoinedTable(rel, tables);
					// And then filter these possible tuples on the actual relationships
					Table actual = rel.evalAll(spaces, joined);
					tables.put(rel, actual);
				} else {
					// Leaf-relation, thus we need to check all constructable tuples from spaces
					tables.put(rel, rel.evalAll(spaces));
				}
				visited.add(rel);
				nextRels.addAll(getImpliedByWithOnlyVisitedChildren(rel, visited));
			}
		}
		return new Relationships(getAllPartOfs(tables), getAllBefores(tables), getAllOverlaps(tables));
	}

	public static RelationSet getRCC8(int i, int b) {
		Set<Relation> rcc8 = new HashSet<Relation>();

		Relation dj = not(overlaps(0, 0, 0, 1));
		dj.setName("DJ");
		rcc8.add(dj);

		Relation ec = overlaps(0, 0, 0, 1).and(not(overlaps(i, i, 0, 1)));
		ec.setName("EC");
		rcc8.add(ec);

		Relation po = overlaps(i, i, 0, 1).and(not(partOf(0, 0, 0, 1))).and(not(partOf(0, 0, 1, 0)));
		po.setName("PO");
		rcc8.add(po);

		Relation eq = partOf(0, 0, 0, 1).and(partOf(0, 0, 1, 0));
		eq.setName("EQ");
		rcc8.add(eq);

		Relation tpp = partOf(0, 0, 0, 1).and(overlaps(b, b, 0, 1)).and(not(partOf(0, 0, 1, 0)));
		tpp.setName("TPP");
		rcc8.add(tpp);

		Relation ntpp = partOf(0, 0, 0, 1).and(not(overlaps(b, b, 0, 1)));
		ntpp.setName("NTPP");
		rcc8.add(ntpp);

		return new RelationSet(rcc8, "RCC8");
	}

	public static RelationSet getAllensIntervalAlgebra(int f, int i, int l) {
    	Set<Relation> allen = new HashSet<Relation>();

		Relation before = before(f, l, 0, 1);
		before.setName("Before");
    	allen.add(before);

    	Relation meets = partOf(f, l, 0, 1).and(partOf(l, f, 1, 0));
    	meets.setName("Meets");
    	allen.add(meets);

    	Relation overlaps = overlaps(i, i, 0, 1).and(not(partOf(0, 0, 0, 1))).and(not(partOf(0, 0, 1, 0)));
    	overlaps.setName("Overlaps");
    	allen.add(overlaps);

    	Relation equal = partOf(0, 0, 0, 1).and(partOf(0, 0, 1, 0));
    	equal.setName("Equal");
    	allen.add(equal);

    	Relation starts = partOf(l, l, 0, 1).and(partOf(l, l, 1, 0)).and(partOf(f, i, 0, 1));
    	starts.setName("Starts");
    	allen.add(starts);

    	Relation during = partOf(l, i, 0, 1).and(partOf(f, i, 0, 1));
    	during.setName("During");
    	allen.add(during);

    	Relation ends = partOf(f, f, 0, 1).and(partOf(f, f, 1, 0)).and(partOf(l, i, 0, 1));
    	ends.setName("Ends");
    	allen.add(ends);

    	Relation after = before(f, l, 1, 0);
    	after.setName("After");
    	allen.add(after);

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
