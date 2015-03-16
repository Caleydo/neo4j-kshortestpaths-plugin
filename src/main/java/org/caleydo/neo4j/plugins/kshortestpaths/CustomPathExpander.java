package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.List;
import java.util.Map;

import org.caleydo.neo4j.plugins.kshortestpaths.constraints.DirectionContraints;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.InlineRelationships;
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
public class CustomPathExpander implements PathExpander<Object>, IRelationshipResolver {
	
	private final DirectionContraints directions;
	private final NodeConstraints nodeConstraints;
	private final RelConstraints relConstraints;
	private final InlineRelationships inline;

	public CustomPathExpander(Map<String,String> directions, List<Map<String,Object>> nodeContraints,List<Map<String,Object>> relContraints) {
		this(new DirectionContraints(directions), new NodeConstraints(nodeContraints), new RelConstraints(relContraints), null);
	}

	public CustomPathExpander(DirectionContraints directions, NodeConstraints nodeContraints,RelConstraints relConstraints, InlineRelationships inline) {
		super();
		this.directions = directions;
		this.nodeConstraints = nodeContraints;
		this.relConstraints = relConstraints;
		this.inline = inline;
		
	}
	
	@Override
	public Iterable<Relationship> expand(final Path path, BranchState<Object> state) {
		final Predicate<Node> goodNode = this.nodeConstraints.prepare(path.nodes());
		final Predicate<Relationship> goodRel = this.relConstraints.prepare(path.relationships());
		final Node endNode = path.endNode();
		Iterable<Relationship> base = getRelationships(endNode);
		return new FilteringIterable<>(base, new Predicate<Relationship>() {
			@Override
			public boolean accept(Relationship item) {
				if (!goodRel.accept(item)) {
					//System.out.println("test: "+item+" bad rel");
					return false;
				}
				Node added = item.getOtherNode(endNode);
				boolean r= goodNode.accept(added);
				//if (!r) {
				//	System.out.println("test: "+added+" bad node");
				//} else {
				//	System.out.println("test: "+item+" "+added+" good");
				//}
				return r;
			}
		});
	}

	@Override
	public Iterable<Relationship> getRelationships(final Node endNode) {
		Iterable<Relationship> base = this.directions.filter(endNode);
		if (inline != null) {
			base = inline.inline(base, endNode);
		}
		return base;
	}

	@Override
	public PathExpander<Object> reverse() {
		return new CustomPathExpander(this.directions.reverse(), this.nodeConstraints, this.relConstraints, inline);
	}
	
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("CustomPathExpander {");
		b.append("node: ").append(nodeConstraints);
		b.append(" rel: ").append(relConstraints);
		b.append(" dir: ").append(directions);
		b.append(" inline: ").append(inline);
		b.append('}');
		return b.toString();
	}
	
}