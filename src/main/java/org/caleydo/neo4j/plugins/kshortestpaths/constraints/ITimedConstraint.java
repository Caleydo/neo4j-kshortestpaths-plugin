package org.caleydo.neo4j.plugins.kshortestpaths.constraints;


public interface ITimedConstraint extends IConstraint {
	
	boolean acceptIntermediate(int hits, int notHits);
	
	boolean accept(int hits, int notHits);
}
