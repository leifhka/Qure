package no.uio.ifi.qure.bintree;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;


public class Bintree {

	private final Set<Block> bt;
	private final boolean normalized;
	private int hashCode;
	private boolean hashCodeSet;

	public Bintree() {
		this.bt = new HashSet<Block>();
		normalized = true;
	}

	public Bintree(Set<Block> bt) {
		this.bt = bt;
		normalized = false;
	}

	private Bintree(Set<Block> bt, boolean normalized) {
		this.bt = bt;
		this.normalized = normalized;
	}

	/**
	 * Returns the bintree consisting of only argument block
	 */
	public static Bintree fromBlock(Block block) {
	    Set<Block> nbt = new HashSet<Block>();
	    nbt.add(block);

	    return new Bintree(nbt);
	}

	/**
	 * Returns a set of blocks equal to argument, but where at most one block is a unique part.
	 */
	static private Set<Block> removeRedundantUniqueParts(Set<Block> bt) {
	    
	    Set<Block> nbt = new HashSet<Block>();
	    boolean picked = false;

	    for (Block b : bt) {
	 	   if (!picked && b.isUniquePart()) {
	 	 	  nbt.add(b.setUniquePart(true));
	 	 	  picked = true;
	 	   } else {
	 	 	  nbt.add(b.setUniquePart(false));
	 	   }
	    }

	    return nbt;
	}

	public String toString() {
		return bt.toString();
	}

	@Override
	public int hashCode() {

	    if (!hashCodeSet) {
	 	   hashCode = 0;
	 	   for (Block b : bt) {
	 	 	  hashCode += b.hashCode();
	 	   }
	 	   hashCodeSet = true;
	    }
	    return hashCode;
	}
	
	public boolean isTop() {
		if (bt.size() != 1) return false;

		Block b = getBlocks().iterator().next(); 
		return b.isTop();
	}

	public boolean isEmpty() {
		return bt.isEmpty();
	}

	public int depth() {

		int depth = 0;

		for (Block b : bt) {
			if (b.getSize() > depth)
			depth = b.getSize();
		}

		return depth;
	}

	public Set<Block> getBlocks() {
		return bt;
	}

	public int size() {
	
	    int size = 0;

	    for (Block b : bt) {
	 	   size = size + b.getSize();
	    }
	
	    return size;
	}

	/**
	 * Constructs the bintree representing the same space as this, but on normal form,
	 * that is, the bintree with the fewest number of blocks.
	 */
	public Bintree normalize() {

		if (normalized)
			return this;

		Set<Block> bn = new HashSet<Block>();
	    
		//Add only non-empty and non-contained blocks to bn
		for (Block b1 : bt) {
			if (!b1.isEmpty()) {
				boolean partOf = false;
				for (Block b2 : bt) {
					if (!b1.equals(b2) && b1.blockPartOf(b2)) {
						partOf = true;
						break;
					}
				}

				if (!partOf) bn.add(b1);
			}
		}
	    
		Set<Block> bnc = new HashSet<Block>(bn); //Make a copy of bn for iteration

		//Then, merge neighbouring blocks
		boolean merged = true;
	    
		while (merged) {
			merged = false;
			for (Block b1 : bnc) {
				for (Block b2 : bnc) {
					if (!b1.equals(b2) && b1.isNeighbours(b2)) {
						bn.remove(b1);
						bn.remove(b2);
						bn.add(b1.getParent().setUniquePart(b1.isUniquePart() || b2.isUniquePart()));
						merged = true;
						break;
					}
				}
			}
			bnc = new HashSet<Block>(bn);
		}
	    
		return new Bintree(bn, true);
	}

	/**
	 * Returns true if the this has exatcly the same blocks
	 * as argument. To test spatial equality, normalize the
	 * bintrees first.
	 */
	public boolean equals(Object o) {

	    if (!(o instanceof Bintree))
	 	   return false;

	    Bintree b = (Bintree) o; 

	    return bt.equals(b.getBlocks());
	}

	public Bintree intersection(Bintree b) {

	    Set<Block> res = new HashSet<Block>();

	    for (Block b1 : getBlocks()) {
	 	   for (Block b2 : b.getBlocks()) {
	 	 	  Block r =  b1.intersectBlocks(b2);
	 	 	  if (!r.isEmpty())
	 	 	 	 res.add(r);
	 	   }
	    }

	    return new Bintree(res);
	}

	public Bintree union(Bintree b) {

		Set<Block> rep = new HashSet<Block>(getBlocks());
		rep.addAll(b.getBlocks());

		return new Bintree(rep);
	}

	public boolean overlaps(Bintree b) {

		if (b == null || b.isEmpty() || isEmpty())
			return false;
		else if (b.isTop() || isTop())
			return true;
		else {
			for (Block b1 : getBlocks()) {
				for (Block b2 : b.getBlocks()) {
					if (b1.blockOverlaps(b2))
						return true;
				}
			}
			return false;
		}
	}

	/**
	 * Returns true of this is contained in argument.
	 * Note that this predicate is only correct for normalized bintrees.
	 */
	public boolean partOf(Bintree b) {

		if (isEmpty())
			return true;
		else if (b == null || b.isEmpty())
			return false;
		else if (b.isTop())
			return true;
		else {
			boolean found;
			for (Block b1 : getBlocks()) {
				found = false;
				for (Block b2 : b.getBlocks()) {
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
}
