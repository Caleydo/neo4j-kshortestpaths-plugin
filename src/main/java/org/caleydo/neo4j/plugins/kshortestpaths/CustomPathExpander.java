package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.DirectionContraints;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.IConstraint;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.IPathConstraint;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.InlineRelationships;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.PathConstraints;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.FilteringIterable;
import org.neo4j.helpers.collection.Iterables;

/**
 * custom path expander
 * @author sam
 *
 */
public class CustomPathExpander implements PathExpander<Object>, Predicate<Path> {
	
	private final DirectionContraints directions;
	private final IPathConstraint constraints;
	private final InlineRelationships inline;
	private final IConstraint perElem;
	
	private Set<Long> extraIgnoreNodes;
	
	private boolean debug = false;
	private Iterable<FakeNode> extraNodes;

	public CustomPathExpander(DirectionContraints directions, IPathConstraint constraints, InlineRelationships inline, Iterable<FakeNode> extraNodes) {
		super();
		this.directions = directions;
		this.constraints = constraints;
		this.extraNodes = extraNodes;
		this.perElem = PathConstraints.getPerElemConstraint(constraints);
		this.inline = inline;
		
	}
	
	public void setExtraNodes(Iterable<FakeNode> extraNodes) {
		this.extraNodes = extraNodes;
	}
	
	public IPathConstraint getConstraints() {
		return constraints;
	}
	
	@Override
	public boolean accept(Path item) {
		return constraints.accept(item);
	}
	
	
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	
	public void setExtraIgnoreNodes(Set<Long> extraIgnoreNodes) {
		this.extraIgnoreNodes = extraIgnoreNodes;
	}
	
	private void debug(Object ... args) {
		if (this.debug) {
			System.out.println(StringUtils.join(args,' '));
		}
	}
	
	@Override
	public Iterable<Relationship> expand(final Path path, BranchState<Object> state) {
		final Node endNode = path.endNode();
		Iterable<Relationship> base = getRelationships(endNode);
		return new FilteringIterable<>(base, new Predicate<Relationship>() {
			@Override
			public boolean accept(Relationship item) {
				Node added = item.getOtherNode(endNode);
				if (!perElem.accept(added, item)) {
					debug("test: "+added+" bad");
					return false;
				}				
				if (extraIgnoreNodes != null && extraIgnoreNodes.contains(added.getId())) {
					return false;
				}
				return true;
			}
		});
	}


	public Iterable<Relationship> getRelationships(final Node node) {
		for(FakeNode n : extraNodes) {
			if (n.equals(node)) {
				return n.getRelationships();
			}
		}
		Iterable<Relationship> base = this.directions.filter(node);
		for(FakeNode n : extraNodes) {
			if (n.equals(node)) {
				base = Iterables.concat(base, Iterables.iterable(n.getRelationship(node)));
			}
		}
		
		if (inline != null) {
			base = inline.inline(base, node);
		}
		return base;
	}

	@Override
	public PathExpander<Object> reverse() {
		return new CustomPathExpander(this.directions.reverse(), this.constraints, inline, extraNodes);
	}
	
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("CustomPathExpander {");
		b.append(" c: ").append(constraints);
		b.append(" dir: ").append(directions);
		b.append(" inline: ").append(inline);
		b.append('}');
		return b.toString();
	}
	
}