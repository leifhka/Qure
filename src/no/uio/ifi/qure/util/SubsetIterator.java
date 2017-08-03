package no.uio.ifi.qure.util;

import java.util.*;
import java.math.BigInteger;

public class SubsetIterator<T> implements Iterator<Set<T>> {
	private int curLen, maxLen;
	private BigInteger toGet, maxToGet;
	private Set<T> next;
	private List<T> elems;
	
	public SubsetIterator(Set<T> set, int minLen, int maxLen) {
		this.maxLen = maxLen;
		elems = new ArrayList<T>(set);
		curLen = minLen;
		toGet = (BigInteger.ONE.shiftLeft(curLen)).subtract(BigInteger.ONE); // Smallest number containing minLen 1s
		maxToGet = toGet.shiftLeft(elems.size()-curLen);
	}
	
	public boolean hasNext() {
		return toGet.compareTo(maxToGet) <= 0;
	}
	
	private void setNextToGet() {
		if (toGet.compareTo(maxToGet) > 0 && curLen < maxLen) { 
			curLen++;
			toGet = (BigInteger.ONE.shiftLeft(curLen)).subtract(BigInteger.ONE); // Smallest number containing minLen 1s
			maxToGet = toGet.shiftLeft(elems.size()-curLen);
		} else {
			// From Bit Twidling Hacks. Sets toGet to be the (lexicographically) next integer with same number of 1-bits.
			BigInteger t = toGet.or(toGet.subtract(BigInteger.ONE)).add(BigInteger.ONE);
			// Next set to 1 the most significant bit to change, 
			// set to 0 the least significant ones, and add the necessary 1 bits.
			toGet = t.or(t.and(t.negate()).divide(toGet.and(toGet.negate())).shiftRight(1).subtract(BigInteger.ONE));
		}
	}
	
	public Set<T> next() {
		Set<T> res = getCurrentSubset();
		setNextToGet();
		return res;
	}

	private Set<T> getCurrentSubset() {
		Set<T> result = new HashSet<T>();
		BigInteger toGetC = toGet;
		for (int i = 0; i < elems.size(); i++) {
			if (toGetC.mod(BigInteger.valueOf(2)).compareTo(BigInteger.ZERO) != 0) {
				result.add(elems.get(i));
			}
			toGetC = toGetC.shiftRight(1);
		}
		return result;
	}
}
