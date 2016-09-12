package no.uio.ifi.qure;

public interface Space {

    public Space intersection(Space o);

    public boolean isEmpty();

    public Space[] split(int dim);

    public Space[] split(boolean xSplit);

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
