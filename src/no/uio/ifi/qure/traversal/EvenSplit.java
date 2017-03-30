package no.uio.ifi.qure.traversal;

import java.util.HashSet;
import java.util.Set;

import no.uio.ifi.qure.bintree.Block;

public class EvenSplit {

	public Block splitBlock;
	public Set<SID> intL;
	public Set<SID> intR;

	public EvenSplit(Block splitBlock, Set<SID> intL, Set<SID> intR) {
		this.splitBlock = splitBlock;
		this.intL = intL;
		this.intR = intR;
	}
}
