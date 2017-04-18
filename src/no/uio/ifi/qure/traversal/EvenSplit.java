package no.uio.ifi.qure.traversal;

import java.util.HashSet;
import java.util.Set;

import no.uio.ifi.qure.bintree.Block;
import no.uio.ifi.qure.space.Space;

public class EvenSplit {

	public Block splitBlock;
	public Set<SID> intL;
	public Set<SID> intR;
	public Space uniL;
	public Space uniR;

	public EvenSplit(Block splitBlock, Set<SID> intL, Set<SID> intR, Space uniL, Space uniR) {
		this.splitBlock = splitBlock;
		this.intL = intL;
		this.intR = intR;
		this.uniL = uniL;
		this.uniR = uniR;
	}
}
