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
import java.util.concurrent.RecursiveTask;

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
		TraverserThread traverser = new TraverserThread(root, config.numThreads);
		Representation representation = traverser.compute();
	   
		if (config.verbose) prog.done();

		representation.setUniverse(spaces.getUniverse());
		return representation;
	}

	class TraverserThread extends RecursiveTask<Representation> {
		static final long serialVersionUID = 42L;
		TreeNode node;
		int numThreads;

		TraverserThread(TreeNode node, int numThreads) {
			this.node = node;
			this.numThreads = numThreads;
		}

		public Representation compute() {
			if (numThreads <= 1) {
				return traverseTree(node);
			} else {
				return traverseTreePar(node, numThreads);
			}
		}

		private Representation traverseTree(TreeNode node) {

			Representation representation;

			if (node.isEmpty() || (!node.hasEvenSplit() && config.atMaxDepth.test(node))) {

				representation = node.makeRepresentation(config.relationSet);
				if (config.verbose) {
					node.getReporter().update(Math.pow(2, 1 + config.maxIterDepth - node.depth())-1);
				}
				node.deleteSpaces();
			} else {
				Pair<TreeNode, TreeNode> nodes = node.splitNodeEvenly(config.numThreads);
				node.deleteSpaces(); // Free memory

				Representation newLeftRep = traverseTree(nodes.fst);
				Representation newRightRep = traverseTree(nodes.snd);

				representation = newLeftRep.merge(newRightRep);

				if (config.verbose) node.getReporter().update();
			}

			representation.addCovering(node.getCovering(), node.getBlock(), config.relationSet.getRoles());

			if (node.getEvenSplitBlock() != null) {
				representation.addSplitBlock(node.getBlock(), node.getEvenSplitBlock());
			}
			return representation;
		}

		private Representation traverseTreePar(TreeNode node, int numThreads) {

			Representation representation;

			if (node.isEmpty() || (!node.hasEvenSplit() && config.atMaxDepth.test(node))) {

				representation = node.makeRepresentation(config.relationSet);
				if (config.verbose) {
					node.getReporter().update(Math.pow(2, 1 + config.maxIterDepth - node.depth())-1);
				}
				node.deleteSpaces();
			} else {
				Pair<TreeNode, TreeNode> nodes = node.splitNodeEvenly(numThreads);
				node.deleteSpaces(); // Free memory

				Representation newLeftRep = null, newRightRep = null;

				TraverserThread leftThread = new TraverserThread(nodes.fst, numThreads);
				leftThread.fork();
				TraverserThread rightThread = new TraverserThread(nodes.snd, numThreads);

				representation = rightThread.compute().merge(leftThread.join());

				if (config.verbose) node.getReporter().update();
			}

			representation.addCovering(node.getCovering(), node.getBlock(), config.relationSet.getRoles());

			if (node.getEvenSplitBlock() != null) {
				representation.addSplitBlock(node.getBlock(), node.getEvenSplitBlock());
			}
			return representation;
		}
	}
}
