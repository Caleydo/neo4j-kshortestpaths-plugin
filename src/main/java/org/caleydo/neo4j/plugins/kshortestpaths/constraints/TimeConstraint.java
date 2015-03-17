package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

public class TimeConstraint {
	private final int min, max;
	
	public TimeConstraint(int min, int max) {
		this.min = min;
		this.max = max;
	}
	public TimeConstraint(Number min, Number max) {
		this(min == null ? 0 : min.intValue(), max == null ? Integer.MAX_VALUE : max.intValue());
	}


	public boolean timesIntermediate(int hits, int pathLength) {
		int notHits = pathLength - hits;
		if (this.max == 0) { //zero times occur, anti pattern
			return hits == 0;
		}
		if (this.max > 0) { //at most n times
			return this.max == Integer.MAX_VALUE ? notHits == 0 : hits <= this.max;
		}
		//at most not n times
		return notHits <= -this.max;
	}
	
	public boolean timesFinal(int hits, int pathLength) {
		int notHits = pathLength - hits;
		if (this.max == 0) { //zero times occur, anti pattern
			return hits == 0;
		}
		if (this.min > 0 && hits < min) {
			return false;
		}
		if (this.max > 0) { //at most n times
			return this.max == Integer.MAX_VALUE ? notHits == 0 : hits <= this.max;
		}
		//at most not n times
		return notHits <= -this.max;
	}
	
	protected void toString(StringBuilder b) {
		b.append(" times:");
		if (max == Integer.MAX_VALUE) {
			b.append("n");
		} else {
			b.append(max);
		}
		if (this.min > 0) {
			b.append( "minTimes:").append(this.min);
		}
	}
}
