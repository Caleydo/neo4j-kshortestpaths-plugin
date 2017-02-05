package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang.StringUtils;
import org.neo4j.helpers.collection.Iterables;

public abstract class ValueConstraint implements Predicate<Object>{
	
	private static Function<Map<String, Object>, ValueConstraint> toPredicate = new Function<Map<String, Object>, ValueConstraint>() {
		@Override
		public ValueConstraint apply(Map<String, Object> from) {
			return of(from);
		}
	};

	static boolean isArray(Object value) {
		return value != null && value.getClass().isArray();
	}
	
	@Override
	public final boolean test(Object value) {
		//convert single value arrays to an array
		if (isArray(value) && ((Object[])value).length == 1) {
			value = ((Object[])value)[0];
		}
		return acceptImpl(value);
	}

	protected abstract boolean acceptImpl(Object value);

	public static ValueConstraint of(Map<String, Object> desc) {
		List<ValueConstraint> cs = new ArrayList<>();
		for(String key : desc.keySet()) {
			if (key.startsWith("$")) {
				cs.add(parse(key.substring(1), desc.get(key)));
			}
		}
		if (cs.size() == 1 ){
			return cs.get(0);
		}
		return and(cs);
	}

	@SuppressWarnings("unchecked")
	private static ValueConstraint parse(String op, Object desc) {
		op = op.toLowerCase();
		switch(op) {
		case "or":
			return or(Iterables.map(toPredicate, (Iterable<Map<String,Object>>)desc));
		case "and":
			return and(Iterables.map(toPredicate, (Iterable<Map<String,Object>>)desc));
		case "not": 
			return not(of((Map<String,Object>)desc));
		case "neq":
		case "not-equal":
			return not(eq(desc));
		case "eq":
		case "equal":
			return eq(desc);
		case "not-contains":
			return not(contains(desc));
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
	
	public static ValueConstraint and(Iterable<ValueConstraint> cs) {
		return new CombinePredicate(true, Iterables.asList(cs));
	}
	public static ValueConstraint or(Iterable<ValueConstraint> cs) {
		return new CombinePredicate(false, Iterables.asList(cs));
	}
	public static ValueConstraint not(ValueConstraint cs) {
		return new NotPredicate(cs);
	}
	
	static class EqualPredicate extends ValueConstraint {

		private Object eq;

		public EqualPredicate(Object value) {
			this.eq = value;
		}
		
		@Override
		protected boolean acceptImpl(Object value) {
			//System.out.println("compare: "+Objects.toString(value)+" "+Objects.toString(eq));
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
			b.append(" = ").append(Objects.toString(eq));
		}
		
		@Override
		public void toCypher(String property, StringBuilder b) {
			b.append(property).append(" = ").append(escaped(eq)).append(' ');
		}
		
	}
	

	static class CombinePredicate extends ValueConstraint {

		private List<ValueConstraint> cs;
		private final boolean isAnd;

		public CombinePredicate(boolean isAnd, List<ValueConstraint> cs) {
			this.isAnd = isAnd;
			this.cs = cs;
		}
		
		@Override
		protected boolean acceptImpl(Object value) {
			for(ValueConstraint c : cs) {
				if (isAnd != c.test(value)) {
					return !isAnd;
				}
			}
			return isAnd;
		}
		
		@Override
		public void toString(StringBuilder b) {
			b.append(" ").append(isAnd ? "and" : "or").append("(").append(StringUtils.join(cs,",")).append(") ");
		}
		
		@Override
		public void toCypher(String property, StringBuilder b) {
			b.append("(");
			String in = isAnd ? " and " : " or ";
			boolean first = true;
			for(ValueConstraint c : cs) {
				if (!first) {
					b.append(in);
				}
				first = false;
				c.toCypher(property, b);
			}
			b.append(") ");
		}
		
	}
	
	static class NotPredicate extends ValueConstraint {

		private ValueConstraint cs;

		public NotPredicate(ValueConstraint cs) {
			super();
			this.cs = cs;
		}

		@Override
		protected boolean acceptImpl(Object value) {
			return !cs.acceptImpl(value);
		}
		
		@Override
		public void toString(StringBuilder b) {
			b.append(" not (").append(cs).append(") ");
		}
		
		@Override
		public void toCypher(String property, StringBuilder b) {
			b.append("NOT (").append(cs).append(") ");
		}
		
	}
	
	public static class ContainsPredicate extends ValueConstraint {

		private Object in;

		public ContainsPredicate(Object value) {
			this.in = value;
		}
		
		public Object getIn() {
			return in;
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
		
		@Override
		public void toCypher(String property, StringBuilder b) {
			b.append(escaped(in)).append(" in ").append(property).append(' ');
		}
	}
	
	public abstract void toCypher(String property, StringBuilder b);
	
	public String escaped(Object in) {
		if (in instanceof String) {
			return "\""+in+"\"";
		}
		return in.toString();
	}

	public abstract void toString(StringBuilder b);
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		toString(b);
		return b.toString();
	}
	
}
