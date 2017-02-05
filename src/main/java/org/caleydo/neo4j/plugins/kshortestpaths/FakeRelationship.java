package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.Map;
import java.util.Objects;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.collection.MapUtil;

/**
 * a faked relationship
 * 
 * @author sam
 *
 */
public class FakeRelationship implements Relationship {
	private final GraphDatabaseService db;
	private final Map<String, Object> properties;
	private final Node source;
	private final Node target;
	private final RelationshipType type;
	private final long relId;

	public FakeRelationship(GraphDatabaseService db, RelationshipType type,
			Node source, Node target, Map<String, Object> properties) {
		super();
		this.db = db;
		this.relId = id(source, target);
		this.type = type;
		this.source = source;
		this.target = target;
		this.properties = properties;
	}

	public static long id(Node s, Node t) {
		long a = s.getId();
		long b = t.getId();
		if (a > b) { //by convention smaller first for undirected ones
			b = s.getId();
			a = t.getId();
		}
		// heuristic combined both and negate
		long id = (1 << 17)  + (a << 16) + b;
		return id;
	}

	@Override
	public GraphDatabaseService getGraphDatabase() {
		return db;
	}

	@Override
	public boolean hasProperty(String key) {
		return properties.containsKey(key);
	}

	@Override
	public Object getProperty(String key) {
		return properties.get(key);
	}

	@Override
	public Object getProperty(String key, Object defaultValue) {
		if (!hasProperty(key)) {
			return defaultValue;
		}
		return getProperty(key);
	}

	@Override
	public void setProperty(String key, Object value) {
		properties.put(key, value);
	}

	@Override
	public Object removeProperty(String key) {
		return properties.remove(key);
	}

	@Override
	public Iterable<String> getPropertyKeys() {
		return properties.keySet();
	}

	@Override
	public long getId() {
		return relId;
	}

	@Override
	public void delete() {
		System.out.println("called delete");
	}

	@Override
	public Node getStartNode() {
		return source;
	}

	@Override
	public Node getEndNode() {
		return target;
	}

	@Override
	public Node getOtherNode(Node node) {
		return node.getId() == source.getId() ? getEndNode() : getStartNode();
	}

	@Override
	public Node[] getNodes() {
		return new Node[] { getStartNode(), getEndNode() };
	}

	@Override
	public RelationshipType getType() {
		return type;
	}

	@Override
	public boolean isType(RelationshipType type) {
		return Objects.equals(getType().name(), type.name());
	}

	public int compareTo(Object rel) {
		Relationship r = (Relationship) rel;
		long ourId = this.getId(), theirId = r.getId();

		if (ourId < theirId) {
			return -1;
		} else if (ourId > theirId) {
			return 1;
		} else {
			return 0;
		}
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Relationship
				&& this.getId() == ((Relationship) o).getId();
	}

	@Override
	public int hashCode() {
		return (int) ((relId >>> 32) ^ relId);
	}

	@Override
	public String toString() {
		return "FakeRelationship[" + this.getId() + this.getStartNode()+" "+this.getEndNode()+ "]";
	}

	@Override
	public Map<String, Object> getProperties(String... keys) {
		MapUtil.MapBuilder<String, Object> builder = new MapUtil.MapBuilder<>();
		for (String key : keys) {
			builder.entry(key, this.properties.get(key));
		}
		return builder.create();		
	}

	@Override
	public Map<String, Object> getAllProperties() {
		return this.properties;
	}

}
