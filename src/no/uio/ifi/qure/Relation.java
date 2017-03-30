package no.uio.ifi.qure;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

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

	public abstract Set<Relation> getPositiveAtomicRels();

	public abstract Set<Relation> getNegativeAtomicRels();

	public abstract boolean isConjunctive(boolean insideNgeation);

	public boolean eval(Space s1, Space s2) { return eval(new Space[]{s1,s2}); }

	public static Relation overlaps(int r1, int r2, int a1, int a2) { return new Overlaps(r1, r2, a1, a2); }

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

	public boolean equals(Object o) {
		if (!(o instanceof And)) return false;

		And oand = (And) o;
		return (conj1.equals(oand.conj1) && conj2.equals(oand.conj2)) ||
		       (conj1.equals(oand.conj2) && conj2.equals(oand.conj1));
	}

	public String toSQL() { //TODO
		return "";
	}

	public boolean eval(Space[] args) {
		return conj1.eval(args) && conj2.eval(args);
	}

	public Set<Relation> getPositiveAtomicRels() {
		return Utils.union(conj1.getPositiveAtomicRels(), conj2.getPositiveAtomicRels());
	}

	public Set<Relation> getNegativeAtomicRels() {
		return Utils.union(conj1.getNegativeAtomicRels(), conj2.getNegativeAtomicRels());
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

	public boolean equals(Object o) {
		if (!(o instanceof Not)) return false;

		Not onot = (Not) o;
		return rel.equals(onot.rel);
	}

	public String toSQL() { //TODO
		return "";
	}

	public boolean eval(Space[] args) {
		return !rel.eval(args);
	}

	public Set<Relation> getPositiveAtomicRels() {
		return rel.getNegativeAtomicRels();
	}

	public Set<Relation> getNegativeAtomicRels() {
		return rel.getPositiveAtomicRels();
	}

	public boolean isConjunctive(boolean insideNgeation) {
		if (insideNgeation)
			return false;
		else 
			return rel.isConjunctive(true);
	}
 }

class Overlaps extends Relation {

	private final int a1, a2, r1, r2;

	public Overlaps(int r1, int r2, int a1, int a2) {
		this.a1 = a1;
		this.a2 = a2;
		this.r1 = r1;
		this.r2 = r2;
	}

	public boolean equals(Object o) {
		if (!(o instanceof Overlaps)) return false;

		Overlaps oov = (Overlaps) o;
		return (a1 == oov.a1 && a2 == oov.a2) ||
		       (a1 == oov.a2 && a2 == oov.a1);
	}

	public boolean eval(Space[] spaceArgs) {
		return spaceArgs[a1].overlaps(spaceArgs[a2]);
	}

	public Set<Relation> getPositiveAtomicRels() {

		Set<Relation> rels = new HashSet<Relation>();
		if (a1 == a2) {
			rels.add(new Overlaps(r1, r2, 0, 0));
		} else {
			rels.add(new Overlaps(r1, r2, 0, 1));
		}
		return rels;
	}

	public Set<Relation> getNegativeAtomicRels() {
		return new HashSet<Relation>();
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

	public boolean equals(Object o) {
		if (!(o instanceof PartOf)) return false;

		PartOf opo = (PartOf) o;
		return a1 == opo.a1 && a2 == opo.a2;
	}

	public boolean eval(Space[] spaceArgs) {
		return spaceArgs[a1].partOf(spaceArgs[a2]);
	}

	public Set<Relation> getPositiveAtomicRels() {

		Set<Relation> rels = new HashSet<Relation>();
		if (a1 == a2) {
			rels.add(new PartOf(r1, r2, 0, 0));
		} else {
			rels.add(new PartOf(r1, r2, 0, 1));
		}
		return rels;
	}

	public Set<Relation> getNegativeAtomicRels() {
		return new HashSet<Relation>();
	}

	public boolean isConjunctive(boolean insideNgeation) { return true; }
}
