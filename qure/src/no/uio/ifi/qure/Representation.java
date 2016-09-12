package no.uio.ifi.qure;

import java.util.Map;
import java.util.Collection;
import java.util.Set;

public class Representation {

    //private Map<Integer, Bintree> representation;
    private Map<Integer, Collection<Block>> representation;
    private Space universe;
    private boolean isNormalized;
    //private Map<Integer, Bintree> witnesses;

    public Representation(Map<Integer, Collection<Block>> representation) {
        this.representation = representation;
        isNormalized = false;
    }

    // public Representation(Map<Integer, Set<Blocks>> representation, Map<Integer, Bintree> witnesses) {
    //     this.representation = representation;
    //     this.witnesses = witnesses;
    //     isNormalized = false;
    // }

    //public void setWitnesses(Map<Integer, Bintree> witnesses) { this.witnesses = witnesses; }

    public void setUniverse(Space universe) { this.universe = universe; }

    //public Map<Integer, Bintree> getWitnesses() { return witnesses; }

    //public Map<Integer, Bintree> getRawRepresentation() { return representation; }
    public Map<Integer, Collection<Block>> getRawRepresentation() { return representation; }

    // public void normalizeBintrees() {
    //     if (isNormalized) return;

    //     for (Integer uri : representation.keySet())
    //         representation.put(uri, representation.get(uri).normalize());

    //     isNormalized = true;
    // }

    public Map<Integer, Collection<Block>> getRepresentation() { return representation; }
    //public Map<Integer, Bintree> getRepresentation() {
    //    normalizeBintrees();
    //    return representation;
    //}

    public Space getUniverse() {
        return universe;
    }
}
