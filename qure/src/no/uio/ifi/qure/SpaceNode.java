package no.uio.ifi.qure;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;

public class SpaceNode {

    private final SpaceProvider spaces;
    private final boolean xSplit; // if this block was splitted along the x-axis.
    private final Bintree block; // the bintree block of this spaceNode.

    public SpaceNode(Bintree block, SpaceProvider spaces, boolean xSplit) {
        this.block = block;
        this.spaces = spaces;
        this.xSplit = xSplit;
    }

    public Set<Integer> getOverlappingURIs() { return spaces.keySet(); }

    public SpaceProvider getSpaceProvider() { return spaces; }

    public boolean isXSplit() { return xSplit; }

    public Bintree getBlock() { return block; }

    public boolean isEmpty() {
        return getOverlappingURIs().isEmpty();
    }
}


