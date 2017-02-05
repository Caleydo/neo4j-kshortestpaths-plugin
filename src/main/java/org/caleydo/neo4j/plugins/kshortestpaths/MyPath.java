package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.Iterator;
import java.util.List;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.Iterables;

public class MyPath implements Path {
	private final List<Node> nodes;
	private final List<Relationship> rels;
	
	public MyPath(List<Node> nodes, List<Relationship> rels) {
		this.nodes = nodes;
		this.rels = rels;
	}

	@Override
	public Node startNode() {
		return nodes.get(0);
	}

	@Override
	public Node endNode() {
		return nodes.get(nodes.size()-1);
	}

	@Override
	public Relationship lastRelationship() {
		return rels.get(rels.size()-1);
	}

	@Override
	public Iterable<Relationship> relationships() {
		return rels;
	}

	@Override
	public Iterable<Relationship> reverseRelationships() {
		return Iterables.reverse(rels);
	}

	@Override
	public Iterable<Node> nodes() {
		return nodes;
	}

	@Override
	public Iterable<Node> reverseNodes() {
		return Iterables.reverse(nodes);
	}

	@Override
	public int length() {
		return nodes.size();
	}

	@Override
	public Iterator<PropertyContainer> iterator() {
		return Iterables.<PropertyContainer>concat(nodes, rels).iterator();
	}

	@Override
	public String toString() {
		return "MyPath [nodes=" + nodes + ", rels=" + rels + "]";
	}

}
