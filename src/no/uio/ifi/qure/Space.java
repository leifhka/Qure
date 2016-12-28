package no.uio.ifi.qure;

public interface Space {

    public Space intersection(Space o);

    public Space union(Space o);

    public boolean isEmpty();

    public Space[] split(int dim);

    public Relationship relate(Space o);

    public String toDBString();

    /**
     *  Returns the relationship between the tRole of this and the oRole of o,
     *  if the role does not exist for this Space-type, it should return null.
     */
    public Relationship relate(int tRole, int oRole, Space o);

    public default boolean intersects(int tRole, int oRole, Space o) {
        Relationship rel = relate(tRole, oRole, o);
        return rel.isIntersects();
    }

    public default boolean covers(int tRole, int oRole, Space o) {
        Relationship rel = relate(tRole, oRole, o);
        return rel.isCovers();
    }

    public default boolean coveredBy(int tRole, int oRole, Space o) {
        Relationship rel = relate(tRole, oRole, o);
        return rel.isCoveredBy();
    }

    public default boolean before(int tRole, int oRole, Space o) {
        Relationship rel = relate(tRole, oRole, o);
        return rel.isBefore();
    }
    public default boolean intersects(Space o) {
        Relationship rel = relate(o);
        return rel.isIntersects();
    }

    public default boolean covers(Space o) {
        Relationship rel = relate(o);
        return rel.isCovers();
    }

    public default boolean coveredBy(Space o) {
        Relationship rel = relate(o);
        return rel.isCoveredBy();
    }

    public default boolean before(Space o) {
        Relationship rel = relate(o);
        return rel.isBefore();
    }
}
