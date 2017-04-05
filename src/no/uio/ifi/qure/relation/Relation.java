package no.uio.ifi.qure.relation;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.space.*;

/* Example relations:
 *   new And(new Not(new PartOf(1,2)),
 *		   new Overlaps(1,2))
 * or
 *   not(partOf(1,2)).and(overlaps(1,2))
 *
 * --------------------------------------
 *
 *   new And(new PartOf(1,2),
 *		   new And(new Overlaps(2,3),
 *			   new Not(new PartOf(1,3))))
 * or
 *   partOf(1,2).and(overlaps(2,3)).and(not(partOf(1,3)))
 */

public abstract class Relation {

	public abstract String toSQL();

	public abstract boolean eval(Space[] args);

	public abstract Set<Relation> getAtomicRels();

	public abstract Set<Integer> getRoles();

	public abstract boolean isConjunctive(boolean insideNgeation);

	@Override
	public abstract boolean equals(Object o);

	@Override
	public abstract int hashCode();

	public boolean eval(Space s1, Space s2) { return eval(new Space[]{s1,s2}); }

	public static Relation overlaps(int r1, int r2, int a1, int a2) { return new Overlaps(r1, r2, a1, a2); }

	public static Relation overlaps(int[] rs, int[] as) { return new Overlaps(rs, as); }

	public static Relation partOf(int r1, int r2, int a1, int a2) { return new PartOf(r1, r2, a1, a2); }

	public Relation and(Relation o) { return new And(this, o); }

	public static Relation not(Relation o) { return new Not(o); }

	public boolean isConjunctive() { return isConjunctive(false); }
}

class And extends Relation {

	private Relation conj1, conj2;

	public And(Relation conj1, Relation conj2) {
		this.conj1 = conj1;
		this.conj2 = conj2;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof And)) return false;

		And oand = (And) o;
		return (conj1.equals(oand.conj1) && conj2.equals(oand.conj2)) ||
		       (conj1.equals(oand.conj2) && conj2.equals(oand.conj1));
	}

	@Override
	public int hashCode() {
		return conj1.hashCode() * conj2.hashCode();
	}

	public String toSQL() { //TODO
		return "";
	}

	public boolean eval(Space[] args) {
		return conj1.eval(args) && conj2.eval(args);
	}

	public Set<Relation> getAtomicRels() {
		return Utils.union(conj1.getAtomicRels(), conj2.getAtomicRels());
	}

	public Set<Integer> getRoles() {
		return Utils.union(conj1.getRoles(), conj2.getRoles());
	}

	public boolean isConjunctive(boolean insideNgeation) {
		return conj1.isConjunctive(insideNgeation) && conj2.isConjunctive(insideNgeation);
	}
}

class Not extends Relation {

	private Relation rel;

	public Not(Relation rel) {
		this.rel = rel;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Not)) return false;

		Not onot = (Not) o;
		return rel.equals(onot.rel);
	}

	@Override
	public int hashCode() {
		return (-1)*rel.hashCode();
	}

	public String toSQL() { //TODO
		return "";
	}

	public boolean eval(Space[] args) {
		return !rel.eval(args);
	}

	public Set<Relation> getAtomicRels() {
		return rel.getAtomicRels();
	}

	public Set<Integer> getRoles() {
		return rel.getRoles();
	}

	public boolean isConjunctive(boolean insideNgeation) {
		if (insideNgeation)
			return false;
		else 
			return rel.isConjunctive(true);
	}
 }

class Overlaps extends Relation {

	private Map<Integer, Set<Integer>> argRoles;

	public Overlaps(int r1, int r2, int a1, int a2) {
		argRoles= new HashMap<Integer, Set<Integer>>();
		argRoles.put(a1, new HashSet<Integer>());
		argRoles.put(a2, new HashSet<Integer>());
		argRoles.get(a1).add(r1);
		argRoles.get(a2).add(r2);
	}

	public Overlaps(int[] rs, int[] as) {

		argRoles = new HashMap<Integer, Set<Integer>>();
		for (int i = 0; i < rs.length; i++) {
			argRoles.put(as[i], new HashSet<Integer>());
		}
		for (int i = 0; i < rs.length; i++) {
			argRoles.get(as[i]).add(rs[i]);
		}
	}

	public Overlaps(Map<Integer, Set<Integer>> argRoles) {
		this.argRoles = argRoles;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Overlaps)) return false;

		Overlaps oov = (Overlaps) o;
		return argRoles.equals(oov.argRoles);
	}

	@Override
	public int hashCode() {
		int hc = 0;
		for (Integer r : argRoles.keySet()) {
			hc += r + argRoles.get(r).hashCode();
		}
		return hc;
	}

	public boolean eval(Space[] spaceArgs) {

		Set<Space> sps = new HashSet<Space>();
		for (Integer arg : argRoles.keySet()) {	
			for (Integer role : argRoles.get(arg)) {
    			sps.add(spaceArgs[arg].getPart(role));
			}
		}
		Pair<Space, Set<Space>> sm = Utils.getSome(sps);
		return sm.fst.overlaps(sm.snd);
	}

	public Set<Relation> getAtomicRels() {

		Map<Integer, Integer> argNormMap = new HashMap<Integer, Integer>();
		int i = 0;
		for (Integer arg : argRoles.keySet()) {
			if (!argNormMap.containsKey(arg)) {
				argNormMap.put(arg, i);
				i++;
			}
		}
		Map<Integer, Set<Integer>> normalizedArgRoles = new HashMap<Integer, Set<Integer>>();
		for (Integer arg : argRoles.keySet()) {
			normalizedArgRoles.put(argNormMap.get(arg), argRoles.get(arg));
		}
		
		Set<Relation> rels = new HashSet<Relation>();
		rels.add(new Overlaps(normalizedArgRoles));
		return rels;
	}

	public Set<Integer> getRoles() {

		Set<Integer> rs = new HashSet<Integer>();
		for (Set<Integer> vrs : argRoles.values()) {
			for (Integer role : vrs) {
				if (role.intValue() != 0) {
        			rs.add(role);
				}
			}
		}
		return rs;
	}

	public String toSQL() { //TODO
		return "";
	}

	public boolean isConjunctive(boolean insideNgeation) { return true; }
}

class PartOf extends Relation {

	private final int a1, a2, r1, r2;

	public PartOf(int r1, int r2, int a1, int a2) {
		this.a1 = a1;
		this.a2 = a2;
		this.r1 = r1;
		this.r2 = r2;
	}

	public String toSQL() { //TODO
		return "";
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PartOf)) return false;

		PartOf opo = (PartOf) o;
		return a1 == opo.a1 && a2 == opo.a2;
	}

	@Override
	public int hashCode() {
		return (r1+a1) + 2*(r2+a2);
	}

	public boolean eval(Space[] spaceArgs) {
		return spaceArgs[a1].getPart(r1).partOf(spaceArgs[a2].getPart(r2));
	}

	public Set<Relation> getAtomicRels() {

		Set<Relation> rels = new HashSet<Relation>();
		if (a1 == a2) {
			rels.add(new PartOf(r1, r2, 0, 0));
		} else {
			rels.add(new PartOf(r1, r2, 0, 1));
		}
		return rels;
	}

	public Set<Integer> getRoles() {
		Set<Integer> rs = new HashSet<Integer>();
		if (r1 != 0) {
    		rs.add(r1);
		}
		if (r1 != 0) {
    		rs.add(r2);
		}
		return rs;
	}

	public boolean isConjunctive(boolean insideNgeation) { return true; }
}
