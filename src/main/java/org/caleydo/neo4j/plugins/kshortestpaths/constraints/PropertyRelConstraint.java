package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.Map;

import org.caleydo.neo4j.plugins.kshortestpaths.constraints.RelConstraints.IRelConstraint;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.Predicate;

public class PropertyRelConstraint extends TimeConstraint implements IRelConstraint {

	private final Predicate<Object> c;
	private final String property;

	public PropertyRelConstraint(Number minTimes, Number times, String property, Predicate<Object> c) {
		super(minTimes, times);
		this.property = property;
		this.c = c;
	}

	public static PropertyRelConstraint parse(String property, Map<String, Object> desc) {
		return new PropertyRelConstraint((Number) desc.get("minTimes"), (Number) desc.get("times"), property,
				OperatorConstraint.parse(desc));
	}

	@Override
	public boolean accept(Relationship item) {
		if (!item.hasProperty(property)) {
			return false;
		}
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
