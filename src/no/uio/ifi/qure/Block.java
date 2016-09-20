package no.uio.ifi.qure;

import java.util.Comparator;
import java.util.Arrays;

public class Block {

    // Total number of bits per block.
    private static final int BLOCK_SIZE = Long.SIZE - 1;

    // Total number of bits for meta information (size + unique part flag)
    private static final int META_SIZE = 1 + (Integer.SIZE - Integer.numberOfLeadingZeros(BLOCK_SIZE));

    // Maximum number of bits reserved for the bit-string.
    private static final int MAX_SIZE = BLOCK_SIZE - META_SIZE;

    // Long that if bitwise-AND-ed with a block returns a long containing only the meta-information.
    private static final long META_SIZE_ONES = (1L << META_SIZE) - 1;

    public static final Block EMPTYBLOCK = new Block(0,-1L);
    public static final Block TOPBLOCK = new Block(0,0L);

    // The long-representation of this block.
    private final long value;

    public Block(long value) {
        this.value = value;
    }

    public Block(int size, long bits) {
        if (size > MAX_SIZE)
            throw new ArithmeticException("Size of block ("+size+") too large, max size of blocks is " + MAX_SIZE);

        // Pushing the value to the most significant bits (but not the sign-bit)
        long futVal = bits << (BLOCK_SIZE - size);

        // Store the size in the least significant bits and leave a bit for unique part-flag
        this.value =  futVal | (size << 1);
    }

    public int depth() {
        return getSize();
    }

    public int getSize() {
        return (int) ((value & META_SIZE_ONES) >> 1);
    }

    public long getRepresentation() {
        return value;
    }

    /**
     * Returns the long containing only the raw bitstring, unshifted.
     */
    public long getRawBits() {
        return value & (~META_SIZE_ONES);
    }

    /**
     * Returns the long containing only the raw bitstring, shifted
     * so that the bitstring is at the least-significant bits.
     */
    public long getRawBitsShifted() {
        return value >> (BLOCK_SIZE - getSize());
    }

    /**
     * Returns a block equalt to this, but with the unique-part flag
     * set if and only if argument is true.
     */
    public Block setUniquePart(boolean isUniquePart) {
        if (isUniquePart)
            return new Block(value | 1L);
        else
            return new Block(value & -2L);
    }

    public boolean isUniquePart() {
        return value % 2 != 0;
    }

    public int hashCode() {
        return (new Long(value)).hashCode();
    }

    public boolean equals(Object o) {

        if (!(o instanceof Block))
            return false;

        return value == ((Block) o).getRepresentation();
    }

    public static Block getTopBlock() {
        return TOPBLOCK;
    }

    public static Block getEmptyBlock() {
        return EMPTYBLOCK;
    }

    /**
     * Returns a block equal to this, but with a 0-bit added to the
     * bitstring, thus representing the left-child of this.
     */
    public Block addZero() {
        return new Block(getSize() + 1, getRawBitsShifted() << 1);
    }

    /**
     * Returns a block equal to this, but with a 1-bit added to the
     * bitstring, thus representing the right-child of this.
     */
    public Block addOne() {
        return new Block(getSize() + 1, (getRawBitsShifted() << 1) + 1);
    }

    public boolean isTop() {
        return getSize() == 0;
    }

    public boolean isEmpty() {
        return value < 0;
    }

    public Block getParent(int n) {

        return new Block(getSize() - n, getRawBitsShifted() >> n);
    }

    public Block getParent() {
        return getParent(1);
    }

    public Block getNeighbor() {

        long neiVal = getRawBitsShifted() ^ 1;

        return new Block(getSize(), neiVal);
    }

    public Block[] split() {

        Block l = addZero();
        Block r = addOne();

        return new Block[]{l, r};
    }
    
    public boolean isNeighbours(Block b) {

        return getSize() == b.getSize() &&
               getRawBitsShifted() == (b.getRawBitsShifted() ^ 1);
    }
    
    public Block intersectBlocks(Block b) {
    
        //Two blocks only overlap if one is part of the other
	if (blockPartOf(b))
	    return this;
	else if (b.blockPartOf(this))
	    return b;
	else
	    return Block.EMPTYBLOCK;
    }

    private int getTrailingBitsSize() {
        return BLOCK_SIZE - getSize();
    }

    private long getMaxContainedRepresentation() {
        return value | ((1L << getTrailingBitsSize()) - 1);
    }

    public boolean blockPartOf(Block b) {

	if (isEmpty())
	    return true;
        if (b.isEmpty())
	    return false;

        return (b.getRepresentation() & -2) <= getRepresentation() &&
               b.getMaxContainedRepresentation() >= getRepresentation();
    }

    public boolean blockOverlaps(Block b) {
        return this.blockPartOf(b) || b.blockPartOf(this);
    }
    
    /**
     * Returns a block representing the bitstring resulting from
     * appending the argument block's bitstring to this' bitstring.
     */
    public Block append(Block b) {

        long newBits = (getRawBitsShifted() << b.getSize()) | b.getRawBitsShifted();
	return new Block(getSize() + b.getSize(), newBits);
    }

    public String toString() {
        
        String zeros = "";
        for (int i = 0; i < Long.numberOfLeadingZeros(value); i++) zeros += "0";
        return zeros + Long.toBinaryString(value);
    }
 }