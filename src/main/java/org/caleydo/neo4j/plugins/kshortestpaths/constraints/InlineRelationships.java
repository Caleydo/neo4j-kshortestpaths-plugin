package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.caleydo.neo4j.plugins.kshortestpaths.FakeGraphDatabase;
import org.caleydo.neo4j.plugins.kshortestpaths.FakeRelationship;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.helpers.collection.Pair;

public class InlineRelationships {
	private final RelationshipType type;
	private final IFakeRelationshipFactory factory;
	private final boolean undirectional;
	private final long notInlineId;
	private boolean debug;
	
	public InlineRelationships(RelationshipType type, final IFakeRelationshipFactory factory, final boolean undirectional, final long notInlineId) {
		super();
		this.type = type;
		this.factory = factory;
		this.undirectional = undirectional;
		this.notInlineId = notInlineId;
	}
	
	public void setDebug(boolean debug) {
		this.debug = debug;
		this.factory.setDebug(debug);
	}
	
	@Override
	public String toString() {
		return "Inline: "+type.name() + " un: "+undirectional+" not:"+notInlineId+" fac:"+factory;
	}
	
	private void debug(Object ... args) {
		if (this.debug) {
			System.out.println(StringUtils.join(args,' '));
		}
	}
	
	public Iterable<Relationship> inline(Iterable<Relationship> rels, final Node source) {
		//assumption just create new edges don't change existing ones
		//assumption don't recreate an edge between an already existing one
		Set<Long> existing = new HashSet<>();
		Collection<Relationship> bad = new ArrayList<Relationship>();
		Map<Long, Collection<Pair<Relationship, Relationship>>> m = new HashMap<>();
		for (Relationship s : rels) {
			//System.out.println(s+" "+s.getStartNode()+" "+s.getEndNode()+" "+s.getEndNode().equals(source));
			Node toInline = s.getOtherNode(source);
			debug(s.getType(), s.getEndNode(), !skip(toInline));
			if (s.isType(type) && s.getEndNode().equals(source) && !skip(toInline)) { //just incoming edges
				//debug("try to inline: "+toInline);
				for(Relationship i : toInline.getRelationships(type)) {
					if (i.equals(s)) {
						continue;
					}
					Long key = i.getOtherNode(toInline).getId();
					if (!m.containsKey(key)) {
						m.put(key,  new ArrayList<Pair<Relationship, Relationship>>());
					}
					m.get(key).add(Pair.of(s, i));
				}
			} else {
				bad.add(s);
				existing.add(toInline.getId());	
			}
		}
		//debug("to fake",m, bad, existing);
		m.remove(source.getId()); //not self loops
		for(Long ex : existing) { //remove all existing ones
			m.remove(ex);
		}
		if (m.isEmpty()) { //nothing to do
			return bad;
		}
		//debug("faking",m);
		//System.out.println(m);
		
		Iterable<Relationship> inlined = Iterables.map(new Function<Map.Entry<Long,Collection<Pair<Relationship, Relationship>>>, Relationship>() {
			@Override
			public Relationship apply(Map.Entry<Long, Collection<Pair<Relationship, Relationship>>> from) {
				return factory.create(from.getValue(), false);
			}
		}, m.entrySet());
		
		Iterable<Relationship> inlined2 = undirectional ? Iterables.map(new Function<Map.Entry<Long,Collection<Pair<Relationship, Relationship>>>, Relationship>() {
			@Override
			public Relationship apply(Map.Entry<Long, Collection<Pair<Relationship, Relationship>>> from) {
				return factory.create(from.getValue(), true);
			}
		}, m.entrySet()): Collections.<Relationship>emptyList();
		
		
		return Iterables.concat(bad, inlined, inlined2);
	}

	

	private boolean skip(Node node) {
		return node.getId() == notInlineId;
	}

	
	public static InlineRelationships of(Map<String,Object> desc, FakeGraphDatabase db) {
		if (desc == null) {
			return null;
		}
		RelationshipType type = RelationshipType.withName(desc.get("inline").toString());
		boolean undirectional = Objects.equals(desc.get("undirectional"),Boolean.TRUE);
		long notInlineId = desc.containsKey("dontInline") ? ((Number)desc.get("dontInline")).longValue() : -1;
		IFakeRelationshipFactory factory = toFactory(desc, db);
		return new InlineRelationships(type, factory, undirectional, notInlineId);
	}

	private static IFakeRelationshipFactory toFactory(Map<String, Object> desc, FakeGraphDatabase db) {
		return new FakeSetRelationshipFactory(desc.get("flag").toString(), (Map<String, String>) desc.get("aggregate"), desc.get("toaggregate").toString(), RelationshipType.withName(desc.get("type").toString()), db);
	}


	public interface IFakeRelationshipFactory {
		Relationship create(Collection<Pair<Relationship, Relationship>> inlinem, boolean reverse);
		
		void setDebug(boolean debug);
	}
	
	public static class FakeSetRelationshipFactory implements IFakeRelationshipFactory{

		private final String flag;
		private final String aggregateInlined;
		private final RelationshipType type;
		private final FakeGraphDatabase w;
		private boolean debug;
		private final Map<String, String> aggregateByLabel;
		
		public FakeSetRelationshipFactory(String flag, Map<String,String> aggregateByLabel, String aggregateInlined, RelationshipType type, FakeGraphDatabase w) {
			super();
			this.flag = flag;
			this.aggregateByLabel = aggregateByLabel;
			this.aggregateInlined = aggregateInlined;
			this.type = type;
			this.w = w;
		}
		
		@Override
		public void setDebug(boolean debug) {
			this.debug = debug;
		}		

		@SuppressWarnings("unchecked")
		@Override
		public Relationship create(Collection<Pair<Relationship, Relationship>> inline, boolean reverse) {
			Pair<Relationship, Relationship> next = inline.iterator().next();
			Node[] sit = resolve(next);
			Node source = sit[0];
			Node target = sit[2];
			
			long id = FakeRelationship.id(reverse ? target : source, reverse ? source : target);
			if (w.hasFake(id)) {
				return w.getRelationshipById(id);
			}
						
			Map<String,Object> properties = new HashMap<String, Object>();
			properties.put(flag, true);
			//properties.put("inlined", inline.toString());
			Map<String,Collection<String>> m = new HashMap<String, Collection<String>>();
			
			for(Pair<Relationship, Relationship> p : inline) {
				Node inlined = p.first().getOtherNode(source);
				for(Label l : inlined.getLabels()) {
					if (aggregateByLabel.containsKey(l.name())) {
						String key = aggregateByLabel.get(l.name());
						if (!m.containsKey(key)) {
							m.put(key,  new ArrayList<String>());
						}
						m.get(key).add(inlined.getProperty(aggregateInlined).toString());
					}
				}
			}
			for(String key : m.keySet()) {
				properties.put(key.toString(), Iterables.asArray(String.class, m.get(key)));
			}
			Relationship rel = new FakeRelationship(source.getGraphDatabase(),type, reverse ? target : source, reverse ? source : target, properties);
			w.putFake(rel);
			return rel;
		}

		private Node[] resolve(Pair<Relationship, Relationship> next) {
			
			Relationship first = next.first();
			Relationship other = next.other();
			Node a= first.getStartNode();
			Node b= first.getEndNode();
			Node c = other.getStartNode();
			Node d = other.getEndNode();
			if (a.getId() == c.getId()) 
				return new Node[]{ b, a, d};
			else if (a.getId() == d.getId())
				return new Node[]{ b, a, c};
			else if (b.getId() == c.getId())
				return new Node[]{ a, c, d};
			return new Node[]{ a, b, c};
		}		
		
	}
}
