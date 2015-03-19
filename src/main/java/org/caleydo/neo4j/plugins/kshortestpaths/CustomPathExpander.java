package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.DirectionContraints;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.IPathConstraint;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.InlineRelationships;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.InlineRelationships.FakeSetRelationshipFactory;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.InlineRelationships.IFakeRelationshipFactory;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.PathConstraints;
import org.neo4j.graphdb.DynamicRelationshipType;
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
	
	private Set<Long> extraIgnoreNodes;
	
	private boolean debug = false;
	private boolean intermediatePaths = true;

	public CustomPathExpander(Map<String,String> directions, Map<String,Object> constraints, Map<String,Object> inline, FakeGraphDatabase db) {
		this(new DirectionContraints(directions), PathConstraints.parse(constraints), of(inline, db));
	} 
	
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
		
	private static InlineRelationships of(Map<String,Object> desc, FakeGraphDatabase db) {
		if (desc == null) {
			return null;
		}
		DynamicRelationshipType type = DynamicRelationshipType.withName(desc.get("inline").toString());
		boolean undirectional = Objects.equals(desc.get("undirectional"),Boolean.TRUE);
		long notInlineId = desc.containsKey("dontInline") ? ((Number)desc.get("dontInline")).longValue() : -1;
		IFakeRelationshipFactory factory = toFactory(desc, db);
		return new InlineRelationships(type, factory, undirectional, notInlineId);
	}

	private static IFakeRelationshipFactory toFactory(Map<String, Object> desc, FakeGraphDatabase db) {
		return new FakeSetRelationshipFactory(desc.get("flag").toString(), desc.get("aggregate").toString(), desc.get("toaggregate").toString(), DynamicRelationshipType.withName(desc.get("type").toString()), db);
	}

	public CustomPathExpander(DirectionContraints directions, IPathConstraint constraints, InlineRelationships inline) {
		super();
		this.directions = directions;
		this.constraints = constraints;
		this.inline = inline;
		
	}
	
	public void setIntermediatePaths(boolean intermediatePaths) {
		this.intermediatePaths = intermediatePaths;
	}
	
	public void setExtraIgnoreNodes(Set<Long> extraIgnoreNodes) {
		this.extraIgnoreNodes = extraIgnoreNodes;
	}
	
	@Override
	public boolean accept(Path path) {
		return this.constraints.accept(path);
	}
	
	private void debug(Object ... args) {
		if (this.debug) {
			System.out.println(StringUtils.join(args,' '));
		}
	}
	
	@Override
	public Iterable<Relationship> expand(final Path path, BranchState<Object> state) {
		final IIntermediate good = this.constraints.prepare(nodes(path), rels(path));
		final Node endNode = path.endNode();
		Iterable<Relationship> base = getRelationships(endNode);
		return new FilteringIterable<>(base, new Predicate<Relationship>() {
			@Override
			public boolean accept(Relationship item) {
				Node added = item.getOtherNode(endNode);
				if (!good.accept(item, added)) {
					debug("test: "+item+" bad");
					return false;
				}
				if (extraIgnoreNodes != null && extraIgnoreNodes.contains(added.getId())) {
					return false;
				}
				return true;
			}
		});
	}


	private Iterable<Relationship> rels(Path path) {
		if (intermediatePaths) {
			return path.relationships();
		}
		return Iterables.<Relationship>empty();
	}

	private Iterable<Node> nodes(Path path) {
		if (intermediatePaths) {
			return path.nodes();
		}
		return Arrays.asList(path.endNode());
	}

	public Iterable<Relationship> getRelationships(final Node endNode) {
		Iterable<Relationship> base = this.directions.filter(endNode);
		if (inline != null) {
			base = inline.inline(base, endNode);
		}
		return base;
	}

	@Override
	public PathExpander<Object> reverse() {
		return new CustomPathExpander(this.directions.reverse(), this.constraints, inline);
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