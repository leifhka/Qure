package no.uio.ifi.qure.space;

import no.uio.ifi.qure.util.Pair;
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

	public TimeSpace clone() {
		return new TimeSpace(start, end);
	}

	public static TimeSpace makeEmpty() { return new TimeSpace(null, null); }

	public TimeSpace toUniverse() { return this; }

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

	public int hashCode() { return start.hashCode() + 2*end.hashCode(); }

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

	public Pair<TimeSpace, TimeSpace> split(int dim) {
		
		long halfDiffInSec = Math.round( ((double) start.until(end, ChronoUnit.SECONDS))/2.0 );
		LocalDateTime mid = getStart().plusSeconds(halfDiffInSec);

		TimeSpace ts1 = new TimeSpace(getStart(), mid);
		TimeSpace ts2 = new TimeSpace(mid, getEnd());

		return new Pair<TimeSpace, TimeSpace>(ts1, ts2);
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
		return intersection(ots).equals(this);
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
