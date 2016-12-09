package no.uio.ifi.qure;

import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.Set;


public interface RawDataProvider {

    public Set<Integer> getInsertURIs();

    public Map<Integer, List<String>> getExternalOverlapping(String whereClause);

    public List<String> getUniverse();

    public Map<Integer, List<String>> getSpaces();
    
    public Map<Integer, List<String>> getSpaces(Set<Integer> uris);

    public Map<Block, Block> getEvenSplits();
}


