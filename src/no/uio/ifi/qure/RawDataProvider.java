package no.uio.ifi.qure;

import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.Set;

public interface RawDataProvider<E> {

    public Set<Integer> getInsertURIs();

    public UnparsedIterator<E> getExternalOverlapping(String whereClause);

    public UnparsedSpace<E> getUniverse();

    public UnparsedIterator<E> getSpaces();
    
    public UnparsedIterator<E> getSpaces(Set<Integer> uris);

    public Map<Block, Block> getEvenSplits();
}


