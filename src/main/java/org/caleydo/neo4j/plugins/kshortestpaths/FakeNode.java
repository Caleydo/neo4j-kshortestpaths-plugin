package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Traverser;
import org.neo4j.graphdb.Traverser.Order;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.kernel.impl.traversal.OldTraverserWrapper;

public class FakeNode implements Node {
	private final long id;
	private final FakeGraphDatabase db;
	private final RelationshipType onlyType = DynamicRelationshipType.withName("FAKE");
	private final Direction onlyDir;
	private final List<Relationship> rels;
	
	public FakeNode(long id, FakeGraphDatabase db,Direction onlyDir, Iterator<Node> nodes) {
		this.id = id;
		this.db = db;
		this.onlyDir = onlyDir;
		this.rels = create(nodes);
	}
	
	@Override
	public Iterable<Relationship> getRelationships() {
		return rels;
	}

	@Override
	public GraphDatabaseService getGraphDatabase() {
		return db;
	}
	
	protected List<Relationship> create(Iterator<Node> nodes) {
		List<Relationship> r = new ArrayList<Relationship>();
		Node n;
		while (nodes.hasNext()) {
			n = nodes.next();
			FakeRelationship fake = new FakeRelationship(db, onlyType, onlyDir == Direction.OUTGOING ? this: n, onlyDir == Direction.INCOMING ? this: n, new HashMap<String, Object>());
			r.add(fake);
			db.putFake(fake);
		}
		return r;
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
    public Traverser traverse( Order traversalOrder,
                               StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator,
                               RelationshipType relationshipType, Direction direction )
    {
        return OldTraverserWrapper.traverse( this,
                traversalOrder, stopEvaluator,
                returnableEvaluator, relationshipType, direction );
    }

    @Override
    public Traverser traverse( Order traversalOrder,
                               StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator,
                               RelationshipType firstRelationshipType, Direction firstDirection,
                               RelationshipType secondRelationshipType, Direction secondDirection )
    {
        return OldTraverserWrapper.traverse( this,
                traversalOrder, stopEvaluator,
                returnableEvaluator, firstRelationshipType, firstDirection,
                secondRelationshipType, secondDirection );
    }

    @Override
    public Traverser traverse( Order traversalOrder,
                               StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator,
                               Object... relationshipTypesAndDirections )
    {
        return OldTraverserWrapper.traverse( this,
                traversalOrder, stopEvaluator,
                returnableEvaluator, relationshipTypesAndDirections );
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

}
