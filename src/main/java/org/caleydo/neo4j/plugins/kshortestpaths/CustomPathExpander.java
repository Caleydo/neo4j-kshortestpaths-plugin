package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.Direction;
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
	private final List<IPathFilter> pathFilters = new ArrayList<>();
	private final Direction dir;
	
	
	public CustomPathExpander(Direction dir) {
		super();
		this.dir = dir;
	}

	public void addPathFilter(IPathFilter filter) {
		this.pathFilters.add(filter);
	}
	
	@Override
	public Iterable<Relationship> expand(final Path path, BranchState<Object> state) {
		final Node endNode = path.endNode();
		
		return new FilteringIterable<>(endNode.getRelationships(dir), new Predicate<Relationship>() {
			@Override
			public boolean accept(Relationship item) {
				for(IPathFilter f : pathFilters) {
					if (!f.accept(path, endNode, item)) {
						return false;
					}
				}
				return true;
			}
		});
	}

	@Override
	public PathExpander<Object> reverse() {
		return this;
	}
	
	
	public interface IPathFilter {
		boolean accept(Path path, Node endNode, Relationship relationship);
	}
	
	public static IPathFilter nodeFilter(final Predicate<? super Node> pred) {
		return new IPathFilter() {
			
			@Override
			public boolean accept(Path path, Node endNode, Relationship relationship) {
				Node node = relationship.getOtherNode(endNode);
				return pred.accept(node);
			}
		};
	}
	
	public static IPathFilter relationshipFilter(final Predicate<? super Relationship> pred) {
		return new IPathFilter() {
			
			@Override
			public boolean accept(Path path, Node endNode, Relationship relationship) {
				return pred.accept(relationship);
			}
		};
	}
}