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

    private final Map<Integer, Node> nodes;
    private final Set<Integer> roots;
    private int overlapsNodeId; // Always negative, and decreasing
    private final Block block;
    private final Set<Integer> uris;
    private final int k;

    public RelationshipGraph(Block block, Set<Integer> uris, int k) {
        this.block = block;
        this.uris = new HashSet<Integer>(uris);
        this.k = k;

        roots = new HashSet<Integer>(uris); // Init all uris as roots, and remove if set parent of some node
        nodes = new HashMap<Integer, Node>();

        for (Integer uri : uris)
            nodes.put(uri, new Node(uri));

        overlapsNodeId = 0;
    }

    public void addUris(Set<Integer> newUris) {
        for (Integer uri : newUris) {
            if (!nodes.containsKey(uri)) {
                uris.add(uri);
                roots.add(uri);
                nodes.put(uri, new Node(uri));
            }
        }
    }

    private int newOverlapsNode() {
        overlapsNodeId--;
        nodes.put(overlapsNodeId, new Node(overlapsNodeId));
        roots.add(overlapsNodeId);
        return overlapsNodeId;
    }

    public boolean isOverlapsNode(int node) { return node < 0; }

    public Set<Integer> getUris() { return uris; }

    public Set<Integer> getRoots() { return roots; }

    public Map<Integer, Node> getNodes() { return nodes; }

    public int size() { return getUris().size(); }

    /**
     * Adds a containment-relationship between child and parent if necessary (not already in graph).
     */ 
    public void addCoveredBy(Integer child, Integer parent) {

        Node cn = nodes.get(child);
        Node pn = nodes.get(parent);

        if (cn.succs.contains(parent))
            return; // Relationship already in node 
        
        // Locally update coveredBy
        cn.succs.add(parent);

        // Locally update covers
        roots.remove(parent);
        pn.preds.add(child);

        // Transitive closure
        cn.succs.addAll(pn.succs);
        pn.preds.addAll(cn.preds);

        for (Integer parentsParent : pn.succs) {
            Node ppn = nodes.get(parentsParent);
            ppn.preds.add(child);
            ppn.preds.addAll(pn.preds);
        }

        for (Integer childsChild : cn.preds) {
            Node ccn = nodes.get(childsChild);
            ccn.succs.add(parent);
            ccn.succs.addAll(cn.succs);
        }
    }

    /**
     * Sets child to be contained in all elements of parents.
     */
    public void addCoveredBy(Integer child, Set<Integer> parents) {

        for (Integer parent : parents)
            addCoveredBy(child, parent);
    }

    private Integer addOverlapsWithoutRedundancyCheck(Set<Integer> parents) {

        Integer child = newOverlapsNode();
        addCoveredBy(child, parents);
        return child;
    }

    /**
     * If parents does not already share a common predecessor overlaps node, a new node is added
     * as such a node.
     * If new node added, we will remove other nodes that now become redundant.
     */
    private Integer addOverlapsWithRedundancyCheck(Set<Integer> parents) {

        if (parents.isEmpty() || overlapsByOverlapsNode(parents)) return null;

        // We then add the new overlaps.
        Integer newNode = newOverlapsNode();
        addCoveredBy(newNode, parents);

        // Overlaps relationship not already contained. 
        // Lastly, remove the nodes becoming redundant when adding the new.
        removeRedundantWRT(newNode);

        return newNode;
    }

    private void removeRedundantWRT(Integer uri) {

        Node n = nodes.get(uri);
        Set<Integer> redundant = getRedundantOverlapNodes(n.succs);
        redundant.remove(uri);
        removeOverlapsNodes(redundant);
    }

    private boolean overlapsByOverlapsNode(Set<Integer> parents) {

        for (Integer p : parents) {
            if (!uris.contains(p) || nodes.get(p).preds.isEmpty())
                return false;
        }

        // We check redundancy by trying to find a common pred (ov. node) for parents.
        Iterator<Integer> parIter = parents.iterator();
        Node par = nodes.get(parIter.next());
        Set<Integer> commonPreds = new HashSet<Integer>();

        // Init commonPreds to contain all overlapsNodes from one parent
        for (Integer pred : par.preds) {
            if (isOverlapsNode(pred))
                commonPreds.add(pred);
        }

        // We then intersects this set with all preds of rest of parents
        while (parIter.hasNext() && !commonPreds.isEmpty()) {
            par = nodes.get(parIter.next());
            commonPreds.retainAll(par.preds);
        }

        return !commonPreds.isEmpty();
    }

    private boolean overlapsByURINode(Set<Integer> parents) {

        for (Integer p : parents) {
            if (!uris.contains(p) || nodes.get(p).preds.isEmpty())
                return false;
        }

        // We check redundancy by trying to find a common pred (ov. node) for parents.
        Iterator<Integer> parIter = parents.iterator();
        Node par = nodes.get(parIter.next());
        Set<Integer> commonPreds = new HashSet<Integer>();
        commonPreds.add(par.uri);

        // Init commonPreds to contain all overlapsNodes from one parent
        for (Integer pred : par.preds) {
            if (!isOverlapsNode(pred))
                commonPreds.add(pred);
        }

        // We then intersects this set with all preds of rest of parents
        while (parIter.hasNext() && !commonPreds.isEmpty()) {
            par = nodes.get(parIter.next());
            Set<Integer> predsNSelf = new HashSet<Integer>(par.preds);
            predsNSelf.add(par.uri);
            commonPreds.retainAll(predsNSelf);
        }

        return !commonPreds.isEmpty();
    }

    private Set<Integer> getRedundantOverlapNodes(Set<Integer> parents) {

        Set<Integer> toRemove = new HashSet<Integer>();

        for (Integer parent : parents) {

            Node pn = nodes.get(parent);
            
            for (Integer pred : pn.preds) {
                Node ppn = nodes.get(pred);
                if (isOverlapsNode(pred) && parents.containsAll(ppn.succs))
                    toRemove.add(pred);
            }
        }

        return toRemove;
    }

    public void removeOverlapsNode(Integer uri) {

        Node n = nodes.get(uri);
        
        for (Integer parent : n.succs) {

            Node pn = nodes.get(parent);
            pn.preds.remove(uri);

            if (pn.preds.isEmpty() && !uri.equals(parent)) {
                roots.add(parent);
            }
        }
            
        nodes.remove(uri);
        roots.remove(uri);
    }

    private void removeOverlapsNodes(Set<Integer> uris) {
        for (Integer uri : uris)
            removeOverlapsNode(uri);
    }

    private Integer popSomeRoot() {
        Integer root = roots.iterator().next();
        roots.remove(root);
        return root;
    }     

    private Integer popNextNode() {

        if (!getRoots().isEmpty()) return popSomeRoot();

        for (Integer uri : nodes.keySet()) {
            Node n = nodes.get(uri);
            if (n.succs.containsAll(n.preds)) // Only loop edges!
                return uri;
        }
    
        System.err.println("Error: No new node popped!");
        System.exit(1);
        return null;
    }

    /**
     * Only for debugging purposes: Checks if the graph contains any redundant nodes.
     */
    private boolean containsRedundancy() {
        for (Integer uri : roots) {
            if (isOverlapsNode(uri)) {
                Set<Integer> red = getRedundantOverlapNodes(nodes.get(uri).succs);
                if (red.size() > 1) {
                    System.out.println("Redundant: " + nodes.get(uri).succs.toString());
                    for (Integer r : red)
                        System.out.println(nodes.get(r).succs.toString());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a graph representing the merge of this and argument.
     * An overlap relationship holds in the merged graph if it holds
     * in either this or o, whereas a containment relationship contains(A,B)
     * holds in the merged graph if it holds in the all the graphs which A is a part of.
     */
    public RelationshipGraph merge(RelationshipGraph o) {

        // Start by making a new node, containing the uris from both o and this
        Set<Integer> urisUnion = new HashSet<Integer>(uris);
        urisUnion.addAll(o.getUris());

        RelationshipGraph newNode = new RelationshipGraph(block.getParent(), urisUnion, k);
        Map<Integer, Node> oNodes = new HashMap<Integer, Node>(o.getNodes());

        Set<Set<Integer>> overlapsToAdd = new HashSet<Set<Integer>>();

        // Then add all relationships from this
        for (Integer uri : nodes.keySet()) {

            Node n = nodes.get(uri);
            Set<Integer> pUris = new HashSet<Integer>(n.succs);
            
            if (!isOverlapsNode(uri) && o.getUris().contains(uri)) {

                Node on = oNodes.get(uri);
                Set<Integer> opUris = new HashSet<Integer>(on.succs);
                pUris.retainAll(opUris); // contains(uri, pUri) holds in newNode iff it holds in both o and this
                newNode.addCoveredBy(uri, pUris);

                if (pUris.size() != opUris.size() || pUris.size() != on.succs.size()) {
                    // Since we have removed some overlaps, we need to add an
                    // overlaps-relationship between uri and the removed succs.
                    Set<Integer> removedSuccs = new HashSet<Integer>(n.succs);
                    removedSuccs.removeAll(pUris);
                    Set<Integer> removedO = new HashSet<Integer>(on.succs);
                    removedO.removeAll(pUris);
                    removedSuccs.addAll(removedO);
                    removedSuccs.add(uri); // These should all overlap

                    overlapsToAdd.add(removedSuccs); //Might be redundant, add after all covers.
                    //newNode.addOverlapsWithRedundancyCheck(removedSuccs);
                }

                oNodes.remove(uri);
            } else if (isOverlapsNode(uri)) {
                overlapsToAdd.add(pUris);
                //newNode.addOverlapsWithoutRedundancyCheck(pUris);
            } else {
                newNode.addCoveredBy(uri, pUris);
            }
        }

        // Add all from o's coveredBy that was not in this, but add overlaps later,
        // as redundancy first can be checked after all containments are added.
        for (Integer uri : oNodes.keySet()) {
            Node n = oNodes.get(uri);
            if (o.isOverlapsNode(uri))
                overlapsToAdd.add(n.succs); //Might be redundant, add after all covers.
            else
                newNode.addCoveredBy(uri, n.succs); 
        }

        //Then add all new overlaps
        for (Set<Integer> ps : overlapsToAdd)
            newNode.addOverlapsWithRedundancyCheck(ps);

        return newNode;
    }

    /**
     * Constructs a relaionship graph based on the relationships between the spaces in spaceNode with
     * overlaps-arity up to overlapsArity.
     */
    public static RelationshipGraph makeRelationshipGraph(SpaceToBintree.Node spaceNode, int overlapsArity) {

        SpaceProvider spaces = spaceNode.getSpaceProvider();
        Set<Integer> uris = spaceNode.getOverlappingURIs();

        RelationshipGraph newNode = new RelationshipGraph(spaceNode.getBlock(), uris, overlapsArity);

        Integer[] urisArr = uris.toArray(new Integer[uris.size()]);

        Set<Intersection> intersections = new HashSet<Intersection>();

        for (int i = 0; i < urisArr.length; i++) { 

            Integer ui = urisArr[i];
            Space si = spaces.get(ui);

            for (int j = i+1; j < urisArr.length; j++) {

                Integer uj = urisArr[j];
                Space sj = spaces.get(uj);

                Relation rel = si.relate(sj);

                if (rel.isIntersects()) {

                    if (rel.isCovers())
                        newNode.addCoveredBy(uj, ui);

                    if (rel.isCoveredBy())
                        newNode.addCoveredBy(ui, uj);
                    
                    if (!rel.isCovers() && !rel.isCoveredBy()) { // Overlaps already represented by containment
                        Set<Integer> elems = new HashSet<Integer>(2);
                        elems.add(ui);
                        elems.add(uj);
                        Space s = si.intersection(sj);
                        Intersection nin = new Intersection(s, elems);
                        intersections.add(nin);
                    }
                }
            }
        }

        newNode.computeKIntersections(intersections, uris, overlapsArity, spaces);
        newNode.minimize(); //KIntersections(overlapsArity);

        return newNode;
    }

    /**
     * Computes the set of maximal overlaps up to arity k, such that the set contains no redundant overlaps.
     * @return A set containing one set per maximal overlap, that is, the intersection of the spaces
     *         for each of the elements in each set is nonempty, but adding any new element to the set
     *         will give an empty intersection.
     */
    public void computeKIntersections(Set<Intersection> intersections, Set<Integer> elems,
                                      int k, SpaceProvider spaces) {

        int size = intersections.size();

        Set<Intersection> ints = new HashSet<Intersection>(intersections);

        for (int i = 3; i <= k; i++) {

            Set<Intersection> iterSet = new HashSet<Intersection>(ints);
            ints = new HashSet<Intersection>(); // Stores next iterations intersections

            for (Intersection in : iterSet) {

                boolean updated = false; // States whether the <in> has become part of a larger intersection

                for (Integer e : elems) {

                    Intersection nin = in.add(e, spaces);

                    if (nin != null) { // Intersection was successfull (i.e. non-empty, not implicit, etc)
                        updated = true;
                        ints.add(nin); 
                    }
                }
                if (!updated)
                    addOverlapsWithRedundancyCheck(in.getElements());
            }
        }

        for (Intersection in : ints) // Need to add remaining intersections
            addOverlapsWithRedundancyCheck(in.getElements());
    }

    private boolean allKaryOverlaps(Set<Integer> curPreds, Integer[] vals, int place, int rsize) {
        if (rsize == 0) {
            return true;
        } else {
            for (int i = place+1; i < vals.length; i++ ) {
                Set<Integer> newCurPreds = new HashSet<Integer>(curPreds);
                newCurPreds.retainAll(nodes.get(vals[i]).preds);

                if (newCurPreds.isEmpty()) return false;

                if (!allKaryOverlaps(newCurPreds, vals, i, rsize-1))
                    return false;
            }
        }
        return true;
    }

    private boolean allKaryOverlapsWith(Set<Integer> uris1, Set<Integer> uris2, int k) {

        Set<Integer> common = new HashSet<Integer>(uris1);
        common.retainAll(uris2);
        Set<Integer> diff1 = new HashSet<Integer>(uris1);
        diff1.removeAll(common);
        Set<Integer> diff2 = new HashSet<Integer>(uris2);
        diff2.removeAll(common);

        for (Integer u2 : diff2) 
            if (!allKaryOverlapsWith(diff1, u2, k)) return false;

        // for (Integer u2 : uris2)
        //     if (!allKaryOverlapsWith(uris1, u2, k)) return false;

        return true;
    }

    private boolean allKaryOverlaps(Set<Integer> uris, int k) {
        return allKaryOverlaps(nodes.keySet(), uris.toArray(new Integer[uris.size()]), -1, k);
    }

    private boolean allKaryOverlapsWith(Set<Integer> rest, Integer u, int rsize) {
        return allKaryOverlaps(nodes.get(u).preds, rest.toArray(new Integer[rest.size()]), -1, rsize-1);
    }

    public void minimizeKIntersectionsOPT(int k) {

        Map<Set<Integer>, Set<Integer>> ints = new HashMap<Set<Integer>, Set<Integer>>();
        for (Integer i : nodes.keySet()) {
            if (isOverlapsNode(i) && nodes.get(i).succs.size() >= k) {
                Set<Integer> pss = new HashSet<Integer>();
                for (Integer u : nodes.get(i).succs) {
                    for (Integer up : nodes.get(u).preds) {
                        for (Integer ups : nodes.get(up).succs) {
                            pss.add(ups);
                        }
                    }
                }
                ints.put(nodes.get(i).succs, pss);
            }
        }

        boolean someUpdated = true;

        for (int i = k; !ints.isEmpty(); i++) {
        
            Map<Set<Integer>, Set<Integer>> iterSet = new HashMap<Set<Integer>, Set<Integer>>(ints);
            ints = new HashMap<Set<Integer>, Set<Integer>>(); // Stores next iterations sets

            for (Set<Integer> in : iterSet.keySet()) {

                boolean updated = false; // States whether the <in> has become part of a larger overlap
                Set<Integer> toRemove = new HashSet<Integer>();
               
                for (Integer u : iterSet.get(in)) {

                    Set<Integer> nin = new HashSet<Integer>(in);
                    nin.add(u);
                    if (!ints.keySet().contains(nin) && allKaryOverlapsWith(in, u, k)) { 
                        ints.put(nin, iterSet.get(in)); 
                        updated = true;
                        someUpdated = true;
                    } else {
                        toRemove.remove(u);
                    }
                }
                if (!updated && i > k) {
                    addOverlapsWithRedundancyCheck(in);
                } else {
                    ints.get(in).removeAll(toRemove);
                }
            }
        }

        for (Set<Integer> in : ints.keySet()) // Need to add remaining intersections
            addOverlapsWithRedundancyCheck(in);
    }

    public void minimizeKIntersections(int k) {

        Set<Set<Integer>> ints = new HashSet<Set<Integer>>();
        for (Integer u : nodes.keySet()) {
            if (isOverlapsNode(u) && nodes.get(u).succs.size() >= k)
                ints.add(nodes.get(u).succs);
        }

        boolean someUpdated = true;

        for (int i = k; someUpdated; i++) {
        
            someUpdated = false;
            Set<Set<Integer>> iterSet = new HashSet<Set<Integer>>(ints);
            ints = new HashSet<Set<Integer>>(); // Stores next iterations sets

            for (Set<Integer> in : iterSet) {

                boolean updated = false; // States whether the <in> has become part of a larger overlap

                Set<Integer> pss = new HashSet<Integer>();
                for (Integer u : in) {
                    for (Integer up : nodes.get(u).preds) {
                        for (Integer ups : nodes.get(up).succs) {
                            if (!in.contains(ups))
                                pss.add(ups);
                        }
                    }
                }
                
                for (Integer u : pss) {

                    Set<Integer> nin = new HashSet<Integer>(in);
                    nin.add(u);
                    if (!ints.contains(nin) && allKaryOverlapsWith(in, u, k)) { 
                        ints.add(nin); 
                        updated = true;
                        someUpdated = true;
                    }
                }
                if (!updated && i > k)
                    addOverlapsWithRedundancyCheck(in);
            }
        }

        for (Set<Integer> in : ints) // Need to add remaining intersections
            addOverlapsWithRedundancyCheck(in);
    }

    private Set<Set<Integer>> findMinimizers(int k, Set<Integer> toCheck) {

        Set<Set<Integer>> overlaps = new HashSet<Set<Integer>>();

        for (Integer u : nodes.keySet()) {
            if (!toCheck.contains(u)) continue;

            Node n = nodes.get(u);

            // We check each k-ary overlaps node, whether this overlaps
            // can be merged with a k+1-ary overlaps. We can do this if
            // for k+1 nodes, all k-ary overlaps exists.
            if (isOverlapsNode(u) && n.succs.size() >= k) {
                Set<Integer> sps = new HashSet<Integer>();

                // We only need to check the overlaps that have at least one common node with u,
                // and represents a k-ary overlap.
                for (Integer s : n.succs) {
                    for (Integer sp : nodes.get(s).preds) {
                        if (isOverlapsNode(sp) && !sp.equals(u) && nodes.get(sp).succs.size() >= k)
                            sps.add(sp);
                    }
                }
                
                for (Integer sp : sps) {
                    Node spn = nodes.get(sp);
                    Set<Integer> commonSuccs = new HashSet<Integer>(); 
                    for (Integer s : spn.succs) {
                        if (n.succs.contains(s) && !containsSomeURI(s))
                            commonSuccs.add(s);
                    }

                    if (commonSuccs.size() >= k-1) {
                        Set<Integer> newOverlaps = new HashSet<Integer>(n.succs);
                        newOverlaps.addAll(spn.succs);
                        if (!overlaps.contains(newOverlaps) &&
                            allKaryOverlapsWith(n.succs, spn.succs, k)) {
                            
                            overlaps.add(newOverlaps);
                            break; 
                        }
                    }
                }
            }
        }
        return overlaps;
    }

    private boolean containsSomeURI(Integer u) {
        
        Node n = nodes.get(u);
        for (Integer p : n.preds) {
            if (!isOverlapsNode(p)) return true;
        }
        return false;
    }

    /**
     * This method minimizes the graph by merging k+1 k-ary overlaps into one k+1-ary overlaps,
     * for any k greater than or equal to config.overlapsArity.
     * Note: This function should only be called right before a call to constructRepresentation.
     */
    public void minimize() {

        Set<Integer> toCheck = new HashSet<Integer>(nodes.keySet());

        int nrB = nodes.keySet().size(); boolean added = false;

        while (true) {

            Set<Set<Integer>> overlaps = findMinimizers(k, toCheck);

            toCheck = new HashSet<Integer>();

            if (!overlaps.isEmpty()) {
                added = true;
                for (Set<Integer> ov : overlaps) {
                    Integer newNode = addOverlapsWithRedundancyCheck(ov);
                    if (newNode != null) toCheck.add(newNode);
                }

            } else {
                break;
            }
        }
    }

    public Representation constructRepresentation(BintreeFactory bf) { //, Map<Integer, Bintree> witnesses) {

        Set<Integer> nus = new HashSet<Integer>(nodes.keySet());
        for (Integer u : nus) {
            if (nodes.containsKey(u) && isOverlapsNode(u)) {
                Node n = nodes.get(u);
                if (overlapsByURINode(n.succs))
                    removeOverlapsNode(u);
            }
        }

        minimize();

        Block[] witnessesArr = bf.makeNDistinct(nodes.keySet().size()+1);

        Map<Integer, Bintree> localRep = new HashMap<Integer, Bintree>();
        Map<Integer, Block> wit = new HashMap<Integer, Block>();

        int i = 0;
        for (Integer node : nodes.keySet()) {
            Block bt;
            bt = block.append(witnessesArr[i++]);
            if (!isOverlapsNode(node))
                wit.put(node, bt);
            localRep.put(node, bf.newBintree(bt));
        }

        while (!nodes.isEmpty()) {

            Integer uri = popNextNode();
            Node n = nodes.get(uri);
            Bintree nodeBT = localRep.get(uri);

            // Propagate child's value to parents
            for (Integer parent : n.succs)
                localRep.put(parent, localRep.get(parent).union(nodeBT));

            nodes.remove(uri);
            for (Integer sUri : n.succs) {
                if (nodes.containsKey(sUri)) {
                    Node s = nodes.get(sUri);
                    s.preds.remove(uri);
                    if (s.preds.isEmpty())
                        roots.add(sUri);
                }
            }
        }

        Map<Integer, Bintree> urisRep = new HashMap<Integer, Bintree>();
        for (Integer uri : localRep.keySet()) {
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
                urisRep.put(uri, new Bintree(cbs, bf));
            }
        }

        Representation rep = new Representation(urisRep);

        return rep;
    }

    class Node {

        Integer uri;
        Set<Integer> preds;
        Set<Integer> succs;

        public Node(Integer uri) {
            this.uri = uri;
            preds = new HashSet<Integer>();
            succs = new HashSet<Integer>();
        }
    }
}

