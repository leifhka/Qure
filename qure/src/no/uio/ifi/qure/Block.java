package no.uio.ifi.qure;

import java.util.Comparator;
import java.util.Arrays;

public class Block {

    public static final Block EMPTYBLOCK = new Block(Long.SIZE,0L);
    public static final Block TOPBLOCK = new Block(0,0L);

    private final int size;
    private final long value;
    private boolean isWit;

    public Block(int size, long value) {
	this.value = value;
	this.size = size;
    }

    public int getSize() {
        return size;
    }

    public long getValue() {
        return value;
    }

    public void setWitness(boolean b) {
        isWit = b;
    }

    public boolean isWitness() {
        return isWit;
    }

    public int hashCode() {
        long allOnes = (1L << size) - 1;
        return (new Long(value & allOnes)).hashCode();
    }

    public boolean equals(Object o) {

        if (!(o instanceof Block))
            return false;

        Block b = (Block) o;

        if (isEmpty() && b.isEmpty())
            return true;

        if (size != b.getSize())
            return false;

        return equalUpTo(size, value, b.getValue());
    }

    public Block clone() {
        return new Block(size, value);
    }

    public static Block getTopBlock() {
        return TOPBLOCK;
    }

    public static Block getEmptyBlock() {
        return EMPTYBLOCK;
    }

    public boolean isBlockRight() {
	return isTop() || (value % 2 != 0);
    }
 
    public boolean isBlockLeft() {
	return isTop() || (value % 2 == 0);
    }
    
    public Block shiftLeft(int n) {

        return new Block(size+n, value << n);
    }

    public Block shiftRight(int n) {

        return new Block(size-n, value >> n);
    }

    public Block addZero() {
        // ANDs with bitstring containing only 1s except for the size'th bit
        // to ensure that the size'th bit is 0
        long allOneButLastIsZero = flipBit(size, ~0);
        return new Block(size + 1, value & allOneButLastIsZero);
    }

    public Block addOne() {
        // ORs with bitstring containing only 0s except for the (size+1)'th bit
        // to ensure that the (size+1)'th bit is 1
        long allZeroButLastIsOne = flipBit(size, 0);
        return new Block(size + 1, value | allZeroButLastIsOne);
    }

    public boolean isTop() {
        return (size == 0);
    }

    public boolean isEmpty() {
	return (size >= Long.SIZE);
    }

    public Block getParent(int n) {

        long parVal = value & ((1L << (size - n)) - 1);

        return new Block(size-n, parVal);
    }

    public Block getParent() {
        return getParent(1);
    }

    public Block getNeighbor() {

        long neiVal = flipBit(size-1, value);

        return new Block(size, neiVal);
    }

    public Block[] split() {

        Block l = new Block(size+1, value);
        Block r = new Block(size+1, value | (1L << size));

        return new Block[]{l, r};
    }
    
    public boolean isNeighbours(Block b) {

	if (size != b.size || isEmpty() || b.isEmpty())
	    return false;

        return equalUpTo(size, b.value, flipBit(size-1, value)); // Only the last bit should be different
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

    public Block[] blockMinus(Block b) {

        if (this.blockPartOf(b))
            return new Block[0];
        if (!b.blockPartOf(this))
            return new Block[]{this};

	int numBlocks = b.size - size; // Number of blocks in the result
	Block[] bs = new Block[numBlocks];
        long diff = b.getValue() >> size; // b's representation relative to this'
        
        // First create the bit suffixes to this' representation that should be in the result
        for (int i = numBlocks-1; i >= 0; i--) {
            bs[i] = new Block(i+1, flipBit(i,diff));
        }
        
        //Append the suffixes to this block
        for (int i = 0; i < numBlocks; i++) {
            bs[i] = this.append(bs[i]);
        }

	return bs;
    }

    public boolean blockPartOf(Block b) {

	if (isEmpty())
	    return true;
        if (size < b.size || b.isEmpty())
	    return false;
	
        return equalUpTo(b.size, value, b.value);
    }

    public boolean blockOverlaps(Block b) {
        return this.blockPartOf(b) || b.blockPartOf(this);
    }
    
    public Block append(Block b) {

        long ones = (1L << getSize()) - 1;
        long cleanValue = getValue() & ones;
        long newval = (b.getValue() << size) | cleanValue;
	return new Block(size + b.getSize(), newval);
    }

    public String toString() {
        
        if (isTop())
            return "e";
        else if (isEmpty())
            return "nil";

        String s = "";
        long valCopy = value;

        for (int i = 0; i < size; i++) {
            if (valCopy % 2 == 0)
                s = "0" + s;
            else
                s = "1" + s;
            
            valCopy = valCopy >> 1;
        }
        return s;
    }

    public static Comparator<Block> getComparator() {
        return new Comparator<Block>() {
            public int compare(Block b1, Block b2) {
                if (b1.isEmpty())
                    return (b2.isEmpty()) ? 0 : -1;
                else if (b1.isTop())
                    return (b2.isTop()) ? 0 : 1;
                else if (b2.isEmpty())
                    return 1;
                else if (b2.isTop())
                    return -1;
                else if (b1.isBlockLeft() && b2.isBlockRight())
                    return -1;
                else if (b1.isBlockRight() && b2.isBlockLeft())
                    return 1;
                else
                    return compare(b1.shiftRight(1), b2.shiftRight(1));
            }
        };
    }

    // public BIBintree asBIBintree() {
    //     
    //     BIBintreeFactory bibf = new BIBintreeFactory();

    //     if (isEmpty()) return bibf.makeEmpty();

    //     BIBintree bib = bibf.makeTop();
    //     long valc = value;

    //     for (int i = 0; i < size; i++) {
    //         if (valc % 2 == 0) 
    //             bib.setBit(2*i);
    //         else
    //             bib.setBit(2*i + 1);
    //     }

    //     return bib;
    // }
        

    public long asMortonBlock() {

        long res = 0;
        long valc = value;

        for (int i = 0; i < size; i++) {
            res = res << 1;
            if (valc % 2 != 0)
                res++;
            res = (res << 1) + 1;
            valc = valc >> 1;
        }

        res = res << ((Long.SIZE - size*2)-1);

        if (res < 0) throw new ArithmeticException("Negative morton block, size of block too large.");

        return res;
    }

    public long asCompactMortonBlock() {

        // We need to store a seq. of bits along with it lenght in 63 bits. 58 + log_2(58) > 63
        if (size > 57)
            throw new ArithmeticException("Size of block ("+size+") too large, cannot make blocks of size greater than 57.");

        long res = 0;
        long valc = value;

        // Reversing the order of the bits
        for (int i = 0; i < size; i++) {
            res = res << 1;
            if (valc % 2 != 0)
                res++;
            valc = valc >> 1;
        }

        // Pushing the value to the most significant bits (but not the sign-bit)
        res = res << (Long.SIZE - 1) - size;

        // Store the size in the least significant bits
        return res | (size << 1);
    }

    public long asCompactMortonBlockWitness() {

        // We need to store a seq. of bits along with it lenght in 63 bits. 58 + log_2(58) > 63
        if (size > 57)
            throw new ArithmeticException("Size of block ("+size+") too large, cannot make blocks of size greater than 57.");

        long res = 0;
        long valc = value;

        // Reversing the order of the bits
        for (int i = 0; i < size; i++) {
            res = res << 1;
            if (valc % 2 != 0)
                res++;
            valc = valc >> 1;
        }

        // Pushing the value to the most significant bits (but not the sign-bit)
        res = res << (Long.SIZE - 1) - size;

        // Store the size in the least significant bits
        return res | ((size << 1)+1);
    }

    private long flipBit(int nr, long val) {
        return val ^ (1L << nr);
    }

    private boolean equalUpTo(int size, long a, long b) {
        
        long allOnes = (1L << size) - 1;
        return (a & allOnes) == (b & allOnes);
    }
 }
