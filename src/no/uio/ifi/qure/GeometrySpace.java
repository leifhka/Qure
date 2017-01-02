package no.uio.ifi.qure;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.util.PolygonExtracter;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.geom.TopologyException;
import com.vividsolutions.jts.precision.EnhancedPrecisionOp;
import com.vividsolutions.jts.geom.IntersectionMatrix;

public class GeometrySpace implements Space {

    private Geometry geo;

    /* Roles */
    public static int INTERIOR = 1;
    public static int BOUNDARY = 2;
    public static int UNIQUE = 4;

    public GeometrySpace(Geometry geo) {
        this.geo = flatten(geo);
    }

    private static Geometry flatten(Geometry gc) {
        if (gc.getNumGeometries() <= 1) return gc;

        //Needed to convert GeometryCollection to Geometry
        List<Geometry> gs = new ArrayList<Geometry>();
        for (int i = 0; i < gc.getNumGeometries(); i++)
            gs.add(gc.getGeometryN(i));

        return gc.getFactory().buildGeometry(gs);
    }

    public Geometry getGeometry() { return geo; }

    public boolean isEmpty() { return geo.isEmpty(); }

    public GeometrySpace union(Space o) {
        Geometry go = ((GeometrySpace) o).getGeometry();
        return new GeometrySpace(geo.union(go));
    }

    public GeometrySpace intersection(Space o) {
        Geometry go = ((GeometrySpace) o).getGeometry();
        return new GeometrySpace(geo.intersection(go));
        //return new GeometrySpace(EnhancedPrecisionOp.intersection(geo, go));
    }

    /**
     * Splits the argument envelope into two partition envelopes. The split is along the x-axis if xSplit,
     * and along the y-axis otherwise.
     */
    private Envelope[] splitEnvelope(Envelope e, boolean xSplit) {

        Envelope e1,e2;

        if (xSplit) { //Making the new bintree blocks, dividing along the x-axis
            double xmid = e.getMinX() + (e.getMaxX() - e.getMinX())/2.0;
            e1 = new Envelope(e.getMinX(), xmid, e.getMinY(), e.getMaxY());
            e2 = new Envelope(xmid, e.getMaxX(), e.getMinY(), e.getMaxY());
        } else { //Dividing along the y-axis
            double ymid = e.getMinY() + (e.getMaxY() - e.getMinY())/2.0;
            e1 = new Envelope(e.getMinX(), e.getMaxX(), e.getMinY(), ymid);
            e2 = new Envelope(e.getMinX(), e.getMaxX(), ymid, e.getMaxY());
        }

        return new Envelope[]{e1, e2};
    }

    public GeometrySpace[] split(int dim) {

        Envelope te = geo.getEnvelopeInternal();

        Envelope[] es = splitEnvelope(te, dim == 0);
        GeometryFactory gf = geo.getFactory();
        GeometrySpace gs1 = new GeometrySpace(gf.toGeometry(es[0]));
        GeometrySpace gs2 = new GeometrySpace(gf.toGeometry(es[1]));

        return new GeometrySpace[]{gs1, gs2};
    }

    public String toDBString() {
        WKBWriter writer = new WKBWriter();
        String str = WKBWriter.toHex(writer.write(geo));
        return "'" + str + "'";
    }

    public String toString() {
        if (geo.isRectangle())
            return geo.getEnvelopeInternal().toString();
        else
            return geo.toString();
    }

    public boolean equals(Object o) {
        return (o instanceof GeometrySpace) && geo.equals(((GeometrySpace) o).getGeometry());
    }

    public int hashCode() {
        return geo.hashCode();
    }

    public boolean overlaps(Space o) {
        if (!(o instanceof GeometrySpace)) return false;

        GeometrySpace ogs = (GeometrySpace) o;
        return geo.intersects(ogs.getGeometry());
    }

    public boolean partOf(Space o) {
        if (!(o instanceof GeometrySpace)) return false;

        GeometrySpace ogs = (GeometrySpace) o;
        return geo.coveredBy(ogs.getGeometry());
    }

    public boolean before(Space o) { return false; }

    public Relationship relate(Space o) {

        Geometry go = ((GeometrySpace) o).getGeometry();
        IntersectionMatrix im = geo.relate(go);

        return new Relationship() {
            public boolean isCovers() {
                return im.isCovers();
            }

            public boolean isCoveredBy() {
                return im.isCoveredBy();
            }

            public boolean isIntersects() {
                return im.isIntersects();
            }

            public boolean isBefore() {
                return false;
            }
        };
    }

    public Set<Integer> extractRoles(Space o) {
        if (!(o instanceof GeometrySpace)) return null;

        GeometrySpace ogs = (GeometrySpace) o;
        Set<Integer> rs = new HashSet<Integer>();

        rs.add(UNIQUE); //Always have UNIQUE
        GeometrySpace boundary = new GeometrySpace(geo.getBoundary());
        if (boundary.overlaps(ogs)) {
            rs.add(BOUNDARY);
            rs.add(BOUNDARY | UNIQUE);
        }
        if (!intersection(ogs).equals(boundary)) { // Interior must intersect o
            rs.add(INTERIOR);
            rs.add(INTERIOR | UNIQUE);
        }
        return rs;
    }

    public boolean overlaps(int tRole, int oRole, Space o) { 
        if (!(o instanceof GeometrySpace)) return false;
        if ((tRole & UNIQUE) != 0 || (oRole & UNIQUE) != 0) return false; //Nothing overlaps a UNIQUE-part

        Geometry ogeo = ((GeometrySpace) o).getGeometry();
        IntersectionMatrix im = geo.relate(ogeo);

        if (tRole == 0 && oRole == 0)
            return im.isIntersects();
        else if (tRole == 0 && (oRole & BOUNDARY) != 0)
            return im.matches("*T*******") || im.matches("****T****");
        else if (tRole == 0 && (oRole & INTERIOR) != 0)
            return im.matches("T********") || im.matches("***T*****");
        else if ((tRole & BOUNDARY) != 0 && oRole == 0)
            return im.matches("***T*****") || im.matches("****T****");
        else if ((tRole & BOUNDARY) != 0 && (oRole & BOUNDARY) != 0)
            return im.matches("****T****");
        else if ((tRole & BOUNDARY) != 0 && (oRole & INTERIOR) != 0)
            return im.matches("***T*****");
        else if ((tRole & INTERIOR) != 0 && oRole == 0)
            return im.matches("T********") || im.matches("*T*******");
        else if ((tRole & INTERIOR) != 0 && (oRole & BOUNDARY) != 0)
            return im.matches("*T*******");
        else if ((tRole & INTERIOR) != 0 && (oRole & INTERIOR) != 0)
            return im.matches("T********");
        else {
            System.err.println("ERROR: the roles " + tRole + ", " + oRole + " cannot be related or does not exist for this type.");
            (new Exception()).printStackTrace();
            System.exit(1);
            return false;
        }
    }

    public boolean partOf(int tRole, int oRole, Space o) {
        if (!(o instanceof GeometrySpace)) return false;
        if ((oRole & UNIQUE) != 0) return false; // Nothing is contained in a UNIQUE-part

        Geometry ogeo = ((GeometrySpace) o).getGeometry();
        IntersectionMatrix im = geo.relate(ogeo);

        if ((tRole | UNIQUE) == UNIQUE && oRole == 0) // tRole is UNIQUE or 0
            return im.isCoveredBy();
        else if ((tRole | UNIQUE) == UNIQUE && (oRole & BOUNDARY) != 0)
            return im.matches("FTFFTF***");
        else if ((tRole | UNIQUE) == UNIQUE && (oRole & INTERIOR) != 0)
            return im.matches("TFFTFF***");
        else if ((tRole & BOUNDARY) != 0 && oRole == 0)
            return im.matches("*****F***");
        else if ((tRole & BOUNDARY) != 0 && (oRole & BOUNDARY) != 0)
            return im.matches("***FTF***");
        else if ((tRole & BOUNDARY) != 0 && (oRole & INTERIOR) != 0)
            return im.matches("***TFF***");
        else if ((tRole & INTERIOR) != 0 && oRole == 0)
            return im.matches("**F******");
        else if ((tRole & INTERIOR) != 0 && (oRole & BOUNDARY) != 0)
            return im.matches("FTF******");
        else if ((tRole & INTERIOR) != 0 && (oRole & INTERIOR) != 0)
            return im.matches("TFF******");
        else {
            System.err.println("ERROR: the roles " + tRole + ", " + oRole + " cannot be related or does not exist for this type.");
            (new Exception()).printStackTrace();
            System.exit(1);
            return false;
        }
    }

    public boolean before(int tRole, int oRole, Space o) { return false; }

    public Relationship relate(int tRole, int oRole, Space o) { return null; } //TODO(?)
}
