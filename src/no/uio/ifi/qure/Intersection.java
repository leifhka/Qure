package no.uio.ifi.qure;

import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

public class Intersection {

    private Space space;
    private Set<SID> elems;

    public Intersection(Space space, Set<SID> elems) {

        this.space = space;
        this.elems = elems;
    }

    public Set<SID> getElements() { return elems; }

    public Space getSpace() { return space; }

    public int hashCode() {
        int res = 0;
        for (SID e : elems) res += e.hashCode();
        return res;
    }

    public boolean equals(Object o) {
        return (o instanceof Intersection) && elems.equals(((Intersection) o).getElements());
    }

    /**
     * Returns a new Intersection object representing the intersection
     * of all elements in this plus the new object e. Returns null
     * if intersection does not exists (is empty).
     */
    public Intersection add(SID e, SpaceProvider spaces) {

        if (elems.contains(e)) return null;

        Space s = spaces.get(e);
        Space nsp = null;

        try {
            nsp = space.intersection(s);
        } catch (Exception ex) {
            System.err.println(ex.toString());
            System.err.println("Offending uri: " + e);
            System.exit(1);
        }

        if (nsp.isEmpty())
            return null;

        Set<SID> nelems = new HashSet<SID>(elems);
        nelems.add(e);

        return new Intersection(nsp, nelems);
    }
}
