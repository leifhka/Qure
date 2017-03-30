package no.uio.ifi.qure.traversal;

public class SID {
	
	private final int id;
	private final int role;

	public SID(int id) {
		this.id = id;
		role = 0;
	}

	public SID(int id, int role) {
		this.id = id;
		this.role = role;
	}

	public int getID() { return id; }

	public int getRole() { return role; }

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SID)) return false;
	
		SID os = (SID) o;
	
		return (getID() == os.getID()) && (getRole() == os.getRole());
	}

	@Override
	public int hashCode() { return role + id; }

	public String toString() { return "(" + id + ", " + role + ")"; }
}
