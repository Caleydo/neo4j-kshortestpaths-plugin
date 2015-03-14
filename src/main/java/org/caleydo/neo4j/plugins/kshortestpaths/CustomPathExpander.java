package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.List;
import java.util.Map;

import org.caleydo.neo4j.plugins.kshortestpaths.constraints.DirectionContraints;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.NodeConstraints;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.RelConstraints;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.FilteringIterable;

/**
 * custom path expander
 * @author sam
 *
 */
public class CustomPathExpander implements PathExpander<Object> {
	
	private final DirectionContraints directions;
	private final NodeConstraints nodeConstraints;
	private final RelConstraints relConstraints;

	public CustomPathExpander(Map<String,String> directions, List<Map<String,Object>> nodeContraints,List<Map<String,Object>> relContraints) {
		this(new DirectionContraints(directions), new NodeConstraints(nodeContraints), new RelConstraints(relContraints));
	}

	public CustomPathExpander(DirectionContraints directions, NodeConstraints nodeContraints,RelConstraints relConstraints) {
		super();
		this.directions = directions;
		this.nodeConstraints = nodeContraints;
		this.relConstraints = relConstraints;
		
	}
	
	@Override
	public Iterable<Relationship> expand(final Path path, BranchState<Object> state) {
		final Predicate<Node> goodNode = this.nodeConstraints.prepare(path.nodes());
		final Predicate<Relationship> goodRel = this.relConstraints.prepare(path.relationships());
		final Node endNode = path.endNode();
		return new FilteringIterable<>(this.directions.filter(endNode), new Predicate<Relationship>() {
			@Override
			public boolean accept(Relationship item) {
				if (!goodRel.accept(item)) {
					return false;
				}
				Node added = item.getOtherNode(endNode);
				return goodNode.accept(added);
			}
		});
	}

	@Override
	public PathExpander<Object> reverse() {
		return new CustomPathExpander(this.directions.reverse(), this.nodeConstraints, this.relConstraints);
	}
	
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("CustomPathExpander {");
		b.append("node: ").append(nodeConstraints);
		b.append("rel: ").append(relConstraints);
		b.append("dir: ").append(directions);
		b.append('}');
		return b.toString();
	}
	
}