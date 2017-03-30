package no.uio.ifi.qure;

public interface Relationship {

	public boolean isCovers();

	public boolean isCoveredBy();

	public boolean isIntersects();

	public boolean isBefore();
}
