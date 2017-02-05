package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.Pair;

public class PathConstraints {
	public static final IPathConstraint acyclic = new ACyclicPathConstraint();

	public static IPathConstraint parse(Map<String,Object> obj) {
		if (obj == null || obj.isEmpty()) {
			return TRUE;
		}
		if (obj.containsKey("$region")) {
			MatchRegion r = toRegion(obj.get("$region"));
			IRegionRelationOperation op = toOperation(obj.get("$relate"));
			return new RegionMatcher(r, parse(obj.get("$query")), op);
		} else if (obj.containsKey("context")) {
			return parseElem(obj);
		} else if (obj.containsKey("$and")) {
			return new CompositePathConstraint(true, asList(obj.get("$and")));
		} else if (obj.containsKey("$or")) {
			return new CompositePathConstraint(false, asList(obj.get("$or")));
		} else if (obj.containsKey("$not")) {
			return new NotPathConstraint(parse(obj.get("$not")));
		} else if (obj.containsKey("$relate")) {
			return new RegionRelation(parse(obj.get("a")), parse(obj.get("b")), toOperation(obj.get("$relate")));
		}
		return parseElem(obj);
	}

	public static IPathConstraint and(Collection<IPathConstraint> c) {
		return new CompositePathConstraint(true, c);
	}
	public static IPathConstraint or(Collection<IPathConstraint> c) {
		return new CompositePathConstraint(false, c);
	}
	public static IPathConstraint and(IPathConstraint... constraints) {
		return and(Arrays.asList(constraints));
	}
	public static IPathConstraint or(IPathConstraint... constraints) {
		return or(Arrays.asList(constraints));
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
		if (v.startsWith("l") || v.startsWith("b")) {
			return RegionRelation.BEFORE;
		}
		if (v.startsWith("n") || v.startsWith("u")) {
			return RegionRelation.UNEQUAL;
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
		String c = Objects.toString(obj.get("context")).toLowerCase();
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

	private static final TrueConstraint TRUE = new TrueConstraint();

	static class TrueConstraint implements IConstraint {
		@Override
		public boolean accept(Node node, Relationship rel) {
			return true;
		}
		@Override
		public void toCypher(StringBuilder b, String var) {
			// b.append("1=1");
		}
		@Override
		public SortedSet<MatchRegion> matches(Path path) {
			SortedSet<MatchRegion> r = new TreeSet<MatchRegion>();
			final int l = path.length();
			for (int i = 0; i < l; ++i) {
				r.add(new MatchRegion(i, i));
			}
			return r;
		}
		@Override
		public boolean test(Path path) {
			return true;
		}
	}

	public static List<IPathConstraint> flatten(IPathConstraint c) {
		List<IPathConstraint> flat = new ArrayList<IPathConstraint>();
		flatten(c, flat);
		return flat;
	}

	private static void flatten(IPathConstraint c, List<IPathConstraint> r) {
		r.add(c);
		if (c instanceof ICompositePathContraint) {
			for(IPathConstraint p : ((ICompositePathContraint) c).children()) {
				flatten(p, r);
			}
		}
	}


	public static Pair<IConstraint,IConstraint> getStartEndConstraints(IPathConstraint p) {
		List<IPathConstraint> flat = flatten(p);

		List<IConstraint> start = new ArrayList<IConstraint>();
		List<IConstraint> end = new ArrayList<IConstraint>();

		for(IPathConstraint c : flat) {
			if (c instanceof RegionMatcher) {
				RegionMatcher m = (RegionMatcher)c;
				if (m.isStartRegion() && isNodeConstraint((IConstraint) m.getConstraint())) {
					start.add((IConstraint)m.getConstraint());
				}
				if (m.isEndRegion() && isNodeConstraint((IConstraint) m.getConstraint())) {
					end.add((IConstraint)m.getConstraint());
				}
			}
		}
		IConstraint start_c = combine(start, false);
		IConstraint end_c = combine(end, false);


		IConstraint perElem = getPerNodeConstraint(p);
		if (perElem != null && perElem != TRUE) {
			start_c = combine(Arrays.asList(start_c, perElem),true);
			end_c = combine(Arrays.asList(end_c, perElem),true);
		}

		return Pair.of(start_c, end_c);
	}

	private static boolean isNodeConstraint(IConstraint c) {
		if (c instanceof CompositePathConstraint) {
			for(IPathConstraint pi : ((CompositePathConstraint)c).children()) {
				if (!isNodeConstraint((IConstraint) pi)) {
					return false;
				}
			}
		}
		if (c instanceof TrueConstraint) {
			return true;
		}
		if (c instanceof ElemConstraint)
			return ((ElemConstraint) c).isNodeContext();
		return false; // unknown
	}

	public static IConstraint getPerNodeConstraint(IPathConstraint p) {
		if (p instanceof ElemConstraint && ((ElemConstraint) p).isNodeContext()) {
			return (IConstraint) p;
		}
		if (p instanceof CompositePathConstraint && ((CompositePathConstraint) p).isAnd) {
			List<IConstraint> r = new ArrayList<IConstraint>();
			for (IPathConstraint pi : ((CompositePathConstraint) p).children()) {
				IConstraint ri = getPerNodeConstraint(pi);
				if (ri != null && ri != TRUE) {
					r.add(ri);
				}
			}
			return combine(r, true);
		}
		return TRUE;
	}

	//just and and direct elem constraints
	public static IConstraint getPerElemConstraint(IPathConstraint p) {
		if (p instanceof ElemConstraint) {
			return (IConstraint)p;
		}
		if (p instanceof CompositePathConstraint && ((CompositePathConstraint) p).isAnd) {
			List<IConstraint> r = new ArrayList<IConstraint>();
			for(IPathConstraint pi : ((CompositePathConstraint)p).children()) {
				IConstraint ri = getPerElemConstraint(pi);
				if (ri != null && ri != TRUE) {
					r.add(ri);
				}
			}
			return combine(r, true);
		}
		return TRUE;
	}

	private static IConstraint combine(List<IConstraint> start, boolean and) {
		if (start.isEmpty()) {
			return null;
		}
		if (start.size() == 1) { return start.get(0); }
		return new CompositePathConstraint(and, start);
	}

	public static List<String> findAndLabels(IConstraint c) {
		List<String> r = new ArrayList<String>();
		if (c instanceof CompositePathConstraint && ((CompositePathConstraint) c).isAnd) {
			for(IPathConstraint p : ((CompositePathConstraint) c).children()) {
				r.addAll(findAndLabels((IConstraint)p));
			}
		} else if (c instanceof ElemConstraint) {
			String l = ((ElemConstraint) c).getLabels();
			if (l != null) {
				r.add(l);
			}
		}
		return r;
	}
}
