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

public class PartOf extends AtomicRelation {

	private final int a0, a1, r0, r1;

	public PartOf(int r0, int r1, int a0, int a1) {
		this.a0 = a0;
		this.a1 = a1;
		this.r0 = r0;
		this.r1 = r1;
	}

	public int getArity() { return 2; }

	public Integer getArgRole(Integer pos) { return (pos.equals(a0)) ? r0 : r1; }

	public boolean relatesArg(int arg) {
		return a0 == arg || a1 == arg;
	}

	public String toString() {
		return "po(<" + r0 + "," + a0 + ">, <" + r1 + "," + a1 + ">)";
	}

	public String toBTSQL(Integer[] args, Config config) {
		if (args[0] != null && args[1] == null) {
			return toBTSQL2(args, config);
		} else {
			return toBTSQL1(args, config);
		}
	}

	private String toBTSQL1Approx(Integer[] args, Config config) {
		String[] selFroWhe = makeSelectFromWhereParts(config.btTableName, config.uriColumn, args);
		String query = "SELECT " + selFroWhe[0] + ", T0.block\n";
		query += "FROM " + selFroWhe[1] + "\n";
		query += "WHERE ";
		if (!selFroWhe[2].equals("")) query += selFroWhe[2] + " AND\n";
		query += "T0.role % 2 != 0 AND\n";
		query += "T0.block >= (T1.block & (T1.block-1)) AND\n";
		query += "T0.block <= (T1.block | (T1.block-1))";
		return query;
	}

	private String toBTSQL1(Integer[] args, Config config) {
		String[] selFroWhe = makeSelectFromWhereParts(config.btTableName, config.uriColumn, args);
		String query = "WITH \n";
		query += "possible AS (" + toBTSQL1Approx(args, config) + "),\n";
		query += "posGids AS (SELECT DISTINCT v" + a0 + ", v" + a1 + " FROM possible),\n";
		query += "posBlocks AS (SELECT DISTINCT block FROM possible),\n";
		query += "rem AS (SELECT v" + a0 + ", v" + a1 + "\n";
		query += "        FROM (SELECT " + selFroWhe[0] + ", T0.block\n";
		query += "              FROM " + selFroWhe[1] + ", posGids AS Pos\n";
		query += "              WHERE T0.role & 1 != 0 AND Pos.v" + a0 + " = T0.gid) AS Pos\n";
		query += "          LEFT OUTER JOIN posBlocks B ON (Pos.block = B.block)\n";
		query += "        WHERE B.block IS NULL)\n";

		query += "SELECT P.v" + a0 + ", P.v" + a1 + "\n";
		query += "FROM posGids AS P LEFT OUTER JOIN rem AS R ON (P.v" + a0 + " = R.v" + a0 + " AND P.v" + a1 + " = R.v" + a1 + ")\n";
		query += "WHERE R.v" + a0 + " IS NULL";
		return query;
	}

	private String toBTSQL2(Integer[] args, Config config) {
		return null; // TODO
	}

	public String toGeoSQL(Integer[] args, Config config) {
		String[] selFroWhe = makeSelectFromWhereParts(config.geoTableName, config.uriColumn, args);
		String query = "SELECT " + selFroWhe[0] + "\n";
		query += "FROM " + selFroWhe[1] + "\n";
		query += "WHERE ";
		if (!selFroWhe[2].equals("")) query += selFroWhe[2] + " AND ";
		query += "ST_coveredBy(T0.geom, T1.geom);";
		return query;
	}

	public boolean isIntrinsic(SID[] tuple) {
		return tuple[a0].getID() == tuple[a1].getID() && stricterRole(tuple[a0].getRole(), tuple[a1].getRole());
	}

	public Set<Map<Integer, Integer>> impliesNonEmpty(AtomicRelation r) {

		if (r.getArity() > 2 || (r instanceof Before)) {
			return null;
		}

		Set<Map<Integer, Integer>> unifiers = new HashSet<Map<Integer, Integer>>();

		if (r instanceof Overlaps) {
			Overlaps ovr = (Overlaps) r;
			if (ovr.getArity() == 1) {
				Integer oRole = Utils.getOnlySome(ovr.getRoles());
				if (stricterRole(r0, oRole)) {
					Map<Integer, Integer> unifier = new HashMap<Integer, Integer>();
					unifier.put(a0, 0);
					unifiers.add(unifier);
				}
				if (stricterRole(r1, oRole)) {
					Map<Integer, Integer> unifier = new HashMap<Integer, Integer>();
					unifier.put(a1, 0);
					unifiers.add(unifier);
				}
			} else {

				// First argument must overlap one of the arguments, and
				// second argument must contain the other argument.
				// We only add one unifier between partOf and Overlaps, but expand before eval
				if (strictnessRelated(r0, ovr.getArgRole(a0)) &&
				    stricterRole(r1, ovr.getArgRole(a1))) {

					Map<Integer, Integer> unifier = new HashMap<Integer, Integer>();
					unifier.put(new Integer(a0), new Integer(a0));
					unifier.put(new Integer(a1), new Integer(a1));
					unifiers.add(unifier);
				} else if (strictnessRelated(r0, ovr.getArgRole(a1)) &&
				    stricterRole(r1, ovr.getArgRole(a0))) {

					Map<Integer, Integer> unifier = new HashMap<Integer, Integer>();
					unifier.put(new Integer(a0), new Integer(a1));
					unifier.put(new Integer(a1), new Integer(a0));
					unifiers.add(unifier);
				}
			}
		} else {
			PartOf pr = (PartOf) r;
			// First argument must be less strict than r's first argument, and
			// second argument must be stricter than  r's other argument.
			if (stricterRole(pr.r0, r0) && stricterRole(r1, pr.r1)) {
				Map<Integer, Integer> unifier = new HashMap<Integer, Integer>();
				unifier.put(new Integer(a0), new Integer(pr.a0));
				unifier.put(new Integer(a1), new Integer(pr.a1));
			}
		}
		return (unifiers.isEmpty()) ? null : unifiers;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PartOf)) return false;

		PartOf opo = (PartOf) o;
		return a0 == opo.a0 && a1 == opo.a1 && r0 == opo.r0 && r1 == opo.r1;
	}

	@Override
	public int hashCode() {
		return (r0+a0) + 2*(r1+a1);
	}

	public boolean eval(Space[] spaceArgs) {
    	return spaceArgs[a0].partOf(spaceArgs[a1]);
	}

	public Set<AtomicRelation> getAtomicRelations() {

		Set<AtomicRelation> rels = new HashSet<AtomicRelation>();
		if (a0 == a1) {
			rels.add(new PartOf(r0, r1, 0, 0));
		} else {
			rels.add(new PartOf(r0, r1, 0, 1));
		}
		return rels;
	}

	public Set<Integer> getRoles() {
		Set<Integer> rs = new HashSet<Integer>();
		rs.add(r0);
		rs.add(r1);
		return rs;
	}

	public Table evalAll(SpaceProvider spaces) {

    	return null; // Should never occur
	}
	
	public Table evalAll(SpaceProvider spaces, Table possible) {

		Table table = new Table(this);

    	for (SID[] tuple : possible.getTuples()) {
			if (eval(toSpaces(tuple, spaces))) {
				table.addTuple(tuple);
	    	}
	    	SID[] rev = reverse(tuple);
			if (eval(toSpaces(rev, spaces))) {
				table.addTuple(rev);
	    	}
    	}
    	return table;
	}

	private SID[] reverse(SID[] tuple) {
		SID[] res = new SID[2];
		res[0] = tuple[1];
		res[1] = tuple[0];
		return res;
	}

	public boolean isConjunctive(boolean insideNgeation) { return true; }
}
