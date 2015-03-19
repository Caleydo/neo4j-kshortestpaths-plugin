package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

public class Region {
	private final int minIndex;
	private final int maxIndex;
	
	public Region(int minIndex, int maxIndex) {
		this.minIndex = minIndex;
		this.maxIndex = maxIndex;
	}
	
	public int getMaxIndex() {
		return maxIndex;
	}
	
	public int getMinIndex() {
		return minIndex;
	}
	
}
