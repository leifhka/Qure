package no.uio.ifi.qure.relation;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.traversal.*;
import no.uio.ifi.qure.space.*;

public abstract class Relation {

	public abstract String toSQL();

	public abstract boolean evalRoled(Space[] args);

	public abstract boolean eval(Space[] args);

	public abstract Set<AtomicRelation> getAtomicRelations();

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

	public static Relation before(int r1, int r2, int a1, int a2) { return new Before(r1, r2, a1, a2); }

	public Relation and(Relation o) { return new And(this, o); }

	public static Relation not(Relation o) { return new Not(o); }

	public boolean isConjunctive() { return isConjunctive(false); }


	public static boolean stricterRole(int i, int j) {
		return (i & j) == j;
	}

	public static boolean strictnessRelated(int i, int j) {
		return stricterRole(i, j) || stricterRole(j, i);
	}

	public static boolean oneStrictnessRelated(int i, Set<Integer> ints) {

		if (ints == null) return false;

		for (int j : ints) {
			if (strictnessRelated(i, j)) return true;
		}
		return false;
	}

	public static boolean oneStricter(int i, Set<Integer> ints) {

		if (ints == null) return false;

		for (int j : ints) {
			if (stricterRole(j, i)) return true;
		}
		return false;
	}

	public static boolean oneLessStrict(int i, Set<Integer> ints) {

		if (ints == null) return false;

		for (int j : ints) {
			if (stricterRole(i, j)) return true;
		}
		return false;
	}
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

	public String toString() {
		return conj1.toString() + " /\\ " + conj2.toString();
	}

	public String toSQL() { //TODO
		return "";
	}

	public boolean evalRoled(Space[] args) {
		return conj1.evalRoled(args) && conj2.evalRoled(args);
	}

	public boolean eval(Space[] args) {
		return conj1.eval(args) && conj2.eval(args);
	}

	public Set<AtomicRelation> getAtomicRelations() {
		return Utils.union(conj1.getAtomicRelations(), conj2.getAtomicRelations());
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

	public String toString() {
		return "~" + rel.toString();
	}

	public String toSQL() { //TODO
		return "";
	}

	public boolean evalRoled(Space[] args) {
		return !rel.evalRoled(args);
	}

	public boolean eval(Space[] args) {
		return !rel.eval(args);
	}


	public Set<AtomicRelation> getAtomicRelations() {
		return rel.getAtomicRelations();
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


