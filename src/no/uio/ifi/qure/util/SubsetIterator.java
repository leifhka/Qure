package no.uio.ifi.qure.util;

import java.util.*;

public class SubsetIterator<T> implements Iterator<Set<T>> {
	private int minLen, maxLen, toGet, index, maxIndex;
	private Set<T> next;
	private List<T> elems;
	
	public SubsetIterator(Set<T> set, int minLen, int maxLen) {
		this.minLen = minLen;
		this.maxLen = maxLen;
		elems = new ArrayList<T>(set);
		toGet = (1 << minLen) - 1; // Smallest number containing minLen 1s
		index = toGet;
		maxIndex = 1 << set.size();
		setNextElem();
	}
	public boolean hasNext() {
		return next != null;
	}
	private void setNextElem() {
		next = null;
		while (index < maxIndex && next == null) {
			int num1s = Integer.bitCount(toGet);
			if (minLen <= num1s && num1s <= maxLen) {
				next = getCurrentSubset();
			}
			index++;
			toGet++;
		}
	}
	
	public Set<T> next() {
		Set<T> res = next;
		setNextElem();
		return res;
	}

	private Set<T> getCurrentSubset() {
		Set<T> result = new HashSet<T>();
		int toGetC = toGet;
		for (int i = 0; i < 32; i++) {
			if (toGetC % 2 != 0) {
				result.add(elems.get(i));
			}
			toGetC = toGetC >> 1;
		}
		return result;
	}
}
