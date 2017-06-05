package no.uio.ifi.qure.traversal;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Arrays;

import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.space.*;
import no.uio.ifi.qure.bintree.*;
import no.uio.ifi.qure.relation.*;

public class RelationshipGraph {

	private final Map<SID, Node> nodes;
	private final Set<SID> topmostNodes;
	private int overlapsNodeId; // Always negative, and decreasing
	private final Block block;
	private final RelationSet relations;

	public RelationshipGraph(Block block, Set<SID> uris, RelationSet relations) {
		this.block = block;
		this.relations = relations;

		topmostNodes = new HashSet<SID>(uris); // Init all uris as roots, and remove if set parent of some node
		nodes = new HashMap<SID, Node>();

		for (SID uri : uris) {
			nodes.put(uri, new Node(uri));
		}
		overlapsNodeId = 0;
	}

	public void addUris(Set<SID> newUris) {
		for (SID uri : newUris) {
			if (!nodes.containsKey(uri)) {
				topmostNodes.add(uri);
				nodes.put(uri, new Node(uri));
			}
		}
	}

	private SID newOverlapsNode() {
		overlapsNodeId--;
		SID ovSID = new SID(overlapsNodeId);
		nodes.put(ovSID, new Node(ovSID));
		return ovSID;
	}

	public boolean isOverlapsNode(SID nodeSID) { return nodeSID.getID() < 0; }

	public Map<SID, Node> getNodes() { return nodes; }

	/**
	 * Adds a containment-relationship between child and parent if necessary (not already in graph).
	 */ 
	public void addCoveredBy(SID child, SID parent) {

		Node cn = nodes.get(child);
		Node pn = nodes.get(parent);

		if (cn.succs.contains(parent)) {
			return; // Relationship already in node 
		}	

		Set<SID> both = new HashSet<SID>(2);
		both.add(child);
		both.add(parent);
		removeOverlapsNodes(getRedundantOverlapNodes(both));
		
		// Locally update coveredBy
		cn.succs.add(parent);
		topmostNodes.remove(child);

		// Locally update covers
		pn.preds.add(child);

		// Transitive closure
		cn.succs.addAll(pn.succs);
		pn.preds.addAll(cn.preds);

		for (SID parentsParent : pn.succs) {
			Node ppn = nodes.get(parentsParent);
			ppn.preds.add(child);
			ppn.preds.addAll(pn.preds);
		}

		for (SID childsChild : cn.preds) {
			Node ccn = nodes.get(childsChild);
			ccn.succs.add(parent);
			ccn.succs.addAll(cn.succs);
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
		Node n1 = nodes.get(u1);
		n1.before.add(u2);
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

		// Overlaps relationship not already contained. 
		// We then add the new overlaps.
		SID newNode = newOverlapsNode();
		addCoveredBy(newNode, parents);

		// Lastly, remove the nodes becoming redundant when adding the new.
		removeRedundantWRT(newNode);

		return newNode;
	}

	private void removeRedundantWRT(SID uri) {

		Node n = nodes.get(uri);
		Set<SID> redundant = getRedundantOverlapNodes(n.succs);
		redundant.remove(uri);
		removeOverlapsNodes(redundant);
	}

	private boolean overlaps(Set<SID> parents) {

		// We check redundancy by trying to find a common pred (ov. node) for parents.
		Iterator<SID> parIter = parents.iterator();
		Node par = nodes.get(parIter.next());
		// Init commonPreds to contain all overlapsNodes from one parent
		Set<SID> commonPreds = new HashSet<SID>(par.preds);

		// We then intersects this set with all preds of rest of parents
		while (parIter.hasNext() && !commonPreds.isEmpty()) {
			par = nodes.get(parIter.next());
			commonPreds.retainAll(par.preds);
		}

		return !commonPreds.isEmpty();
	}

	private Set<SID> getRedundantOverlapNodes(Set<SID> parents) {

		Set<SID> toRemove = new HashSet<SID>();

		for (SID parent : parents) {

			Node pn = nodes.get(parent);
			
			for (SID pred : pn.preds) {
				Node ppn = nodes.get(pred);
				if (isOverlapsNode(pred) && parents.containsAll(ppn.succs)) {
					toRemove.add(pred);
				}
			}
		}

		return toRemove;
	}

	public void removeOverlapsNode(SID uri) {

		Node n = nodes.get(uri);
		
		for (SID parent : n.succs) {
			Node pn = nodes.get(parent);
			pn.preds.remove(uri);
		}
			
		nodes.remove(uri);
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

		// SID[] urisArr = uris.toArray(new SID[uris.size()]);
		// Set<Intersection> intersections = new HashSet<Intersection>();
		// graph.computeBinaryRelations(urisArr, spaces, intersections, intMap);
		// graph.computeKIntersections(intersections, uris, spaces, intMap);

		graph.computeRelationshipGraphOpt(spaces);

		return graph;
	}

	private void addRelationshipToGraph(AtomicRelation rel, Integer[] tuple) {

		SID[] sids = rel.toSIDs(tuple);
		
		if (rel instanceof Overlaps) {
			addOverlapsWithRedundancyCheck(Utils.asSet(sids));
		} else if (rel instanceof PartOf) {
			addCoveredBy(sids[0], sids[1]);
		} else {
			addBefore(sids[0], sids[1]);
		}
	}
		

	private void addRelationshipsToGraph(Map<AtomicRelation, Set<Integer[]>> tuples) {

		// Add PartOfs first, so that redundancy checks are correct for Overlaps
		for (AtomicRelation rel : tuples.keySet()) {
			if (rel instanceof PartOf) {
				for (Integer[] tuple : tuples.get(rel)) {
					addRelationshipToGraph(rel, tuple);
				}
			}
		}
		
		for (int i = RelationSet.getHighestArity(tuples.keySet()); i > 0; i--) {
			for (AtomicRelation rel : RelationSet.getRelationsWithArity(i, tuples.keySet())) {
				if (!(rel instanceof PartOf) && rel.getArity() > 1) {
					for (Integer[] tuple : tuples.get(rel)) {
						addRelationshipToGraph(rel, tuple);
					}
				}
			}
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
		Node n1 = nodes.get(sid1);
		for (SID sid2 : sids) {
			Node n2 = nodes.get(sid2);
			if (!n1.before.equals(n2.before)) {
				return false;
			}
		}
		return true;
	}

	private boolean beforeAll(SID sid1, Set<SID> sids) {
		
		Node n1 = nodes.get(sid1);
		for (SID sid2 : sids) {
			if (!n1.before.contains(sid2)) {
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

		for (SID toAdd : nodes.keySet()) {
			updateClasses(toAdd, equivs);
		}
		return equivs;
	}

	// TODO: Make more optimal ordering based on partOf-relationships
	private SID[] getNodesOrder() {
		SID[] order = new SID[nodes.keySet().size()];

		int i = 0;
		for (Set<SID> bfc : computeBFClasses()) {
			for (SID sid : bfc) {
				order[i] = sid;
				i++;
			}
		}
		return order;
	}

//	private Set<SID> imidiatePreds(Node n) {
//
//		Set<SID> res = new HashSet<SID>(n.preds);
//
//		for (SID pred : n.preds) {
//			res.removeAll(nodes.get(pred).preds);
//		}
//		return res;
//	}
//
//	private int orderNodes(SID[] order, int i, SID uri) {
//
//		Node n = nodes.get(uri);
//		if (n.visited) {
//			return i;
//		} else {
//			n.visited = true;
//		}
//		for (SID preds : imidiatePreds(n)) {
//			i = orderNodes(order, i, preds);
//		}
//		order[i++] = uri;
//		return i;
//	}
//
//	private SID[] getNodesOrder() {
//
//		SID[] order = new SID[nodes.keySet().size()];
//		int k = 0;
//		for (SID tm : sortAccToBefore(topmostNodes)) {
//			k = orderNodes(order, k, tm);
//		}
//
//		if (k == order.length) {
//			return order;
//		}
//
//		// We have (topmost) cycles, which are not yet visited
//		for (SID uri : nodes.keySet()) {
//			Node n = nodes.get(uri);
//			if (n.visited) {
//				continue;
//			} else {
//				k = orderNodes(order, k, uri);
//			}
//		}
//		return order;
//	}
//

	// TODO: Long method, split into smaller!
	public Representation constructRepresentation() { 

		// Construct sufficient unique parts and order nodes according to infix traversal
		Block[] witnessesArr = Block.makeNDistinct(nodes.keySet().size()+1);
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
		for (SID uri : nodes.keySet()) {
			Node n = nodes.get(uri);
			Bintree nodeBT = localRep.get(uri);

			for (SID pred : n.preds) {
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

	class Node {

		SID uri;
		boolean visited; // Used for post-fix ordering of nodes
		Set<SID> preds;
		Set<SID> succs;
		Set<SID> before;

		public Node(SID uri) {
			this.uri = uri;
			visited = false;
			preds = new HashSet<SID>();
			succs = new HashSet<SID>();
			before = new HashSet<SID>();
		}
	}
}
