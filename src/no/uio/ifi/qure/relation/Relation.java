package no.uio.ifi.qure.relation;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import no.uio.ifi.qure.Config;
import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.traversal.*;
import no.uio.ifi.qure.space.*;

public abstract class Relation {

	private String name;

	public abstract boolean eval(Space[] args);

	public abstract Set<AtomicRelation> getNormalizedAtomicRelations();

	public abstract Set<AtomicRelation> getPositiveAtomicRelations();

	public abstract Set<AtomicRelation> getNegativeAtomicRelations();
	
	public abstract Set<Integer> getRoles();

	public abstract Set<Integer> getArguments();

	public String getName() { return name; }

	public void setName(String name) { this.name = name; }

	public abstract boolean relatesArg(int arg);

	@Override
	public abstract boolean equals(Object o);

	@Override
	public abstract int hashCode();

	public String[] makeSelectFromWhereParts(String tableName, String uriColumn, String[] vals) {
		String select = "", from = "", where = "", sepSelFro = "", sepWhere = "";
		for (int i = 0; i < vals.length; i++) {
			if (relatesArg(i)) {
				select += sepSelFro + "T" + i + "." + uriColumn + " AS " + "v" + i;
				from += sepSelFro + tableName + " AS T" + i;
				sepSelFro = ", ";
				if (vals[i] != null) {
					where += sepWhere + "T" + i + "." + uriColumn + " = " + vals[i];
					sepWhere = " AND ";
				}
			}
		}
		return new String[]{select, from, where};
	}

	private String makeIdentityRelation(String table, String column) {
		String sel = "SELECT ";
		String from = " FROM ";
		String del = "";
		for (int i = 0; i < getArity(); i++) {
			sel += del + "T" + i + "." + column + " AS v" + i;
			from += del + table + " AS T" + i;
			del = ", ";
		}
		return sel + from;
	}

	public String toBTSQL(String[] vals, Config config) {
		Set<AtomicRelation> ps = getPositiveAtomicRelations();
		Set<AtomicRelation> ns = getNegativeAtomicRelations();

		String query;
		if (ps.isEmpty()) {
			query = makeIdentityRelation(config.btTableName, config.uriColumn);
		} else {
			query = "SELECT * FROM ";
			String del = "";
			int i = 0;
			for (AtomicRelation ar : ps) {
				query += del + "(" + ar.toBTSQL(vals, config) + ") AS T" + i;
				i++;
				del = " NATURAL JOIN ";
			}
		}

		if (!ns.isEmpty()) {
			for (AtomicRelation ar : ns) {
				query = "(" + query + ") EXCEPT (" + ar.toBTSQL(vals, config) + ")";
			}
		}
		return query;
	}

	public String toGeoSQL(String[] vals, Config config, SpaceProvider spaces) {
		Set<AtomicRelation> ps = getPositiveAtomicRelations();
		Set<AtomicRelation> ns = getNegativeAtomicRelations();

		String query;
		if (ps.isEmpty()) {
			query = makeIdentityRelation(config.geoTableName, config.uriColumn);
		} else {
			query = "SELECT * FROM ";
			String del = "";
			int i = 0;
			for (AtomicRelation ar : ps) {
				query += del + "(" + ar.toGeoSQL(vals, config, spaces) + ") AS T" + i;
				i++;
				del = " NATURAL JOIN ";
			}
		}

		if (!ns.isEmpty()) {
			for (AtomicRelation ar : ns) {
				query = "(" + query + ") EXCEPT (" + ar.toGeoSQL(vals, config, spaces) + ")";
			}
		}
		return query;
	}

	public int getArity() {
		return getArguments().size();
	}

	public boolean eval(Space s1, Space s2) {
		Space[] l = new Space[2];
		l[0] = s1;
		l[1] = s2;
		return eval(l);
	}

	public static AtomicRelation overlaps(int r1, int r2, int a1, int a2) { return new Overlaps(r1, r2, a1, a2); }

	public static AtomicRelation overlaps(int[] rs, int[] as) { return new Overlaps(rs, as); }

	public static AtomicRelation partOf(int r1, int r2, int a1, int a2) { return new PartOf(r1, r2, a1, a2); }

	public static AtomicRelation before(int r1, int r2, int a1, int a2) { return new Before(r1, r2, a1, a2); }

	public Relation and(Relation o) { return new And(this, o); }

	public static Relation not(AtomicRelation o) { return new Not(o); }

	public static boolean stricterRole(int i, int j) {
		return (i & j) == j;
	}

	public static boolean strictnessRelated(int i, int j) {
		return stricterRole(i, j) || stricterRole(j, i);
	}

	public static Set<Integer> getStricter(Set<Integer> possible, int role) {

    	Set<Integer> stricter = new HashSet<Integer>();
    	for (int pos : possible) {
        	if (pos != role && stricterRole(pos, role)) stricter.add(pos);
    	}
    	return stricter;
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

	public boolean eval(Space[] args) {
		return conj1.eval(args) && conj2.eval(args);
	}

	public boolean relatesArg(int arg) {
		return conj1.relatesArg(arg) || conj2.relatesArg(arg);
	}

	public Set<AtomicRelation> getNormalizedAtomicRelations() {
		return Utils.union(conj1.getNormalizedAtomicRelations(), conj2.getNormalizedAtomicRelations());
	}

	public Set<AtomicRelation> getPositiveAtomicRelations() {
		return Utils.union(conj1.getPositiveAtomicRelations(), conj2.getPositiveAtomicRelations());
	}

	public Set<AtomicRelation> getNegativeAtomicRelations() {
		return Utils.union(conj1.getNegativeAtomicRelations(), conj2.getNegativeAtomicRelations());
	}

	public Set<Integer> getRoles() {
		return Utils.union(conj1.getRoles(), conj2.getRoles());
	}

	public Set<Integer> getArguments() {
		return Utils.union(conj1.getArguments(), conj2.getArguments());
	}
}

class Not extends Relation {

	private AtomicRelation rel;

	public Not(AtomicRelation rel) {
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

	public boolean eval(Space[] args) {
		return !rel.eval(args);
	}


	public boolean relatesArg(int arg) {
		return rel.relatesArg(arg);
	}

	public Set<AtomicRelation> getNormalizedAtomicRelations() {
		return rel.getNormalizedAtomicRelations();
	}

	public Set<AtomicRelation> getPositiveAtomicRelations() {
		return new HashSet<AtomicRelation>();
	}

	public Set<AtomicRelation> getNegativeAtomicRelations() {
		Set<AtomicRelation> r = new HashSet<AtomicRelation>();
		r.add(rel);
		return r;
	}

	public Set<Integer> getRoles() {
		return rel.getRoles();
	}

	public Set<Integer> getArguments() {
		return rel.getArguments();
	}
}
