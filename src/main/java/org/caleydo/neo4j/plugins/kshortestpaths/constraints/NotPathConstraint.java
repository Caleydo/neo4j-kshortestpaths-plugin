package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.BitSet;
import java.util.SortedSet;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.Iterables;

public class NotPathConstraint implements ICompositePathContraint, IConstraint {
	private final IPathConstraint constraint;
	
	public NotPathConstraint(IPathConstraint constraint) {
		this.constraint = constraint;
	}
	
	@Override
	public Iterable<? extends IPathConstraint> children() {
		return Iterables.iterable(constraint);
	}
	
	@Override
	public boolean test(Path path) {
		return ! this.constraint.test(path);
	}
	
	@Override
	public SortedSet<MatchRegion> matches(Path path) {
		BitSet total = new BitSet();
		total.set(0, path.length());
		total.andNot(MatchRegion.toSet(constraint.matches(path), path.length()));
		return MatchRegion.from(total);
	}
	
	@Override
	public boolean accept(Node node, Relationship rel) {
		if (constraint instanceof IConstraint) {
			return ((IConstraint) constraint).accept(node, rel);
		}
		return true;
	}
	
	
	@Override
	public void toCypher(StringBuilder b, String var) {
		if (constraint instanceof IConstraint) {
			b.append(" not (");
			((IConstraint) constraint).toCypher(b, var);
			b.append(") ");
		}
	}

	@Override
	public String toString() {
		return "NotPathConstraint [constraint=" + constraint + "]";
	}
	
	
}
