package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.Map;

import org.caleydo.neo4j.plugins.kshortestpaths.constraints.RelConstraints.IRelConstraint;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.Predicate;

public class PropertyLabelConstraint extends TimeConstraint implements IRelConstraint {

	private final  Predicate<Object> c;

	public PropertyLabelConstraint(Integer times, Predicate<Object> c) {
		super(times);
		this.c = c;
	}

	public static PropertyLabelConstraint parse(Map<String, Object> desc) {
		return new PropertyLabelConstraint((Integer) desc.get("times"),
				OperatorConstraint.parse(desc));
	}

	@Override
	public boolean accept(Relationship item) {
		return c.accept(item.getType().name());
	}

}
