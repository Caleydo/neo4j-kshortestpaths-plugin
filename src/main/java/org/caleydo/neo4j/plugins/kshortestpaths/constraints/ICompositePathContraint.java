package org.caleydo.neo4j.plugins.kshortestpaths.constraints;


public interface ICompositePathContraint extends IPathConstraint {
	Iterable<? extends IPathConstraint> children();
}