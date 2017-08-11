package no.uio.ifi.qure.traversal;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

import no.uio.ifi.qure.bintree.*;
import no.uio.ifi.qure.relation.Relation;
import no.uio.ifi.qure.space.Space;

public class Representation {

	private Map<Integer, Bintree> representation;
	private Map<Block, Block> splits;
	private Space universe;

	public Representation() {
		representation = new HashMap<Integer, Bintree>();
		splits = new HashMap<Block, Block>();
	}

	public Representation(Map<Integer, Bintree> representation) {
		this.representation = representation;
		splits = new HashMap<Block, Block>();
	}

	public void setUniverse(Space universe) { this.universe = universe; }

	public Map<Block, Block> getEvenSplits() { return splits; }

	public void addSplitBlock(Block block, Block split) { splits.put(block, split); }

	public void addAllSplitBlocks(Map<Block, Block> newSplits) { splits.putAll(newSplits); }

	public Map<Integer, Bintree> getRepresentation() { return representation; }

	public Space getUniverse() { return universe; }

	public Representation merge(Representation other) {

		Map<Integer, Bintree> orep = other.getRepresentation();

		for (Integer oid : orep.keySet()) {
			if (!representation.containsKey(oid))
				representation.put(oid, orep.get(oid));
			else 
				representation.put(oid, representation.get(oid).union(orep.get(oid)));
		}

		splits.putAll(other.getEvenSplits());

		return this;
	}

	private Set<SID> getStrictest(Set<SID> covering, Set<Integer> roles) {
		
		Set<SID> strictest = new HashSet<SID>();
		for (SID s : covering) {
			boolean containsStricter = false;
			for (Integer role : Relation.getStricter(roles, s.getRole())) {
				if (covering.contains(new SID(s.getID(), role))) {
					containsStricter = true;
					break;
				}
			}
			if (!containsStricter) strictest.add(s);
		}
		return strictest;
	}

	public void addCovering(Set<SID> covering, Block block, Set<Integer> roles) {
		//if (roles.size() > 1) covering = getStrictest(covering, roles);
		
		for (SID uri : covering) {
			Bintree old = representation.get(uri.getID());
			Bintree toAdd = Bintree.fromBlock(block.setUniquePart(true).addRole(uri.getRole()));
			if (old == null) {
				representation.put(uri.getID(), toAdd);
			} else {
				representation.put(uri.getID(), old.union(toAdd));
			}
		}
	}
}
