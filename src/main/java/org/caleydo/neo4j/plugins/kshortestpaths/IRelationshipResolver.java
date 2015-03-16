package org.caleydo.neo4j.plugins.kshortestpaths;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public interface IRelationshipResolver {
	Iterable<Relationship> getRelationships(Node node); 
}
