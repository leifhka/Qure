package no.uio.ifi.qure.traversal;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Set;

import no.uio.ifi.qure.bintree.*;
import no.uio.ifi.qure.space.Space;

public class Representation {

    private Map<Integer, Bintree> representation;
    private Map<Block, Block> splits;
    private Space universe;

    public Representation() {
        representation = new HashMap<Integer, Bintree>();
        splits = new HashMap<Block, Block>();
    }

    public Representation(Map<Integer, Bintree> representation) {
        this.representation = representation;
        splits = new HashMap<Block, Block>();
    }

    public void setUniverse(Space universe) { this.universe = universe; }

    public Map<Block, Block> getEvenSplits() { return splits; }

    public void addSplitBlock(Block block, Block split) { splits.put(block, split); }

    public void addAllSplitBlocks(Map<Block, Block> newSplits) { splits.putAll(newSplits); }

    public Map<Integer, Bintree> getRepresentation() { return representation; }

    public Space getUniverse() { return universe; }

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

    public void addCovering(Set<SID> covering, Block block) {
        for (SID uri : covering)
            representation.put(uri.getID(), Bintree.fromBlock(block.setUniquePart(true).addRole(uri.getRole())));
    }
}
