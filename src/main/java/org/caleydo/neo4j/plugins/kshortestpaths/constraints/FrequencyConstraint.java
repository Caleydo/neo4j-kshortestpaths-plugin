package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.List;
import java.util.Map;

import org.neo4j.helpers.collection.Iterables;

public class FrequencyConstraint {
	private static final int unbound = Integer.MAX_VALUE;
	private final int min;
	private final int max;
	
	public FrequencyConstraint(int min, int max) {
		this.min = min;
		this.max = max;
	}
	
	public static FrequencyConstraint of(Map<String,Object> desc) {
		Object t = desc.get("times");
		if (t == null) { //all
			return new FrequencyConstraint(0, unbound);
		}
		if (t instanceof Number) {
			return new FrequencyConstraint(0, ((Number)t).intValue());
		}
		if (t instanceof Iterable<?>) {
			@SuppressWarnings("unchecked")
			List<Number> n = Iterables.toList((Iterable<Number>)t);
			return new FrequencyConstraint(n.get(0).intValue(), n.get(1).intValue());
		}
		return new FrequencyConstraint(0, unbound);
	}

	public boolean acceptIntermediate(int hits, int notHits) {
		if (max == unbound && notHits > 0) {
			return false;
		}
		if (hits > max) {
			return false;
		}
		if (max < 0 && notHits > -max) {
			return false;
		}
		return true;
	}
	
	public boolean accept(int hits, int notHits) {
		if (!acceptIntermediate(hits, notHits)) {
			return false;
		}
		if (hits < min) {
			return false;
		}
		if (min < 0 && notHits < -min) {
			return false;
		}
		return true;
	}
}
