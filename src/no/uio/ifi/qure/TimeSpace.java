package no.uio.ifi.qure;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.HashSet;

public class TimeSpace implements Space {

    private final LocalDateTime start; 
    private final LocalDateTime end;

    public static int UNIQUE = 1;
    public static int INTERIOR = 2;
    public static int FIRST = 4;
    public static int LAST = 8;

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

    public Set<Integer> extractRoles(Space o) {
        if (!(o instanceof TimeSpace)) return null;

        TimeSpace ots = (TimeSpace) o;
        TimeSpace intersection = (TimeSpace) this.intersection(ots);

        Set<Integer> rs = new HashSet<Integer>();
        
        if (this.equals(intersection))
            rs.add(UNIQUE);
        if (!intersection.getStart().equals(intersection.getEnd()))
            rs.add(INTERIOR);
        if ((new TimeSpace(getStart(), getStart())).partOf(intersection))
            rs.add(FIRST);
        if ((new TimeSpace(getEnd(), getEnd())).partOf(intersection))
            rs.add(LAST);

        return rs;
    }

    public Relationship relate(int tRole, int oRole, Space o) { //TODO(?)

        if (!(o instanceof TimeSpace))
            return null;

        else return null;
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

    public boolean before(int tRole, int oRole, Space o) {

        if (!(o instanceof TimeSpace) || isEmpty() || o.isEmpty()) return false;

        TimeSpace ots = (TimeSpace) o;

        if ((tRole | UNIQUE | LAST) == (UNIQUE | LAST) && //tRole is a combination of 0, UNIQUE and LAST
            (oRole | UNIQUE | FIRST) == (UNIQUE | FIRST)) //oRole is a combination of 0, UNIQUE and FIRST
            return getEnd().isBefore(ots.getStart());
        else if ((tRole | UNIQUE | LAST) == (UNIQUE | LAST) && (oRole & INTERIOR) != 0)
            return getEnd().isBefore(ots.getStart()) || getEnd().isEqual(ots.getStart());
        else if ((tRole | UNIQUE | LAST) == (UNIQUE | LAST) && (oRole & LAST) != 0)
            return getEnd().isBefore(ots.getEnd());
        else if ((tRole & FIRST) != 0 && (oRole & FIRST) != 0)
            return getStart().isBefore(ots.getStart());
        else if ((tRole & FIRST) != 0 && (oRole & INTERIOR) != 0)
            return getStart().isBefore(ots.getStart()) || getStart().isEqual(ots.getStart());
        else if ((tRole & FIRST) != 0 && (oRole & LAST) != 0)
            return getStart().isBefore(ots.getEnd());
        else if ((tRole & INTERIOR) != 0 && (oRole & FIRST) != 0)
            return getEnd().isBefore(ots.getStart()) || getEnd().isEqual(ots.getStart());
        else if ((tRole & INTERIOR) != 0 && (oRole & INTERIOR) != 0)
            return getEnd().isBefore(ots.getStart()) || getEnd().isEqual(ots.getStart()); // (?)
        else if ((tRole & INTERIOR) != 0 && (oRole & LAST) != 0)
            return getEnd().isBefore(ots.getEnd()) || getEnd().isEqual(ots.getEnd());
        else if ((tRole & LAST) != 0 && (oRole & LAST) != 0)
            return getEnd().isBefore(ots.getEnd()); 
        else {
            System.err.println("ERROR: the roles " + tRole + ", " + oRole + " cannot be related or does not exist for this type.");
            (new Exception()).printStackTrace();
            System.exit(1);
            return false;
        }
    }

    public boolean overlaps(int tRole, int oRole, Space o) { 
        if (!(o instanceof TimeSpace)) return false;

        if ((tRole & UNIQUE) != 0 || isEmpty() || (oRole & UNIQUE) != 0 || o.isEmpty())
            return false;

        TimeSpace ots = (TimeSpace) o;

        if (tRole == 0 && oRole == 0)
            return !intersection(ots).isEmpty();
        else if (tRole == 0 && (oRole & FIRST) != 0)
            return hasPointPart(ots.getStart());
        else if (tRole == 0 && (oRole & LAST) != 0)
            return hasPointPart(ots.getEnd());
        else if (tRole == 0 && (oRole & INTERIOR) !=  0)
            return hasPointProperPart(ots.getStart()) || hasPointProperPart(ots.getEnd());
        else if ((tRole & FIRST) != 0 && oRole == 0)
            return ots.hasPointPart(getStart());
        else if ((tRole & FIRST) != 0 && (oRole & FIRST) != 0)
            return getStart().isEqual(ots.getStart());
        else if ((tRole & FIRST) != 0 && (oRole & LAST) != 0)
            return getStart().isEqual(ots.getEnd());
        else if ((tRole & FIRST) != 0 && (oRole & INTERIOR) !=  0)
            return ots.hasPointProperPart(getStart());
        else if ((tRole & LAST) != 0 && oRole == 0)
            return ots.hasPointPart(getEnd());
        else if ((tRole & LAST) != 0 && (oRole & FIRST) != 0)
            return getEnd().isEqual(ots.getStart());
        else if ((tRole & LAST) != 0 && (oRole & LAST) != 0)
            return getEnd().isEqual(ots.getEnd());
        else if ((tRole & LAST) != 0 && (oRole & INTERIOR) !=  0)
            return ots.hasPointProperPart(getEnd());
        else if ((tRole & INTERIOR) != 0 && oRole == 0)
            return hasPointProperPart(ots.getStart()) || hasPointProperPart(ots.getEnd());
        else if ((tRole & INTERIOR) != 0 && (oRole & FIRST) != 0)
            return hasPointProperPart(ots.getStart());
        else if ((tRole & INTERIOR) != 0 && (oRole & LAST) != 0)
            return hasPointProperPart(ots.getEnd());
        else if ((tRole & INTERIOR) != 0 && (oRole & INTERIOR) !=  0)
            return hasPointProperPart(ots.getStart()) || hasPointProperPart(ots.getEnd());
        else {
            System.err.println("ERROR: the roles " + tRole + ", " + oRole + " cannot be related or does not exist for this type.");
            (new Exception()).printStackTrace();
            System.exit(1);
            return false;
        }
    }

    public boolean partOf(int tRole, int oRole, Space o) { 
        if (!(o instanceof TimeSpace)) return false;
        if ((oRole & UNIQUE) != 0 || o.isEmpty()) return false;

        TimeSpace ots = (TimeSpace) o;

        if (tRole == 0 && oRole == 0)
            return partOf(ots);
        else if (tRole == 0 && (oRole & FIRST) != 0)
            return isPoint() && getStart().isEqual(ots.getStart());
        else if (tRole == 0 && (oRole & LAST) != 0)
            return isPoint() && getStart().isEqual(ots.getEnd());
        else if (tRole == 0 && (oRole & INTERIOR) !=  0)
            return ots.hasPointProperPart(getStart()) && ots.hasPointProperPart(getEnd());
        else if ((tRole & FIRST) != 0 && oRole == 0)
            return ots.hasPointPart(getStart());
        else if ((tRole & FIRST) != 0 && (oRole & FIRST) != 0)
            return getStart().isEqual(ots.getStart());
        else if ((tRole & FIRST) != 0 && (oRole & LAST) != 0)
            return getStart().isEqual(ots.getEnd());
        else if ((tRole & FIRST) != 0 && (oRole & INTERIOR) !=  0)
            return ots.hasPointProperPart(getStart());
        else if ((tRole & LAST) != 0 && oRole == 0)
            return ots.hasPointPart(getEnd());
        else if ((tRole & LAST) != 0 && (oRole & FIRST) != 0)
            return getEnd().isEqual(ots.getStart());
        else if ((tRole & LAST) != 0 && (oRole & LAST) != 0)
            return getEnd().isEqual(ots.getEnd());
        else if ((tRole & LAST) != 0 && (oRole & INTERIOR) !=  0)
            return ots.hasPointProperPart(getEnd());
        else if ((tRole & INTERIOR) != 0 && oRole == 0)
            return ots.hasPointPart(getStart()) || ots.hasPointPart(getEnd());
        else if ((tRole & INTERIOR) != 0 && (oRole & FIRST) != 0)
            return isPoint() && getStart().isEqual(ots.getStart());
        else if ((tRole & INTERIOR) != 0 && (oRole & LAST) != 0)
            return isPoint() && getStart().isEqual(ots.getEnd());
        else if ((tRole & INTERIOR) != 0 && (oRole & INTERIOR) !=  0)
            return ots.hasPointPart(getStart()) || ots.hasPointPart(getEnd());
        else {
            System.err.println("ERROR: the roles " + tRole + ", " + oRole + " cannot be related or does not exist for this type.");
            (new Exception()).printStackTrace();
            System.exit(1);
            return false;
        }
    }

    public Relationship relate(Space o) {

        if (!(o instanceof TimeSpace)) return null;

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
