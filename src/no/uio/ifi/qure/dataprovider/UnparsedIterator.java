package no.uio.ifi.qure;

import java.util.Iterator;

public abstract class UnparsedIterator<E> implements Iterator<UnparsedSpace<E>> {

	abstract public boolean hasNext();

	abstract public UnparsedSpace<E> next();

	abstract public int size();
}
