package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.BitSet;
import java.util.SortedSet;
import java.util.TreeSet;



public class MatchRegion implements Comparable<MatchRegion> {
	private int minIndex;
	private int maxIndex;

	public MatchRegion(int minIndex, int maxIndex) {
		this.minIndex = minIndex;
		this.maxIndex = maxIndex;	
	}
	
	@Override
	public String toString() {
		return "MatchRegion [minIndex=" + minIndex + ", maxIndex=" + maxIndex + "]";
	}

	public MatchRegion toAbs(int length) {
		return new MatchRegion(getMinIndex(length), getMaxIndex(length));
	}
	
	
	public static SortedSet<MatchRegion> from(BitSet s) {
		SortedSet<MatchRegion> r = new TreeSet<MatchRegion>();
		if (s.isEmpty()) {
			return r;
		}
		int start = s.nextSetBit(0), prev = start;
		for(int i = s.nextSetBit(start); i >= 0; i = s.nextSetBit(i+1)) {
			if (i > (prev+1)) { //break in the set
				r.add(new MatchRegion(start, prev));
				start = i;
			}
			prev = i;
		}
		r.add(new MatchRegion(start, prev));
		return r;
	}

	public static BitSet toSet(Iterable<MatchRegion> it, int length) {
		BitSet s = new BitSet();
		for(MatchRegion r : it) {
			r.toBits(s, length);
		}
		return s;
	}
	
	@Override
	public int compareTo(MatchRegion o) {
		int m = this.minIndex - o.minIndex;
		if (m != 0) {
			return m;
		}
		return this.maxIndex - o.maxIndex;
	}
	
	public void toBits(BitSet s, int length) {
		s.set(getMinIndex(length), getMaxIndex(length)+1);
	}

	public int getMinIndex(int length) {
		if (minIndex < 0) {
			return length + minIndex;
		}
		return minIndex;
	}
	
	public int getMaxIndex(int length) {
		if (maxIndex < 0) {
			return length + maxIndex;
		}
		return maxIndex;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + maxIndex;
		result = prime * result + minIndex;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MatchRegion other = (MatchRegion) obj;
		if (maxIndex != other.maxIndex)
			return false;
		if (minIndex != other.minIndex)
			return false;
		return true;
	}

	public boolean isStart() {
		return this.minIndex == this.maxIndex && this.minIndex == 0;
	}
	public boolean isEnd() {
		return this.minIndex == this.maxIndex && this.minIndex == -1;
	}
}
