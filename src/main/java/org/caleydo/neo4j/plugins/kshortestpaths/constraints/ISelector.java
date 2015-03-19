package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import org.neo4j.graphdb.PropertyContainer;

public interface ISelector {
	Object get(PropertyContainer container);
	
	String toCypher(String var, boolean isNode);
}
