package no.uio.ifi.qure.space;

import no.uio.ifi.qure.*;
import no.uio.ifi.qure.traversal.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.HashSet;

public class TimeSpace implements Space {

	private final LocalDateTime start; 
	private final LocalDateTime end;

	public static int INTERIOR = 1;
	public static int FIRST = 2;
	public static int LAST = 4;

	public TimeSpace(LocalDateTime start, LocalDateTime end) {

		if (start != null && end != null && (start.isBefore(end) || start.equals(end))) {
			this.start = start;
			this.end = end;
		} else {
			this.start = null;
			this.end = null;
		}
	}

	public LocalDateTime getStart() { return start; }

	public LocalDateTime getEnd() { return end; }

	public static TimeSpace makeEmpty() { return new TimeSpace(null, null); }

	public TimeSpace intersection(Space o) {
		
		if (!(o instanceof TimeSpace)) {
			return null;
		}

		TimeSpace ots = (TimeSpace) o;
		
		if (ots.isEmpty() || isEmpty()) {
			return makeEmpty();
		}

		LocalDateTime newStart = (ots.getStart().isBefore(getStart())) ? getStart() : ots.getStart();
		LocalDateTime newEnd = (ots.getEnd().isBefore(getEnd())) ? ots.getEnd() : getEnd();

		return new TimeSpace(newStart, newEnd);
	}

	public TimeSpace union(Space o) {

		if (!(o instanceof TimeSpace)) {
			return null;
		}

		TimeSpace ots = (TimeSpace) o;
		
		if (ots.isEmpty()) {
			return this;
		} else if (isEmpty()) {
			return ots;
		}

		LocalDateTime newStart = (ots.getStart().isBefore(getStart())) ? ots.getStart() : getStart();
		LocalDateTime newEnd = (ots.getEnd().isBefore(getEnd())) ? getEnd() : ots.getEnd();

		return new TimeSpace(newStart, newEnd);
	}

	public boolean isEmpty() { return start == null || end == null; }

	public boolean equals(Object o) {

		if (!(o instanceof TimeSpace)) return false;

		TimeSpace ots = (TimeSpace) o;

		if (ots.isEmpty()) {
			return isEmpty();
		} else if (isEmpty()) {
			return ots.isEmpty();
		} else {
			return ots.getStart().equals(getStart()) && ots.getEnd().equals(getEnd());
		}
	}

	public TimeSpace[] split(int dim) {
		
		long halfDiffInSec = Math.round( ((double) start.until(end, ChronoUnit.SECONDS))/2.0 );
		LocalDateTime mid = getStart().plusSeconds(halfDiffInSec);

		TimeSpace ts1 = new TimeSpace(getStart(), mid);
		TimeSpace ts2 = new TimeSpace(mid, getEnd());

		return new TimeSpace[]{ts1, ts2};
	}

	public Set<Integer> extractRoles(Space o) {
		if (!(o instanceof TimeSpace)) {
			return null;
		}

		TimeSpace ots = (TimeSpace) o;
		TimeSpace intersection = (TimeSpace) this.intersection(ots);

		Set<Integer> rs = new HashSet<Integer>();
		
		if (!intersection.getStart().equals(intersection.getEnd())) {
			rs.add(INTERIOR);
		}
		if ((new TimeSpace(getStart(), getStart())).partOf(intersection)) {
			rs.add(FIRST);
		}
		if ((new TimeSpace(getEnd(), getEnd())).partOf(intersection)) {
			rs.add(LAST);
		}
		return rs;
	}

	public boolean before(Space o) {

		if (!(o instanceof TimeSpace) || isEmpty() || o.isEmpty()) return false;

		TimeSpace ots = (TimeSpace) o;
		return getEnd().isBefore(ots.getStart());
	}

	public boolean overlaps(Space o) {
		if (!(o instanceof TimeSpace)) return false;

		TimeSpace ots = (TimeSpace) o;
		return !intersection(ots).isEmpty();
	}

	public boolean partOf(Space o) {
		if (!(o instanceof TimeSpace)) return false;

		TimeSpace ots = (TimeSpace) o;
		return !intersection(ots).equals(this);
	}

	private boolean isPoint() {
		return !isEmpty() && !getStart().isEqual(getEnd());
	}

	private boolean hasPointProperPart(LocalDateTime t) {
		return !isEmpty() &&
			   getStart().isBefore(t) &&
			   getEnd().isAfter(t);
	}

	private boolean hasPointPart(LocalDateTime t) {
		return !isEmpty() &&
			   (getStart().isBefore(t) || getStart().isEqual(t)) &&
			   (getEnd().isAfter(t) || getEnd().isEqual(t));
	}

	public TimeSpace getPart(int role) {
		if (role == 0) {
			return this;
		} else if (role == FIRST) {
			return new TimeSpace(start, start);
		} else if (role == LAST) {
			return new TimeSpace(end, end);
		} else if (role == INTERIOR) {
			return new TimeSpace(start.plusSeconds(1), end.minusSeconds(1));
		} else {
			assert(role == (role & (FIRST | LAST | INTERIOR)));
			return makeEmpty();
		}
	}

	public Relationship relate(Space o) {

		if (!(o instanceof TimeSpace)) {
			return null;
		}

		TimeSpace ots = (TimeSpace) o;

		TimeSpace intersection = (TimeSpace) this.intersection(ots);

		boolean intersects = !intersection.isEmpty();
		boolean isCovers = intersection.equals(ots);
		boolean isCoveredBy = intersection.equals(this);
		boolean isBefore = intersection.isEmpty() && this.before(ots);

		return new Relationship() {
			public boolean isIntersects() { return intersects; }
			public boolean isCovers() { return isCovers; }
			public boolean isCoveredBy() { return isCoveredBy; }
			public boolean isBefore() { return isBefore; }
		};
	}

	public String toString() {
		if (isEmpty()) {
			return "[]";
		} else {
			return "[" + getStart().toString() + ", " + getEnd().toString() + "]";
		}
	}

	public String toDBString() {
		if (isEmpty()) {
			return "NULL, NULL";
		} else {
			return "'" + getStart().toString() + "', '" + getEnd().toString() + "'";
		}
	}
}
