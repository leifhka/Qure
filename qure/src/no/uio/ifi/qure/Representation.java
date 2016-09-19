package no.uio.ifi.qure;

import java.util.Map;
import java.util.Collection;
import java.util.Set;

public class Representation {

    private Map<Integer, Bintree> representation;
    private Space universe;
    private boolean isNormalized;

    public Representation(Map<Integer, Bintree> representation) {
        this.representation = representation;
        isNormalized = false;
    }

    public void setUniverse(Space universe) { this.universe = universe; }

    public void normalizeBintrees() {
        if (isNormalized) return;

        for (Integer uri : representation.keySet())
            representation.put(uri, representation.get(uri).normalize());

        isNormalized = true;
    }

    public Map<Integer, Bintree> getRepresentation() { return representation; }

    public Space getUniverse() {
        return universe;
    }
}
