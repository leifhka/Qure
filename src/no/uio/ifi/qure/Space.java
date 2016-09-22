package no.uio.ifi.qure;

public interface Space {

    public Space clone();

    public Space intersection(Space o);

    public Space union(Space o);

    public boolean isEmpty();

    public Space[] split(int dim);

    public Relation relate(Space o);

    public String toDBString();

    public boolean isPoint();

    public int getComplexityMeasure();

    public default boolean intersects(Space o) {
        Relation rel = relate(o);
        return rel.isIntersects();
    }

    public default boolean covers(Space o) {
        Relation rel = relate(o);
        return rel.isCovers();
    }

    public default boolean coveredBy(Space o) {
        Relation rel = relate(o);
        return rel.isCoveredBy();
    }
}
