package no.uio.ifi.qure.traversal;

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

import no.uio.ifi.qure.*;
import no.uio.ifi.qure.relation.*;
import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.bintree.*;
import no.uio.ifi.qure.space.*;

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

	public Representation constructRepresentations(SpaceProvider spaces, RelationSet relationSet) {

		Progress prog = new Progress("Traversing tree...", Math.pow(2, config.maxIterDepth+1)-1,
		                             0.001, "##0.000");
		if (config.verbose) prog.init();

		Block.setBlockSize(config.blockSize, relationSet.getAtomicRoles().size());
		TreeNode root = new TreeNode(Block.getTopBlock(), spaces, evenSplits, 0, config);
		root.setReporter(prog.makeReporter());
		Representation representation = traverseTree(root);
	   
		if (config.verbose) prog.done();

		representation.setUniverse(spaces.getUniverse());
		return representation;
	}

	private Representation traverseTree(TreeNode node) {

		Representation representation;

		if (node.isEmpty() || (!node.hasEvenSplit() && config.atMaxDepth.test(node))) {
			representation = node.makeRepresentation(config.relationSet);
			if (config.verbose) {
				node.getReporter().update(Math.pow(2, 1 + config.maxIterDepth - node.depth())-1);
			}
		} else {
			TreeNode[] nodes = node.splitNodeEvenly();
			node.deleteSpaces(); // Free memory
			TreeNode leftNode = nodes[0];
			TreeNode rightNode = nodes[1];
 
			Representation newLeftRep = traverseTree(leftNode);
			Representation newRightRep = traverseTree(rightNode);
		
			representation = newLeftRep.merge(newRightRep);

			if (config.verbose) node.getReporter().update();
		}

		representation.addCovering(node.getCovering(), node.getBlock());

		if (node.getEvenSplitBlock() != null) {
			representation.addSplitBlock(node.getBlock(), node.getEvenSplitBlock());
		}
		return representation;
	}
}
