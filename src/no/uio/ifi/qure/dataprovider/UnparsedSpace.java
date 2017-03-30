package no.uio.ifi.qure.dataprovider;

import java.util.List;

public class UnparsedSpace<E> {
	
	public Integer uri;
	public List<E> unparsedSpace;

	public UnparsedSpace(Integer uri, List<E> unparsedSpace) {
		this.uri = uri;
		this.unparsedSpace = unparsedSpace;
	}
}
