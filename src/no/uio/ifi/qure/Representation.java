package no.uio.ifi.qure;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Set;

public class Representation {

    private Map<Integer, Bintree> representation;
    private Map<Block, Block> splits;
    private Space universe;
    private boolean isNormalized;

    public Representation(Map<Integer, Bintree> representation) {
        this.representation = representation;
        splits = new HashMap<Block, Block>();
        isNormalized = false;
    }

    public void setUniverse(Space universe) { this.universe = universe; }

    public Map<Block, Block> getEvenSplits() { return splits; }

    public void addSplitBlock(Block block, Block split) { splits.put(block, split); }

    public Map<Integer, Bintree> getRepresentation() { return representation; }

    public Space getUniverse() { return universe; }

    public void normalizeBintrees() {
        if (isNormalized) return;

        for (Integer uri : representation.keySet())
            representation.put(uri, representation.get(uri).normalize());

        isNormalized = true;
    }

    public Representation merge(Representation other) {

        Map<Integer, Bintree> orep = other.getRepresentation();

        for (Integer oid : orep.keySet()) {
            if (!representation.containsKey(oid))
                representation.put(oid, orep.get(oid));
            else 
                representation.put(oid, representation.get(oid).union(orep.get(oid)));
        }

        splits.putAll(other.getEvenSplits());

        return this;
    }

}
