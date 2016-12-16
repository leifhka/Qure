package no.uio.ifi.qure;

public interface Relation {

    public String toSQL();

    public boolean eval(Space[] args);

    public default boolean eval(Space s1, Space s2) {
        return eval(new Space[]{s1,s2});
    }

    class Conjunction implements Relation {
    
        private Relation conj1, conj2;
    
        public Conjunction(Relation conj1, Relation conj2) {
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
    
    class Negation implements Relation {
    
        private Relation rel;
    
        public Negation(Relation rel) {
            this.rel = rel;
        }
    
        public String toSQL() {
            return ""; //TODO
        }
    
        public boolean eval(Space[] args) {
            return !rel.eval(args);
        }
    
        public Conjunction and(Relation o) {
            return new Conjunction(rel, o);
        }
    }
    
    class KaryOverlaps implements Relation {

        private final Integer[] args;
    
        public KaryOverlaps(Integer[] args) {
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
            
            if (spaceArgs.length == 0) return false;
            
            Space s = spaceArgs[args[0]];
    
            for (int i = 0; i < args.length; i++)
                s = s.intersection(spaceArgs[args[i]]);
    
            return !s.isEmpty();
        }
    }

    class Overlaps extends KaryOverlaps {

        public Overlaps(Integer[] args) {
            super(args);
        }

        public Overlaps(Integer a1, Integer a2) {
            super(new Integer[]{a1, a2});
        }
    }
    
    class PartOf implements Relation {

        private final Integer[] args;
    
        public PartOf(Integer a1, Integer a2) {
            this.args = new Integer[]{a1, a2};
        }
    
        public String toSQL() {
            return ""; //TODO
        }
    
        public boolean eval(Space[] spaceArgs) {
            
            if (spaceArgs.length < 2) return false;
            
            return spaceArgs[args[0]].covers(spaceArgs[args[1]]);
        }
    }
}
