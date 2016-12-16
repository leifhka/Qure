package no.uio.ifi.qure;

/* Example relations:
 *   new And(Not(PartOf(1,2)),
 *           Overlaps(1,2))
 *
 *   new And(PartOf(1,2),
 *           And(Overlaps(2,3),
 *               Not(PartOf(1,3))))
 */

public interface Relation {

    public String toSQL();

    public boolean eval(Space[] args);

    public default boolean eval(Space s1, Space s2) {
        return eval(new Space[]{s1,s2});
    }

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
            
            if (spaceArgs.length == 0) return false;
            
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
            
            if (spaceArgs.length < 2) return false;
            
            return spaceArgs[args[0]].covers(spaceArgs[args[1]]);
        }
    }
}
