package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.neo4j.graphdb.Path;

interface IRegionRelationOperation {
	SortedSet<MatchRegion> combine(SortedSet<MatchRegion> a, SortedSet<MatchRegion> b, int length);

	boolean match(MatchRegion a, MatchRegion b, int length);
}

class EqualRegionRelation implements IRegionRelationOperation {
	@Override
	public SortedSet<MatchRegion> combine(SortedSet<MatchRegion> a,
			SortedSet<MatchRegion> b, int length) {
		SortedSet<MatchRegion> r = new TreeSet<>();
		for(MatchRegion ai : a) {
			MatchRegion aa = ai.toAbs(length);
			for(MatchRegion bi : b) {
				if (bi.toAbs(length).equals(aa)) {
					r.add(aa);
					break;
				}
			}
		}
		return a;
	}
	@Override
	public boolean match(MatchRegion a, MatchRegion b, int length) {
		return a.toAbs(length).equals(b.toAbs(length));
	}

	@Override
	public String toString() {
		return "equal";
	}
}
class UnEqualRegionRelation implements IRegionRelationOperation {
	@Override
	public SortedSet<MatchRegion> combine(SortedSet<MatchRegion> a,
			SortedSet<MatchRegion> b, int length) {
		SortedSet<MatchRegion> r = new TreeSet<>();
		for(MatchRegion ai : a) {
			MatchRegion aa = ai.toAbs(length);
			for(MatchRegion bi : b) {
				if (!bi.toAbs(length).equals(aa)) {
					r.add(aa);
					break;
				}
			}
		}
		return a;
	}
	@Override
	public boolean match(MatchRegion a, MatchRegion b, int length) {
		return !a.toAbs(length).equals(b.toAbs(length));
	}

	@Override
	public String toString() {
		return "unequal";
	}
}

class SequenceRegionRelation implements IRegionRelationOperation {
	@Override
	public SortedSet<MatchRegion> combine(SortedSet<MatchRegion> a,
			SortedSet<MatchRegion> b, int length) {
		SortedSet<MatchRegion> r = new TreeSet<>();
		for(MatchRegion ai : a) {
			int s = ai.getMinIndex(length);
			int m = ai.getMaxIndex(length) +1 ;
			for(MatchRegion bi : b) {
				if (bi.getMinIndex(length) == m) {
					r.add(new MatchRegion(s, bi.getMaxIndex(length)));
					break;
				}
			}
		}
		return r;
	}
	@Override
	public boolean match(MatchRegion ai, MatchRegion bi, int length) {
		int m = ai.getMaxIndex(length) +1 ;
		if (bi.getMinIndex(length) == m) {
				return true;
			}
		return false;
	}

	@Override
	public String toString() {
		return "sequence";
	}
}

class AfterRegionRelation implements IRegionRelationOperation {
	@Override
	public SortedSet<MatchRegion> combine(SortedSet<MatchRegion> a,
			SortedSet<MatchRegion> b, int length) {
		SortedSet<MatchRegion> r = new TreeSet<>();
		for(MatchRegion ai : a) {
			int s = ai.getMinIndex(length);
			int m =ai.getMaxIndex(length);
			for(MatchRegion bi : b) {
				if (m > bi.getMinIndex(length)) {
					r.add(new MatchRegion(s, bi.getMaxIndex(length)));
					break;
				}
			}
		}
		return r;
	}
	@Override
	public boolean match(MatchRegion ai, MatchRegion bi, int length) {
		int m =ai.getMaxIndex(length);
		if (m > bi.getMinIndex(length)) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "after";
	}
}
class BeforeRegionRelation implements IRegionRelationOperation {
	@Override
	public SortedSet<MatchRegion> combine(SortedSet<MatchRegion> a,
			SortedSet<MatchRegion> b, int length) {
		SortedSet<MatchRegion> r = new TreeSet<>();
		for(MatchRegion ai : a) {
			int s = ai.getMinIndex(length);
			int m =ai.getMaxIndex(length);
			for(MatchRegion bi : b) {
				if (m < bi.getMinIndex(length)) {
					r.add(new MatchRegion(s, bi.getMaxIndex(length)));
					break;
				}
			}
		}
		return r;
	}
	@Override
	public boolean match(MatchRegion ai, MatchRegion bi, int length) {
		int m =ai.getMaxIndex(length);
		if (m < bi.getMinIndex(length)) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "before";
	}
}

class OverlapRegionRelation implements IRegionRelationOperation {
	@Override
	public SortedSet<MatchRegion> combine(SortedSet<MatchRegion> a,
			SortedSet<MatchRegion> b, int length) {
		SortedSet<MatchRegion> r = new TreeSet<>();
		for(MatchRegion ai : a) {
			int s = ai.getMinIndex(length);
			int m = ai.getMaxIndex(length);
			for(MatchRegion bi : b) {
				int bmin = bi.getMinIndex(length);
				if (bmin >= s && bmin <= m) {
					r.add(new MatchRegion(s, bi.getMaxIndex(length)));
					break;
				}
			}
		}
		return r;
	}
	@Override
	public boolean match(MatchRegion ai, MatchRegion bi, int length) {
		int s = ai.getMinIndex(length);
		int m = ai.getMaxIndex(length);
		int bmin = bi.getMinIndex(length);
		if (bmin >= s && bmin <= m) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "overlap";
	}
}


public class RegionRelation implements ICompositePathContraint,ISequenceDependentConstraint {
	private final IPathConstraint a;
	private final IPathConstraint b;
	private final IRegionRelationOperation op;


	public RegionRelation(IPathConstraint a, IPathConstraint b,
			IRegionRelationOperation op) {
		super();
		this.a = a;
		this.b = b;
		this.op = op;
	}

	@Override
	public boolean test(Path path) {
		return !matches(path).isEmpty();
	}

	@Override
	public SortedSet<MatchRegion> matches(Path path) {
		SortedSet<MatchRegion> as = a.matches(path);
		if (as.isEmpty()) {
			return as;
		}
		SortedSet<MatchRegion> bs = b.matches(path);
		if (bs.isEmpty()) {
			return bs;
		}
		SortedSet<MatchRegion> r = this.op.combine(as, bs, path.length());
		// System.out.println(this.toString()+' '+r+' '+as+' '+bs);
		return r;
	}

	@Override
	public Iterable<IPathConstraint> children() {
		return Arrays.asList(a,b);
	}

	public static IRegionRelationOperation OVERLAP = new OverlapRegionRelation();
	public static IRegionRelationOperation AFTER = new AfterRegionRelation();
	public static IRegionRelationOperation BEFORE = new BeforeRegionRelation();
	public static IRegionRelationOperation EQUAL = new EqualRegionRelation();
	public static IRegionRelationOperation UNEQUAL = new UnEqualRegionRelation();
	public static IRegionRelationOperation SEQUENCE = new SequenceRegionRelation();


	@Override
	public String toString() {
		return "RegionRelation [a=" + a + ", b=" + b + ", op=" + op + "]";
	}

}
