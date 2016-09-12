package no.uio.ifi.qure;

import java.util.Arrays;
import java.util.ArrayList;

public class ISBintreeFactory implements BintreeFactory {

    public String toString() {
        return "ISBintree";
    }

    public ISBintree makeNew(Block[] e) {
	return new ISBintree(e,this);
    }

    public ISBintree makeEmpty() {
	return new ISBintree(new Block[0], this);
    }

    public ISBintree makeTop() {
	return new ISBintree(new Block[]{new Block(0,0)}, this);
    }

    /**
     * @param n the number of distinct objects to return
     * @return an array of n disjoint bintrees
     */
    public ISBintree[] makeNDistinct(int n) {
         
        double log2n = Math.log(n)/Math.log(2);
        int size = Math.toIntExact(Math.round(Math.ceil(log2n)));

        ISBintree[] res = new ISBintree[n];
        Block nb;

        for (int i = 0; i < n; i++) {
            nb = new Block(size, i);
            res[i] = new ISBintree(new Block[]{nb}, this);
        }
        return res;
    }

    /**
     * @param g The geometry to construct an above aprox. from as a bintree
     * @param bb The geometric representation of the current block
     * @param xaxis True if we are going to divide along the x-axis, false is along the y-axis
     * @param level The resolution depth that the bintree should have
     * @param block The current path from root
     * @param blocks The list of blocks that contains the final representation
     */
    private void aboveAproxIter(Space g, Space bb, boolean xaxis, int level, Block block, ArrayList<Block> blocks) {
        
        if (level <= 0) {
            if (g.intersects(bb)) {
                blocks.add(block);
            }
            return;
        }

        Space[] bbs = bb.split(xaxis);
        Space bg1 = bbs[0];
        Space bg2 = bbs[1];       

        if (g.intersects(bg1)) {
            if (g.covers(bg1)) {
                blocks.add(block.addZero());
            } else {
                aboveAproxIter(g, bg1, !xaxis, level-1, block.addZero(), blocks);
            }
        } 

        if (g.intersects(bg2)) {
            if (g.covers(bg2)) {
                blocks.add(block.addOne());
            } else {
                aboveAproxIter(g, bg2, !xaxis, level-1, block.addOne(), blocks);
            }
        }
    }


    /**
     * @param g The geometry to construct a below aprox. from as a bintree
     * @param bb The geometric representation of the current block
     * @param xaxis True if we are going to divide along the x-axis, false is along the y-axis
     * @param level The resolution depth that the bintree should have
     * @param block The current path from root
     * @param blocks The list of blocks that contains the final representation
     */
    private void belowAproxIter(Space g, Space bb, boolean xaxis, int level, Block block, ArrayList<Block> blocks) {
        
        if (level <= 0) {
            if (g.covers(bb)) {
                blocks.add(block);
            }
            return;
        }

        Space[] bbs = bb.split(xaxis);
        Space bg1 = bbs[0];
        Space bg2 = bbs[1];       

        if (g.intersects(bg1)) {
            if (g.covers(bg1)) {
                blocks.add(block.addZero());
            } else {
                belowAproxIter(g, bg1, !xaxis, level-1, block.addZero(), blocks);
            }
        } 

        if (g.intersects(bg2)) {
            if (g.covers(bg2)) {
                blocks.add(block.addOne());
            } else {
                belowAproxIter(g, bg2, !xaxis, level-1, block.addOne(), blocks);
            }
        } 
    }

    /**
     * @param g The geometry to construct an above aprox. from as a bintree
     * @param universe The geometric representation of the universe
     * @param level The resolution depth that the bintree should have
     * @return A bintree of representing the area of g at resolution depth level, aproximated from above.
     */
    public ISBintree makeAboveAprox(Space g, Space universe, int level) {
	ArrayList<Block> blocks = new ArrayList<Block>();
	aboveAproxIter(g, universe, true, level, Block.getTopBlock(), blocks);
        return new ISBintree(blocks.toArray(new Block[blocks.size()]), this);
    }

    /**
     * @param g The geometry to construct a below aprox. from as a bintree
     * @param universe The geometric representation of the universe
     * @param level The resolution depth that the bintree should have
     * @return A bintree of representing the area of g at resolution depth level, aproximated from below.
     */
    public ISBintree makeBelowAprox(Space g, Space universe, int level) {
        ArrayList<Block> blocks = new ArrayList<Block>();
        belowAproxIter(g, universe, true, level, Block.getTopBlock(), blocks);
        return new ISBintree(blocks.toArray(new Block[blocks.size()]), this);
    }

}
