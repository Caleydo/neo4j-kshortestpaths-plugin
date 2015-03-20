package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.SortedSet;
import java.util.TreeSet;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

public class ElemConstraint implements IConstraint, IPathConstraint, ISubPathConstraint {
	private final ISelector selector;
	private final ValueConstraint constraint;
	private final boolean nodeContext;
	
	public ElemConstraint(ISelector selector, ValueConstraint constraint, boolean isNodeContext) {
		super();
		this.selector = selector;
		this.constraint = constraint;
		this.nodeContext = isNodeContext;
	}
	
	public boolean isNodeContext() {
		return this.nodeContext;
	}
		
	@Override
	public boolean accept(Node node, Relationship rel) {
		PropertyContainer c  = select(node, rel);
		if (c == null) { //accept missing ones
			return true;
		}
		Object value = selector.get(c);
		return constraint.accept(value);
	}
	
	@Override
	public boolean accept(Path path) {
		int c = 0;
		if (nodeContext) {
			for(Node node : path.nodes()) {
				if (this.accept(node, null)) {
					c++;
				}
			}
		} else {
			for(Relationship rel : path.relationships()) {
				if (this.accept(null,rel)) {
					c++;
				}
			}
			c+=1; //for the last fake one
		}
		return c > 0;
	}
	
	@Override
	public SortedSet<MatchRegion> matches(Path path) {
		SortedSet<MatchRegion> r = new TreeSet<MatchRegion>();
		int i = 0;
		if (nodeContext) {
			for(Node node : path.nodes()) {
				if (this.accept(node, null)) {
					r.add(new MatchRegion(i, i));
				}
				i++;
			}
		} else {
			for(Relationship rel : path.relationships()) {
				if (this.accept(null,rel)) {
					r.add(new MatchRegion(i, i));
				}
				i++;
			}
		}
		return r;
	}

	private PropertyContainer select(Node node, Relationship rel) {
		return nodeContext ? node : rel;
	}

	@Override
	public void toCypher(StringBuilder b, String var) {
		String key = this.selector.toCypher(var, isNodeContext());
		this.constraint.toCypher(key, b);
	}

	@Override
	public String toString() {
		return "ElemConstraint [selector=" + selector + ", constraint=" + constraint + ", nodeContext=" + nodeContext + "]";
	}
}
