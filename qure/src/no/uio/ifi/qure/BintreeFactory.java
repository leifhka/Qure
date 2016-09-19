package no.uio.ifi.qure;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class BintreeFactory {

    /**
     * Constructs a new bintree from the argument block.
     */
    public Bintree newBintree(Block block) {
        Set<Block> nbt = new HashSet<Block>();
        nbt.add(block);
        return new Bintree(nbt, this);
    }

    /**
     * Returns the empty bintree.
     */
    public Bintree makeEmpty() {
	return new Bintree(new HashSet<Block>(), this);
    }

    /**
     * Returns the block containing all other blocks.
     */
    public Block makeTopBlock() {
        return Block.TOPBLOCK;
    }

    /**
     * Returns the bintree containint all other bintrees.
     */
    public Bintree makeTop() {
        Set<Block> t = new HashSet<Block>();
        t.add(new Block(0,0));
	return new Bintree(t, this);
    }

    /**
     * @param n the number of distinct objects to return
     * @return an array of n pairwise disjoint bintrees
     */
    public Block[] makeNDistinct(int n) {
         
        double log2n = Math.log(n)/Math.log(2);
        int size = Math.toIntExact(Math.round(Math.ceil(log2n)));

        Block[] res = new Block[n];

        for (int i = 0; i < n; i++) {
            res[i] = new Block(size, i);
        }
        return res;
    }
}
