package no.uio.ifi.qure.space;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

import no.uio.ifi.qure.*;
import no.uio.ifi.qure.relation.*;
import no.uio.ifi.qure.util.*;
import no.uio.ifi.qure.bintree.*;
import no.uio.ifi.qure.traversal.*;

public interface SpaceProvider {

	public void populateBulk();

	public void populateUpdate();

	public Map<SID, ? extends Space> getSpaces();

	public Space getUniverse();

	public Space makeEmptySpace();

	public Set<SID> getCoversUniverse();

	public default Set<SID> keySet() {
		return getSpaces().keySet();
	}

	public default boolean isEmpty() {
		return keySet().isEmpty();
	}

	public default int size() {
		return keySet().size();
	}

	public Space get(SID uri);

	public void populateWithExternalOverlapping();

	public SpaceProvider[] splitProvider(int split);

	public SpaceProvider[] splitProvider(int split, EvenSplit evenSplit);

	// TODO: Long method, split into smaller 
	public default EvenSplit getEvenSplit(int split, Config config) {

		Space spL = makeEmptySpace(), spR = makeEmptySpace();
		Space[] splitLR = getUniverse().split(split);
		Space splitL = splitLR[0], splitR = splitLR[1];

		Set<SID> undecided = new HashSet<SID>(keySet());
		Set<SID> intL = new HashSet<SID>(), intR = new HashSet<SID>();
		Set<SID> intSpL = Utils.getIntersecting(splitL, undecided, getSpaces(), config.numThreads);
		Set<SID> intSpR = Utils.getIntersecting(splitR, undecided, getSpaces(), config.numThreads);

		int diff = Math.abs(intSpL.size() - intSpR.size());
		int bestDiff = diff;

		Block splitBlock = Block.getTopBlock();
		Set<SID> intAllL = intSpL, intAllR = intSpR;
		EvenSplit bestSplit = new EvenSplit(splitBlock, intSpL, intSpR);

		int i = 0;
		double ratio = config.minRatio;

		while (i++ < config.maxSplits && ratio < config.minRatio) {

			if (intAllR.size() > intAllL.size()) {

				spL = spL.union(splitL);
				intL.addAll(intSpL);
				undecided = new HashSet<SID>(intSpR);

				splitLR = splitR.split(split);
				splitBlock = splitBlock.addOne();

			} else {

				spR = spR.union(splitR);
				intR.addAll(intSpR);
				undecided = new HashSet<SID>(intSpL);

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
				                          new HashSet<SID>(intAllL), 
				                          new HashSet<SID>(intAllR));
				bestDiff = diff;
			}

			ratio = ((double) Math.min(intAllR.size(), intAllL.size())) / Math.max(intAllR.size(), intAllL.size());
		}
		return bestSplit;
	}
}
