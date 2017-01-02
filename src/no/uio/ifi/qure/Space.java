package no.uio.ifi.qure;

import java.util.Set;

public interface Space {

    public Space intersection(Space o);

    public Space union(Space o);

    public boolean isEmpty();

    public Space[] split(int dim);

    public String toDBString();

    public Relationship relate(Space o);
    
    public boolean overlaps(Space o);

    public boolean before(Space o);

    public boolean partOf(Space o);

    public default boolean hasPart(Space o) {
        return o.partOf(this);
    }

    public Set<Integer> extractRoles(Space block);

    /**
     *  Returns the relationship between the tRole of this and the oRole of o,
     *  if the role does not exist for this Space-type, it should return null.
     */
    public Relationship relate(int tRole, int oRole, Space o);

    public boolean overlaps(int tRole, int oRole, Space o);

    public boolean partOf(int tRole, int oRole, Space o);

    public default boolean hasPart(int tRole, int oRole, Space o) {
        return o.partOf(oRole, tRole, this);
    }

    public boolean before(int tRole, int oRole, Space o);
}
