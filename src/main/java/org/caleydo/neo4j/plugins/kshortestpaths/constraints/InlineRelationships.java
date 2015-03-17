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
import org.caleydo.neo4j.plugins.kshortestpaths.FakeGraphDatabase;
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
		Set<Long> existing = new HashSet<>();
		Collection<Relationship> bad = new ArrayList<Relationship>();
		MultiMap m = new MultiValueMap();
		for (Relationship s : rels) {
			//System.out.println(s+" "+s.getStartNode()+" "+s.getEndNode()+" "+s.getEndNode().equals(source));
			Node toInline = s.getOtherNode(source);
			if (s.isType(type) && s.getEndNode().equals(source) && !skip(toInline)) { //just incoming edges
				for(Relationship i : toInline.getRelationships(type)) {
					if (i.equals(s)) {
						continue;
					}
					m.put(i.getOtherNode(toInline).getId(), Pair.of(s, i));
				}
			} else {
				bad.add(s);
				existing.add(toInline.getId());	
			}
		}
		m.remove(source.getId()); //not self loops
		for(Long ex : existing) { //remove all existing ones
			m.remove(ex);
		}
		if (m.isEmpty()) { //nothing to do
			return bad;
		}
		//System.out.println(m);
		
		@SuppressWarnings("unchecked")
		Iterable<Relationship> inlined = Iterables.map(new Function<Map.Entry<Long,Collection<Pair<Relationship, Relationship>>>, Relationship>() {
			@Override
			public Relationship apply(Map.Entry<Long, Collection<Pair<Relationship, Relationship>>> from) {
				return factory.create(from.getValue(), false);
			}
		}, m.entrySet());
		
		@SuppressWarnings("unchecked")
		Iterable<Relationship> inlined2 = undirectional ? Iterables.map(new Function<Map.Entry<Node,Collection<Pair<Relationship, Relationship>>>, Relationship>() {
			@Override
			public Relationship apply(Map.Entry<Node, Collection<Pair<Relationship, Relationship>>> from) {
				return factory.create(from.getValue(), true);
			}
		}, m.entrySet()): Arrays.asList();
		
		
		return Iterables.concat(bad, inlined, inlined2);
	}

	

	private boolean skip(Node node) {
		return node.getId() == notInlineId;
	}



	public interface IFakeRelationshipFactory {
		Relationship create(Collection<Pair<Relationship, Relationship>> inlinem, boolean reverse);
	}
	
	public static class FakeSetRelationshipFactory implements IFakeRelationshipFactory{

		private final String flag;
		private final String aggregate;
		private final String aggregateInlined;
		private final RelationshipType type;
		private final FakeGraphDatabase w;
		
		public FakeSetRelationshipFactory(String flag, String aggregate, String aggregateInlined, RelationshipType type, FakeGraphDatabase w) {
			super();
			this.flag = flag;
			this.aggregate = aggregate;
			this.aggregateInlined = aggregateInlined;
			this.type = type;
			this.w = w;
		}
		

		@Override
		public Relationship create(Collection<Pair<Relationship, Relationship>> inline, boolean reverse) {
			Pair<Relationship, Relationship> next = inline.iterator().next();
			Pair<Node,Node> st = resolve(next);
			Node source = st.first();
			Node target = st.other();
			
			long id = FakeRelationship.id(reverse ? target : source, reverse ? source : target);
			if (w.hasFake(id)) {
				return w.getRelationshipById(id);
			}
						
			Map<String,Object> properties = new HashMap<String, Object>();
			properties.put(flag, true);
			properties.put("inlined", inline.toString());
			String[] r = new String[inline.size()];
			
			int i = 0;
			for(Pair<Relationship, Relationship> p : inline) {
				r[i++] = Objects.toString(p.first().getOtherNode(source).getProperty(aggregateInlined));
			}
			properties.put(aggregate, r);
			
			Relationship rel = new FakeRelationship(source.getGraphDatabase(),type, reverse ? target : source, reverse ? source : target, properties);
			w.putFake(rel);
			return rel;
		}

		private Pair<Node, Node> resolve(Pair<Relationship, Relationship> next) {
			
			Relationship first = next.first();
			Relationship other = next.other();
			Node a= first.getStartNode();
			Node b= first.getEndNode();
			Node c = other.getStartNode();
			Node d = other.getEndNode();
			if (a.getId() == c.getId()) 
				return Pair.of(b,d);
			else if (a.getId() == d.getId())
				return Pair.of(b,c);
			else if (b.getId() == c.getId())
				return Pair.of(a, d);
			return Pair.of(a, c);
		}


		@Override
		public String toString() {
			return "FakeSetRelationshipFactory [flag=" + flag + ", aggregate="
					+ aggregate + ", aggregateInlined=" + aggregateInlined
					+ ", type=" + type + "]";
		}
		
		
	}
}
