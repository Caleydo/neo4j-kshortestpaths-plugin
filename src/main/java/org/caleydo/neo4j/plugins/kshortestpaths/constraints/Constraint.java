package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.Iterables;

public class Constraint implements ITimedConstraint {
	private final FrequencyConstraint frequency;
	private final ISelector selector;
	private final Predicate<Object> constraint;
	private final boolean isNodeContext;
	
	public Constraint(FrequencyConstraint frequency, ISelector selector,
			Predicate<Object> constraint, boolean isNodeContext) {
		super();
		this.frequency = frequency;
		this.selector = selector;
		this.constraint = constraint;
		this.isNodeContext = isNodeContext;
	}
		
	public static ITimedConstraint of(Map<String,Object> desc) {
		ITimedConstraint t = parseAggregate(desc);
		if (t != null) {
			return t;
		}		
		FrequencyConstraint f = FrequencyConstraint.of(desc);
		ISelector s= toSelector(desc.get("prop"));
		Predicate<Object> constraint = ValueConstraint.of(desc);
		boolean isNodeContext = !"rel".equalsIgnoreCase((String)desc.get("context"));
		return new Constraint(f, s, constraint, isNodeContext);
	}
	
	

	@SuppressWarnings("unchecked")
	private static ITimedConstraint parseAggregate(Map<String, Object> desc) {
		if (desc.size() == 1) {
			Entry<String, Object> entry = Iterables.first(desc.entrySet());
			switch(entry.getKey()) {
			case "$and" : return new AndConstraint(toList(entry.getValue()));
			case "$or": return new OrConstraint(toList(entry.getValue()));
			case "$not": return new NotConstraint(of((Map<String, Object>) entry.getValue()));
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static List<ITimedConstraint> toList(Object value) {
		List<ITimedConstraint> t = new ArrayList<>();
		if (value instanceof Iterable<?>) {
			for(Map<String, Object> v : ((Iterable<Map<String, Object>>)value)) {
				t.add(of(v));
			}
		}
		return t;
	}

	private static ISelector toSelector(Object v) {
		return v == null ? LabelSelector.INSTANCE : new PropertySelector(v.toString());
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

	private PropertyContainer select(Node node, Relationship rel) {
		return isNodeContext ? node : rel;
	}
	
	@Override
	public boolean acceptIntermediate(int hits, int notHits) {
		return frequency.acceptIntermediate(hits, notHits);
	}
	
	@Override
	public boolean accept(int hits, int notHits) {
		return frequency.accept(hits, notHits);
	}

	@Override
	public String toString() {
		return "Constraint [frequency=" + frequency + ", selector=" + selector
				+ ", constraint=" + constraint + ", isNodeContext="
				+ isNodeContext + "]";
	}	
}

class AndConstraint implements ITimedConstraint {
	private final List<ITimedConstraint> constraints;
	
	public AndConstraint(List<ITimedConstraint> constraints) {
		this.constraints = constraints;
	}

	@Override
	public boolean accept(Node node, Relationship rel) {
		for(ITimedConstraint c : constraints) {
			if (!c.accept(node, rel)) {
				return false;
			}
		}
		return true;
	}
	@Override
	public boolean accept(int hits, int notHits) {
		for(ITimedConstraint c : constraints) {
			if (!c.accept(hits,notHits)) {
				return false;
			}
		}
		return true;
	}
	@Override
	public boolean acceptIntermediate(int hits, int notHits) {
		for(ITimedConstraint c : constraints) {
			if (!c.acceptIntermediate(hits,notHits)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return "AndConstraint [constraints=" + constraints + "]";
	}
}


class OrConstraint implements ITimedConstraint {
	private final List<ITimedConstraint> constraints;
	
	public OrConstraint(List<ITimedConstraint> constraints) {
		this.constraints = constraints;
	}

	@Override
	public boolean accept(Node node, Relationship rel) {
		for(ITimedConstraint c : constraints) {
			if (c.accept(node, rel)) {
				return true;
			}
		}
		return false;
	}
	@Override
	public boolean accept(int hits, int notHits) {
		for(ITimedConstraint c : constraints) {
			if (c.accept(hits,notHits)) {
				return true;
			}
		}
		return false;
	}
	@Override
	public boolean acceptIntermediate(int hits, int notHits) {
		for(ITimedConstraint c : constraints) {
			if (c.acceptIntermediate(hits,notHits)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "OrConstraint [constraints=" + constraints + "]";
	}
}

class NotConstraint implements ITimedConstraint {
	private final ITimedConstraint c;
	
	public NotConstraint(ITimedConstraint c) {
		this.c = c;
	}

	@Override
	public boolean accept(Node node, Relationship rel) {
		return !c.accept(node, rel);
	}
	
	@Override
	public boolean accept(int hits, int notHits) {
		return accept(notHits, hits);
	}
	
	@Override
	public boolean acceptIntermediate(int hits, int notHits) {
		return acceptIntermediate(notHits, hits);
	}

	@Override
	public String toString() {
		return "NotConstraint [c=" + c + "]";
	}
}

class RegionConstraint implements ITimedConstraint {
	
}