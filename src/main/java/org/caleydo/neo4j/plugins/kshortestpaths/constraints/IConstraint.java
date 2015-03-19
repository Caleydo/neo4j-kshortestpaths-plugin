package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public interface IConstraint extends IPathConstraint {
	boolean accept(Node node, Relationship rel);
	
	void toCypher(StringBuilder b, String var);
}


