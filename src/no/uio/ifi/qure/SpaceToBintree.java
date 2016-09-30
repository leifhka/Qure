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
    private Map<Block, Block> evenSplits;

    public SpaceToBintree(Config config) {

        this.config = config;
        evenSplits = new HashMap<Block, Block>();
    }

    public SpaceToBintree(Config config, Map<Block, Block> evenSplits) {

        this.config = config;
        this.evenSplits = evenSplits;
    }

    public Representation constructRepresentations(SpaceProvider spaces) {

        Progress prog = new Progress("Traversing tree...", Math.pow(2, config.maxIterDepth+1)-1,
                                     0.001, "##0.000");
        if (config.verbose) prog.init();

        TreeNode root = new TreeNode(Block.TOPBLOCK, spaces, evenSplits, 0);
        root.setReporter(prog.makeReporter());
        TreeNode newRoot = traverseTree(root);
        
        if (config.verbose) prog.done();

        Representation rootRep = newRoot.getRepresentation();
        rootRep.setUniverse(spaces.getUniverse());
        return rootRep;
    }

    private TreeNode traverseTree(TreeNode node) {

        TreeNode newNode;

        if (node.isEmpty() || (!node.hasEvenSplit() && config.atMaxDepth.test(node))) {
            newNode = node.makeRepresentation(config.overlapsArity);
            if (config.verbose)
                node.getReporter().update(Math.pow(2, 1 + config.maxIterDepth - node.depth())-1);
        } else {
            TreeNode[] nodes = node.splitNodeEvenly(config.dim, config.maxSplit, config.maxDiff);
            node.deleteSpaces(); // Free memory
            TreeNode leftNode = nodes[0];
            TreeNode rightNode = nodes[1];
    
            TreeNode newLeftNode = traverseTree(leftNode);
            TreeNode newRightNode = traverseTree(rightNode);
        
            newNode = newLeftNode.merge(newRightNode);

            if (config.verbose) node.getReporter().update();
        }

        newNode = newNode.addCovering(node.getCovering());
        if (node.getEvenSplitBlock() != null)
            newNode.getRepresentation().addSplitBlock(node.getBlock(), node.getEvenSplitBlock());

        return newNode;
    }

    private TreeNode traverseTreeOld(TreeNode node) {

        if (node.isEmpty()) {
            if (config.verbose)
                node.getReporter().update(Math.pow(2, 1 + config.maxIterDepth - node.depth())-1);
            return new TreeNode(node.getBlock());
        }

        TreeNode newNode;

        if (config.atMaxDepth.test(node)) {
            newNode = new TreeNode(node.getBlock(),
                                   RelationshipGraph.makeRelationshipGraph(node, config.overlapsArity));
            if (config.verbose)
                node.getReporter().update(Math.pow(2, 1 + config.maxIterDepth - node.depth())-1);
        } else {
            TreeNode[] nodes = node.splitNodeEvenly(config.dim, config.maxSplit, config.maxDiff);
            node.deleteSpaces(); // Free memory
            TreeNode leftNode = nodes[0];
            TreeNode rightNode = nodes[1];
    
            TreeNode newLeftNode = traverseTree(leftNode);
            TreeNode newRightNode = traverseTree(rightNode);
        
            newNode = newLeftNode.merge(newRightNode);

            if (config.verbose) node.getReporter().update();
        }

        if (config.atRepDepth.test(newNode)) { //newNode.depth() == config.representationDepth) {
            
            newNode = new TreeNode(newNode.getBlock(), 
                                   newNode.getGraph().constructRepresentation());
        }

        newNode = newNode.addCovering(node.getCovering());

        return newNode;
    }
}
