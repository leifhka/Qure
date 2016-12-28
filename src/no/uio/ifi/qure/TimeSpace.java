package no.uio.ifi.qure;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class TimeSpace implements Space {

    private final LocalDateTime start; 
    private final LocalDateTime end;

    public static int interior = 1;
    public static int boundary = 2;
    public static int unique = 4;

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
        
        if (!(o instanceof TimeSpace)) return null;

        TimeSpace ots = (TimeSpace) o;
        
        if (ots.isEmpty() || isEmpty()) return makeEmpty();

        LocalDateTime newStart = (ots.getStart().isBefore(getStart())) ? getStart() : ots.getStart();
        LocalDateTime newEnd = (ots.getEnd().isBefore(getEnd())) ? ots.getEnd() : getEnd();

        return new TimeSpace(newStart, newEnd);
    }

    public TimeSpace union(Space o) {

        if (!(o instanceof TimeSpace)) return null;

        TimeSpace ots = (TimeSpace) o;
        
        if (ots.isEmpty())
            return this;
        else if (isEmpty())
            return ots;

        LocalDateTime newStart = (ots.getStart().isBefore(getStart())) ? ots.getStart() : getStart();
        LocalDateTime newEnd = (ots.getEnd().isBefore(getEnd())) ? getEnd() : ots.getEnd();

        return new TimeSpace(newStart, newEnd);
    }

    public boolean isEmpty() { return start == null || end == null; }

    public boolean equals(Object o) {

        if (!(o instanceof TimeSpace)) return false;

        TimeSpace ots = (TimeSpace) o;

        if (ots.isEmpty())
            return isEmpty();
        else if (isEmpty())
            return ots.isEmpty();
        else
            return ots.getStart().equals(getStart()) && ots.getEnd().equals(getEnd());
    }

    public TimeSpace[] split(int dim) {
        
        long halfDiffInSec = Math.round( ((double) start.until(end, ChronoUnit.SECONDS))/2.0 );
        LocalDateTime mid = getStart().plusSeconds(halfDiffInSec);

        TimeSpace ts1 = new TimeSpace(getStart(), mid);
        TimeSpace ts2 = new TimeSpace(mid, getEnd());

        return new TimeSpace[]{ts1, ts2};
    }

    // TODO: Alternate solution: Define each relation for roles, not the general relate (as it is not needed(?)
    // General relate only needed in initial part of constructGraph, but after this we will only use
    // Relation's eval on the actual relations needed.
    public Relationship relate(int tRole, int oRole, Space o) {

        if (!(o instanceof TimeSpace))
            return null;

        if ((tRole & unique) != 0) return null; //TODO
        else if ((oRole & unique) != 0) return null; //TODO
        else return null; //TODO
    }

    public boolean isBefore(Space o) {

        if (!(o instanceof TimeSpace) || isEmpty() || o.isEmpty()) return false;

        TimeSpace ots = (TimeSpace) o;
        return getEnd().isBefore(ots.getStart());
    }

    public Relationship relate(Space o) {

        if (!(o instanceof TimeSpace)) return null;

        TimeSpace ots = (TimeSpace) o;

        TimeSpace intersection = (TimeSpace) this.intersection(ots);

        boolean intersects = !intersection.isEmpty();
        boolean isCovers = intersection.equals(ots);
        boolean isCoveredBy = intersection.equals(this);
        boolean isBefore = intersection.isEmpty() && this.isBefore(ots);

        return new Relationship() {
            public boolean isIntersects() { return intersects; }
            public boolean isCovers() { return isCovers; }
            public boolean isCoveredBy() { return isCoveredBy; }
            public boolean isBefore() { return isBefore; }
        };
    }

    public String toString() {
        if (isEmpty())
            return "[]";
        else
            return "[" + getStart().toString() + ", " + getEnd().toString() + "]";
    }

    public String toDBString() {
        if (isEmpty())
            return "NULL, NULL";
        else
            return "'" + getStart().toString() + "', '" + getEnd().toString() + "'";
    }

}
