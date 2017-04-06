package no.uio.ifi.qure.space;

import java.util.Set;

import no.uio.ifi.qure.traversal.Relationship;

public interface Space {

	public Space intersection(Space o);

	public Space union(Space o);

	public boolean isEmpty();

	public Space[] split(int dim);

	public String toDBString();

	public Relationship relate(Space o);

	public default boolean overlaps(Set<Space> sps) {
		
		Space res = this;
		for (Space s : sps) {
			res = res.intersection(s);
		}
		return !res.isEmpty();
	}

	
	public boolean overlaps(Space o);

	public boolean before(Space o);

	public boolean partOf(Space o);

	public default boolean hasPart(Space o) {
		return o.partOf(this);
	}

	public Space getPart(int role);
}
