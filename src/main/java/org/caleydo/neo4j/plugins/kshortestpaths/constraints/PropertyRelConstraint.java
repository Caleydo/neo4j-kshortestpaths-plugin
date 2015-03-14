package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.Map;

import org.caleydo.neo4j.plugins.kshortestpaths.constraints.RelConstraints.IRelConstraint;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.Predicate;

public class PropertyRelConstraint extends TimeConstraint implements IRelConstraint {

	private final Predicate<Object> c;
	private final String property;

	public PropertyRelConstraint(Integer times, String property, Predicate<Object> c) {
		super(times);
		this.property = property;
		this.c = c;
	}

	public static PropertyRelConstraint parse(String property, Map<String, Object> desc) {
		return new PropertyRelConstraint((Integer) desc.get("times"), property,
				OperatorConstraint.parse(desc));
	}

	@Override
	public boolean accept(Relationship item) {
		return c.accept(item.getProperty(property));
	}

}
