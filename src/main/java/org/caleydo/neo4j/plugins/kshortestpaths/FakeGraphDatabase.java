package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Traverser;
import org.neo4j.graphdb.Traverser.Order;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.graphdb.traversal.BidirectionalTraversalDescription;
import org.neo4j.graphdb.traversal.TraversalDescription;


public class FakeGraphDatabase implements GraphDatabaseService {
	private GraphDatabaseService w;
	
	private Map<Long, Relationship> fakes = new HashMap<>();

	public FakeGraphDatabase(GraphDatabaseService w) {
		super();
		this.w = w;
	}
	
	public void putFake(Relationship fake) {
		fakes.put(fake.getId(), fake);
	}

	public boolean hasFake(long id) {
		return fakes.containsKey(id);
	}

	@Override
	public Node createNode() {
		return w.createNode();
	}

	@Override
	public Node createNode(Label... labels) {
		return w.createNode(labels);
	}

	@Override
	public Node getNodeById(long id) {
		return w.getNodeById(id);
	}

	@Override
	public Relationship getRelationshipById(long id) {
		if (hasFake(id)) {
			return fakes.get(id);
		}
		return w.getRelationshipById(id);
	}


	@Override
	public Iterable<Node> getAllNodes() {
		return w.getAllNodes();
	}

	@Override
	public ResourceIterable<Node> findNodesByLabelAndProperty(Label label,
			String key, Object value) {
		return w.findNodesByLabelAndProperty(label, key, value);
	}

	@Override
	public Iterable<RelationshipType> getRelationshipTypes() {
		return w.getRelationshipTypes();
	}

	@Override
	public boolean isAvailable(long timeout) {
		return w.isAvailable(timeout);
	}

	@Override
	public void shutdown() {
		w.shutdown();
	}

	@Override
	public Transaction beginTx() {
		return w.beginTx();
	}

	@Override
	public <T> TransactionEventHandler<T> registerTransactionEventHandler(
			TransactionEventHandler<T> handler) {
		return w.registerTransactionEventHandler(handler);
	}

	@Override
	public <T> TransactionEventHandler<T> unregisterTransactionEventHandler(
			TransactionEventHandler<T> handler) {
		return w.unregisterTransactionEventHandler(handler);
	}

	@Override
	public KernelEventHandler registerKernelEventHandler(
			KernelEventHandler handler) {
		return w.registerKernelEventHandler(handler);
	}

	@Override
	public KernelEventHandler unregisterKernelEventHandler(
			KernelEventHandler handler) {
		return w.unregisterKernelEventHandler(handler);
	}

	@Override
	public Schema schema() {
		return w.schema();
	}

	@Override
	public IndexManager index() {
		return w.index();
	}

	@Override
	public TraversalDescription traversalDescription() {
		return w.traversalDescription();
	}

	@Override
	public BidirectionalTraversalDescription bidirectionalTraversalDescription() {
		return w.bidirectionalTraversalDescription();
	}
	
	@Override
	public boolean equals(Object obj) {
		return w.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return w.hashCode();
	}
	
	@Override
	public String toString() {
		return w.toString()+'F';
	}

	public Node inject(Node source) {
		return new FakeNode(source);
	}
	
	class FakeNode implements Node {
		private final Node w;

		public FakeNode(Node w) {
			super();
			this.w = w;
		}

		@Override
		public GraphDatabaseService getGraphDatabase() {
			return FakeGraphDatabase.this;
		}
		
		@Override
		public boolean equals(Object obj) {
			return w.equals(obj);
		}
		
		@Override
		public int hashCode() {
			return w.hashCode();
		}
		
		@Override
		public String toString() {
			return w.toString()+'F';
		}

		@Override
		public boolean hasProperty(String key) {
			return w.hasProperty(key);
		}

		@Override
		public Object getProperty(String key) {
			return w.getProperty(key);
		}

		@Override
		public long getId() {
			return w.getId();
		}

		@Override
		public Object getProperty(String key, Object defaultValue) {
			return w.getProperty(key, defaultValue);
		}

		@Override
		public void delete() {
			w.delete();
		}

		@Override
		public void setProperty(String key, Object value) {
			w.setProperty(key, value);
		}

		@Override
		public Iterable<Relationship> getRelationships() {
			return w.getRelationships();
		}

		@Override
		public boolean hasRelationship() {
			return w.hasRelationship();
		}

		@Override
		public Iterable<Relationship> getRelationships(
				RelationshipType... types) {
			return w.getRelationships(types);
		}

		@Override
		public Object removeProperty(String key) {
			return w.removeProperty(key);
		}

		@Override
		public Iterable<Relationship> getRelationships(Direction direction,
				RelationshipType... types) {
			return w.getRelationships(direction, types);
		}

		@Override
		public Iterable<String> getPropertyKeys() {
			return w.getPropertyKeys();
		}

		@Override
		public boolean hasRelationship(RelationshipType... types) {
			return w.hasRelationship(types);
		}

		@Override
		public boolean hasRelationship(Direction direction,
				RelationshipType... types) {
			return w.hasRelationship(direction, types);
		}

		@Override
		public Iterable<Relationship> getRelationships(Direction dir) {
			return w.getRelationships(dir);
		}

		@Override
		public boolean hasRelationship(Direction dir) {
			return w.hasRelationship(dir);
		}

		@Override
		public Iterable<Relationship> getRelationships(RelationshipType type,
				Direction dir) {
			return w.getRelationships(type, dir);
		}

		@Override
		public boolean hasRelationship(RelationshipType type, Direction dir) {
			return w.hasRelationship(type, dir);
		}

		@Override
		public Relationship getSingleRelationship(RelationshipType type,
				Direction dir) {
			return w.getSingleRelationship(type, dir);
		}

		@Override
		public Relationship createRelationshipTo(Node otherNode,
				RelationshipType type) {
			return w.createRelationshipTo(otherNode, type);
		}

		@Override
		public Traverser traverse(Order traversalOrder,
				StopEvaluator stopEvaluator,
				ReturnableEvaluator returnableEvaluator,
				RelationshipType relationshipType, Direction direction) {
			return w.traverse(traversalOrder, stopEvaluator,
					returnableEvaluator, relationshipType, direction);
		}

		@Override
		public Traverser traverse(Order traversalOrder,
				StopEvaluator stopEvaluator,
				ReturnableEvaluator returnableEvaluator,
				RelationshipType firstRelationshipType,
				Direction firstDirection,
				RelationshipType secondRelationshipType,
				Direction secondDirection) {
			return w.traverse(traversalOrder, stopEvaluator,
					returnableEvaluator, firstRelationshipType, firstDirection,
					secondRelationshipType, secondDirection);
		}

		@Override
		public Traverser traverse(Order traversalOrder,
				StopEvaluator stopEvaluator,
				ReturnableEvaluator returnableEvaluator,
				Object... relationshipTypesAndDirections) {
			return w.traverse(traversalOrder, stopEvaluator,
					returnableEvaluator, relationshipTypesAndDirections);
		}

		@Override
		public void addLabel(Label label) {
			w.addLabel(label);
		}

		@Override
		public void removeLabel(Label label) {
			w.removeLabel(label);
		}

		@Override
		public boolean hasLabel(Label label) {
			return w.hasLabel(label);
		}

		@Override
		public Iterable<Label> getLabels() {
			return w.getLabels();
		}
		
	}
	
}
