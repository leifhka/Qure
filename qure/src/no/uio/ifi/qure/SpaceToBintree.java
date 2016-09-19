package no.uio.ifi.qure;

import java.util.Set;
import java.util.Map;
import java.util.Stack;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Predicate;

public class SpaceToBintreeRec {

    private Config config;
    private Progress prog;
    //private Map<Integer, Bintree> witnesses;

    public SpaceToBintreeRec(Config config) {

        this.config = config;
    }

    public Representation constructRepresentations(SpaceProvider spaces) {

        prog = new Progress("Traversing tree...", Math.pow(2, config.maxIterDepth+1)-1,
                            0.001, "##0.000");
        if (config.verbose) prog.init();

        //witnesses = new HashMap<Integer, Bintree>();

        Node root = new Node(config.bf.makeTop(), spaces, 0);
        Node newRoot = traverseTree(root);
        
        if (config.verbose) prog.done();

        Representation rootRep = newRoot.getRepresentation();
        rootRep.setUniverse(spaces.getUniverse());
        //rootRep.setWitnesses(witnesses);
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
                               RelationshipNode.makeRelationshipNode(node, config.overlapsArity));
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
                               newNode.getGraph().constructRepresentation(config.bf)); //, witnesses));
        }

        newNode = newNode.addCovering(node.getCovering());

        return newNode;
    }

    protected class Node {

        private RelationshipNode graph;
        private Representation representation;
        private Set<Integer> covering;
        private SpaceProvider spaces;
        private final int split; // if this block was splitted along the x-axis.
        private final Bintree block; // the bintree block of this spaceNode.

        public Node(Bintree block, SpaceProvider spaces, int split) {
            this.block = block;
            this.spaces = spaces;
            this.split = split;

            graph = null;
            representation = null;
        }

        public Node(Bintree block, RelationshipNode graph) {
            this.block = block;
            this.graph = graph;

            representation = null;
            spaces = null;
            split = 0;
        }

        public Node(Bintree block, Representation representation) {
            this.block = block;
            this.representation = representation;

            graph = null;
            spaces = null;
            split = 0;
        }

        public Node(Bintree block) {
            this.block = block;

            graph = new RelationshipNode(block, new HashSet<Integer>(), config.overlapsArity);
            //representation = new Representation(new HashMap<Integer, Bintree>(), new HashMap<Integer, Bintree>());
            representation = new Representation(new HashMap<Integer, Collection<Block>>());
            spaces = null;
            split = 0;
        }

        public Set<Integer> getOverlappingURIs() { return spaces.keySet(); }

        public Set<Integer> getCovering() {
            return (covering == null) ? spaces.getCoversUniverse() : covering;
        }

        public SpaceProvider getSpaceProvider() { return spaces; }

        public int split() { return split; }

        public Bintree getBlock() { return block; }

        public int depth() { return block.depth(); }

        public boolean isEmpty() {
            return spaces == null || getOverlappingURIs().isEmpty();
        }

        public boolean isGraph() { return graph != null; }

        public boolean isRepresentation() { return representation != null; }

        public RelationshipNode getGraph() { return graph; }

        public Representation getRepresentation() { return representation; }

        public Node addCovering(Set<Integer> covering) {

            if (isGraph()) {

                RelationshipNode graph = getGraph();
                graph.addUris(covering);

                for (Integer pUri : covering) {
                    for (Integer cUri : graph.getNodes().keySet()) {
                        if (pUri != cUri)
                            graph.addCoveredBy(cUri, pUri);
                    }
                }
            } else if (isRepresentation()) {

                for (Integer uri : covering)
                    representation.getRawRepresentation().put(uri, block.toBlockSet());
            } else {
                System.out.println("ERROR: Trying to add covering to a node that is neither rep. or graph.");
                System.exit(1);
            }

            return this;
        }

        public Node merge(Node other) {

            if (isGraph() && other.isGraph()) {
                return new Node(block.getParentBlock(), graph.merge(other.getGraph()));
            } else if (isRepresentation() && other.isRepresentation()) {

                //Map<Integer, Bintree> result = representation.getRawRepresentation();
                //Map<Integer, Bintree> orep = other.getRepresentation().getRawRepresentation();

                Map<Integer, Collection<Block>> result = representation.getRawRepresentation();
                Map<Integer, Collection<Block>> orep = other.getRepresentation().getRawRepresentation();

                for (Integer oid : orep.keySet()) {
                    if (!result.containsKey(oid))
                        result.put(oid, orep.get(oid));
                    else 
                        result.get(oid).addAll(orep.get(oid));
                }

                //Map<Integer, Bintree> witnesses = representation.getWitnesses();
                //witnesses.putAll(other.getRepresentation().getWitnesses());

                orep = null; //Free memory
                other.representation = null; //Free memory

                return new Node(block.getParentBlock(), representation);
            } else {
                System.err.println("ERROR: Trying to merge graph with representation!");
                System.exit(0);
                return null;
            }
        }

        public Node[] splitNode(int dim) {

            Bintree[] bs = block.splitBlock();
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
