package no.uio.ifi.qure;

import java.util.Set;
import java.util.Collection;

public interface Bintree {

    public String toString();

    /**
     * @return the bintree representing the left part of this tree.
     */    
    // public Bintree left();

    // /**
    //  * @return the bintree representing the right part of this tree.
    //  */
    // public Bintree right();

    // /**
    //  * @return true if the left subtree is non-empty, false otherwise.
    //  */
    // public boolean isLeft();

    // /**
    //  * @return true if the right subtree is non-empty, false otherwise.
    //  */
    // public boolean isRight();

    /**
     * @return true if the tree is equal to the factory's makeTop()-tree, false otherwise.
     */
    public boolean isTop();

    /**
     * @return true if the tree is equal to the factory's makeEmpty()-tree, false otherwise.
     */
    public boolean isEmpty();

    /**
     * @return true if this bintree is a block, i.e. has exactly one branch, false otherwise.
     */
     public boolean isBlock();

    /**
     * @return the maximum length from the root of the tree down to a leaf-node.
     */
    public int depth();

    public int size();

    /**
     * @return a bintree representing the same area in case n is larger than the depth of the shallowest
     *   branch, but where all branches have depth n. But will return an aproximation if n is less than
     *   the shallowst branch's depth.
     */
    //public Bintree allToDepth(int n);

    /**
     * @return a bintree where e is appended to every leaf-node of this tree.
     */
    //public Bintree appendToAll(Bintree e);

    /**
     * @return a bintree where e is appended to one leaf-node of this tree.
     */
    public Bintree appendToOne(Bintree e);

    /**
     * @return one block/branch of this bintree
     */
    //public Bintree pickOneBranch(int level);

    /**
     * @return an array of all blocks/paths in this bintree at depth level
     */
    //public Bintree[] toBlocks(int level);

    /**
     * @return an array of all blocks/path in this bintree
     */
     public Bintree[] toBlocks();

    /**
     * @return null if top or not a block, otherwise the bintree representing the parent bloc of this
     */
    public Bintree getParentBlock();

    /**
     * @return null if top or not a block, otherwise the bintree representing the neighbor block of this
     */
    public Bintree getNeighborBlock();

    /**
     * @return null if not a block, otherwise the pair children blocks of this bintree 
     */
    public Bintree[] splitBlock();

    /**
     * @return the equivanlent normalized bintree.
     */
    public Bintree normalize();

    public boolean equals(Object b);

    public int hashCode();

    //public Bintree intersection(Bintree b);

    public Bintree union(Bintree b);

    //public Bintree minus(Bintree b);

    //public Bintree complement();

    public boolean overlaps(Bintree b);

    public boolean partOf(Bintree b);

    public String[] asDBStrings();

    public String[] asDBStrings(Bintree witness);

    public Collection<Block> toBlockSet();

    public Collection<Block> toBlockSet(Bintree wit);
}

