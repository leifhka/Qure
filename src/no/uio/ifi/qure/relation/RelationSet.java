package no.uio.ifi.qure.relation;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
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

	// TODO: Does not work, need to take unifier into account
	public static AtomicRelation getSmallestRelationWithHighestArity(Set<AtomicRelation> rels,
	                                                                 Map<AtomicRelation, Set<Integer[]>> tuples) {
		Pair<AtomicRelation, Set<AtomicRelation>> some = Utils.getSome(getRelationsWithArity(getHighestArity(rels), rels));
		AtomicRelation sha = some.fst;
		int smallest = tuples.get(sha).size();
		for (AtomicRelation rel : some.snd) {
			int s = tuples.get(rel).size();
			if (s < smallest) {
				smallest = s;
				sha = rel;
			}
		}
		return sha;
	}

	public static Set<AtomicRelation> getRelationsWithArity(int arr, Set<AtomicRelation> rels) {
		Set<AtomicRelation> result = new HashSet<AtomicRelation>();
		
		for (AtomicRelation rel : rels) {
			if (rel.getArity() == arr) result.add(rel);
		}
		return result;
	}

	public Set<Table> makeTables(AtomicRelation rel, Map<AtomicRelation, Table> tables) {

		return null; //TODO
	}

	public void removeAdded(Set<Map<Integer, Integer>> unis, Table table, Table added) {

		for (Map<Integer, Integer> unifier : unis) {
			for (Integer[] tuple : added.tuples) {
				// Remove tuple based on unifier from table
			}
		}
	}

	public Map<AtomicRelation, Set<Integer[]>> computeRelationships(SpaceProvider spaces, Map<Integer, Set<SID>> roleToSID) {
		
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
					Set<Tables> possibleTables = makeTables(rel, tables);
					Pair<Table, Set<Tables>> smallestP = Utils.getSmallest(possibleTables);
					Table possible = smallestP.fst;
				
					for (Table larger : smallestP.snd) {
						possible = possible.join(larger);
					}
					Table actual = rel.evalAll(possible);
					tables.put(rel, actual);
					for (AtomicRelation implies : getImplies(rel)) {
						removeAdded(new Pair<AtomicRelation, AtomicRelation>(rel, implies), tables.get(implies), actual);
					}
				} else {
					// Leaf-relation, thus we need to check all constructable tuples from spaces
					tables.put(rel, rel.evalAll(spaces, roleToSID));
				}
				visited.add(rel);
				nextRels.addAll(getImpliedByWithOnlyVisitedChildren(rel, visited));
			}
		}
		return tuples;
	}

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
	

	private class Table {
		private final Set<Integer[]> tuples;
		private final List<Map<Integer, Set<Integer[]>>> indecies;

		public Table(int nrColumns) {
			tuples = new HashSet<Integer[]>();
			indecies = new ArrayList<Map<Integer, Set<Integer[]>>>();
			for (int i = 0; i < nrColumns; i++) {
				indecies.add(new HashMap<Integer, Set<Integer[]>>());
			}
		}

		/**
		 * Returns the tuple that results from applying the reverse of unifier to tuple, putting null/wild-card
		 * for non-matched positions
		 */
		private Integer[] fromUnifier(Integer[] tuple, Map<Integer, Integer> unifier, int arity) {
			Integer[] res = new Integer[arity];
			for (int i = 0; i < arity; i++) {
				if (unifier.get(i) != null) {
					res[i] = tuple[unifier.get(i)];
				}
			}
			return res;
		}
		
		public Table fromTable(Table other, Map<Integer, Integer> unifier, int nrColumns) {

			Table res = new Table(nrColumns);
			for (Integer[] tuple : other.tuples) {
				res.addTuple(fromUnifier(tuple, unifier, nrColumns));
			}
			return res;
		}

		public int size() { return tuples.size(); }

		public void addTuple(Integer[] tuple) {
			tuples.add(tuple);
			for (int i = 0; i < tuple.size(); i++) {
				if (tuple.get(i) != null) {
					indecies.get(i).putIfAbsent(tuple[i], new HashSet<Integer[]>());
					indecies.get(i).get(tuple.[i]).add(tuple);
				}
			}
		}

		/**
		 * Returns the tuple-join of t1 and t2 with null being a wild-card
		 */
		public static Integer[] join(Integer[] t1, Integer[] t2) {
			Integer[] res = new Integer[]();
			for (int i = 0; i < t1.length; i++) {
				if (t1[i] == null) {
					res[i] = t2[i];
				} else if (t2[i] == null || t1[i].equals(t2[i])) {
					res[i] = t1[i];
				} else {
					return null;
				}
			}
			return res;
		}

		/**
		 * Returns the set of tuples from this table that joins on all fields with argument
		 * where null is a wild-card
		 */
		public Set<Integer[]> getJoinable(Integer[] tuple) {
			Set<Integer[]> res = indecies.get(0).get(tuple[0]);
			for (int i = 1; i < tuple.length; i++) {
				res.retainAll(indecies.get(i).get(tuple[i]));
			}
			return res;
		}

		/**
		 * Returns a table containing the relational join of this and other
		 */
		public Table join(Table other) {
			Table res = new Table(indecies.size());
			for (Integer[] tuple : tuples) {
				for (Integer[] joinable : other.getJoinable(tuple)) {
					res.addTuple(join(tuple, joinable));
				}
			}
			return res;
		}
	}
}
