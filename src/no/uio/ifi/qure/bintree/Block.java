package no.uio.ifi.qure.bintree;

import java.util.Comparator;
import java.util.Arrays;

public class Block {

	// Total number of bits per block.
	private static int BLOCK_SIZE = Long.SIZE - 1;

	// Total number of bits for size meta information (how many bits needed to store the length of bit-string)
	private static int SIZE_META_SIZE = Integer.SIZE - Integer.numberOfLeadingZeros(BLOCK_SIZE);

	// Total number of bits for role and unique part flag information, default 1 (only flag)
	private static int ROLE_META_SIZE = 1;

	// Total number of bits for meta information (default size + unique part flag)
	private static int META_SIZE = SIZE_META_SIZE + ROLE_META_SIZE;

	// Maximum number of bits reserved for the bit-string.
	private static int MAX_SIZE = BLOCK_SIZE - META_SIZE;

	// Long that if bitwise-AND-ed with a block returns a long containing only the meta-information.
	private static long META_SIZE_ONES = (1L << META_SIZE) - 1;

	public static Block EMPTYBLOCK = new Block(0,-1L);
	public static Block TOPBLOCK = new Block(0,0L);

	public static void setBlockSize(int size, int numRoles) { 
		BLOCK_SIZE = size;
		SIZE_META_SIZE = Integer.SIZE - Integer.numberOfLeadingZeros(BLOCK_SIZE);
		ROLE_META_SIZE = 1 + numRoles;
		META_SIZE = SIZE_META_SIZE + ROLE_META_SIZE;
		MAX_SIZE = BLOCK_SIZE - META_SIZE;
		META_SIZE_ONES = (1L << META_SIZE) - 1;
	}

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
		this.value =  futVal | (size << ROLE_META_SIZE); 
	}

	public Block addRole(int role) {
		return new Block(value | (((long) role) << 1)); // Shift past flag-bit
	}

	public int depth() {
		return getSize();
	}

	public int getSize() {
		return (int) ((value & META_SIZE_ONES) >> ROLE_META_SIZE);
	}

	public long getRepresentation() {
		return value;
	}

	/**
	 * Returns the i'th bit in the bit-string, where the 0'th bit is the most significant, that is,
	 * the bit representing the first split.
	 */
	public long getBit(int i) { return (getRawBitsShifted() >> (getSize()-(i+1))) & 1; }

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

	public long getRoles() {
		return (value & META_SIZE_ONES) >> 1;
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
		return new Block(TOPBLOCK.getRepresentation());
	}

	public static Block getEmptyBlock() {
		return new Block(EMPTYBLOCK.getRepresentation());
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

	public Block getParentAtDepth(int depth) {
		return getParent(depth - getSize());
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
			return getEmptyBlock();
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
		
		String vs = Long.toBinaryString(value);
		String zeros = "";
		for (int i = 0; i < BLOCK_SIZE - vs.length(); i++) zeros += "0";

		return zeros + vs;
	}

	/**
	 * @param n the number of distinct objects to return
	 * @return an array of n pairwise disjoint bintrees
	 */
	public static Block[] makeNDistinct(int n) {
		 
		double log2n = Math.log(n)/Math.log(2);
		int size = Math.toIntExact(Math.round(Math.ceil(log2n)));

		Block[] res = new Block[n];

		for (int i = 0; i < n; i++) {
			res[i] = new Block(size, i);
		}
		return res;
	}
 }
