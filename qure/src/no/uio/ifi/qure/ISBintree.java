package no.uio.ifi.qure;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class ISBintree implements Bintree {

    private final ISBintreeFactory bf;
    private final Block[] bt;
    private final boolean normalized;
    private int hashCode;
    private boolean hashCodeSet;

    public ISBintree(Block[] bt, ISBintreeFactory bf) {
	this.bt = bt;
	this.bf = bf;
        normalized = false;
    }

    private ISBintree(Block[] bt, ISBintreeFactory bf, boolean normalized) {
	this.bf = bf;
	this.bt = bt;
	this.normalized = normalized;
    }

    public Block[] getRepresentation() {
	return bt;
    }
    
    public String toString() {
	return Arrays.toString(bt);
    }

    public Set<Block> toBlockSet() {
        Set<Block> res = new HashSet<Block>();
        for (Block b : bt)
            res.add(b);
        return res;
    }

    public Set<Block> toBlockSet(Bintree wit) {
        Set<Block> res = new HashSet<Block>();
        for (Block b : bt)
            res.add(b);
        return res;
    }

    @Override
    public int hashCode() {

        if (!hashCodeSet) {
            hashCode = 0;
            for (int i = 0; i < bt.length; i++) {
                hashCode += bt[i].hashCode();
            }
            hashCodeSet = true;
        }
        return hashCode;
    }
    
    public ISBintree left() {

	int count = 0;

	for (int i = 0; i < bt.length; i++) {
	    if (bt[i].isBlockLeft())
		count++;
	}

	Block[] bn = new Block[count];
	int j = 0;
	for (int i = 0; i < bt.length; i++) {
	    if (bt[i].isBlockLeft())
		bn[j++] = bt[i].shiftRight(1);
	}

	return new ISBintree(bn,bf);
    }

    public ISBintree right() {
	
	int count = 0;

	for (int i = 0; i < bt.length; i++) {
	    if (bt[i].isBlockRight())
		count++;
	}

	Block[] bn = new Block[count];
	int j = 0;
	for (int i = 0; i < bt.length; i++) {
	    if (bt[i].isBlockRight())
		bn[j++] = bt[i].shiftRight(1);
	}

	return new ISBintree(bn,bf);
    }

    public boolean isLeft() {

	for (int i = 0; i < bt.length; i++) {
	    if (bt[i].isBlockLeft())
		return true;
	}
	return false;
    }

    public boolean isRight() {
	
	for (int i = 0; i < bt.length; i++) {
	    if (bt[i].isBlockRight())
		return true;
	}
	return false;
    }

    public boolean isTop() {
	return equals(bf.makeTop());
    }

    public boolean isEmpty() {
	return equals(bf.makeEmpty());
    }

    public boolean isBlock() {
        return bt.length == 1;
    }

    public int depth() {

	int depth = 0;

	for (Block b : bt) {
	    if (b.getSize() > depth)
		depth = b.getSize();
	}

	return depth;
    }

    // public ISBintree[] toBlocks(int level) {

    //     //ISBintree rest = this.normalize(); //Need to normalize to remove empty blocks
    //     ISBintree rest = this.allToDepth(level);
    //     Block[] bbs = rest.getRepresentation();

    //     ISBintree[] result = new ISBintree[bbs.length];

    //     for (int i = 0; i < result.length; i++) {
    //         result[i] = new ISBintree(new Block[]{bbs[i]}, bf);
    //     }

    //     return result;
    // }

    public ISBintree[] toBlocks() {

        ISBintree rest = this.normalize();
        ISBintree[] res = new ISBintree[bt.length];
        for (int i = 0; i < bt.length; i++)
            res[i] = new ISBintree(new Block[]{bt[i]}, bf);

        return res;
    }


    // public ISBintree pickOneBranch(int level) {

    //     Block nb = bt[0].clone();

    //     for (int i = level - nb.getSize(); i > 0; i--) {
    //         nb = nb.addZero();
    //     }

    //     return new ISBintree(new Block[]{nb}, bf);
    // }

    public ISBintree appendToOne(Bintree ob) {
        
        Block[] obrep = ((ISBintree) ob).getRepresentation();
        Block[] resBlocks = Arrays.copyOf(bt, bt.length + obrep.length - 1);
        int btLen = bt.length;
        Block tbl = resBlocks[btLen-1];

        int i = btLen-1;
    
        for (Block obl : obrep) {
            resBlocks[i++] = tbl.append(obl);
        }
        
        return new ISBintree(resBlocks, bf);
    }

    public ISBintree getParentBlock() {

        if (!isBlock()) return null;

        return new ISBintree(new Block[]{bt[0].getParent()}, bf);
    }

    public ISBintree getNeighborBlock() {

        if (!isBlock()) return null;

        return new ISBintree(new Block[]{bt[0].getNeighbor()}, bf);
    }

    public ISBintree[] splitBlock() {

        if (!isBlock()) return null;

        Block[] lr = bt[0].split();
        ISBintree l = new ISBintree(new Block[]{lr[0]}, bf);
        ISBintree r = new ISBintree(new Block[]{lr[1]}, bf);

        return new ISBintree[]{l, r};
    }

    public int size() {
    
        int size = 0;

        for (Block b : bt) {
            size = size + b.getSize();
        }
    
        return size;
    }

    public ISBintree normalize() {

	if (normalized)
	    return this;

	Set<Block> bn = new HashSet<Block>();
        
        //Remove all empty blocks and add to bn
	for (Block b : bt) {
            if (!b.isEmpty())
	        bn.add(b);
        }
        
        Block[] bnc = bn.toArray(new Block[bn.size()]); //Make an array copy of bn for iteration

        //Then, merge neighbouring blocks
        boolean merged = true;
        
        while (merged) {
            merged = false;
	    for (int i = 0; i < bnc.length; i++) {
	        for (int j = i+1; j < bnc.length; j++) {
	    	    if (bnc[i].isNeighbours(bnc[j])) {
	    	        bn.remove(bnc[i]);
	    	        bn.remove(bnc[j]);
	    	        bn.add(bnc[i].getParent());
                        merged = true;
                        break;
	    	    }
	        }
	    }
            bnc = bn.toArray(new Block[bn.size()]);
        }
        
        //Lastly, remove all blocks that are a part of some other block
	for (int i = 0; i < bnc.length; i++) {
	    for (int j = i+1; j < bnc.length; j++) {
		if (bnc[i].blockPartOf(bnc[j])) {
		    bn.remove(bnc[i]);
		} else if (bnc[j].blockPartOf(bnc[i])) {
		    bn.remove(bnc[j]);
		}
	    }
	}

	bnc = bn.toArray(new Block[bn.size()]);

	return new ISBintree(bnc, bf, true);
    }

    public boolean equals(Object b) {

        if (!(b instanceof ISBintree))
            return false;

	Block[] bbt = ((ISBintree) b).getRepresentation();
	Block[] tbt = getRepresentation();

	boolean contained;

	for (int i = 0; i < tbt.length; i++) {
	    contained = false;
	    for (int j = 0; j < bbt.length; j++) {
		if (tbt[i].equals(bbt[j])) {
		    contained = true;
		    break;
		}
	    }
	    if (!contained) {
		return false;
	    }
	}

        return true;
    }

    // public ISBintree intersection(Bintree b) {

    //     Block[] btb = ((ISBintree) b).getRepresentation();
    //     ArrayList<Block> res = new ArrayList<Block>();

    //     Block r;
    //     for (int i = 0; i < bt.length; i++) {
    //         for (int j = 0; j < btb.length; j++) {
    //             r =  bt[i].intersectBlocks(btb[j]);
    //             if (!r.isEmpty())
    //                 res.add(r);
    //         }
    //     }

    //     return new ISBintree(res.toArray(new Block[res.size()]),bf);
    // }

    private static Block[] concatenate(Block[] b1, Block[] b2) {

        // int equal = 0;

        // Block[] b2copy = Arrays.copyOf(b2, b2.length);

        // for (int i = 0; i < b2.length; i++) {
        //     for (int j = 0; j < b1.length; j++) {
        //         if (b2[i].equals(b1[j])) {
        //             equal++;
        //             b2copy[i] = null;
        //             break;
        //         }
        //     }
        // }

	// Block[] res = Arrays.copyOf(b1, b1.length + b2.length - equal);

        // int i = 0;
        // int j = 0;
	// while (i < b2.length - equal) {
        //     if (b2copy[j] != null) {
	//         res[i+b1.length] = b2copy[j];
        //         i++;
        //     } 
        //     j++;
	// }

        Set<Block> resSet = new HashSet<Block>();
        for (int i = 0; i < b1.length; i++)
            resSet.add(b1[i]);

        for (int i = 0; i < b2.length; i++)
            resSet.add(b2[i]);

	return resSet.toArray(new Block[resSet.size()]);
    }
  
    public ISBintree union(Bintree b) {

	Block[] brep = ((ISBintree) b).getRepresentation();
	return new ISBintree(concatenate(bt, brep), bf);
    }

    // public ISBintree unionAll(Bintree[] bs) {

    //     ISBintree res = bf.makeEmpty();

    //     for (Bintree b : bs) {
    //         res = res.union(b);
    //     }

    //     return res;
    // }

    // public ISBintree minus(Bintree b) {

    //     Block[] bbt = ((ISBintree) b).getRepresentation();
    //     Block[] btc = Arrays.copyOf(bt, bt.length);

    //     ArrayList<Block> btAL;
    //     Block[] minus;

    //     for (int i = 0; i < bbt.length; i++) {
    //         btAL = new ArrayList<Block>();
    //         for (int j = 0; j < btc.length; j++) {
    //             minus = btc[j].blockMinus(bbt[i]);
    //             for (Block mb : minus)
    //                 btAL.add(mb);
    //         }
    //         btc = btAL.toArray(new Block[btAL.size()]);
    //     }

    //     return new ISBintree(btc, bf);
    // }

    // public ISBintree minus(Bintree b) {

    //     Block[] brep = ((ISBintree) b).getRepresentation();
    //     Block[] res = Arrays.copyOf(bt, bt.length);
    //     Block[] minus;

    //     for (int i = 0; i < brep.length; i++) {
    //         for (int j = 0; j < res.length; j++) {
    //     	if (res[j].blockPartOf(brep[i])) {
    //     	    res[j] = Block.getEmptyBlock();
    //     	} else if (res[j].isEmpty() || !(brep[i].blockPartOf(res[j]))) {
    //     	    continue;
    //     	} else {
    //     	    minus = res[j].blockMinus(brep[i]);
    //     	    res[j] = Block.getEmptyBlock();
    //     	    res = concatenate(res,minus);
    //     	    break;
    //     	}
    //         }
    //     }

    //     return new ISBintree(res,bf);
    // }

    // public ISBintree complement() {
    //     return bf.makeTop().minus(this);
    // }

    public boolean overlaps(Bintree b) {

        if (b == null || b.isEmpty() || isEmpty())
            return false;
        else if (b.isTop() || isTop())
            return true;
        else {
            for (Block b1 : getRepresentation()) {
                for (Block b2 : ((ISBintree) b).getRepresentation()) {
                    if (b1.blockOverlaps(b2))
                        return true;
                }
            }
            return false;
        }
    }

    public boolean partOf(Bintree b) {

        if (isEmpty())
            return true;
        else if (b == null || b.isEmpty())
            return false;
        else if (b.isTop())
            return true;
        else {
            boolean found;
            for (Block b1 : getRepresentation()) {
                found = false;
                for (Block b2 : ((ISBintree) b).getRepresentation()) {
                    if (b1.blockPartOf(b2)) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    return false;
            }
            return true;
        }
    }

    // public ISBintree allToDepth(int n) {

    //     ISBintree[] toAppend;
    //     ArrayList<Block> res = new ArrayList<Block>();
    //     
    //     for (int i = 0; i < bt.length; i++) {
    //         int s = bt[i].getSize();
    //         if (s >= n) {
    //             res.add(bt[i].getParent(s-n));
    //         } else {
    //             toAppend = bf.makeNDistinct(Math.toIntExact(Math.round(Math.pow(2,n - bt[i].getSize()))));
    //             for (int j = 0; j < toAppend.length; j++) {
    //                 res.add(bt[i].append(toAppend[j].getRepresentation()[0]));
    //             }
    //         }
    //     }

    //     return new ISBintree(res.toArray(new Block[res.size()]), bf);
    // }
 
    // public ISBintree appendToAll(Bintree o) {

    //     Block[] obt = ((ISBintree) o).getRepresentation();

    //     Block[] res = new Block[obt.length*bt.length];

    //     for (int i = 0; i < obt.length; i++) {
    //         for (int j = 0; j < bt.length; j++) {
    //     	res[bt.length*i + j] = bt[j].append(obt[i]);
    //         }
    //     }
    //     
    //     return new ISBintree(res,bf);
    // }

    public String[] asDBStrings() {

        String[] s = new String[bt.length];

        for (int i = 0; i < s.length; i++) {
            s[i] = "" + bt[i].asCompactMortonBlock();
       }

       return s;
    }

    public String[] asDBStrings(Bintree witness) {

        Block ub = ((ISBintree) witness).getRepresentation()[0];
        String[] s = new String[bt.length];

        for (int i = 0; i < s.length; i++) {
            if (ub.blockPartOf(bt[i]))
                s[i] = "" + bt[i].asCompactMortonBlockWitness();
            else
                s[i] = "" + bt[i].asCompactMortonBlock();
       }

       return s;
    }
}
