package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.caleydo.neo4j.plugins.kshortestpaths.FakeRelationship;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.Function;
import org.neo4j.helpers.Pair;
import org.neo4j.helpers.collection.Iterables;

public class InlineRelationships {
	private final RelationshipType type;
	private final IFakeRelationshipFactory factory;
	private final boolean undirectional;
	private final long notInlineId;

	public InlineRelationships(RelationshipType type, final IFakeRelationshipFactory factory, final boolean undirectional, final long notInlineId) {
		super();
		this.type = type;
		this.factory = factory;
		this.undirectional = undirectional;
		this.notInlineId = notInlineId;
	}
	
	@Override
	public String toString() {
		return "Inline: "+type.name() + " un: "+undirectional+" not:"+notInlineId+" fac:"+factory;
	}
	
	public Iterable<Relationship> inline(Iterable<Relationship> rels, final Node source) {
		//assumption just create new edges don't change existing ones
		//assumption don't recreate an edge between an already existing one
		Set<Node> existing = new HashSet<>();
		Collection<Relationship> bad = new ArrayList<Relationship>();
		MultiMap m = new MultiValueMap();
		for (Relationship s : rels) {
			//System.out.println(s+" "+s.getStartNode()+" "+s.getEndNode()+" "+s.getEndNode().equals(source));
			if (s.isType(type) && s.getEndNode().equals(source) && !skip(s.getOtherNode(source))) { //just incoming edges
				Node toInline = s.getOtherNode(source);
				for(Relationship i : toInline.getRelationships(type)) {
					if (i.equals(s)) {
						continue;
					}
					m.put(i.getOtherNode(toInline), Pair.of(s, i));
				}
			} else {
				bad.add(s);
				existing.add(s.getOtherNode(source));	
			}
		}
		m.remove(source); //not self loops
		for(Node ex : existing) { //remove all existing ones
			m.remove(ex);
		}
		if (m.isEmpty()) { //nothing to do
			return bad;
		}
		//System.out.println(m);
		
		@SuppressWarnings("unchecked")
		Iterable<Relationship> inlined = Iterables.map(new Function<Map.Entry<Node,Collection<Pair<Relationship, Relationship>>>, Relationship>() {
			@Override
			public Relationship apply(Map.Entry<Node, Collection<Pair<Relationship, Relationship>>> from) {
				return factory.create(source, from.getKey(), from.getValue(), false);
			}
		}, m.entrySet());
		
		@SuppressWarnings("unchecked")
		Iterable<Relationship> inlined2 = undirectional ? Iterables.map(new Function<Map.Entry<Node,Collection<Pair<Relationship, Relationship>>>, Relationship>() {
			@Override
			public Relationship apply(Map.Entry<Node, Collection<Pair<Relationship, Relationship>>> from) {
				return factory.create(source, from.getKey(), from.getValue(), true);
			}
		}, m.entrySet()): Arrays.asList();
		
		
		return Iterables.concat(bad, inlined, inlined2);
	}

	

	private boolean skip(Node node) {
		return node.getId() == notInlineId;
	}



	public interface IFakeRelationshipFactory {
		Relationship create(Node source, Node target, Collection<Pair<Relationship, Relationship>> inlinem, boolean reverse);
	}
	
	public static class FakeSetRelationshipFactory implements IFakeRelationshipFactory {

		private final String flag;
		private final String aggregate;
		private final String aggregateInlined;
		private final RelationshipType type;
		
		public FakeSetRelationshipFactory(String flag, String aggregate, String aggregateInlined, RelationshipType type) {
			super();
			this.flag = flag;
			this.aggregate = aggregate;
			this.aggregateInlined = aggregateInlined;
			this.type = type;
		}
		
		private Map<Long, Relationship> cache = new HashMap<Long, Relationship>();

		@Override
		public Relationship create(Node source, Node target,
				Collection<Pair<Relationship, Relationship>> inline, boolean reverse) {
			
			long id = FakeRelationship.id(reverse ? target : source, reverse ? source : target);
			
			Map<String,Object> properties = new HashMap<String, Object>();
			properties.put(flag, true);
			String[] r = new String[inline.size()];
			if (cache.containsKey(id)) {
				return cache.get(id);
			}
			
			int i = 0;
			for(Pair<Relationship, Relationship> p : inline) {
				r[i++] = Objects.toString(p.first().getOtherNode(source).getProperty(aggregateInlined));
			}
			properties.put(aggregate, r);
			
			Relationship rel = new FakeRelationship(source.getGraphDatabase(),type, reverse ? target : source, reverse ? source : target, properties);
			cache.put(rel.getId(), rel);
			return rel;
		}

		@Override
		public String toString() {
			return "FakeSetRelationshipFactory [flag=" + flag + ", aggregate="
					+ aggregate + ", aggregateInlined=" + aggregateInlined
					+ ", type=" + type + "]";
		}
		
		
	}
}
