package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

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

	public CustomPathExpander(DirectionContraints directions, IPathConstraint constraints, InlineRelationships inline, Iterable<FakeNode> extraNodes, boolean acyclic) {
		super();
		this.directions = directions;
		if (acyclic) {
			this.constraints = PathConstraints.and(constraints, PathConstraints.acyclic);
		} else {
			this.constraints = constraints;			
		}
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
	public boolean test(Path item) {
		return constraints.test(item);
	}
	
	
	public void setDebug(boolean debug) {
		this.debug = debug;
		if (this.inline != null) {
			this.inline.setDebug(debug);
		}
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
		debug("resolve relationships: "+endNode);
		for(FakeNode n : extraNodes) {
			if (n.equals(endNode)) {
				debug("found start/end: "+endNode);
				return n.getRelationships();
			}
		}
		try {
			Iterable<Relationship> base = getRelationships(endNode);
			List<Relationship> s = Iterables.asList(new FilteringIterable<>(base, new Predicate<Relationship>() {
				@Override
				public boolean test(Relationship item) {
					Node added = item.getOtherNode(endNode);
					for(FakeNode n : extraNodes) { //keep fake nodes
						if (n.equals(added)) {
							debug("keep fake node: "+n);
							return true;
						}
					}
					if (!perElem.accept(added, item)) {
						debug("test: "+added+" bad");
						return false;
					}				
					if (extraIgnoreNodes != null && extraIgnoreNodes.contains(added.getId())) {
						return false;
					}
					debug("accept: ",added,item);
					return true;
				}
			}));
			debug("RESOLVED: ",endNode, s.size());
			return s;
		} catch (RuntimeException e) {
			System.out.println("error"+e);
			e.printStackTrace();
		}
		return Iterables.empty();
		
	}


	public Iterable<Relationship> getRelationships(final Node node) {
		Iterable<Relationship> base = Iterables.asList(this.directions.filter(node));
		for(FakeNode n : extraNodes) {
			if (n.hasRelationship(node)) {
				debug("add fake relationship back"+n+" "+node);
				base = Iterables.concat(base, Iterables.iterable(n.getRelationship(node)));
			}
		}
		debug("rels: "+Iterables.asList(base));
		if (inline != null) {
			base = inline.inline(base, node);
		}
		//debug("inlined rels: "+Iterables.toList(base));
		return base;
	}

	@Override
	public PathExpander<Object> reverse() {
		debug("create reversed version", this.directions.reverse());
		CustomPathExpander p = new CustomPathExpander(this.directions.reverse(), this.constraints, inline, extraNodes, false);
		p.setDebug(debug);
		p.setExtraIgnoreNodes(extraIgnoreNodes);
		p.setExtraNodes(extraNodes);
		return p;
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