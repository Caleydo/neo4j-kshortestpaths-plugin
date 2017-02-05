package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.SortedSet;
import java.util.function.Predicate;

import org.neo4j.graphdb.Path;


public interface IPathConstraint extends Predicate<Path>{
	SortedSet<MatchRegion> matches(Path path);
}
