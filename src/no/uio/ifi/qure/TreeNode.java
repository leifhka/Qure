package no.uio.ifi.qure;

import java.util.HashMap;
import java.util.Map;
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
    private Map<Block, Block> evenSplits;
    private Block evenSplitBlock;
    private Progress.Reporter reporter;
    private final NodeType type;

    public TreeNode(Block block, SpaceProvider spaces, Map<Block, Block> evenSplits, int split) {
        this.block = block;
        this.spaces = spaces;
        this.split = split;
        this.evenSplits = evenSplits;

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

    public Block getEvenSplitBlock() { return evenSplitBlock; }

    public boolean hasEvenSplit() { return evenSplits.containsKey(block); }

    public void setReporter(Progress.Reporter reporter) { this.reporter = reporter; }

    public Progress.Reporter getReporter() { return reporter; }

    public Set<Integer> getOverlappingURIs() { return spaces.keySet(); }

    public int size() { return spaces.keySet().size(); }

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
            Representation newRep = representation.merge(other.getRepresentation());
            TreeNode newNode = new TreeNode(block.getParent(), newRep);
            return newNode;
        } else {
            System.err.println("ERROR: Trying to merge graph with representation!");
            System.err.println("Types: " + type + ", " + other.type);
            System.err.println("Depths: " + block.depth() + ", " + other.block.depth());
            System.exit(1);
            return null;
        }
    }

    public TreeNode[] splitNodeEvenly(int dim, int maxSplit, int maxDiff) {

        EvenSplit evenSplit;
        if (evenSplits.containsKey(block)) {
            Block evenSplitBlock = evenSplits.get(block);
            evenSplit = new EvenSplit(evenSplitBlock, getOverlappingURIs(), getOverlappingURIs());
        } else {
            evenSplit = getSpaceProvider().getEvenSplit(split, maxSplit, maxDiff);
            evenSplitBlock = evenSplit.splitBlock;
        }

        SpaceProvider[] sps = getSpaceProvider().splitProvider(split, evenSplit);
        return makeChildNodes(dim, sps);
    }

    public TreeNode[] splitNodeRegularly(int dim) {

        SpaceProvider[] sps = getSpaceProvider().splitProvider(split);
        return makeChildNodes(dim, sps);
    }

    private Map<Block, Block> getSplitBlocks(Block b) {

        Map<Block, Block> m = new HashMap<Block, Block>();
        int n = evenSplits.size(); 
        for (Block block : evenSplits.keySet()) {
            if (block.blockPartOf(b))
                m.put(block, evenSplits.get(block));
        }
        return m;
    }

    private TreeNode[] makeChildNodes(int dim, SpaceProvider[] sps) {

        Block[] bs = block.split();
        TreeNode[] result = new TreeNode[sps.length];

        for (int i = 0; i < sps.length; i++) {
            result[i] = new TreeNode(bs[i], sps[i], getSplitBlocks(bs[i]), (split+1) % dim);
            int b = sps[i].size();
            if (!sps[i].isEmpty() && !result[i].hasEvenSplit())
                sps[i].populateWithExternalOverlapping();
        }

        result[0].setReporter(reporter);
        for (int i = 1; i < result.length; i++) result[i].setReporter(reporter.newReporter());

        return result;
    }

    public TreeNode makeRepresentation(int overlapsArity) {

        if (spaces.isEmpty()) return new TreeNode(block, new Representation());
        if (block.depth() < 15 && spaces.size() > 20) System.out.println("ERR: " + block.depth() + " " + spaces.size());
        RelationshipGraph graph = RelationshipGraph.makeRelationshipGraph(this, overlapsArity);
        return new TreeNode(block, graph.constructRepresentation());
    }

    public void deleteSpaces() {
        covering = spaces.getCoversUniverse();
        spaces = null;
    }
}
