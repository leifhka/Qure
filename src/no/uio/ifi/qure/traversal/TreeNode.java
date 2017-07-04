package no.uio.ifi.qure.traversal;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import no.uio.ifi.qure.Config;
import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.relation.*;
import no.uio.ifi.qure.space.*;
import no.uio.ifi.qure.bintree.*;

public class TreeNode {

	private Set<SID> covering; // the URI of spaces covering this node.
	private SpaceProvider spaces; // the spaces overlapping this node.
	private int split; // splitting dimension.
	private Block block; // the bintree block of this spaceNode.
	private Map<Block, Block> evenSplits;
	private Block evenSplitBlock;
	private Reporter reporter;
	private Config config;

	public TreeNode(Block block, SpaceProvider spaces, Map<Block, Block> evenSplits,
	                int split, Config config) {

		this.block = block;
		this.spaces = spaces;
		this.split = split;
		this.evenSplits = evenSplits;
		this.config = config;
	}

	public boolean isEmpty() { return getOverlappingURIs().isEmpty(); }

	public Block getEvenSplitBlock() { return evenSplitBlock; }

	public boolean hasEvenSplit() { return evenSplits.containsKey(block); }

	public void setReporter(Reporter reporter) { this.reporter = reporter; }

	public Reporter getReporter() { return reporter; }

	public Set<SID> getOverlappingURIs() { return spaces.keySet(); }

	public int size() { return spaces.keySet().size(); }

	public Set<SID> getCovering() { return (covering == null) ? spaces.getCoversUniverse() : covering; }

	public SpaceProvider getSpaceProvider() { return spaces; }

	public int split() { return split; }

	public Block getBlock() { return block; }

	public int depth() { return block.depth(); }

	public Pair<TreeNode, TreeNode> splitNodeEvenly(int numThreads) {

		Pair<? extends SpaceProvider, ? extends SpaceProvider> sps;

		if (evenSplits.containsKey(block)) {
			sps = getSpaceProvider().splitProvider(split, evenSplits.get(block), getOverlappingURIs());
		} else {
			EvenSplit evenSplit = getSpaceProvider().getEvenSplit(split, config.maxSplits, config.minRatio, numThreads);
			evenSplitBlock = evenSplit.splitBlock; // Save for representation, will be written to DB
			sps = getSpaceProvider().splitProvider(split, evenSplit);
		}

		return makeChildNodes(sps);
	}

	public Pair<TreeNode, TreeNode> splitNodeRegularly() {

		Pair<? extends SpaceProvider, ? extends SpaceProvider> sps = getSpaceProvider().splitProvider(split);
		return makeChildNodes(sps);
	}

	private Map<Block, Block> getSplitBlocks(Block b) {

		Map<Block, Block> m = new HashMap<Block, Block>();
		for (Block block : evenSplits.keySet()) {
			if (block.blockPartOf(b)) {
				m.put(block, evenSplits.get(block));
			}
		}
		return m;
	}

	private TreeNode makeChildNode(Block b, SpaceProvider sp) {

		TreeNode child = new TreeNode(b, sp, getSplitBlocks(b), (split+1) % config.dim, config);
		if (!sp.isEmpty() && !child.hasEvenSplit()) {
			sp.populateWithExternalOverlapping();
		}

		return child;
	}

	private Pair<TreeNode, TreeNode> makeChildNodes(Pair<? extends SpaceProvider, ? extends SpaceProvider> sps) {

		Pair<Block, Block> bs = block.split();
		TreeNode childL = makeChildNode(bs.fst, sps.fst);
		TreeNode childR = makeChildNode(bs.snd, sps.snd);

		childL.setReporter(reporter);
		childR.setReporter(reporter.newReporter());
		return new Pair<TreeNode, TreeNode>(childL, childR);
	}

	public Representation makeRepresentation(RelationSet rels) {

		if (spaces.isEmpty()) {
			return new Representation();
		}
		RelationshipGraph graph = RelationshipGraph.makeRelationshipGraph(this, rels);
		return graph.constructRepresentation();
	}

	public void deleteSpaces() {
		covering = new HashSet<SID>(spaces.getCoversUniverse());
		spaces.clear();
		spaces = null;
	}
}
