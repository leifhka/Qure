package no.uio.ifi.qure;

import java.util.ArrayList;
import java.util.List;

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

    public GeometrySpace clone() { return new GeometrySpace((Geometry)(Geometry)  getGeometry().clone()); }

    public Geometry getGeometry() { return geo; }

    public boolean isEmpty() { return geo.isEmpty(); }

    public boolean isPoint() { return geo.getDimension() == 0; }

    public int getComplexityMeasure() { return geo.getNumPoints(); }

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
        return str;
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

    public GeometryRelation relate(Space o) {

        Geometry go = ((GeometrySpace) o).getGeometry();
        return new GeometryRelation(geo.relate(go));
    }
}
