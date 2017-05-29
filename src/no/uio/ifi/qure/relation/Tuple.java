package no.uio.ifi.qure.space;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import com.vividsolutions.jts.geom.TopologyException;

public class Tuple {

	private final Collection<Integer> elems;

	public Tuple(Collection<Integer> elems) {
		this.elems = elems;
	}

	public Collection<Integer> getElements() { return elems; }

	public int size() { return elems.size(); }

	public int hashCode() {
		return elems.hashCode();
	}

	public boolean equals(Object o) {
		return (o instanceof Tuple) && elems.equals(((Tuple) o).getElements());
	}

	public Tuple addIDOnly(Integer id) {
		Collection<Integer> newElems = null;
		if (elems instanceof Set) {
			newElems = new HashSet<Integer>(elems);
		} else if (elems instanceof List) {
			newElems = new ArrayList<Integer>(elems);
		} else {
			System.err.println("Tuple uses neither Set nor List!");
			System.exit(1);
		}
		newElems.add(id);
		return new Tuple(newElems);
	}

	/**
	 * Returns a new Intersection object representing the intersection
	 * of all elements in this plus the new object e. Returns null
	 * if intersection does not exists (is empty).
	 */
	public Tuple add(Integer e) {

		if (elems.contains(e)) {
    		return null;
		}

		Set<SID> nelems = new HashSet<SID>(elems);
		nelems.add(e);

		return new Tuple(nelems);
	}
}
