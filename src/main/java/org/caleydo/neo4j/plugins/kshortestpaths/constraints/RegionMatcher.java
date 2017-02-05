package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.neo4j.graphdb.Path;

public class RegionMatcher implements ICompositePathContraint, ISequenceDependentConstraint {
	private final IPathConstraint c;
	private final MatchRegion region;
	private final IRegionRelationOperation op;

	public RegionMatcher(MatchRegion region, IPathConstraint c, IRegionRelationOperation op) {
		this.region = region;
		this.c = c;
		this.op = op;
	}

	public IPathConstraint getConstraint() {
		return c;
	}

	@Override
	public Iterable<IPathConstraint> children() {
		return Arrays.asList(c);
	}

	@Override
	public boolean test(Path path) {
		return !matches(path).isEmpty();
	}

	@Override
	public SortedSet<MatchRegion> matches(Path path) {
		SortedSet<MatchRegion> matches = this.c.matches(path);
		MatchRegion r = this.region.toAbs(path.length());
		SortedSet<MatchRegion> result = new TreeSet<MatchRegion>();
		for(MatchRegion m : matches) {
			if (op.match(m, r, path.length())) {
				result.add(m);
			}
		}
		// System.out.println(this.toString()+' '+r+' '+matches.contains(r)+' '+matches+" "+result);
		return result;
	}

	public boolean isStartRegion() {
		return this.region.isStart() && this.op == RegionRelation.EQUAL && areAllConstraints();
	}
	public boolean isEndRegion() {
		return this.region.isEnd() && this.op == RegionRelation.EQUAL && areAllConstraints();
	}

	private boolean areAllConstraints() {
		List<IPathConstraint> list = PathConstraints.flatten(c);
		for(IPathConstraint cc : list) {
			if (!(cc instanceof IConstraint)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return "RegionMatcher [c=" + c + ", region=" + region + ", op=" + op + "]";
	}


}
