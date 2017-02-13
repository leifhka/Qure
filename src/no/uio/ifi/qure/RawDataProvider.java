package no.uio.ifi.qure;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;


public interface RawDataProvider {

    public Set<Integer> getInsertURIs();

    public Map<Integer, String> getExternalOverlapping(String whereClause);

    public String getUniverse();

    public Map<Integer,String> getSpaces();
    
    public Map<Integer,String> getSpaces(Set<Integer> uris);

    public Map<Block, Block> getEvenSplits();
}


