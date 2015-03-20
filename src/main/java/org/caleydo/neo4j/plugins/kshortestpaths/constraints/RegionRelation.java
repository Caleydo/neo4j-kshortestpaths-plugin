package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.neo4j.graphdb.Path;

interface IRegionRelationOperation {
	SortedSet<MatchRegion> combine(SortedSet<MatchRegion> a, SortedSet<MatchRegion> b, int length);
}

class EqualRegionRelation implements IRegionRelationOperation {
	@Override
	public SortedSet<MatchRegion> combine(SortedSet<MatchRegion> a,
			SortedSet<MatchRegion> b, int length) {
		a.retainAll(b); //keep identical ones
		return a;
	}
}

class SequenceRegionRelation implements IRegionRelationOperation {
	@Override
	public SortedSet<MatchRegion> combine(SortedSet<MatchRegion> a,
			SortedSet<MatchRegion> b, int length) {
		SortedSet<MatchRegion> r = new TreeSet<>();
		for(MatchRegion ai : a) {
			int s = ai.getMinIndex(length);
			int m =ai.getMaxIndex(length) +1 ;
			for(MatchRegion bi : b) {
				if (bi.getMinIndex(length) == m) {
					r.add(new MatchRegion(s, bi.getMaxIndex(length)));
					break;
				}
			}
		}
		return r;
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
				if (bi.getMinIndex(length) > m) {
					r.add(new MatchRegion(s, bi.getMaxIndex(length)));
					break;
				}
			}
		}
		return r;
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
	public boolean accept(Path path) {
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
		return this.op.combine(as, bs, path.length());
	}

	@Override
	public Iterable<IPathConstraint> children() {
		return Arrays.asList(a,b);
	}
	
	public static IRegionRelationOperation OVERLAP = new OverlapRegionRelation();
	public static IRegionRelationOperation AFTER = new AfterRegionRelation();
	public static IRegionRelationOperation EQUAL = new EqualRegionRelation();
	public static IRegionRelationOperation SEQUENCE = new SequenceRegionRelation();


	@Override
	public String toString() {
		return "RegionRelation [a=" + a + ", b=" + b + ", op=" + op + "]";
	}
	
}
