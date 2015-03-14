package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.Map;

import org.caleydo.neo4j.plugins.kshortestpaths.constraints.NodeConstraints.INodeConstraint;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.Function;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.Iterables;

public class LabelConstraint extends TimeConstraint implements INodeConstraint {

	private final Predicate<Object> c;

	public LabelConstraint(Integer times, Predicate<Object> c) {
		super(times);
		this.c = c;
	}

	public static LabelConstraint parse(Map<String, Object> desc) {
		return new LabelConstraint((Integer) desc.get("times"),
				OperatorConstraint.parse(desc));
	}

	@Override
	public boolean accept(Node item) {
		return c.accept(Iterables.map(new Function<Label, String>() {
			@Override
			public String apply(Label from) {
				return from.name();
			};
		}, item.getLabels()));
	}

}
