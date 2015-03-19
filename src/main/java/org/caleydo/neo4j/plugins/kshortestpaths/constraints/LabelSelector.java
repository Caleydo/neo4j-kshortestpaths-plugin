package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.Function;
import org.neo4j.helpers.collection.Iterables;

public class LabelSelector implements ISelector{
	public static final LabelSelector INSTANCE = new LabelSelector();
	
	private LabelSelector() {
	}
	
	@Override
	public Object get(PropertyContainer container) {
		if (container instanceof Node) {
			return Iterables.toArray(String.class, Iterables.map(new Function<Label,String>() {
				@Override
				public String apply(Label from) {
					return from.name();
				}
			}, ((Node)container).getLabels()));
		}
		if (container instanceof Relationship) {
			return ((Relationship) container).getType().name();
		}
		return null;
	}
	@Override
	public String toString() {
		return " label";
	}
}
