package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.Map;

import org.caleydo.neo4j.plugins.kshortestpaths.constraints.NodeConstraints.INodeConstraint;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.Predicate;

public class PropertyNodeConstraint extends TimeConstraint implements INodeConstraint {

	private final Predicate<Object> c;
	private final String property;

	public PropertyNodeConstraint(Integer times, String property, Predicate<Object> c) {
		super(times);
		this.property = property;
		this.c = c;
	}

	public static PropertyNodeConstraint parse(String property, Map<String, Object> desc) {
		return new PropertyNodeConstraint((Integer) desc.get("times"), property,
				OperatorConstraint.parse(desc));
	}

	@Override
	public boolean accept(Node item) {
		return c.accept(item.getProperty(property));
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("{ prop:").append(property).append(' ');
		b.append(c);
		super.toString(b);
		b.append("}");
		return b.toString();
	}

}
