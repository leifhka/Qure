
package no.uio.ifi.qure.relation;

import java.util.Set;

import no.uio.ifi.qure.traversal.SID;

public class Relationships {

	private Set<SID[]> partOfs, befores;
	private Set<Set<SID>> overlaps;

	public Relationships(Set<SID[]> partOfs, Set<SID[]> befores, Set<Set<SID>> overlaps) {
		this.partOfs = partOfs;
		this.befores = befores;
		this.overlaps = overlaps;
	}

	public Set<SID[]> getPartOfs() { return partOfs; }
	
	public Set<SID[]> getBefores() { return befores; }

	public Set<Set<SID>> getOverlaps() { return overlaps; }
}
