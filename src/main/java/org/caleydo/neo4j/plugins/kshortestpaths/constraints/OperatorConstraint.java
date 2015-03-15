package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.neo4j.helpers.Function;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.Predicates;
import org.neo4j.helpers.collection.Iterables;

public abstract class OperatorConstraint implements Predicate<Object>{
	
	private static Function<Map<String, Object>, Predicate<Object>> toPredicate = new Function<Map<String, Object>, Predicate<Object>>() {
		@Override
		public Predicate<Object> apply(Map<String, Object> from) {
			return parse(from);
		}
	};

	static boolean isArray(Object value) {
		return value != null && value.getClass().isArray();
	}
	
	@Override
	public final boolean accept(Object value) {
		//convert single value arrays to an array
		if (isArray(value) && ((Object[])value).length == 1) {
			value = ((Object[])value)[0];
		}
		return acceptImpl(value);
	}

	protected abstract boolean acceptImpl(Object value);

	public static Predicate<Object> parse(Map<String, Object> desc) {
		List<Predicate<Object>> cs = new ArrayList<>();
		for(String key : desc.keySet()) {
			if (key.startsWith("$")) {
				cs.add(parse(key.substring(1), desc.get(key)));
			}
		}
		if (cs.size() == 1 ){
			return cs.get(0);
		}
		return Predicates.and(cs);
	}

	@SuppressWarnings("unchecked")
	private static Predicate<Object> parse(String op, Object desc) {
		op = op.toLowerCase();
		switch(op) {
		case "or":
			return Predicates.or(Iterables.map(toPredicate, (Iterable<Map<String,Object>>)desc));
		case "and":
			return Predicates.and(Iterables.map(toPredicate, (Iterable<Map<String,Object>>)desc));
		case "eq":
		case "equal":
			return eq(desc);
		case "contains":
			return contains(desc);
		}
		return null;
	}

	public static ContainsPredicate contains(Object value) {
		return new ContainsPredicate(value);
	}

	public static EqualPredicate eq(Object value) {
		return new EqualPredicate(value);
	}
	
	static class EqualPredicate extends OperatorConstraint {

		private Object eq;

		public EqualPredicate(Object value) {
			this.eq = value;
		}
		
		@Override
		protected boolean acceptImpl(Object value) {
			if (isArray(value) != isArray(this.eq)) {
				return false;
			}
			if (isArray(this.eq)) {
				return Arrays.deepEquals((Object[])value, (Object[])this.eq);
			}
			return Objects.equals(value, this.eq);
		}	

		
		@Override
		public void toString(StringBuilder b) {
			b.append(" eq:").append(Objects.toString(eq));
		}
		
	}
	
	static class ContainsPredicate extends OperatorConstraint {

		private Object in;

		public ContainsPredicate(Object value) {
			this.in = value;
		}
		
		@Override
		protected boolean acceptImpl(Object value) {
			if (isArray(value)) {
				Object[] v = (Object[])value;
				for(Object vi : v) {
					if (Objects.equals(vi,  this.in)) {
						return true;
					}
				}
				return false;
			}
			//single value
			return Objects.equals(value, this.in);
		}		

		
		@Override
		public void toString(StringBuilder b) {
			b.append(" contains:").append(isArray(in) ? Arrays.toString((Object[])in): Objects.toString(in));
		}
	}
	
	public abstract void toString(StringBuilder b);
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		toString(b);
		return b.toString();
	}
	
}
