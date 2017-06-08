package no.uio.ifi.qure.util;

public class Pair<A,B> {

	public final A fst;
	public final B snd;

	public Pair(A fst, B snd) {
		this.fst = fst;
		this.snd = snd;
	}

	public String toString() {
		return "(" + fst.toString() + ", " + snd.toString() + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Pair)) return false;
		Pair<?,?> op = (Pair<?,?>) o;
		return fst.equals(op.fst) && snd.equals(op.snd);
	}

	@Override
	public int hashCode() {
		return fst.hashCode() + snd.hashCode();
	}
}
