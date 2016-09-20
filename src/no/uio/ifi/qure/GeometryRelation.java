package no.uio.ifi.qure;

import com.vividsolutions.jts.geom.IntersectionMatrix;

public class GeometryRelation implements Relation {

    IntersectionMatrix im;

    public GeometryRelation(IntersectionMatrix im) {
        this.im = im;
    }

    public boolean isCovers() {
        return im.isCovers();
    }

    public boolean isCoveredBy() {
        return im.isCoveredBy();
    }

    public boolean isIntersects() {
        return im.isIntersects();
    }
}
