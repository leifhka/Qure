package no.uio.ifi.qure;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Arrays;

public class RelationshipGraph {

    private final Map<SID, Node> nodes;
    private final Set<SID> topmostNodes;
    private int overlapsNodeId; // Always negative, and decreasing
    private final Block block;
    private final Set<SID> uris;
    private final int overlapsArity;

    public RelationshipGraph(Block block, Set<SID> uris, int overlapsArity) {
        this.block = block;
        this.uris = new HashSet<SID>(uris);
        this.overlapsArity = overlapsArity;

        topmostNodes = new HashSet<SID>(uris); // Init all uris as roots, and remove if set parent of some node
        nodes = new HashMap<SID, Node>();

        for (SID uri : uris)
            nodes.put(uri, new Node(uri));

        overlapsNodeId = 0;
    }

    public void addUris(Set<SID> newUris) {
        for (SID uri : newUris) {
            if (!nodes.containsKey(uri)) {
                uris.add(uri);
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

    public Set<SID> getUris() { return uris; }

    public Map<SID, Node> getNodes() { return nodes; }

    public int size() { return getUris().size(); }

    /**
     * Adds a containment-relationship between child and parent if necessary (not already in graph).
     */ 
    public void addCoveredBy(SID child, SID parent) {

        Node cn = nodes.get(child);
        Node pn = nodes.get(parent);

        if (cn.succs.contains(parent))
            return; // Relationship already in node 
        
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

        for (SID parent : parents)
            addCoveredBy(child, parent);
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

        if (parents.isEmpty() || overlaps(parents)) return null;

        // We then add the new overlaps.
        SID newNode = newOverlapsNode();
        addCoveredBy(newNode, parents);

        // Overlaps relationship not already contained. 
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

        for (SID p : parents) {
            if (!uris.contains(p) || nodes.get(p).preds.isEmpty())
                return false;
        }

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
                if (isOverlapsNode(pred) && parents.containsAll(ppn.succs))
                    toRemove.add(pred);
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
        for (SID uri : uris)
            removeOverlapsNode(uri);
    }

    private void addOverlaps(SID ui, SID uj, Space s, Set<Intersection> intersections,
                             Map<SID, Set<SID>> intMap) {
        Set<SID> elems = new HashSet<SID>(2);
        elems.add(ui);
        elems.add(uj);
        Intersection nin = new Intersection(s, elems);
        intersections.add(nin);
        intMap.get(ui).add(uj);
        intMap.get(uj).add(ui);
        //addOverlapsWithoutRedundancyCheck(elems);
    }

    /**
     * Constructs a relaionship graph based on the relationships between the spaces in spaceNode with
     * overlaps-arity up to overlapsArity.
     */
    public static RelationshipGraph makeRelationshipGraph(TreeNode spaceNode, int overlapsArity) {

        SpaceProvider spaces = spaceNode.getSpaceProvider();
        Set<SID> uris = spaceNode.getOverlappingURIs();

        Map<SID, Set<SID>> intMap = new HashMap<SID, Set<SID>>();
        for (SID uri : uris) intMap.put(uri, new HashSet<SID>());
        RelationshipGraph graph = new RelationshipGraph(spaceNode.getBlock(), uris, overlapsArity);

        SID[] urisArr = uris.toArray(new SID[uris.size()]);
        Set<Intersection> intersections = new HashSet<Intersection>();

        graph.computeBinaryRelations(urisArr, spaces, intersections, intMap);
        graph.computeKIntersections(intersections, uris, spaces, intMap);

        return graph;
    }

    // TODO: Let method take extra argument, a set of positive base relations extracted from
    // the relation definitions, and compute these relationships for the spaces, and update the graph
    // accordingly.
    private void computeBinaryRelations(SID[] urisArr, SpaceProvider spaces, 
                                        Set<Intersection> intersections, Map<SID, Set<SID>> intMap) {

        for (int i = 0; i < urisArr.length; i++) { 

            SID ui = urisArr[i];
            Space si = spaces.get(ui);

            for (int j = i+1; j < urisArr.length; j++) {

                SID uj = urisArr[j];
                Space sj = spaces.get(uj);

                Relationship rel = si.relate(sj);

                if (rel.isIntersects()) {

                    if (rel.isCovers())
                        addCoveredBy(uj, ui);

                    if (rel.isCoveredBy())
                        addCoveredBy(ui, uj);
                    
                    if (!rel.isCovers() && !rel.isCoveredBy()) { // Overlaps already represented by containment
                        Space s = si.intersection(sj);
                        addOverlaps(ui, uj, s, intersections, intMap);
                    }
                } else if (rel.isBefore()) {
                    addBefore(ui,uj);
                }
            }
        }
    }

    /**
     * Computes the set of maximal overlaps up to arity k, such that the set contains no redundant overlaps.
     * @return A set containing one set per maximal overlap, that is, the intersection of the spaces
     *         for each of the elements in each set is nonempty, but adding any new element to the set
     *         will give an empty intersection.
     */
    public void computeKIntersections(Set<Intersection> intersections, Set<SID> elems,
                                      SpaceProvider spaces, Map<SID, Set<SID>> intMap) {

        Set<Intersection> ints = new HashSet<Intersection>(intersections);
        Set<Set<SID>> added = new HashSet<Set<SID>>();

        for (int i = 3; i <= overlapsArity; i++) {

            Set<Intersection> iterSet = new HashSet<Intersection>(ints);
            ints = new HashSet<Intersection>(); // Stores next iterations intersections

            for (Intersection in : iterSet) {

                boolean updated = false; // States whether the <in> has become part of a larger intersection

                // Need only check the elements already intersection all elements in nin
                // so we compute the intersection of all overlapping elements
                Set<SID> possible = new HashSet<SID>();
                boolean first = true;
                for (SID ine : in.getElements()) {
                    if (first) {
                        possible.addAll(intMap.get(ine));
                        first = false;
                    } else {
                        possible.retainAll(intMap.get(ine));
                    }
                }
                possible.removeAll(in.getElements());

                for (SID e : possible) {

                    if (ints.contains(new Intersection(null, Utils.add(in.getElements(), e)))) {
                        updated = true;
                        continue;
                    }

                    Intersection nin = null;
                    nin = in.add(e, spaces);

                    if (nin != null) { // Intersection was successfull (i.e. non-empty)
                        updated = true;
                        ints.add(nin); 
                        addOverlapsWithRedundancyCheck(nin.getElements());
                    }
                }
                if (!updated && !added.contains(in.getElements())) {
                    addOverlapsWithRedundancyCheck(in.getElements());
                    added.add(in.getElements());
                }
            }
        }

        for (Intersection in : ints) // Need to add remaining intersections
            addOverlapsWithRedundancyCheck(in.getElements());
    }

    private Set<SID> imidiatePreds(Node n) {

        Set<SID> res = new HashSet<SID>(n.preds);

        for (SID pred : n.preds) 
            res.removeAll(nodes.get(pred).preds);

        return res;
    }

    private int orderNodes(SID[] order, int i, SID uri) {

        Node n = nodes.get(uri);
        if (n.visited) return i;
        else n.visited = true;

        for (SID preds : sortAccToBefore(imidiatePreds(n)))
            i = orderNodes(order, i, preds);

        order[i++] = uri;
        return i;
    }

    private SID[] sortAccToBefore(Set<SID> uris) {

        SID[] order = new SID[uris.size()];
        Set<SID> visited = new HashSet<SID>();
        int i = order.length - 1;

        for (SID u : uris) {

            if (visited.contains(u)) continue;
            Node n = nodes.get(u);

            for (SID aftr : n.before) {
                if (visited.contains(aftr) || !uris.contains(aftr)) continue;
                order[i--] = aftr;
                visited.add(aftr);
            }
            order[i--] = u;
            visited.add(u);
        }

        return order;
    }

    private SID[] getNodesOrder() {

        SID[] order = new SID[nodes.keySet().size()];
        int k = 0;
        for (SID tm : sortAccToBefore(topmostNodes))
            k = orderNodes(order, k, tm);

        if (k == order.length) return order;

        // We have (topmost) cycles, which are not yet visited
        for (SID uri : nodes.keySet()) {
            Node n = nodes.get(uri);
            if (n.visited) 
                continue;
            else 
                k = orderNodes(order, k, uri);
        }

        return order;
    }

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
            if (!isOverlapsNode(order[i])) wit.put(order[i], bt);
        }

        // Propagate nodes representations according to containments
        for (SID uri : nodes.keySet()) {
            Node n = nodes.get(uri);
            Bintree nodeBT = localRep.get(uri);

            for (SID pred : n.preds)
                nodeBT = nodeBT.union(localRep.get(pred));
            localRep.put(uri, nodeBT);
        }

        // Set unique part-blocks and add to final representation
		// TODO: Add roles properly to bintree rep.
        Map<Integer, Bintree> urisRep = new HashMap<Integer, Bintree>();
        for (SID uri : localRep.keySet()) {
            if (!isOverlapsNode(uri)) {
                Set<Block> bs = localRep.get(uri).normalize().getBlocks();
                Block w = wit.get(uri);
                Set<Block> cbs = new HashSet<Block>();
                for (Block b : bs) {
                    if (w.blockPartOf(b)) 
                        cbs.add(b.setUniquePart(true));
                    else
                        cbs.add(b);
                }
                urisRep.put(uri.getID(), new Bintree(cbs));
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
