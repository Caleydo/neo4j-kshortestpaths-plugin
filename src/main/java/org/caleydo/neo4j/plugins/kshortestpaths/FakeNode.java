package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.helpers.collection.MapUtil;

public class FakeNode implements Node {
	private final long id;
	private final FakeGraphDatabase db;
	private final RelationshipType onlyType = RelationshipType.withName("FAKE");
	private final Direction onlyDir;
	private Map<Long,Relationship> rels;

	public FakeNode(long id, FakeGraphDatabase db,Direction onlyDir, Iterator<Node> nodes) {
		this.id = id;
		this.db = db;
		this.onlyDir = onlyDir;
		this.rels = create(nodes);
	}

	@Override
	public String toString() {
		return "FakeNode [id=" + id + ", onlyType=" + onlyType + ", onlyDir=" + onlyDir+"]";
	}

	@Override
	public Iterable<Relationship> getRelationships() {
		return rels.values();
	}

	@Override
	public GraphDatabaseService getGraphDatabase() {
		return db;
	}

	protected Map<Long,Relationship> create(Iterator<Node> nodes) {
		Map<Long,Relationship> r = new HashMap<Long, Relationship>();
		Node n;
		while (nodes.hasNext()) {
			n = nodes.next();
			FakeRelationship fake = new FakeRelationship(db, onlyType, onlyDir == Direction.OUTGOING ? this: n, onlyDir == Direction.INCOMING ? this: n, new HashMap<String, Object>());
			r.put(n.getId(),fake);
			db.putFake(fake);
		}
		// System.out.println(onlyDir+" "+r.values());
		return r;
	}

	public boolean hasRelationship(Node node) {
		return rels.containsKey(node.getId());
	}

	public Relationship getRelationship(Node node) {
		return rels.get(node.getId());
	}


	@Override
	public boolean hasProperty(String key) {
		return false;
	}


	@Override
	public boolean hasRelationship() {
		return true;
	}

	@Override
	public Object getProperty(String key) {
		return null;
	}

	@Override
	public Object getProperty(String key, Object defaultValue) {
		return null;
	}

	@Override
	public void setProperty(String key, Object value) {

	}

	@Override
	public Object removeProperty(String key) {
		return null;
	}

	@Override
	public Iterable<String> getPropertyKeys() {
		return Iterables.empty();
	}

	@Override
	public long getId() {
		return this.id;
	}

	@Override
	public void delete() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterable<Relationship> getRelationships(RelationshipType... types) {
		if (hasRelationship(types)) {
			return getRelationships();
		}
		return Iterables.empty();
	}

	@Override
	public Iterable<Relationship> getRelationships(Direction direction, RelationshipType... types) {
		if (direction == onlyDir && hasRelationship(types)) {
			return getRelationships();
		}
		return Iterables.empty();
	}

	@Override
	public boolean hasRelationship(RelationshipType... types) {
		for(RelationshipType t : types) {
			if (t.equals(this.onlyType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasRelationship(Direction direction, RelationshipType... types) {
		return this.onlyDir == direction && hasRelationship(types);
	}

	@Override
	public Iterable<Relationship> getRelationships(Direction dir) {
		if (hasRelationship(dir))
			return getRelationships();
		return Iterables.empty();
	}

	@Override
	public Iterable<Relationship> getRelationships(RelationshipType type, Direction dir) {
		if (hasRelationship(type, dir))
			return getRelationships();
		return Iterables.empty();
	}

	@Override
	public boolean hasRelationship(RelationshipType type, Direction dir) {
		return dir == onlyDir && type.equals(onlyType);
	}

	@Override
	public Relationship getSingleRelationship(RelationshipType type, Direction dir) {
		if (hasRelationship(type, dir)) {
			return Iterables.first(getRelationships());
		}
		return null;
	}

	@Override
	public boolean hasRelationship(Direction dir) {
		return dir == onlyDir;
	}

	@Override
	public Relationship createRelationshipTo(Node otherNode, RelationshipType type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addLabel(Label label) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeLabel(Label label) {
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean hasLabel(Label label) {
		return false;
	}

	@Override
	public Iterable<Label> getLabels() {
		return Iterables.empty();
	}

	@Override
    public boolean equals( Object o )
    {
        if ( !(o instanceof Node) )
        {
            return false;
        }
        return this.getId() == ((Node) o).getId();

    }

    @Override
    public int hashCode()
    {
        return (int) (id ^ (id >>> 32));
    }

	@Override
	public int getDegree() {
		return rels.size();
	}

	@Override
	public int getDegree(Direction direction) {
		if (direction == onlyDir) {
			return getDegree();
		}
		return 0;
	}

	@Override
	public int getDegree(RelationshipType type) {
		if (onlyType.equals(type)) {
			return getDegree();
		}
		return 0;
	}

	@Override
	public int getDegree(RelationshipType type, Direction direction) {
		if (onlyType.equals(type) && direction == onlyDir) {
			return getDegree();
		}
		return 0;
	}

	@Override
	public Iterable<RelationshipType> getRelationshipTypes() {
		return Collections.singleton(onlyType);
	}
	
	@Override
	public Map<String, Object> getAllProperties() {
		return Collections.emptyMap();
	}

	@Override
	public Map<String, Object> getProperties(String... keys) {
		return Collections.emptyMap();
	}

}
