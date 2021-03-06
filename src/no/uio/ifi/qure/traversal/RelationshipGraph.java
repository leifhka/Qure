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
	private final Set<SID> topmostNodes;
	private int overlapsNodeId; // Always negative, and decreasing
	private final Block block;
	private final RelationSet relations;

	public RelationshipGraph(Block block, Set<SID> uris, RelationSet relations) {
		this.block = block;
		this.relations = relations;

		topmostNodes = new HashSet<SID>(uris); // Init all uris as roots, and remove if set parent of some node
		partOf = new HashMap<SID, Set<SID>>();
		hasPart = new HashMap<SID, Set<SID>>();
		before = new HashMap<SID, Set<SID>>();

		for (SID uri : uris) {
			addUri(uri);
		}
		for (SID uri : uris) {
			for (Integer role : Relation.getStricter(relations.getRoles(), uri.getRole())) {
				SID part = new SID(uri.getID(), role);
				if (uris.contains(part)) {
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

		Set<SID> merged = new HashSet<SID>(sids);
		for (SID p : sids) {
			for (SID ov : hasPart.get(p)) {
				if (isOverlapsNode(ov) && canBeMerged(Utils.union(sids, partOf.get(ov)), sids)) {
					merged = Utils.union(sids, partOf.get(ov));
				}
			}
		}
		return merged;
	}

	private boolean canBeMerged(Set<SID> toCheck, Set<SID> toAdd) {

		for (Set<SID> subset : Utils.getSubsets(toCheck, 2, relations.getHighestArity())) {
			if (subset.containsAll(toAdd)) continue; // toAdd not yet overlapping, but is going to be added
			for (AtomicRelation rel : relations.getAtomicRelations()) {
				if (rel instanceof Overlaps && ((Overlaps) rel).compatible(subset) && !overlaps(subset)) {
					return false;
				}
			}
		}
		return true;
	}
	
	private void removeRedundantWRT(SID uri) {

		Set<SID> redundant = getRedundantOverlapNodes(partOf.get(uri));
		redundant.remove(uri);
		removeOverlapsNodes(redundant);
	}

	private boolean overlaps(Set<SID> parents) {

		// We check redundancy by trying to find a common pred (ov. node) for parents.
		Iterator<SID> parIter = parents.iterator();
		SID par = parIter.next();
		// Init commonPreds to contain all overlapsNodes from one parent
		Set<SID> commonPreds = new HashSet<SID>(hasPart.get(par));

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
		Set<SID> uris = spaceNode.getOverlappingURIs();

		Map<SID, Set<SID>> intMap = new HashMap<SID, Set<SID>>();
		for (SID uri : uris) {
			intMap.put(uri, new HashSet<SID>());
		}
		RelationshipGraph graph = new RelationshipGraph(spaceNode.getBlock(), uris, relations);
		graph.computeRelationshipGraphOpt(spaces);

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
	private void computeRelationshipGraphOpt(SpaceProvider spaces) {
		addRelationshipsToGraph(relations.computeRelationships(spaces));
	}

	private boolean sameBefore(SID sid1, Set<SID> sids) {
		for (SID sid2 : sids) {
			if (!before.get(sid1).equals(before.get(sid2))) {
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
	
	private void updateClasses(SID toAdd, List<Set<SID>> equivs) {
		
		for (int i = 0; i < equivs.size(); i++) {
			Set<SID> eqClass = equivs.get(i);
			if (sameBefore(toAdd, eqClass)) {
				eqClass.add(toAdd);
				return;
			} else if (beforeAll(toAdd, eqClass)) {
				Set<SID> newClass = new HashSet<SID>();
				newClass.add(toAdd);
				equivs.add(i, newClass); // Add to end of equivs
				return;
			}
		}
		// Not added, so we add it as a new class
		Set<SID> newClass = new HashSet<SID>();
		newClass.add(toAdd);
		equivs.add(newClass); // Add to end of equivs
	}
	
	private List<Set<SID>> computeBFClasses() {

		List<Set<SID>> equivs = new ArrayList<Set<SID>>();

		for (SID toAdd : partOf.keySet()) {
			updateClasses(toAdd, equivs);
		}
		return equivs;
	}

	// TODO: Make more optimal ordering based on partOf-relationships
	private SID[] getNodesOrder() {
		SID[] order = new SID[partOf.keySet().size()];

		int i = 0;
		for (Set<SID> bfc : computeBFClasses()) {
			for (SID sid : bfc) {
				order[i] = sid;
				i++;
			}
		}
		return order;
	}

	// TODO: Long method, split into smaller!
	public Representation constructRepresentation() { 

		// Construct sufficient unique parts and order nodes according to infix traversal
		Block[] witnessesArr = Block.makeNDistinct(partOf.keySet().size()+1);
		SID[] order = getNodesOrder();

		Map<SID, Bintree> localRep = new HashMap<SID, Bintree>();
		Map<SID, Block> wit = new HashMap<SID, Block>();

		// Distribute unique parts
		for (int i = 0; i < order.length; i++) {

			Block bt = block.append(witnessesArr[i]);
			localRep.put(order[i], Bintree.fromBlock(bt));
	
			if (!isOverlapsNode(order[i])) {
				wit.put(order[i], bt);
			}
		}

		// Propagate node's representations according to containments
		for (SID uri : hasPart.keySet()) {
			Bintree nodeBT = localRep.get(uri);

			for (SID pred : hasPart.get(uri)) {
				nodeBT = nodeBT.union(localRep.get(pred));
			}
			localRep.put(uri, nodeBT);
		}

		// Set unique part-blocks and add to final representation
		Map<Integer, Bintree> urisRep = new HashMap<Integer, Bintree>();
		for (SID uri : localRep.keySet()) {
			if (!isOverlapsNode(uri)) {
				Set<Block> bs = localRep.get(uri).normalize().getBlocks();
				Block w = wit.get(uri);
				Set<Block> cbs = new HashSet<Block>();
				for (Block b : bs) {
					if (w.blockPartOf(b)) {
						cbs.add(b.setUniquePart(true).addRole(uri.getRole()));
					} else {
						cbs.add(b.addRole(uri.getRole()));
					}
				}
				if (!urisRep.containsKey(uri.getID())) {
					urisRep.put(uri.getID(), new Bintree(cbs));
				} else {
					urisRep.put(uri.getID(), urisRep.get(uri.getID()).union(new Bintree(cbs)));
				}
			}
		}
		return new Representation(urisRep);
	}
}
