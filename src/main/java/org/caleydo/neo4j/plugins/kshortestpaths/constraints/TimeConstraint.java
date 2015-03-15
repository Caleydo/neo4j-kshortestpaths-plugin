package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

public class TimeConstraint {
	private final int n;
	
	public TimeConstraint(int n) {
		this.n = n;
	}
	public TimeConstraint(Number value) {
		this(value == null ? Integer.MAX_VALUE : value.intValue());
	}


	public boolean times(int hits, int pathLength) {
		int notHits = pathLength - hits;
		if (this.n == 0) { //zero times occur, anti pattern
			return hits == 0;
		}
		if (this.n > 0) { //at most n times
			return this.n == Integer.MAX_VALUE ? notHits == 0 : hits <= this.n;
		}
		//at most not n times
		return notHits <= -this.n;
	}
	
	protected void toString(StringBuilder b) {
		b.append(" times:");
		if (n == Integer.MAX_VALUE) {
			b.append("n");
		} else {
			b.append(n);
		}
	}
}
