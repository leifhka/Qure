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

	public void clear();

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

	public SpaceProvider makeSubProvider(Space uni, Set<SID> sids);

	public default Pair<SpaceProvider, SpaceProvider> makeSubProviders(Space childUniL, Space childUniR,
	                                                                   Set<SID> intL, Set<SID> intR) {
		SpaceProvider subPL = makeSubProvider(childUniL, intL);
		SpaceProvider subPR = makeSubProvider(childUniR, intR);
		return new Pair<SpaceProvider, SpaceProvider>(subPL, subPR);
	}

	public default Pair<SpaceProvider, SpaceProvider> splitProvider(int split) {
		Pair<? extends Space, ? extends Space> childUniverseres = getUniverse().split(split);
		return makeSubProviders(childUniverseres.fst, childUniverseres.snd, keySet(), keySet());
	}

	public default Pair<SpaceProvider, SpaceProvider> splitProvider(int split, Block splitBlock, Set<SID> sids) {
		Pair<? extends Space, ? extends Space> splitSpaces = computeSpacesFromSplit(splitBlock, split);
		return makeSubProviders(splitSpaces.fst, splitSpaces.snd, sids, sids);
	}

	public default Pair<SpaceProvider, SpaceProvider> splitProvider(int split, EvenSplit evenSplit) {
		return makeSubProviders(evenSplit.uniL, evenSplit.uniR, evenSplit.intL, evenSplit.intR);
	}

	public default Pair<? extends Space, ? extends Space> computeSpacesFromSplit(Block splitBlock, int split) {

		Space spL = makeEmptySpace(), spR = makeEmptySpace();
		Pair<? extends Space, ? extends Space> splitLR = getUniverse().split(split);
		Space splitL = splitLR.fst, splitR = splitLR.snd;

		for (int i = 0; i < splitBlock.depth(); i++) {

			if (splitBlock.getBit(i) == 1L) {
				spL = spL.union(splitL);
				splitLR = splitR.split(split);
				splitL = splitLR.fst;
				splitR = splitLR.snd;
			} else {
				spR = spR.union(splitR);
				splitLR = splitL.split(split);
				splitL = splitLR.fst;
				splitR = splitLR.snd;
			}
		}

		return new Pair<Space, Space>(spL.union(splitL), spR.union(splitR));
	}

	// TODO: Long method, split into smaller 
	public default EvenSplit getEvenSplit(int split, Config config) {

		Space spL = makeEmptySpace(), spR = makeEmptySpace();
		Pair<? extends Space, ? extends Space> splitLR = getUniverse().split(split);
		Space splitL = splitLR.fst.toUniverse(), splitR = splitLR.snd.toUniverse();

		Set<SID> undecided = new HashSet<SID>(keySet());
		Set<SID> intL = new HashSet<SID>(), intR = new HashSet<SID>();
		Set<SID> intSpL = Utils.getIntersecting(splitL, undecided, getSpaces(), config.numThreads);
		Set<SID> intSpR = Utils.getIntersecting(splitR, undecided, getSpaces(), config.numThreads);

		int diff = Math.abs(intSpL.size() - intSpR.size());
		int bestDiff = diff;

		Block splitBlock = Block.getTopBlock();
		Set<SID> intAllL = intSpL, intAllR = intSpR;
		EvenSplit bestSplit = new EvenSplit(splitBlock, intSpL, intSpR, splitL, splitR);

		int i = 0;
		double ratio = ((double) Math.min(intSpR.size(), intSpL.size())) / Math.max(intSpR.size(), intSpL.size());

		while (i++ < config.maxSplits && ratio < config.minRatio) {

			if (intAllR.size() > intAllL.size()) {

				spL = spL.union(splitL).toUniverse();
				intL.addAll(intSpL);
				undecided = new HashSet<SID>(intSpR);

				splitLR = splitR.split(split);
				splitBlock = splitBlock.addOne();

			} else {

				spR = spR.union(splitR).toUniverse();
				intR.addAll(intSpR);
				undecided = new HashSet<SID>(intSpL);

				splitLR = splitL.split(split);
				splitBlock = splitBlock.addZero();
			}

			splitL = splitLR.fst.toUniverse();
			splitR = splitLR.snd.toUniverse();

			intSpL = Utils.getIntersecting(splitL, undecided, getSpaces(), config.numThreads);
			intSpR = Utils.getIntersecting(splitR, undecided, getSpaces(), config.numThreads);

			intAllL = Utils.union(intL, intSpL);
			intAllR = Utils.union(intR, intSpR);
			diff = Math.abs(intAllL.size() - intAllR.size());

			if (diff < bestDiff) {
				bestSplit = new EvenSplit(new Block(splitBlock.getRepresentation()), 
				                          new HashSet<SID>(intAllL), 
				                          new HashSet<SID>(intAllR),
				                          spL.union(splitL).toUniverse(),
				                          spR.union(splitR).toUniverse());
				bestDiff = diff;
			}

			ratio = ((double) Math.min(intAllR.size(), intAllL.size())) / Math.max(intAllR.size(), intAllL.size());
		}
		return bestSplit;
	}
}
