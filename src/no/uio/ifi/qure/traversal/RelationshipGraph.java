package no.uio.ifi.qure.traversal;

import java.util.*;

import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.space.*;
import no.uio.ifi.qure.bintree.*;
import no.uio.ifi.qure.relation.*;

public class RelationshipGraph {

	private final Map<SID, Set<SID>> partOf;
	private final Map<SID, Set<SID>> hasPart;
	private final Map<SID, Set<SID>> before;
	private final Map<SID, Set<SID>> after;
	private final SpaceProvider spaces;
	private final Set<SID> topmostNodes;
	private int overlapsNodeId; // Always negative, and decreasing
	private final Block block;
	private final RelationSet relations;

	public RelationshipGraph(Block block, SpaceProvider spaces, RelationSet relations) {
		this.block = block;
		this.relations = relations;
		this.spaces = spaces;

		topmostNodes = new HashSet<SID>(spaces.keySet()); // Init all uris as roots, and remove if set parent of some node
		partOf = new HashMap<SID, Set<SID>>();
		hasPart = new HashMap<SID, Set<SID>>();
		before = new HashMap<SID, Set<SID>>();
		after = new HashMap<SID, Set<SID>>();

		for (SID uri : spaces.keySet()) {
			addUri(uri);
		}
		for (SID uri : spaces.keySet()) {
			for (Integer role : Relation.getStricter(relations.getRoles(), uri.getRole())) {
				SID part = new SID(uri.getID(), role);
				if (spaces.keySet().contains(part) && !uri.equals(part)) {
					addCoveredBy(part, uri);
				}
			}
		}
		overlapsNodeId = 0;
	}

	private void addUri(SID uri) {
		topmostNodes.add(uri);
		partOf.put(uri, new HashSet<SID>());
		hasPart.put(uri, new HashSet<SID>());
		before.put(uri, new HashSet<SID>());
		after.put(uri, new HashSet<SID>());
	}

	public void addUris(Set<SID> newUris) {
		for (SID uri : newUris) {
			if (!hasPart.containsKey(uri)) {
				addUri(uri);
			}
		}
	}

	private SID newOverlapsNode() {
		overlapsNodeId--;
		SID ovSID = new SID(overlapsNodeId);
		addUri(ovSID);
		return ovSID;
	}

	public boolean isOverlapsNode(SID nodeSID) { return nodeSID.getID() < 0; }

	/**
	 * Adds a containment-relationship between child and parent if necessary (not already in graph).
	 */ 
	public void addCoveredBy(SID child, SID parent) {

		if (!isOverlapsNode(child) && !isOverlapsNode(parent)) {
			Set<SID> both = new HashSet<SID>(2);
			both.add(child);
			both.add(parent);
			removeOverlapsNodes(getRedundantOverlapNodes(both));
		}
		
		// Locally update coveredBy
		partOf.get(child).add(parent);
		topmostNodes.remove(child);

		// Locally update covers
		hasPart.get(parent).add(child);

		// Transitive closure
		partOf.get(child).addAll(partOf.get(parent));
		hasPart.get(parent).addAll(hasPart.get(child));

		for (SID parentsParent : partOf.get(parent)) {
			hasPart.get(parentsParent).add(child);
			hasPart.get(parentsParent).addAll(hasPart.get(parent));
		}

		for (SID childsChild : hasPart.get(child)) {
			partOf.get(childsChild).add(parent);
			partOf.get(childsChild).addAll(partOf.get(child));
		}
	}

	/**
	 * Sets child to be contained in all elements of parents.
	 */
	public void addCoveredBy(SID child, Set<SID> parents) {

		for (SID parent : parents) {
			addCoveredBy(child, parent);
		}
	}

	private void addBefore(SID u1, SID u2) {
		before.get(u1).add(u2);
		after.get(u2).add(u1);
	}

	private SID addOverlapsWithoutRedundancyCheck(Set<SID> parents) {

		SID child = newOverlapsNode();
		addCoveredBy(child, parents);
		return child;
	}

	/**
	 * If parents does not already share a common predecessor overlaps node, a new node is added
	 * as such a node.
	 * If new node added, we will remove other nodes that now become redundant.
	 */
	private SID addOverlapsWithRedundancyCheck(Set<SID> parents) {

		if (parents.size() < 2 || overlaps(parents)) return null;

		SID newNode = newOverlapsNode();
		// Check if overlaps can be part of larger merge of overlaps
		Set<SID> newParents = findMerge(parents);
		addCoveredBy(newNode, newParents);

		// Lastly, remove the nodes becoming redundant when adding the new.
		removeRedundantWRT(newNode);

		return newNode;
	}

	private Set<SID> findMerge(Set<SID> sids) {

		Set<SID> possible = new HashSet<SID>();
		for (SID p : sids) {
			for (SID ov : hasPart.get(p)) {
				if (isOverlapsNode(ov)) {
					possible.add(ov);
				}
			}
		}

		Set<SID> merged = new HashSet<SID>(sids);
		for (SID ov : possible) {
			if (canBeMerged(partOf.get(ov), sids)) {
				merged = Utils.union(sids, partOf.get(ov));
			}
		}
		return merged;
	}

	private boolean canBeMerged(Set<SID> toCheck, Set<SID> toAdd) {

		Iterator<Set<SID>> subsetIter = Utils.getSubsets(Utils.union(toAdd, toCheck), 2, relations.getHighestArity());
		while (subsetIter.hasNext()) {
			Set<SID> subset = subsetIter.next();
			if (!subset.containsAll(toAdd) && !toCheck.containsAll(subset) && !overlaps(subset)) {
				return false;
			}
		}
		return true;
	}
	
	private void removeRedundantWRT(SID uri) {

		Set<SID> redundant = getRedundantOverlapNodes(partOf.get(uri));
		redundant.remove(uri);
		removeOverlapsNodes(redundant);
	}

	private Pair<SID, Set<SID>> getNodeWithFewestParts(Set<SID> sids) {

		Iterator<SID> iter = sids.iterator();
		SID fewestParts = iter.next();
		int numP = hasPart.get(fewestParts).size();
		while (iter.hasNext()) {
			SID nSid = iter.next();
			int nNumP = hasPart.get(nSid).size();
			if (nNumP < numP) {
				fewestParts = nSid;
				numP = nNumP;
			}
		}
		return new Pair<SID, Set<SID>>(fewestParts, Utils.remove(sids, fewestParts));
	}

	private boolean overlaps(Set<SID> parents) {

		// We check overlaps by trying to find a common pred (ov. node) for parents.
		Pair<SID, Set<SID>> fewestParts = getNodeWithFewestParts(parents);
		// Init commonPreds to contain all overlapsNodes from one parent
		Set<SID> commonPreds = new HashSet<SID>(hasPart.get(fewestParts.fst));
		Iterator<SID> parIter = fewestParts.snd.iterator();
		// We then intersects this set with all preds of rest of parents
		while (parIter.hasNext() && !commonPreds.isEmpty()) {
			commonPreds.retainAll(hasPart.get(parIter.next()));
		}
		return !commonPreds.isEmpty();
	}

	private Set<SID> getRedundantOverlapNodes(Set<SID> parents) {

		Set<SID> toRemove = new HashSet<SID>();
		for (SID parent : parents) {
			for (SID pred : hasPart.get(parent)) {
				if (isOverlapsNode(pred) && parents.containsAll(partOf.get(pred))) {
					toRemove.add(pred);
				}
			}
		}

		return toRemove;
	}

	public void removeOverlapsNode(SID uri) {

		for (SID parent : partOf.get(uri)) {
			hasPart.get(parent).remove(uri);
		}
		hasPart.remove(uri);
		partOf.remove(uri);
		before.remove(uri);
	}

	private void removeOverlapsNodes(Set<SID> uris) {
		for (SID uri : uris) {
			removeOverlapsNode(uri);
		}
	}

	/**
	 * Constructs a relaionship graph based on the relationships between the spaces in spaceNode with
	 * overlaps-arity up to overlapsArity.
	 */
	public static RelationshipGraph makeRelationshipGraph(TreeNode spaceNode, RelationSet relations) {

		SpaceProvider spaces = spaceNode.getSpaceProvider();

		RelationshipGraph graph = new RelationshipGraph(spaceNode.getBlock(), spaces, relations);
		graph.computeRelationshipGraphOpt();

		return graph;
	}

	private void addRelationshipsToGraph(Relationships relationships) {

		// Add PartOfs first, so that redundancy checks are correct for Overlaps
		for (SID[] tuple : relationships.getPartOfs()) {
			addCoveredBy(tuple[0], tuple[1]);
		}
		for (SID[] tuple : relationships.getBefores()) {
			addBefore(tuple[0], tuple[1]);
		}
		for (Set<SID> tuple : relationships.getOverlaps()) {
			addOverlapsWithRedundancyCheck(tuple);
		}
	}

	/**
	 * Constructs the relationship graph between the SIDs by traversing the implication graph between
	 * the relationships topologically. It computes all satisfying tuples in each layer based on possible tuples
	 * from the highest-arity relations from the lower level.
	 */
	private void computeRelationshipGraphOpt() {
		addRelationshipsToGraph(relations.computeRelationships(spaces));
	}

	private boolean sameBefore(SID sid1, Set<SID> sids) {
		for (SID sid2 : sids) {
			if (!before.get(sid1).containsAll(before.get(sid2)) ||
			    !before.get(sid2).containsAll(before.get(sid1)) ||
			    !after.get(sid1).containsAll(after.get(sid2)) ||
			    !after.get(sid2).containsAll(after.get(sid1))) {
				return false;
			}
		}
		return true;
	}

	private boolean beforeAll(SID sid1, Set<SID> sids) {
	
		for (SID sid2 : sids) {
			if (!before.get(sid1).contains(sid2)) {
				return false;
			}
		}
		return true;
	}
	
	private void updateClasses(SID toAdd, List<Set<SID>> equivs, Set<SID> added) {

		Set<SID> toAddCov = Utils.difference(partOf.get(toAdd), added);
		Set<SID> eqClassToAddTo = null;

		for (int i = 0; i < equivs.size(); i++) {
			Set<SID> eqClass = equivs.get(i);
			if (sameBefore(toAdd, eqClass)) {
				eqClassToAddTo = eqClass;
				break;
			} else if (beforeAll(toAdd, eqClass)) {
				eqClassToAddTo = new HashSet<SID>();
				equivs.add(i, eqClassToAddTo); // Add to end of equivs
				break;
			}
		}
		// Not added, so we add it as a new class
		if (eqClassToAddTo == null) {
			eqClassToAddTo = new HashSet<SID>();
			equivs.add(eqClassToAddTo);
		}

		eqClassToAddTo.add(toAdd);
		eqClassToAddTo.addAll(toAddCov);
		added.add(toAdd);
		added.addAll(toAddCov);
	}
	
	private List<Set<SID>> computeBFClasses() {

		List<Set<SID>> equivs = new ArrayList<Set<SID>>();
		Set<SID> added = new HashSet<SID>();

		for (SID toAdd : partOf.keySet()) {
			updateClasses(toAdd, equivs, added);
		}
		return equivs;
	}

	private Set<SID> getCycle(SID sid) {

		Set<SID> cycle = new HashSet<SID>();
		for (SID part : hasPart.get(sid)) {
			if (hasPart.get(part).contains(sid)) {
				cycle.add(part);
			}
		}
		return cycle;
	}

	private Set<Set<SID>> getCycles(Set<SID> sids) {

		Set<Set<SID>> cycles = new HashSet<Set<SID>>();
		for (SID sid : sids) {
			if (partOf.get(sid).contains(sid)) { // Part of cycle, due to transitive closure
				cycles.add(getCycle(sid));
			} else {
				Set<SID> singleton = new HashSet<SID>();
				singleton.add(sid);
				cycles.add(singleton);
			}
		}
		return cycles;
	}

	private Set<SID> getImediateParts(SID node, Set<SID> nodes) {
		Set<SID> parts  = Utils.intersection(hasPart.get(node), nodes);
		Set<SID> partsParts = new HashSet<SID>();
		for (SID part : parts) {
			if (!node.equals(part)) partsParts.addAll(hasPart.get(part));
		}
		return Utils.difference(parts, partsParts);
	}

	private void topSortRec(SID node, List<SID> sorted, Set<SID> added, Set<SID> nodes) {
		sorted.add(node);
		added.add(node);
		for (SID child : getImediateParts(node, nodes)) {
			if (!added.contains(child)) topSortRec(child, sorted, added, nodes);
		}
	}

	private List<SID> topSort(Set<SID> sids) {

		List<SID> sorted = new ArrayList<SID>();
		Set<SID> added = new HashSet<SID>();

		Set<SID> roots = new HashSet<SID>();
		for (SID s : sids) {
			Set<SID> partOfs = Utils.intersection(partOf.get(s), sids);
			if (partOfs.isEmpty())
				roots.add(s);
			else if (partOf.get(s).contains(s))// && getCycle(s).containsAll(Utils.intersection(hasPart.get(s), sids)))
				roots.add(s);
		}

		for (SID root : roots) {
			topSortRec(root, sorted, added, sids);
		}
		return sorted;
	}

	private Map<SID, Set<SID>> toRepresentativeNodes(Set<Set<SID>> cycles) {
		Map<SID, Set<SID>> reps = new HashMap<SID, Set<SID>>();
		for (Set<SID> cycle : cycles) {
			reps.put(Utils.unpackSingleton(cycle), cycle);
		}
		return reps;
	}

	private void getNodesOrder(List<SID> order, Map<SID, Set<SID>> cycles) {
		int i = 0;
		for (Set<SID> bfc : computeBFClasses()) {
			Map<SID, Set<SID>> reps = toRepresentativeNodes(getCycles(bfc));
			cycles.putAll(reps);
			for (SID sid : topSort(reps.keySet())) {
				order.add(i, sid);
				i++;
			}
		}
	}

	private boolean hasSameOverlapsAndParts(SID s1, SID s2) {
		
		Set<SID> s2Parts = new HashSet<SID>(hasPart.get(s2));
		for (SID s1Part : hasPart.get(s1)) {
			if (isOverlapsNode(s1Part)) {
				boolean found = false;
				Set<SID> ovs1 = Utils.remove(Utils.remove(partOf.get(s1Part), s1), s2);
				for (SID s2Part : s2Parts) {
					if (isOverlapsNode(s2Part)) {
						Set<SID> ovs2 = Utils.remove(Utils.remove(partOf.get(s2Part), s2), s1);
						if (ovs1.equals(ovs2)) {
							found = true;
							s2Parts.remove(s2Part);
							break;
						}
					}
				}
				if (!found) {
					return false;
				}
			} else if (!s2Parts.contains(s1Part)) {
				return false;
			} else {
				s2Parts.remove(s1Part);
			}
		}
		return s2Parts.isEmpty();
	}

	private boolean hasSameRelationships(SID s1, SID s2) {

		if (!partOf.get(s1).equals(partOf.get(s2)) ||
		    !before.get(s1).equals(before.get(s2)) ||
		    !after.get(s1).equals(after.get(s2))) {

			return false;
		}
		// Now only need to check if same overlaps and same non-overlaps parts
		return hasSameOverlapsAndParts(s1, s2);
	}

	private void removeRedundantOverlapNodesAfterEquate(SID s) {
		Set<Set<SID>> sOvs = new HashSet<Set<SID>>();
		for (SID ovn : new HashSet<SID>(hasPart.get(s))) {
			if (isOverlapsNode(ovn)) {
				Set<SID> ov = partOf.get(ovn);
				if (sOvs.contains(ov)) {
					removeOverlapsNode(ovn);
				} else {
					sOvs.add(ov);
				}
			}
		}
	}

	private void equateNodes(SID s1, SID s2) {
		addCoveredBy(s1, s2);
		addCoveredBy(s2, s1);
		removeRedundantOverlapNodesAfterEquate(s1);
	}

	private void equateRoleNodes() {
		Set<SID> checked = new HashSet<SID>();
		for (Pair<Integer, Integer> cbe : relations.getCanBeEquated()) {
			for (SID sid : new HashSet<SID>(partOf.keySet())) {
				if (checked.contains(sid)) continue;

				if (sid.getRole() == cbe.fst) {
					SID sndRS = new SID(sid.getID(), cbe.snd);
					if (partOf.containsKey(sndRS) && hasSameRelationships(sid, sndRS)) {
						equateNodes(sid, sndRS);
						checked.add(sndRS);
					}
				} else if (sid.getRole() == cbe.snd) {
					SID fstRS = new SID(sid.getID(), cbe.fst);
					if (partOf.containsKey(fstRS) && hasSameRelationships(sid, fstRS)) {
						equateNodes(sid, fstRS);
						checked.add(fstRS);
					}
				}
			}
		}
	}

	private void distributeUniqueParts(List<SID> order, Block[] witnessesArr, Map<SID, Bintree> localRep,
	                                   Map<SID, Block> wit, Map<SID, Set<SID>> cycles) {
		// Distribute unique parts
		int k = 0;
		for (int i = 0; i < order.size(); i++) {
			SID s = order.get(i);

			if (cycles.get(s).size() == 1 && hasPart.get(s).size() > 0 &&
			    !relations.needsUniquePart(s.getRole())) {
				continue;
			}

			Block bt = block.append(witnessesArr[k++]);
			Bintree bintree = Bintree.fromBlock(bt);
			localRep.put(s, bintree);

			for (SID ss : cycles.get(s)) {
				if (!isOverlapsNode(ss)) {
					wit.put(ss, bt);
				}
			}
		}
	}

	private void propagateParts(Map<SID, Bintree> localRep) {
		// Propagate node's representations according to containments
		for (SID uri : hasPart.keySet()) {
			Bintree nodeBT = localRep.getOrDefault(uri, new Bintree());

			Set<SID> cotro = new HashSet<SID>(hasPart.get(uri)); // Children Of This Role-part Only, not stricter roles
			for (Integer role : relations.getRoles()) {
				if (role != uri.getRole() && Relation.stricterRole(role, uri.getRole())) {
					SID c = new SID(uri.getID(), role);
					if (!hasPart.containsKey(c)) continue;
					cotro.removeAll(hasPart.get(c));
					cotro.remove(c);
				}
			}

			for (SID pred : cotro) {
				if (localRep.containsKey(pred)) nodeBT = nodeBT.union(localRep.get(pred));
			}
			localRep.put(uri, nodeBT);
		}
	}

	private void finalizeRepresenataion(Map<SID, Bintree> localRep, Map<SID, Block> wit, Map<Integer, Bintree> finalRep) {

		// Set unique part-blocks and add to final representation
		for (SID uri : localRep.keySet()) {
			if (!isOverlapsNode(uri)) {
				Set<Block> bs = localRep.get(uri).normalize().getBlocks();
				Set<Block> cbs = new HashSet<Block>();
				Block w = wit.get(uri);
				for (Block b : bs) {
					if (w != null && w.blockPartOf(b)) {
						cbs.add(b.setUniquePart(true).addRole(uri.getRole()));
					} else {
						cbs.add(b.addRole(uri.getRole()));
					}
				}
				if (!finalRep.containsKey(uri.getID())) {
					finalRep.put(uri.getID(), new Bintree(cbs));
				} else {
					finalRep.put(uri.getID(), finalRep.get(uri.getID()).union(new Bintree(cbs)));
				}
				finalRep.put(uri.getID(), finalRep.get(uri.getID()).normalize());
			}
		}
	}

	public Representation constructRepresentation() {

		//equateRoleNodes();

		// Construct sufficient unique parts and order nodes according to infix traversal
		Block[] witnessesArr = Block.makeNDistinct(partOf.keySet().size()+1);
		List<SID> order = new ArrayList<SID>();
		Map<SID, Set<SID>> cycles = new HashMap<SID, Set<SID>>();
		getNodesOrder(order, cycles);

		Map<SID, Bintree> localRep = new HashMap<SID, Bintree>();
		Map<SID, Block> wit = new HashMap<SID, Block>();

		distributeUniqueParts(order, witnessesArr, localRep, wit, cycles);
		propagateParts(localRep);

		Map<Integer, Bintree> finalRep = new HashMap<Integer, Bintree>();
		finalizeRepresenataion(localRep, wit, finalRep);

		return new Representation(finalRep);
	}
}
