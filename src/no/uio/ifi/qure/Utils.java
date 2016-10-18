package no.uio.ifi.qure;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class Utils {

    public static Set<Integer> getIntersecting(Space space, Set<Integer> elems,
                                               Map<Integer, ? extends Space> map) {

        Set<Integer> intersecting = new HashSet<Integer>();

        for (Integer uri : elems) {
            if (map.get(uri).intersects(space))
                intersecting.add(uri);
        }

        return intersecting;
    }

    public static Set<Integer> intersection(Set<Integer> s1, Set<Integer> s2) {
        Set<Integer> u = new HashSet<Integer>(s1);
        u.retainAll(s2);
        return u;
    }

    public static Set<Integer> difference(Set<Integer> s1, Set<Integer> s2) {
        Set<Integer> u = new HashSet<Integer>(s1);
        u.removeAll(s2);
        return u;
    }

    public static Set<Integer> union(Set<Integer> s1, Set<Integer> s2) {
        Set<Integer> u = new HashSet<Integer>(s1);
        u.addAll(s2);
        return u;
    }
}
