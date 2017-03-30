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

	public Space getPart(int role);
}
