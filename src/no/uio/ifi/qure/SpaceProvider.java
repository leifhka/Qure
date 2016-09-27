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

    public SpaceProvider[] splitProvider(int split, int depth, EvenSplit evenSplit);

    public default EvenSplit getEvenSplit(int split, int maxSplits, int maxDiff) {

        Space spL = makeEmptySpace(), spR = makeEmptySpace();
        Space[] splitLR = getUniverse().split(split);
        Space splitL = splitLR[0], splitR = splitLR[1];

        Set<Integer> undecided = new HashSet<Integer>(keySet());
        Set<Integer> intL = new HashSet<Integer>(), intR = new HashSet<Integer>();
        Set<Integer> intSpL = Utils.getIntersecting(splitL, undecided, getSpaces());
        Set<Integer> intSpR = Utils.getIntersecting(splitR, undecided, getSpaces());

        int diff = Math.abs(intSpL.size() - intSpR.size());
        int bestDiff = diff;

        Block splitBlock = Block.TOPBLOCK;
        EvenSplit bestSplit = new EvenSplit(splitBlock, intSpL, intSpR);

        int i = 0;

        while (i++ < maxSplits && diff > maxDiff) {

            if (intSpR.size() > intSpL.size()) {

                spL = spL.union(splitL);
                intL.addAll(intSpL);
                undecided.removeAll(intSpL);

                splitLR = splitR.split(split);
                splitL = splitLR[0];
                splitR = splitLR[1];
                splitBlock = splitBlock.addOne();

            } else {

                spR = spR.union(splitR);
                intR.addAll(intSpR);
                undecided.removeAll(intSpR);

                splitLR = splitL.split(split);
                splitL = splitLR[0];
                splitR = splitLR[1];
                splitBlock = splitBlock.addZero();
            }

            intSpL = Utils.getIntersecting(splitL, undecided, getSpaces());
            intSpR = Utils.getIntersecting(splitR, undecided, getSpaces());
            diff = Math.abs(Utils.union(intL, intSpL).size() - Utils.union(intR, intSpR).size());
            if (diff < bestDiff) {
                bestSplit = new EvenSplit(splitBlock, Utils.union(intL, intSpL), Utils.union(intR, intSpR));
                bestDiff = diff;
            }
        }
        return bestSplit;
    }
}
