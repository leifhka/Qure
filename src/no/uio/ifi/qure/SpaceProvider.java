package no.uio.ifi.qure;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public interface SpaceProvider {

    public void populateBulk();

    public void populateUpdate();

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

    public SpaceProvider[] splitProvider(int split);

    public SpaceProvider[] splitProvider(int split, EvenSplit evenSplit);

    public default EvenSplit getEvenSplit(int split, Config config) {

        Space spL = makeEmptySpace(), spR = makeEmptySpace();
        Space[] splitLR = getUniverse().split(split);
        Space splitL = splitLR[0], splitR = splitLR[1];

        Set<Integer> undecided = new HashSet<Integer>(keySet());
        Set<Integer> intL = new HashSet<Integer>(), intR = new HashSet<Integer>();
        Set<Integer> intSpL = Utils.getIntersecting(splitL, undecided, getSpaces(), config.numThreads);
        Set<Integer> intSpR = Utils.getIntersecting(splitR, undecided, getSpaces(), config.numThreads);

        int diff = Math.abs(intSpL.size() - intSpR.size());
        int bestDiff = diff;

        Block splitBlock = Block.getTopBlock();
        Set<Integer> intAllL = intSpL, intAllR = intSpR;
        EvenSplit bestSplit = new EvenSplit(splitBlock, intSpL, intSpR);

        int i = 0;

        while (i++ < config.maxSplits && ((double) Math.min(intAllR.size(), intAllL.size())) / Math.max(intAllR.size(), intAllL.size()) < config.minRatio) {

            if (intAllR.size() > intAllL.size()) {

                spL = spL.union(splitL);
                intL.addAll(intSpL);
                undecided = new HashSet<Integer>(intSpR);

                splitLR = splitR.split(split);
                splitBlock = splitBlock.addOne();

            } else {

                spR = spR.union(splitR);
                intR.addAll(intSpR);
                undecided = new HashSet<Integer>(intSpL);

                splitLR = splitL.split(split);
                splitBlock = splitBlock.addZero();
            }

            splitL = splitLR[0];
            splitR = splitLR[1];

            intSpL = Utils.getIntersecting(splitL, undecided, getSpaces(), config.numThreads);
            intSpR = Utils.getIntersecting(splitR, undecided, getSpaces(), config.numThreads);

            intAllL = Utils.union(intL, intSpL);
            intAllR = Utils.union(intR, intSpR);
            diff = Math.abs(intAllL.size() - intAllR.size());
            if (diff < bestDiff) {
                bestSplit = new EvenSplit(new Block(splitBlock.getRepresentation()), 
                                          new HashSet<Integer>(intAllL), 
                                          new HashSet<Integer>(intAllR));
                bestDiff = diff;
            }
        }
        return bestSplit;
    }
}
