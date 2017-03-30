package no.uio.ifi.qure;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import no.uio.ifi.qure.util.*;
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

	public TreeNode[] splitNodeEvenly() {

		EvenSplit evenSplit;
		if (evenSplits.containsKey(block)) {
			evenSplit = new EvenSplit(evenSplits.get(block), getOverlappingURIs(), getOverlappingURIs());
		} else {
			evenSplit = getSpaceProvider().getEvenSplit(split, config);
			evenSplitBlock = evenSplit.splitBlock; // Save for representation, will be written to DB
		}

		SpaceProvider[] sps = getSpaceProvider().splitProvider(split, evenSplit);

		return makeChildNodes(sps);
	}

	public TreeNode[] splitNodeRegularly() {

		SpaceProvider[] sps = getSpaceProvider().splitProvider(split);
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

	private TreeNode[] makeChildNodes(SpaceProvider[] sps) {

		Block[] bs = block.split();
		TreeNode[] result = new TreeNode[sps.length];

		for (int i = 0; i < sps.length; i++) {
			result[i] = new TreeNode(bs[i], sps[i], getSplitBlocks(bs[i]), (split+1) % config.dim, config);
			if (!sps[i].isEmpty() && !result[i].hasEvenSplit()) {
				sps[i].populateWithExternalOverlapping();
			}
		}

		result[0].setReporter(reporter);
		for (int i = 1; i < result.length; i++) {
			result[i].setReporter(reporter.newReporter());
		}
		return result;
	}

	public Representation makeRepresentation(int overlapsArity) {

		if (spaces.isEmpty()) {
			return new Representation();
		}
		RelationshipGraph graph = RelationshipGraph.makeRelationshipGraph(this, overlapsArity);
		return graph.constructRepresentation();
	}

	public void deleteSpaces() {
		covering = spaces.getCoversUniverse();
		spaces = null;
	}
}
