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
	
	public Set<Integer> getArguments() {
		Set<Integer> res = new HashSet<Integer>();
		res.add(a0);
		res.add(a1);
		return res;
	}

	public int getArg(int a) {
		if (a == 0) {
			return a0;
		} else {
			return a1;
		}
	}

	public Integer getArgRole(Integer pos) { return (pos.equals(a0)) ? r0 : r1; }

	public boolean relatesArg(int arg) {
		return a0 == arg || a1 == arg;
	}

	public String toString() {
		return "po(<" + r0 + "," + a0 + ">, <" + r1 + "," + a1 + ">)";
	}

	public String toBTSQL(String[] vals, Config config) {
		if (vals[a0] != null && vals[a1] == null) {
			return toBTSQL2(vals, config);
		} else if (vals[a1] != null) {
			return toBTSQL1(vals, config);
		} else {
			return null; //TODO
		}
	}

		public String toBTSQL1Approx(String[] vals, Config config) {
		String[] selFroWhe = makeSelectFromWhereParts(config.btTableName, config.uriColumn, vals);
		String query = "    SELECT DISTINCT T" + a0 + ".gid AS v" + a0 + ", T" + a0 + ".block\n";
		query += "    FROM " + selFroWhe[1] + "\n";
		query += "    WHERE ";
		if (!selFroWhe[2].equals("")) query += selFroWhe[2] + " AND\n";
		//query += "      T" + a0 + ".role & " + (1 | (r0 << 1)) + " = " + (1 | (r0 << 1)) + " AND\n";
		//if (r1 != 0) query += "      T" + a1 + ".role & " + (r1 << 1) + " = " + (r1 << 1) + " AND\n";
		query += "      (T" + a0 + ".role = " + (1 | (r0 << 1));
		if (r0 == 0) {
			query += " OR T" + a0 + ".role = " + (1 | (1 << 1));
			query += " OR T" + a0 + ".role = " + (1 | (2 << 1));
		}
		query += ") AND\n";
		query += "      T" + a0 + ".block > (T" + a1 + ".block & (T" + a1 + ".block-1)) AND\n";
		query += "      T" + a0 + ".block <= (T" + a1 + ".block | (T" + a1 + ".block-1))";
		return query;
	}

	private String toBTSQL1(String[] vals, Config config) {
		String[] selFroWhe = makeSelectFromWhereParts(config.btTableName, config.uriColumn, vals);
		String query = "WITH \n";
		query += "possible AS (\n" + toBTSQL1Approx(vals, config) + "),\n";
		query += "posGids AS (SELECT DISTINCT v" + a0 + " FROM possible),\n";
		query += "rem AS (SELECT DISTINCT T." + config.uriColumn + " AS v" + a0 + "\n"
		               + "FROM " + config.btTableName + " AS T, posGids AS Pos \n"
		               + "WHERE (T.role = " + (1 | (r0 << 1)); 
		if (r0 == 0) {
			query += " OR T.role = " + (1 | (1 << 1));
			query += " OR T.role = " + (1 | (2 << 1));
		}
		query += ") \n";
		query += " AND Pos.v" + a0 + " = T.gid AND (T.gid, T.block) NOT IN (SELECT * FROM possible))\n";

		String a0Sel = "P.v" + a0;
		String a1Sel = vals[a1] + " AS v" + a1;
		String selStr = (a0 < a1) ?  a0Sel + ", " + a1Sel : a1Sel + ", " + a0Sel;
		query += "SELECT DISTINCT " + selStr + "\n";
		query += "FROM posGids AS P LEFT OUTER JOIN rem AS R ON (P.v" + a0 + " = R.v" + a0 + ")\n";
		query += "WHERE R.v" + a0 + " IS NULL";
		return query;
	}

	private String toBTSQL2Approx(String[] vals, Config config) {
		String[] selFroWhe = makeSelectFromWhereParts(config.btTableName, config.uriColumn, vals);
		String query = "    SELECT DISTINCT T" + a1 + "." + config.uriColumn + " AS v" + a1 + ", T" + a0 + ".block\n";
		query += "    FROM " + selFroWhe[1] + ",\n";
		query += "         (" + makeValuesFrom(config) + ") AS V(n)\n";
		query += "    WHERE ";
		if (!selFroWhe[2].equals("")) query += selFroWhe[2] + " AND\n";
		
		query += "      (T" + a0 + ".role = " + (1 | (r0 << 1));
		if (r0 == 0) {
			query += " OR T" + a0 + ".role = " + (1 | (1 << 1));
			query += " OR T" + a0 + ".role = " + (1 | (2 << 1));
		}
		query += ") AND\n";
		
		//query += "      T" + a0 + ".role & " + (1 | (r0 << 1)) + " = " + (1 | (r0 << 1)) + " AND\n";
		//if (r1 != 0) query += "      T" + a1 + ".role & " + (r1 << 1) + " = " + (r1 << 1) + " AND\n";
		query += "      (T" + a0 + ".block = T" + a1 + ".block OR\n";
		query += "       (T" + a0 + ".block != T" + a0 + ".block & ~(V.n-1) AND\n";
		query += "        T" + a1 + ".block = ((T" + a0 + ".block & ~(V.n-1)) | V.n)))";
		return query;
	}

	private String toBTSQL2(String[] vals, Config config) {
		String[] selFroWhe = makeSelectFromWhereParts(config.btTableName, config.uriColumn, vals);

		String query = "WITH \n";
		query += "  possible AS (\n" + toBTSQL2Approx(vals, config) + "),\n";
		query += "  posGids AS (SELECT DISTINCT v" + a1 + " FROM possible),\n";
		query += "  allBlocks AS (SELECT DISTINCT block\n " 
		                       + "FROM " + config.btTableName + "\n"
		                      + " WHERE gid = " + vals[a0] + " AND ";
		query += "(role = " + (1 | (r0 << 1));
		if (r0 == 0) {
			query += " OR role = " + (1 | (1 << 1));
			query += " OR role = " + (1 | (2 << 1));
		}
		query += ")),\n";
		query += "  rem AS (\n";
		query += "        SELECT DISTINCT v" + a1 + "\n";
		query += "        FROM posGids AS Pos,\n";
		query += "             allBlocks AS AB\n";
		query += "        WHERE (Pos.v" + a1 + ", AB.block) NOT IN (SELECT * FROM possible))\n";
		if (a0 < a1) {
			query += "SELECT DISTINCT " + vals[a0] + " AS v" + a0 + ", P.v" + a1 + "\n";
		} else {
			query += "SELECT DISTINCT P.v" + a1 + ", " + vals[a0] + " AS v" + a0 + "\n";
		}
		query += "FROM posGids AS P LEFT OUTER JOIN rem AS R ON (P.v" + a1 + " = R.v" + a1 + ")\n";
		query += "WHERE R.v" + a1 + " IS NULL";
		return query;
	}

	public String toGeoSQL(String[] vals, Config config, SpaceProvider sp) {
		return sp.toSQL(this, vals, config);
	}

	public boolean isIntrinsic(Integer[] tuple) {
		return tuple[a0] == tuple[a1] && stricterRole(r0, r1);
	}

	public Set<Map<Integer, Integer>> impliesNonEmpty(AtomicRelation r) {

		if (r.getArity() > 2 || (r instanceof Before)) {
			return null;
		}

		Set<Map<Integer, Integer>> unifiers = new HashSet<Map<Integer, Integer>>();

		if (r instanceof Overlaps) {
			Overlaps ovr = (Overlaps) r;
			if (ovr.getArity() == 1) {
				Integer oRole = Utils.unpackSingleton(ovr.getRoles());
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

	public Set<AtomicRelation> getNormalizedAtomicRelations() {

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

    	for (Integer[] tuple : possible.getTuples()) {
			if (eval(toSpaces(toSIDs(tuple), spaces))) {
				table.addTuple(tuple);
	    	}
	    	Integer[] rev = reverse(tuple);
			if (eval(toSpaces(toSIDs(rev), spaces))) {
				table.addTuple(rev);
	    	}
    	}
    	return table;
	}

}
