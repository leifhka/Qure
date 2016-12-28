package no.uio.ifi.qure;

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

public interface Relation {

    public String toSQL();

    public boolean eval(Space[] args);

    public default boolean eval(Space s1, Space s2) {
        return eval(new Space[]{s1,s2});
    }

    public static Relation overlaps(int a1, int a2) { return new Overlaps(a1, a2); }

    public static Relation overlaps(int[] as) { return new Overlaps(as); }

    public static Relation partOf(int a1, int a2) { return new PartOf(a1, a2); }

    public Relation and(Relation o) { return new And(this, o); }

    public static Relation not(Relation o) { return new Not(o); }

    class And implements Relation {
    
        private Relation conj1, conj2;
    
        public And(Relation conj1, Relation conj2) {
            this.conj1 = conj1;
            this.conj2 = conj2;
        }
    
        public String toSQL() {
            return ""; //TODO
        }
    
        public boolean eval(Space[] args) {
            return conj1.eval(args) && conj2.eval(args);
        }
    }
    
    class Not implements Relation {
    
        private Relation rel;
    
        public Not(Relation rel) {
            this.rel = rel;
        }
    
        public String toSQL() {
            return ""; //TODO
        }
    
        public boolean eval(Space[] args) {
            return !rel.eval(args);
        }
    }
    
    class Overlaps implements Relation {

        private final int[] args;

        public Overlaps(int a1, int a2) {
            this.args = new int[]{a1, a2};
        }
    
        public Overlaps(int[] args) {
            if (args.length < 2) {
                System.err.println("ERROR: Overlaps needs at least 2 arguments!");
                System.exit(1);
            }
            this.args = args;
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
    }
   
    class PartOf implements Relation {

        private final int[] args;
    
        public PartOf(int a1, int a2) {
            this.args = new int[]{a1, a2};
        }
    
        public String toSQL() {
            return ""; //TODO
        }
    
        public boolean eval(Space[] spaceArgs) {
            return spaceArgs[args[0]].covers(spaceArgs[args[1]]);
        }
    }
}
