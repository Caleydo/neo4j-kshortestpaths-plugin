package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.Map;

import org.caleydo.neo4j.plugins.kshortestpaths.constraints.RelConstraints.IRelConstraint;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.Predicate;

public class PropertyLabelConstraint extends TimeConstraint implements IRelConstraint {

	private final  Predicate<Object> c;

	public PropertyLabelConstraint(Number minTimes, Number times, Predicate<Object> c) {
		super(minTimes, times);
		this.c = c;
	}

	public static PropertyLabelConstraint parse(Map<String, Object> desc) {
		return new PropertyLabelConstraint((Number) desc.get("minTimes"), (Number) desc.get("times"),
				OperatorConstraint.parse(desc));
	}

	@Override
	public boolean accept(Relationship item) {
		return c.accept(item.getType().name());
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("{ label ");
		b.append(c);
		super.toString(b);
		b.append("}");
		return b.toString();
	}

}
