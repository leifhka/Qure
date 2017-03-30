package no.uio.ifi.qure;

import java.util.HashSet;
import java.util.Set;

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
