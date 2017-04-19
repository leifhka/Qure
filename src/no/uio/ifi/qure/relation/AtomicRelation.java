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


public abstract class AtomicRelation extends Relation {

	public abstract boolean impliesNonEmpty(AtomicRelation r);

	public abstract boolean impliedByNonEmpty(AtomicRelation r);

	public abstract boolean isValid();

	public abstract int getArity();

    public abstract boolean isOverlaps();

	public abstract Set<List<SID>> evalAll(Map<SID, ? extends Space> spaces, Map<Integer, Set<SID>> roleToSID);
}

