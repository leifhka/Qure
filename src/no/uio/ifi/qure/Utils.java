package no.uio.ifi.qure;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Utils {

    public static void getIntersectionsSingle(Space uni, Set<Integer> elems,
                                             Map<Integer, ? extends Space> map,
                                             Map<Integer, Space> intersections,
                                             Set<Integer> covering) {
        for (Integer uri : elems) {

            Space gs = map.get(uri);
            Space ngs = gs.intersection(uni);

            if (!ngs.isEmpty())  {
                if (ngs.hasPart(uni))
                    covering.add(uri);
                else
                    intersections.put(uri, ngs);
            }
        }
    }

    public static void getIntersections(Space uni, Set<Integer> elems,
                                        Map<Integer, ? extends Space> map, int numThreads,
                                        Map<Integer, Space> intersections,
                                        Set<Integer> covering) {
        
        if (elems.size() < 1000) {
            getIntersectionsSingle(uni, elems, map, intersections, covering);
            return;
        }

        int partitionSize = (int) Math.ceil( ((float) elems.size()) / numThreads );

        Integer[] elemsArr = elems.toArray(new Integer[elems.size()]);

        List<Thread> threads = new ArrayList<Thread>();
        List<Map<Integer, Space>> intersectionMaps = new ArrayList<Map<Integer, Space>>();
        List<Set<Integer>> coversSets = new ArrayList<Set<Integer>>();
        int cur = 0;

        for (int i = 0; i < numThreads; i++) {

            Map<Integer, Space> intMap = new HashMap<Integer, Space>();
            Set<Integer> covSet = new HashSet<Integer>();
            final int maxInd = Math.min(cur + partitionSize, elemsArr.length);
            final int minInd = cur;

            Thread is = new Thread() {
                public void run() {
                    for (int j = minInd; j < maxInd; j++) {

                        Integer uri = elemsArr[j];
                        Space gs = map.get(uri);
                        Space ngs = gs.intersection(uni);

                        if (!ngs.isEmpty())  {
                            if (ngs.hasPart(uni))
                                covSet.add(uri);
                            else
                                intMap.put(uri, ngs);
                        }
                    }
                }
            };
            intersectionMaps.add(intMap);
            coversSets.add(covSet);
            threads.add(is);
            is.start();
            cur += partitionSize;
        }

        try {
            for (Thread thread : threads)
                thread.join();
        } catch (InterruptedException ex) {
            System.err.println(ex.toString());
            System.exit(1);
        }

        for (Map<Integer, Space> res : intersectionMaps)
            intersections.putAll(res);

        for (Set<Integer> res : coversSets)
            covering.addAll(res);

    }

    public static Set<Integer> getIntersecting(Space space, Set<Integer> elems,
                                               Map<Integer, ? extends Space> map, int numThreads) {
        
        if (elems.size() < 1000) return getIntersectingSingle(space, elems, map);

        int partitionSize = (int) Math.ceil( ((float) elems.size()) / numThreads );

        Integer[] elemsArr = elems.toArray(new Integer[elems.size()]);

        List<Thread> threads = new ArrayList<Thread>();
        List<Set<Integer>> results = new ArrayList<Set<Integer>>();
        int cur = 0;

        for (int i = 0; i < numThreads; i++) {

            Set<Integer> res = new HashSet<Integer>();
            final int maxInd = Math.min(cur + partitionSize, elemsArr.length);
            final int minInd = cur;

            Thread is = new Thread() {
                public void run() {
                    Set<Integer> intersecting = new HashSet<Integer>();
                    for (int j = minInd; j < maxInd; j++) {
                        Integer uri = elemsArr[j];
                        if (map.get(uri).overlaps(space))
                            res.add(uri);
                    }
                }
            };
            results.add(res);
            threads.add(is);
            is.start();
            cur += partitionSize;
        }

        try {
            for (Thread thread : threads)
                thread.join();
        } catch (InterruptedException ex) {
            System.err.println(ex.toString());
            System.exit(1);
        }

        Set<Integer> intersecting = new HashSet<Integer>();
        for (Set<Integer> res : results)
            intersecting.addAll(res);

        return intersecting;
    }

    public static Set<Integer> getIntersectingSingle(Space space, Set<Integer> elems,
                                                     Map<Integer, ? extends Space> map) {

        Set<Integer> intersecting = new HashSet<Integer>();

        for (Integer uri : elems) {
            if (map.get(uri).overlaps(space))
                intersecting.add(uri);
        }

        return intersecting;
    }

    public static <T> Set<T> intersection(Set<T> s1, Set<T> s2) {
        Set<T> u = new HashSet<T>(s1);
        u.retainAll(s2);
        return u;
    }

    public static <T> Set<T> difference(Set<T> s1, Set<T> s2) {
        Set<T> u = new HashSet<T>(s1);
        u.removeAll(s2);
        return u;
    }

    public static <T> Set<T> union(Set<T> s1, Set<T> s2) {
        Set<T> u = new HashSet<T>(s1);
        u.addAll(s2);
        return u;
    }

    public static <T> Set<T> add(Set<T> s, T e) {
        Set<T> u = new HashSet<T>(s);
        u.add(e);
        return u;
    }
}
