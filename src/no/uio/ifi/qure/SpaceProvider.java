package no.uio.ifi.qure;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public interface SpaceProvider {

    public void populate();

    public Map<Integer, ? extends Space> getSpaces();

    public Space getUniverse();

    public Space makeEmptySpace();

    public Set<Integer> getCoversUniverse();

    public default Set<Integer> keySet() {
        return getSpaces().keySet();
    }

    public default boolean isEmpty() {
        return keySet().isEmpty();
    }

    public default int size() {
        return keySet().size();
    }

    public Space get(Integer uri);

    public void populateWithExternalOverlapping();

    public SpaceProvider[] splitProvider(int split, int depth);

    public SpaceProvider[] splitProvider(int split, int depth, Block splitBlock);

    public default Set<Integer> getIntersecting(Space space) {

        Set<Integer> intersecting = new HashSet<Integer>();

        for (Integer uri : keySet()) {
            if (get(uri).intersects(space))
                intersecting.add(uri);
        }

        return intersecting;
    }

    public default Block getEvenSplit(int split, int maxSplits, int maxDiff) {

        Space spL = makeEmptySpace(), spR = makeEmptySpace();
        Space[] splitLR = getUniverse().split(split);
        Space splitL = splitLR[0], splitR = splitLR[1];

        Set<Integer> intSpL = getIntersecting(spL.union(splitL));
        Set<Integer> intSpR = getIntersecting(spR.union(splitR));
        int diff = Math.abs(intSpL.size() - intSpR.size());
        int bestDiff = diff;

        Block splitBlock = Block.TOPBLOCK;
        Block bestSplit = splitBlock;

        int i = 0;

        while (i++ < maxSplits && diff > maxDiff) {

            if (intSpR.size() > intSpL.size()) {
                spL = spL.union(splitL);
                splitLR = splitR.split(split);
                splitL = splitLR[0];
                splitR = splitLR[1];
                splitBlock = splitBlock.addOne();
            } else {
                spR = spR.union(splitR);
                splitLR = splitL.split(split);
                splitL = splitLR[0];
                splitR = splitLR[1];
                splitBlock = splitBlock.addZero();
            }

            intSpL = getIntersecting(spL.union(splitL));
            intSpR = getIntersecting(spR.union(splitR));
            diff = Math.abs(intSpL.size() - intSpR.size());
            if (diff < bestDiff) {
                bestSplit = splitBlock;
                bestDiff = diff;
            }
        }
        return bestSplit;
    }
}
