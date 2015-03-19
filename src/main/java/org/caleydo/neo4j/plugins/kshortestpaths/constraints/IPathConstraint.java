package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.SortedSet;

import org.neo4j.graphdb.Path;
import org.neo4j.helpers.Predicate;


public interface IPathConstraint extends Predicate<Path>{
	SortedSet<MatchRegion> matches(Path path);
}
