package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import org.neo4j.graphdb.PropertyContainer;

public class PropertySelector implements ISelector{
	private final String property;


	public PropertySelector(String property) {
		super();
		this.property = property;
	}

	@Override
	public Object get(PropertyContainer container) {
		return container.hasProperty(property) ? container.getProperty(property) : null;
	}

	@Override
	public String toString() {
		return " prop:"+property;
	}

	@Override
	public String toCypher(String var, boolean isNode) {
		return var+"."+property;
	}
}
