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
    private Set<Integer> elems;

    public Intersection(Space space, Set<Integer> elems) {

        this.space = space;
        this.elems = elems;
    }

    public Set<Integer> getElements() { return elems; }

    public Space getSpace() { return space; }

    public int hashCode() {
        int res = 0;
        for (Integer e : elems) res += e.hashCode();
        return res;
    }

    public boolean equals(Object o) {
        return (o instanceof Intersection) && elems.equals(((Intersection) o).getElements());
    }

    public Intersection add(Integer e, SpaceProvider spaces) {

        Space s = spaces.get(e);
        Space nsp = null;

        try {
            nsp = space.intersection(s);
        } catch (Exception ex) {
            System.err.println(ex.toString());
            System.err.println("Offending uri: " + e);
            System.exit(1);
        }

        if (elems.contains(e) || nsp.isEmpty())
            return null;

        Set<Integer> nelems = new HashSet<Integer>(elems);
        nelems.add(e);

        return new Intersection(nsp, nelems);
    }

}
