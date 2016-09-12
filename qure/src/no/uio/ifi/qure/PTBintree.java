package no.uio.ifi.qure;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.List;

public class PTBintree implements Bintree {

    private final PTBintree left;
    private final PTBintree right;
    private final boolean overlapsBlock;
    private final PTBintreeFactory bf;

    public PTBintree(PTBintree left, PTBintree right, boolean overlapsBlock, PTBintreeFactory bf) {

        this.left = left;
        this.right = right;
        this.overlapsBlock = overlapsBlock;
        this.bf = bf;
    }

    public String toString() {

        if (isTop())
	    return "00";
	if (isEmpty())
	    return "nil";

	String s = "";
	s += (isLeft()) ? "1" : "0";
	s += (isRight()) ? "1" : "0";

	if (isLeft())
	    s += left().toString();
	if (isRight())
	    s += right().toString();

	return s;
    }

    public PTBintree left() {
	if (isTop())
            return bf.makeTop();
        else if (left == null)
            return bf.makeEmpty();
        else
            return left;
    }

    public PTBintree right() {
	if (isTop())
            return bf.makeTop();
        else if (right == null)
            return bf.makeEmpty();
        else
            return right;
    }

    public boolean isLeft() {
	return overlapsBlock && (!left().isEmpty());
    }

    public boolean isRight() {
	return overlapsBlock && (!right().isEmpty());
    }

    public boolean isTop() {
	return overlapsBlock && (left == null) && (right == null);
    }

    public boolean isEmpty() {
	return !overlapsBlock;
    }	

    public boolean isBlock() {

        if (isTop())
            return true;
        else if (isLeft())
            return (!isRight()) && left().isBlock();
        else if (isRight())
            return right().isBlock();
        else
            return false;
    }

    public int depth() {

	if (isEmpty())
	    return -1;
	if (isTop())
	    return 0;

	int l = (left().isEmpty()) ? 0 : left().depth();
	int r = (right().isEmpty()) ? 0 : right().depth();

	return 1 + Math.max(l,r);
    }

    public int size() {
        
        if (isTop() || isEmpty())
            return 1;
        else
            return 1 + (left().isEmpty() ? 1 : left().size()) + (right().isEmpty() ? 1 : right().size());
    }

    // public PTBintree allToDepth(int n) {

    //     if (isEmpty())
    //         return bf.makeEmpty();
    //     if (n <= 0)
    //         return bf.makeTop();

    //     PTBintree nLeft = (isLeft()) ? left().allToDepth(n-1) : null;
    //     PTBintree nRight = (isRight()) ? right().allToDepth(n-1) : null;

    //     return new PTBintree(nLeft, nRight, overlapsBlock, bf);
    // }

    // public PTBintree appendToAll(Bintree b) {

    //     if (isEmpty())
    //         return bf.makeEmpty();
    //     if (isTop())
    //         return (PTBintree) b;

    //     PTBintree nLeft = (isLeft()) ? left().appendToAll(b) : null; 
    //     PTBintree nRight = (isRight()) ? right().appendToAll(b) : null; 

    //     return new PTBintree(nLeft, nRight, overlapsBlock, bf);
    // }

    public PTBintree appendToOne(Bintree b) {
    
        if (isEmpty())
            return bf.makeEmpty();
        if (isTop())
            return (PTBintree) b;

        PTBintree nLeft, nRight;
        if (isLeft()) {
 	    nLeft = left().appendToOne(b);
	    nRight = (isRight()) ? right() : null;
        } else {
 	    nRight = right().appendToOne(b);
	    nLeft = null;
        }

	return new PTBintree(nLeft, nRight, overlapsBlock, bf);
    }

    public PTBintree pickOneBranch(int level) {

        if (isEmpty())
            return bf.makeEmpty();
        if (isTop() && level <= 0)
            return bf.makeTop();
        
        PTBintree nLeft = null;
        PTBintree nRight = null;
        if (isLeft())
            nLeft = left().pickOneBranch(level-1);
        else
            nRight = right().pickOneBranch(level-1);
    
        return new PTBintree(nLeft, nRight, true, bf);
    } 

    // public PTBintree[] toBlocks(int level) {

    //     boolean wasEm = isEmpty(); //R
    //     PTBintree rest = this.normalize();
    //     
    //     List<PTBintree> result = new ArrayList<PTBintree>();
    //     if (!wasEm && rest.isEmpty()) System.out.println("ERROR4: empty after norm!"); //R

    //     while (!rest.isEmpty()) {
    //         PTBintree branch = rest.pickOneBranch(level);
    //         result.add(branch);
    //         rest = rest.minus(branch).normalize(); //Need to normalize for the emptiness check to pass.
    //     }

    //     return result.toArray(new PTBintree[result.size()]);
    // }

    public PTBintree[] toBlocks() {

        boolean wasEm = isEmpty(); //R

        PTBintree rest = this.normalize();
        if (!wasEm && rest.isEmpty()) System.out.println("ERROR4: empty after norm!"); //R

        Set<PTBintree> blocks = toBlocksRec();

        return blocks.toArray(new PTBintree[blocks.size()]);
    }

    private Set<PTBintree> toBlocksRec() {

        HashSet<PTBintree> res = new HashSet<PTBintree>();

        if (isEmpty()) {
            return res;
        } else if (isTop()) {
            res.add(this);
            return res;
        } else {
            if (isLeft()) {
                for (PTBintree b : left().toBlocksRec())
                    res.add(new PTBintree(b, null, true, bf));
            } 
            if (isRight()) {
                for (PTBintree b : right().toBlocksRec())
                    res.add(new PTBintree(null, b, true, bf));
            }
            
            return res;
        }
    }

    public PTBintree getParentBlock() {

        if (isLeft()) {
            if (isRight()) return null;
            
            if (left().isTop()) return bf.makeTop();
            
            return new PTBintree(left().getParentBlock(), null, true, bf);

        } else if (isRight()) {
            
            if (right().isTop()) return bf.makeTop();
            
            return new PTBintree(null, right().getParentBlock(), true, bf);
        } else {
            return null;
        }
    }

    public PTBintree getNeighborBlock() {
        
        if (isLeft()) {
            if (isRight()) return null;

            if (left().isTop()) return new PTBintree(null, bf.makeTop(), true, bf);

            return new PTBintree(left().getNeighborBlock(), null, true, bf);

        } else if (isRight()) {

            if (right().isTop()) return new PTBintree(bf.makeTop(), null, true, bf);

            return new PTBintree(null, right().getNeighborBlock(), true, bf);
        } else {
            return null;
        }
    }

    public PTBintree[] splitBlock() {

        if (isTop()) {
            return new PTBintree[]{new PTBintree(bf.makeTop(), null, true, bf),
                                   new PTBintree(null, bf.makeTop(), true, bf)};
        } else if (isLeft()) {
            if (isRight()) return null;

            PTBintree[] lsplit = left().splitBlock();
            return new PTBintree[]{new PTBintree(lsplit[0], null, true, bf),
                                   new PTBintree(lsplit[1], null, true, bf)};
        } else if (isRight()) {

            PTBintree[] rsplit = right().splitBlock();
            return new PTBintree[]{new PTBintree(null, rsplit[0], true, bf),
                                   new PTBintree(null, rsplit[1], true, bf)};
        } else {
            return null;
        }
    }

    public PTBintree normalize() {

	if (isTop() || isEmpty())
	    return this;

        PTBintree nLeft = left().normalize();
	PTBintree nRight = right().normalize();

	if (nLeft.isTop() && nRight.isTop()) {

	    return bf.makeTop();

	} else {

            if (nLeft.isEmpty())
                nLeft = null;
            if (nRight.isEmpty())
                nRight = null;

            boolean nOverlapsBlock = (nLeft != null) || (nRight != null);
	    return new PTBintree(nLeft, nRight, nOverlapsBlock, bf);
	}
    }

    public int hashCode() {
        return toString().hashCode();
    }

    public boolean equals(Object b) {

        if (!(b instanceof PTBintree))
            return false;
	if (b == null)
            return (isEmpty()) ? true : false;

        PTBintree bpt = (PTBintree) b;
    
        if (bpt.isTop())
            return (isTop()) ? true : false;
	
	return (isEmpty() == bpt.isEmpty()) &&
	    ((left != null) ? left.equals(bpt.left()) : bpt.left().isEmpty()) &&
	    ((right != null) ? right.equals(bpt.right()) : bpt.right().isEmpty());
    }

    // public PTBintree intersection(Bintree b) {
    //     
    //     if (b == null)
    //         return bf.makeEmpty();
    //     if (isTop() || b.isEmpty())
    //         return (PTBintree) b;
    //     if (b.isTop() || isEmpty())
    //         return this;

    //     PTBintree nLeft, nRight;
    //     nLeft = (isLeft()) ? left().intersection(b.left()) : null;
    //     nRight = (isRight()) ? right().intersection(b.right()) : null;
    //     boolean nNotOverlapsBlock = (nLeft == null || nLeft.isEmpty()) &&
    //                                 (nRight == null || nRight.isEmpty());

    //     return new PTBintree(nLeft, nRight, !nNotOverlapsBlock, bf);
    // }
    
    public PTBintree union(Bintree o) {

        PTBintree b = (PTBintree) o;

        if (b == null || b.isEmpty() || isTop())
            return this;
        if (b.isTop() || isEmpty())
            return (PTBintree) b;

        PTBintree nLeft, nRight;
        nLeft = (isLeft()) ? left().union(b.left()) : (PTBintree) b.left();
        nRight = (isRight()) ? right().union(b.right()) : (PTBintree) b.right();

        return new PTBintree(nLeft, nRight, true, bf);
    }

    // public PTBintree complement() {

    //     if (isTop())
    //         return bf.makeEmpty();
    //     if (isEmpty())
    //         return bf.makeTop();

    //     PTBintree nLeft, nRight;
    //     nLeft = (isLeft()) ? left().complement() : bf.makeTop();
    //     nRight = (isRight()) ? right().complement() : bf.makeTop();
    //     
    //     if (nLeft.isEmpty())
    //         nLeft = null;
    //     if (nRight.isEmpty())
    //         nRight = null;

    //     boolean nOverlapsBlock = nLeft != null || nRight != null;

    //     return new PTBintree(nLeft, nRight, nOverlapsBlock, bf);
    // }

    // public PTBintree minus(Bintree b) {

    //     if (b == null)
    //         return this;

    //     return this.intersection(b.complement());
    // }

    public boolean overlaps(Bintree o) {

        PTBintree b = (PTBintree) o;

        if (b == null || b.isEmpty() || isEmpty())
            return false;
        else if (isTop() || b.isTop())
            return true;
        else
            return left().overlaps(b.left()) || right().overlaps(b.right());
    }

    public boolean partOf(Bintree o) {

        PTBintree b = (PTBintree) o;

        if (isEmpty())
            return true;
        else if (b == null || b.isEmpty())
            return false;
        else if (b.isTop())
            return true;
        else
            return left().partOf(b.left()) && right().partOf(b.right());
    }

    /**
     * Constructs a collection of BIBintrees, each of size less than bits,
     * such that their union represents the same bintree as this.
     * !!! Not tested !!!
     */
    // public Collection<BIBintree> toBIBintrees(int bits) {

    //     ArrayList<Block> bs = new ArrayList<Block>();
    //     toISBintreeBlocks(Block.TOPBLOCK, bs);
    //     bs.sort(Block.getComparator());
    //     
    //     ArrayList<BIBintree> bibs = new ArrayList<BIBintree>(bs.size());

    //     for (Block b : bs)
    //         bibs.add(b.asBIBintree());

    //     ArrayList<BIBintree> res = new ArrayList<BIBintree>();

    //     BIBintreeFactory bibf = new BIBintreeFactory();

    //     BIBintree union = bibs.get(0);
    //     BIBintree prev = union;
    //     bibs.remove(0);

    //     for (BIBintree bib : bibs) {
    //         union = prev.union(bib);
    //         if (union.size() > bits) {
    //             res.add(prev);
    //             union = bib;
    //         }
    //     }

    //     res.add(union);

    //     return res;
    // }

    public ISBintree toISBintree() {

        ISBintreeFactory isbf = new ISBintreeFactory();
        ArrayList<Block> bs = new ArrayList<Block>();
        toISBintreeBlocks(Block.TOPBLOCK, bs);
        Block[] blocks = bs.toArray(new Block[bs.size()]);
        return new ISBintree(blocks, isbf);
    }

    public Collection<Block> toBlockSet() {

        Block[] isbArr = toISBintree().getRepresentation();
        Collection<Block> res = new ArrayList<Block>();
        for (Block b : isbArr) res.add(b);
        return res;
    }

    public Collection<Block> toBlockSet(Bintree witness) {

        PTBintree[] ptb = toBlocks();
        Collection<Block> res = new ArrayList<Block>();
        for (PTBintree b : ptb) {
        
            Block isb = b.toISBintree().getRepresentation()[0];
            if (witness.partOf(b)) isb.setWitness(true);
            res.add(isb);
        }
        return res;
    }

    private void toISBintreeBlocks(Block b, ArrayList<Block> bs) {

        if (isTop()) {
            bs.add(b);
            return;
        }
        if (isEmpty()) {
            return;
        }

        if (isLeft()) {
            left().toISBintreeBlocks(b.addZero(), bs);
        }
        if (isRight()) {
            right().toISBintreeBlocks(b.addOne(), bs);
        }
    }

    public String[] asDBStrings() {
        return toISBintree().asDBStrings();
    }

    public String[] asDBStrings(Bintree witness) {
        return toISBintree().asDBStrings(witness);
    }
}

