package no.uio.ifi.qure.space;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import com.vividsolutions.jts.geom.TopologyException;

import no.uio.ifi.qure.traversal.SID;

public class Tuple {

	private final Space space;
	private final Collection<SID> elems;

	public Tuple(Space space, Collection<SID> elems) {

		this.space = space;
		this.elems = elems;
	}

	public Tuple(Collection<SID> elems) {

		this.space = null;
		this.elems = elems;
	}

	public Collection<SID> getElements() { return elems; }

	public Space getSpace() { return space; }

	public int size() { return elems.size(); }

	public int hashCode() {
		return elems.hashCode();
	}

	public boolean equals(Object o) {
		return (o instanceof Tuple) && elems.equals(((Tuple) o).getElements());
	}

	public Tuple addSIDOnly(SID sid) {
		Collection<SID> newElems = null;
		if (elems instanceof Set) {
			newElems = new HashSet<SID>(elems);
		} else if (elems instanceof List) {
			newElems = new ArrayList<SID>(elems);
		} else {
			System.err.println("Tuple uses neither Set nor List!");
			System.exit(1);
		}
		newElems.add(sid);
		return new Tuple(newElems);
	}

	/**
	 * Returns a new Intersection object representing the intersection
	 * of all elements in this plus the new object e. Returns null
	 * if intersection does not exists (is empty).
	 */
	public Tuple add(SID e, Space s) {

		if (elems.contains(e)) {
    		return null;
		}

		Space nsp = null;
		try {
			nsp = space.intersection(s);
		} catch (TopologyException ex) {
			System.err.println(ex.toString());
			System.err.println("Offending uri: " + e);
			System.exit(1);
		}

		if (nsp.isEmpty()) return null;

		Set<SID> nelems = new HashSet<SID>(elems);
		nelems.add(e);

		return new Tuple(nsp, nelems);
	}
}
