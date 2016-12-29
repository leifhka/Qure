package no.uio.ifi.qure;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

/* Example relations:
 *   new And(new Not(new PartOf(1,2)),
 *           new Overlaps(1,2))
 * or
 *   not(partOf(1,2)).and(overlaps(1,2))
 *
 * --------------------------------------
 *
 *   new And(new PartOf(1,2),
 *           new And(new Overlaps(2,3),
 *               new Not(new PartOf(1,3))))
 * or
 *   partOf(1,2).and(overlaps(2,3)).and(not(partOf(1,3)))
 */

public abstract class Relation {

    public abstract String toSQL();

    public abstract boolean eval(Space[] args);

    public boolean eval(Space s1, Space s2) {
        return eval(new Space[]{s1,s2});
    }

    public static Relation overlaps(int r1, int r2, int a1, int a2) { return new Overlaps(r1, r2, a1, a2); }

    public static Relation overlaps(int[] rs, int[] as) { return new Overlaps(rs, as); }

    public static Relation partOf(int r1, int r2, int a1, int a2) { return new PartOf(r1, r2, a1, a2); }

    public Relation and(Relation o) { return new And(this, o); }

    public static Relation not(Relation o) { return new Not(o); }

    public abstract Set<Relation> getPositiveAtomicRels();
}

class And extends Relation {

    private Relation conj1, conj2;

    public And(Relation conj1, Relation conj2) {
        this.conj1 = conj1;
        this.conj2 = conj2;
    }

    public boolean equals(Object o) {
        if (!(o instanceof And)) return false;

        And oand = (And) o;
        return (conj1.equals(oand.conj1) && conj2.equals(oand.conj2)) ||
               (conj1.equals(oand.conj2) && conj2.equals(oand.conj1));
    }

    public String toSQL() {
        return ""; //TODO
    }

    public boolean eval(Space[] args) {
        return conj1.eval(args) && conj2.eval(args);
    }

    public Set<Relation> getPositiveAtomicRels() {
        return Utils.union(conj1.getPositiveAtomicRels(), conj2.getPositiveAtomicRels());
    }
}

class Not extends Relation {

    private Relation rel;

    public Not(Relation rel) {
        this.rel = rel;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Not)) return false;

        Not onot = (Not) o;
        return rel.equals(onot.rel);
    }

    public String toSQL() {
        return ""; //TODO
    }

    public boolean eval(Space[] args) {
        return !rel.eval(args);
    }

    public Set<Relation> getPositiveAtomicRels() {
        return new HashSet<Relation>();
    }
 }

class Overlaps extends Relation {

    private final int[] args;
    private final int[] roles;

    public Overlaps(int r1, int r2, int a1, int a2) {
        this.args = new int[]{a1, a2};
        this.roles = new int[]{r1, r2};
    }

    public Overlaps(int[] roles, int[] args) {
        if (args.length < 2) {
            System.err.println("ERROR: Overlaps needs at least 2 arguments!");
            System.exit(1);
        } else if (args.length != roles.length) {
            System.err.println("ERROR: Overlaps needs at same number of roles as arguments!");
            System.exit(1);
        }
        this.args = args;
        this.roles = roles;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Overlaps)) return false;

        Overlaps oov = (Overlaps) o;

        Map<Integer, Set<Integer>> ts = new HashMap<Integer, Set<Integer>>();
        for (int i = 0; i < args.length; i++) {
            if (!ts.containsKey(args[i])) ts.put(args[i], new HashSet<Integer>());
            ts.get(args[i]).add(roles[i]);
        }

        Map<Integer, Set<Integer>> os = new HashMap<Integer, Set<Integer>>();
        for (int i = 0; i < oov.args.length; i++) {
            if (!os.containsKey(oov.args[i])) os.put(oov.args[i], new HashSet<Integer>());
            os.get(oov.args[i]).add(oov.roles[i]);
        }

        return ts.entrySet().equals(os.entrySet());
    }

    public String toSQL() {
        return ""; //TODO
    }

    public boolean eval(Space[] spaceArgs) {
        
        Space s = spaceArgs[args[0]];

        for (int i = 0; i < args.length; i++)
            s = s.intersection(spaceArgs[args[i]]);

        return !s.isEmpty();
    }

    public Set<Relation> getPositiveAtomicRels() {

        int[] normArgs = new int[args.length];
        Map<Integer, Integer> argsMap = new HashMap<Integer, Integer>();
        for (int i = 0; i < args.length; i++) {
            if (argsMap.containsKey(args[i])) {
                normArgs[i] = argsMap.get(args[i]);
            } else {
                normArgs[i] = i;
                argsMap.put(args[i], i);
            }
        }

        Set<Relation> rels = new HashSet<Relation>();
        rels.add(new Overlaps(roles, normArgs));
        return rels;
    }
}

class PartOf extends Relation {

    private final int a1, a2, r1, r2;

    public PartOf(int r1, int r2, int a1, int a2) {
        this.a1 = a1;
        this.a2 = a2;
        this.r1 = r1;
        this.r2 = r2;
    }

    public String toSQL() {
        return ""; //TODO
    }

    public boolean equals(Object o) {
        if (!(o instanceof PartOf)) return false;

        PartOf opo = (PartOf) o;
        return a1 == opo.a1 && a2 == opo.a2;
    }

    public boolean eval(Space[] spaceArgs) {
        return spaceArgs[a1].relate(spaceArgs[a2]).isCoveredBy();
    }

    public Set<Relation> getPositiveAtomicRels() {

        Set<Relation> rels = new HashSet<Relation>();
        if (a1 == a2)
            rels.add(new PartOf(r1, r2, 0, 0));
        else
            rels.add(new PartOf(r1, r2, 0, 1));
        return rels;
    }
}
