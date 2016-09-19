package no.uio.ifi.qure;

import java.util.Set;
import java.util.Map;
import java.util.Stack;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Predicate;

public class SpaceToBintree {

    private Config config;
    private Progress prog;
    private BintreeFactory bf;

    public SpaceToBintree(Config config) {

        this.config = config;
        this.bf = config.bf;
    }

    public Representation constructRepresentations(SpaceProvider spaces) {

        prog = new Progress("Traversing tree...", Math.pow(2, config.maxIterDepth+1)-1,
                            0.001, "##0.000");
        if (config.verbose) prog.init();

        Node root = new Node(bf.makeTopBlock(), spaces, 0);
        Node newRoot = traverseTree(root);
        
        if (config.verbose) prog.done();

        Representation rootRep = newRoot.getRepresentation();
        rootRep.setUniverse(spaces.getUniverse());
        return rootRep;
    }

    private Node traverseTree(Node node) {

        if (node.isEmpty()) {
            if (config.verbose)
                prog.update(Math.pow(2, 1 + config.maxIterDepth - node.depth())-1);
            return new Node(node.block);
        }

        Node newNode;

        if (config.atMaxDepth.test(node)) {
            newNode = new Node(node.getBlock(),
                               RelationshipGraph.makeRelationshipGraph(node, config.overlapsArity));
            if (config.verbose)
                prog.update(Math.pow(2, 1 + config.maxIterDepth - node.depth())-1);
        } else {
            Node[] nodes = node.splitNode(config.dim);
            node.deleteSpaces(); // Free memory
            Node leftNode = nodes[0];
            Node rightNode = nodes[1];
    
            Node newLeftNode = traverseTree(leftNode);
            Node newRightNode = traverseTree(rightNode);
        
            newNode = newLeftNode.merge(newRightNode);

            if (config.verbose) prog.update();
        }

        if (newNode.depth() == config.representationDepth) {
            
            newNode = new Node(newNode.getBlock(), 
                               newNode.getGraph().constructRepresentation(config.bf));
        }

        newNode = newNode.addCovering(node.getCovering());

        return newNode;
    }

    protected class Node {

        private RelationshipGraph graph;
        private Representation representation;
        private Set<Integer> covering;
        private SpaceProvider spaces;
        private final int split; // if this block was splitted along the x-axis.
        private final Block block; // the bintree block of this spaceNode.

        public Node(Block block, SpaceProvider spaces, int split) {
            this.block = block;
            this.spaces = spaces;
            this.split = split;

            graph = null;
            representation = null;
        }

        public Node(Block block, RelationshipGraph graph) {
            this.block = block;
            this.graph = graph;

            representation = null;
            spaces = null;
            split = 0;
        }

        public Node(Block block, Representation representation) {
            this.block = block;
            this.representation = representation;

            graph = null;
            spaces = null;
            split = 0;
        }

        public Node(Block block) {
            this.block = block;

            graph = new RelationshipGraph(block, new HashSet<Integer>(), config.overlapsArity);
            representation = new Representation(new HashMap<Integer, Bintree>());
            spaces = null;
            split = 0;
        }

        public Set<Integer> getOverlappingURIs() { return spaces.keySet(); }

        public Set<Integer> getCovering() {
            return (covering == null) ? spaces.getCoversUniverse() : covering;
        }

        public SpaceProvider getSpaceProvider() { return spaces; }

        public int split() { return split; }

        public Block getBlock() { return block; }

        public int depth() { return block.depth(); }

        public boolean isEmpty() {
            return spaces == null || getOverlappingURIs().isEmpty();
        }

        public boolean isGraph() { return graph != null; }

        public boolean isRepresentation() { return representation != null; }

        public RelationshipGraph getGraph() { return graph; }

        public Representation getRepresentation() { return representation; }

        public Node addCovering(Set<Integer> covering) {

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
                    representation.getRepresentation().put(uri, bf.newBintree(block));
            } else {
                System.err.println("ERROR: Trying to add covering to a node that is neither rep. or graph.");
                System.exit(1);
            }

            return this;
        }

        public Node merge(Node other) {

            if (isGraph() && other.isGraph()) {
                return new Node(block.getParent(), graph.merge(other.getGraph()));
            } else if (isRepresentation() && other.isRepresentation()) {

                Map<Integer, Bintree> result = representation.getRepresentation();
                Map<Integer, Bintree> orep = other.getRepresentation().getRepresentation();

                for (Integer oid : orep.keySet()) {
                    if (!result.containsKey(oid))
                        result.put(oid, orep.get(oid));
                    else 
                        result.put(oid, result.get(oid).union(orep.get(oid)));
                }

                return new Node(block.getParent(), representation);
            } else {
                System.err.println("ERROR: Trying to merge graph with representation!");
                System.exit(1);
                return null;
            }
        }

        public Node[] splitNode(int dim) {

            Block[] bs = block.split();
            int childDepth = block.depth() + 1;

            SpaceProvider[] sps = getSpaceProvider().splitProvider(split, childDepth);

            Node[] result = new Node[sps.length];

            for (int i = 0; i < sps.length; i++) {

                if (!sps[i].isEmpty()) {

                    Node child = new Node(bs[i], sps[i], (split+1) % dim);

                    if (childDepth == config.representationDepth)
                        child.getSpaceProvider().populateWithExternalOverlapping();
                    
                    result[i] = child;

                } else {
                    result[i] = new Node(bs[i], sps[i], (split+1) % dim);
                }
            }

            return result;
        }

        public void deleteSpaces() {
            covering = spaces.getCoversUniverse();
            spaces = null;
        }
    }
}
