package no.uio.ifi.qure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TreeNode {

    private static enum NodeType {GRAPH, REPRESENTATION, TO_SPLIT, EMPTY};

    private RelationshipGraph graph; // the relationship graph of this node.
    private Representation representation; // the bintree representation of this node.
    private Set<Integer> covering; // the URI of spaces covering this node.
    private SpaceProvider spaces; // the spaces overlapping this node.
    private int split; // splitting dimension.
    private Block block; // the bintree block of this spaceNode.
    private Progress.Reporter reporter;
    private final NodeType type;

    public TreeNode(Block block, SpaceProvider spaces, int split) {
        this.block = block;
        this.spaces = spaces;
        this.split = split;

        type = NodeType.TO_SPLIT;
    }

    public TreeNode(Block block, RelationshipGraph graph) {
        this.block = block;
        this.graph = graph;

        type = NodeType.GRAPH;
    }

    public TreeNode(Block block, Representation representation) {
        this.block = block;
        this.representation = representation;

        type = NodeType.REPRESENTATION;
    }

    public TreeNode(Block block) {
        this.block = block;

        graph = new RelationshipGraph(block, new HashSet<Integer>(), 0);
        representation = new Representation(new HashMap<Integer, Bintree>());

        type = NodeType.EMPTY;
    }

    public void setReporter(Progress.Reporter reporter) { this.reporter = reporter; }

    public Progress.Reporter getReporter() { return reporter; }

    public Set<Integer> getOverlappingURIs() { return spaces.keySet(); }

    public Set<Integer> getCovering() { return (covering == null) ? spaces.getCoversUniverse() : covering; }

    public SpaceProvider getSpaceProvider() { return spaces; }

    public int split() { return split; }

    public Block getBlock() { return block; }

    public int depth() { return block.depth(); }

    public boolean isEmpty() { return spaces == null || getOverlappingURIs().isEmpty(); }

    public boolean isGraph() { return type == NodeType.GRAPH || type == NodeType.EMPTY; }

    public boolean isRepresentation() { return type == NodeType.REPRESENTATION || type == NodeType.EMPTY; }

    public RelationshipGraph getGraph() { return graph; }

    public Representation getRepresentation() { return representation; }

    public TreeNode addCovering(Set<Integer> covering) {

        if (isGraph()) {

            RelationshipGraph graph = getGraph();
            graph.addUris(covering);

            for (Integer pUri : covering) {
                for (Integer cUri : graph.getNodes().keySet()) {
                    if (pUri != cUri)
                        graph.addCoveredBy(cUri, pUri);
                }
            }
        } else if (isRepresentation()) {

            for (Integer uri : covering)
                representation.getRepresentation().put(uri, Bintree.fromBlock(block));
        } else {
            System.err.println("ERROR: Trying to add covering to a node that is neither rep. or graph.");
            System.exit(1);
        }

        return this;
    }

    public TreeNode merge(TreeNode other) {

        if (isGraph() && other.isGraph()) {
            return new TreeNode(block.getParent(), graph.merge(other.getGraph()));
        } else if (isRepresentation() && other.isRepresentation()) {
            return new TreeNode(block.getParent(), representation.merge(other.getRepresentation()));
        } else {
            System.err.println("ERROR: Trying to merge graph with representation!");
            System.err.println("Types: " + type + ", " + other.type);
            System.err.println("Depths: " + block.depth() + ", " + other.block.depth());
            System.exit(1);
            return null;
        }
    }

    public TreeNode[] splitNodeEvenly(int dim, int repDepth, int maxSplit, int maxDiff) {

        Block splitBlock = getSpaceProvider().getEvenSplit(split, maxSplit, maxDiff);
        int childDepth = block.depth() + 1;
        SpaceProvider[] sps = getSpaceProvider().splitProvider(split, childDepth, splitBlock);
        return makeChildNodes(dim, repDepth, sps);
    }

    public TreeNode[] splitNodeRegularly(int dim, int repDepth) {

        int childDepth = block.depth() + 1;
        SpaceProvider[] sps = getSpaceProvider().splitProvider(split, childDepth);
        return makeChildNodes(dim, repDepth, sps);
    }

    private TreeNode[] makeChildNodes(int dim, int repDepth, SpaceProvider[] sps) {

        Block[] bs = block.split();
        TreeNode[] result = new TreeNode[sps.length];
        int childDepth = block.depth() + 1;

        for (int i = 0; i < sps.length; i++) {

            TreeNode child = new TreeNode(bs[i], sps[i], (split+1) % dim);

            if (!sps[i].isEmpty() && childDepth == repDepth)
                child.getSpaceProvider().populateWithExternalOverlapping();
                
            result[i] = child;
        }
        
        result[0].setReporter(reporter);
        for (int i = 1; i < result.length; i++) result[i].setReporter(reporter.newReporter());

        return result;
    }

    public void deleteSpaces() {
        covering = spaces.getCoversUniverse();
        spaces = null;
    }
}
