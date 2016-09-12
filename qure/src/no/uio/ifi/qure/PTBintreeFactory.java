package no.uio.ifi.qure;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class PTBintreeFactory implements BintreeFactory {

    public PTBintreeFactory() {}

    public String toString() {

        return "PTBintree";
    }

    public PTBintree makeEmpty() {

        return new PTBintree(null, null, false, this);
    }

    public PTBintree makeTop() {

        return new PTBintree(null, null, true, this);
    }

    private void distinctMaker(PTBintree[] bs, int from, int to) {
	
	int n = to-from;

	if (n < 1)
	    return;

	int newTo = from + ((int) Math.round(n/2));

 	for (int i = from; i <= newTo; i++) {
	    bs[i] = bs[i].appendToOne(new PTBintree(makeTop(), null, true, this));
	}
	for (int i = newTo+1; i <= to; i++) {
	    bs[i] = bs[i].appendToOne(new PTBintree(null, makeTop(), true, this));
	}

	distinctMaker(bs, from, newTo);
	distinctMaker(bs, newTo+1, to);
    }

    /**
     * @param n the number of distinct objects to return
     * @return an array of n disjoint bintrees
     */
    public PTBintree[] makeNDistinct(int n) {

	if (n <= 0)
	    return null;
	
        PTBintree[] bs = new PTBintree[n];

	for (int i = 0; i < n; i++)
            bs[i] = makeTop();

	distinctMaker(bs, 0, n-1);
	return bs;
    }

    /**
     * @param g The geometry to construct an above aprox. from as a bintree
     * @param bb The geometric representation of the current block
     * @param xaxis True if we are going to divide along the x-axis, false is along the y-axis
     * @param level The resolution depth that the bintree should have
     * @return A bintree representing the area of g at resolution depth level, aproximated from above.
     */
    private PTBintree aboveAproxIter(Space g, Space bb, boolean xaxis, int level) {
        
        if (level <= 0) {
            if (g.intersects(bb)) {
                return makeTop();
            } else {
                return makeEmpty();
            }
        }

        Space[] bbs = bb.split(xaxis);
        Space bg1 = bbs[0];
        Space bg2 = bbs[1];       

        PTBintree nLeft = null;
        PTBintree nRight = null;

        if (g.intersects(bg1)) {
            if (g.covers(bg1)) {
                nLeft = makeTop();
            } else {
                nLeft = aboveAproxIter(g, bg1, !xaxis, level-1);
            }
        } 

        if (g.intersects(bg2)) {
            if (g.covers(bg2)) {
                nRight = makeTop();
            } else {
                nRight = aboveAproxIter(g, bg2, !xaxis, level-1);
            }
        } 
        if ((nLeft == null || nLeft.isEmpty()) && (nRight == null || nRight.isEmpty())) {
            return makeEmpty();
        } else {
            return new PTBintree(nLeft, nRight, true, this);
        }
    }


    /**
     * @param g The geometry to construct a below aprox. from as a bintree
     * @param bb The geometric representation of the current block
     * @param xaxis True if we are going to divide along the x-axis, false is along the y-axis
     * @param level The resolution depth that the bintree should have
     * @return A bintree representing the area of g at resolution depth level, aproximated from below.
     */
    private PTBintree belowAproxIter(Space g, Space bb, boolean xaxis, int level) {
        
        if (level <= 0) {
            if (g.covers(bb)) {
                return makeTop();
            } else {
                return makeEmpty();
            }
        }

        Space[] bbs = bb.split(xaxis);
        Space bg1 = bbs[0];
        Space bg2 = bbs[1];       

        PTBintree nLeft = null;
        PTBintree nRight = null;

        if (g.intersects(bg1)) {
            if (g.covers(bg1)) {
                nLeft = makeTop();
            } else {
                nLeft = belowAproxIter(g, bg1, !xaxis, level-1);
            }
        } 

        if (g.intersects(bg2)) {
            if (g.covers(bg2)) {
                nRight = makeTop();
            } else {
                nRight = belowAproxIter(g, bg2, !xaxis, level-1);
            }
        } 
        if ((nLeft == null || nLeft.isEmpty()) && (nRight == null || nRight.isEmpty())) {
            return makeEmpty();
        } else {
            return new PTBintree(nLeft, nRight, true, this);
        }
    }

    /**
     * @param g The geometry to construct an above aprox. from as a bintree
     * @param universe The geometric representation of the universe
     * @param level The resolution depth that the bintree should have
     * @return A bintree of representing the area of g at resolution depth level, aproximated from above.
     */
    public PTBintree makeAboveAprox(Space g, Space universe, int level) {

        return aboveAproxIter(g, universe, true, level);
    }

    /**
     * @param g The geometry to construct a below aprox. from as a bintree
     * @param universe The geometric representation of the universe
     * @param level The resolution depth that the bintree should have
     * @return A bintree of representing the area of g at resolution depth level, aproximated from below.
     */
    public PTBintree makeBelowAprox(Space g, Space universe, int level) {

        return belowAproxIter(g, universe, true, level);
    }
}
