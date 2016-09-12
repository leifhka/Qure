package no.uio.ifi.qure;

import java.util.Map;
import java.util.Set;

public interface SpaceProvider {

    public void populate();

    public Map<Integer, ? extends Space> getSpaces();

    public Space getUniverse();

    public Set<Integer> getCoversUniverse();

    public default Set<Integer> keySet() {
        return getSpaces().keySet();
    }

    public default boolean isEmpty() {
        return keySet().isEmpty();
    }

    public Space get(Integer uri);

    public void populateWithExternalOverlapping();

    public SpaceProvider[] splitProvider(boolean xSplit, int depth);

    public SpaceProvider[] splitProvider(int split, int depth);
}
