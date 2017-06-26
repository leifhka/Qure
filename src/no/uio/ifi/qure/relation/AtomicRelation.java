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
import no.uio.ifi.qure.Config;


public abstract class AtomicRelation extends Relation {

	/**
	 * Returns true iff this relation implies r for anny instantiation of the arguments with non-empty spaces.
	 */
	public abstract Set<Map<Integer, Integer>> impliesNonEmpty(AtomicRelation r);

	public abstract Integer getArgRole(Integer pos);

	public abstract Table evalAll(SpaceProvider spaces);

	public abstract Table evalAll(SpaceProvider spaces, Table possible);

	public abstract boolean isIntrinsic(SID[] tuple);

	public abstract boolean relatesArg(int arg);

	public Set<AtomicRelation> getPositiveAtomicRelations() {
		Set<AtomicRelation> r = new HashSet<AtomicRelation>();
		r.add(this);
		return r;
	}

	public Set<AtomicRelation> getNegativeAtomicRelations() {
		return new HashSet<AtomicRelation>();
	}

	public Space[] toSpaces(SID[] args, SpaceProvider spaces) {
		Space[] sps = new Space[args.length];
		for (int i = 0; i < sps.length; i++) {
			sps[i] = spaces.get(args[i]);
		}
		return sps;
	}

	public SID[] toSIDs(Integer[] tuple) {
		SID[] sids = new SID[tuple.length];
		for (int i = 0; i < tuple.length; i++) {
			sids[i] = new SID(tuple[i], getArgRole(i));
		}
		return sids;
	}

	public String[] makeSelectFromWhereParts(String tableName, String uriColumn, Integer[] vals) {
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

	public String makeValuesFrom(Config config) {
		String res = "SELECT (1" + ((config.blockSize > 31) ? "::bigint" : "") + " << N.n) ";
		res += "FROM (VALUES ";
		String sep = "";
		for (int i = 1; i < config.blockSize; i++) {
			res += sep + "(" + i + ")";
			sep = ", ";
		}
		res += ") AS N(n)";
		return res;
	}

}
