package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.DirectionContraints;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.InlineRelationships;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.InlineRelationships.FakeSetRelationshipFactory;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.InlineRelationships.IFakeRelationshipFactory;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.NodeConstraints;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.RelConstraints;
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
	private final NodeConstraints nodeConstraints;
	private final RelConstraints relConstraints;
	private final InlineRelationships inline;
	
	private Set<Long> extraIgnoreNodes;
	
	private boolean debug = false;

	public CustomPathExpander(Map<String,String> directions, List<Map<String,Object>> nodeContraints,List<Map<String,Object>> relContraints, Map<String,Object> inline, FakeGraphDatabase db) {
		this(new DirectionContraints(directions), new NodeConstraints(nodeContraints), new RelConstraints(relContraints), of(inline, db));
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

	public CustomPathExpander(DirectionContraints directions, NodeConstraints nodeContraints,RelConstraints relConstraints, InlineRelationships inline) {
		super();
		this.directions = directions;
		this.nodeConstraints = nodeContraints;
		this.relConstraints = relConstraints;
		this.inline = inline;
		
	}
	
	public void setExtraIgnoreNodes(Set<Long> extraIgnoreNodes) {
		this.extraIgnoreNodes = extraIgnoreNodes;
	}
	
	@Override
	public boolean accept(Path path) {
		return this.nodeConstraints.isValid(path.nodes()) && this.relConstraints.isValid(path.relationships());
	}
	
	private void debug(Object ... args) {
		if (this.debug) {
			System.out.println(StringUtils.join(args,' '));
		}
	}
	
	@Override
	public Iterable<Relationship> expand(final Path path, BranchState<Object> state) {
		final Predicate<Node> goodNode = this.nodeConstraints.prepare(Iterables.<Node>empty()); //path.nodes());
		final Predicate<Relationship> goodRel = this.relConstraints.prepare(Iterables.<Relationship>empty()); //path.relationships());
		final Node endNode = path.endNode();
		Iterable<Relationship> base = getRelationships(endNode);
		return new FilteringIterable<>(base, new Predicate<Relationship>() {
			@Override
			public boolean accept(Relationship item) {
				if (!goodRel.accept(item)) {
					debug("test: "+item+" bad rel");
					return false;
				}
				Node added = item.getOtherNode(endNode);
				if (extraIgnoreNodes != null && extraIgnoreNodes.contains(added.getId())) {
					return false;
				}
				boolean r= goodNode.accept(added);
				if (!r) {
					debug("test: "+added+" bad node");
				} else {
					debug("test: "+item+" "+added+" good");
				}
				return r;
			}
		});
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