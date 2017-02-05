package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.collection.Pair;
import org.neo4j.helpers.collection.Iterables;

public class DirectionContraints {
	private final List<Pair<? extends RelationshipType,Direction>> filter;
	private boolean allSameDir;
	
	public DirectionContraints(Map<String, String> directions) {
		this(parse(directions));
		
	}

	private static List<Pair<? extends RelationshipType,Direction>> parse(Map<String, String> directions) {
		List<Pair<? extends RelationshipType,Direction>> r = new ArrayList<>();
		if (directions != null) {
			for(Map.Entry<String,String> entry : directions.entrySet()) {
				r.add(Pair.of(RelationshipType.withName(entry.getKey()), toDir(entry.getValue())));
			}
		}
		return r;
	}
	
	public static DirectionContraints of(String label, Direction dir) {
		List<Pair<? extends RelationshipType,Direction>> r = new ArrayList<>();
		r.add(Pair.of(RelationshipType.withName(label), dir));
		return new DirectionContraints(r);
	}
	public static DirectionContraints of(String label, Direction dir, String label2, Direction dir2) {
		List<Pair<? extends RelationshipType,Direction>> r = new ArrayList<>();
		r.add(Pair.of(RelationshipType.withName(label), dir));
		r.add(Pair.of(RelationshipType.withName(label2), dir2));
		return new DirectionContraints(r);
	}

	private static Direction toDir(String value) {
		value = value.toLowerCase();
		if (value.startsWith("i"))
			return Direction.INCOMING;
		if (value.startsWith("o"))
			return Direction.OUTGOING;
		return Direction.BOTH;
	}

	private DirectionContraints(List<Pair<? extends RelationshipType, Direction>> filter) {
		this.filter = filter;
		this.allSameDir = areAllSameDir(filter);
	}

	private static boolean areAllSameDir(List<Pair<? extends RelationshipType, Direction>> filter) {
		if (!filter.isEmpty()) {
			Direction allSameDir = filter.get(0).other();
			for(Pair<? extends RelationshipType, Direction> p : filter) {
				if (allSameDir != p.other()) {
					return false;
				}
			}
			return true;
		}
		return true;
	}

	public Iterable<Relationship> filter(final Node node) {
		if (this.filter.isEmpty()) {
			return node.getRelationships();
		}
		if (this.filter.size() == 1) {
			Pair<? extends RelationshipType, Direction> p = this.filter.get(0);
			return node.getRelationships(p.other(), p.first());
		}
		if (this.allSameDir) {
			return node.getRelationships(this.filter.get(0).other(), getRelationshipTypes());
		}
		List<Iterable<Relationship>> r = new ArrayList<>();
		for(Pair<? extends RelationshipType, Direction> p : filter) {
			r.add(node.getRelationships(p.other(), p.first()));
		}
		return Iterables.concat(r);
	}

	private RelationshipType[] getRelationshipTypes() {
		RelationshipType[] r = new RelationshipType[this.filter.size()];
		for(int i = 0; i < r.length; ++i) {
			r[i] = this.filter.get(i).first();
		}
		return r;
	}

	public DirectionContraints reverse() {
		List<Pair<? extends RelationshipType,Direction>> r = new ArrayList<>();
		for(Pair<? extends RelationshipType,Direction> entry: this.filter) {
			r.add(Pair.of(entry.first(), entry.other().reverse()));
		}
		return new DirectionContraints(r);
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("[");
		b.append(StringUtils.join(this.filter, ","));
		b.append("]");
		return b.toString();
	}

}
