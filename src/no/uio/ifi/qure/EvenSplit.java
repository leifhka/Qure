package no.uio.ifi.qure;

import java.util.HashSet;
import java.util.Set;

public class EvenSplit {

    public Block splitBlock;
    public Set<Integer> intL;
    public Set<Integer> intR;

    public EvenSplit(Block splitBlock, Set<Integer> intL, Set<Integer> intR) {
        this.splitBlock = splitBlock;
        this.intL = intL;
        this.intR = intR;
    }
}
