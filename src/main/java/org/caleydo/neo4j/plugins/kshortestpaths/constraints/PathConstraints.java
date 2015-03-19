package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import org.neo4j.graphdb.Path;

public class PathConstraints {
	public static IPathConstraint parse(Map<String,Object> obj) {
		if (obj == null || obj.isEmpty()) {
			return TRUE;
		}
		if (obj.containsKey("$region")) {
			MatchRegion r = toRegion(obj.get("$region"));
			obj.remove("$region");
			return new RegionMatcher(r, parse(obj));
		} else if (obj.containsKey("$context")) {
			return parseElem(obj);
		} else if (obj.containsKey("$and")) {
			return new CompositePathConstraint(true, asList(obj.get("$and")));
		} else if (obj.containsKey("$or")) {
			return new CompositePathConstraint(false, asList(obj.get("$or")));
		} else if (obj.containsKey("$relate")) {
			return new RegionRelation(parse(obj.get("a")), parse(obj.get("b")), toOperation(obj.get("$relate")));
		}
		return parseElem(obj);
	}
	
	private static IRegionRelationOperation toOperation(Object object) {
		String v = object.toString().toLowerCase();
		if (v.startsWith("m") || v.startsWith("o")) {
			return RegionRelation.OVERLAP;
		}
		if (v.startsWith("s")) {
			return RegionRelation.SEQUENCE;
		}
		if (v.startsWith("g") || v.startsWith("a")) {
			return RegionRelation.AFTER;
		}
		
		return RegionRelation.EQUAL;
	}

	private static List<IPathConstraint> asList(Object object) {
		List<IPathConstraint> r = new ArrayList<IPathConstraint>();
		if (object instanceof Iterable<?>) {
			for(Object obj : (Iterable<?>)object) {
				r.add(parse(obj));
			}
		} else if (object instanceof Map<?,?>) {
			r.add(parse(object));
		}
		return r;
	}

	private static IPathConstraint parseElem(Map<String, Object> obj) {
		String c = Objects.toString(obj.get("$context")).toLowerCase();
		boolean isNodeContext = c.startsWith("n") || c.isEmpty();
		ValueConstraint constraint = ValueConstraint.of(obj);
		ISelector s;
		if (obj.containsKey("prop")) {
			s = new PropertySelector(obj.get("prop").toString());
		} else {
			s = LabelSelector.INSTANCE;
		}
		return new ElemConstraint(s, constraint, isNodeContext);
	}

	private static MatchRegion toRegion(Object object) {
		if (object instanceof Number) {
			int i = ((Number)object).intValue();
			return new MatchRegion(i, i);
		} else if (object instanceof List<?>) {
			@SuppressWarnings("unchecked")
			List<Number> l = (List<Number>)object;
			return new MatchRegion(l.get(0).intValue(), l.get(1).intValue());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static IPathConstraint parse(Object object) {
		if (object instanceof Map<?,?>) {
			return parse((Map<String,Object>)object);
		}
		return TRUE;
	}

	private static final IPathConstraint TRUE = new TrueConstraint();
	
	static class TrueConstraint implements IPathConstraint {
		@Override
		public SortedSet<MatchRegion> matches(Path path) {
			SortedSet<MatchRegion> r = new TreeSet<MatchRegion>();
			r.add(new MatchRegion(0, path.length()));
			return r;
		}
		@Override
		public boolean accept(Path path) {
			return true;
		}
	}
}
