package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.Arrays;
import java.util.SortedSet;

import org.neo4j.graphdb.Path;

public class RegionMatcher implements ICompositePathContraint, ISequenceDependentConstraint {
	private final IPathConstraint c;
	private final MatchRegion region;

	public RegionMatcher(MatchRegion region, IPathConstraint c) {
		this.region = region;
		this.c = c;
	}

	@Override
	public Iterable<IPathConstraint> children() {
		return Arrays.asList(c);
	}
	
	@Override
	public boolean accept(Path path) {
		return !matches(path).isEmpty();
	}

	@Override
	public SortedSet<MatchRegion> matches(Path path) {
		SortedSet<MatchRegion> matches = this.c.matches(path);
		MatchRegion r = this.region.toAbs(path.length());
		if (matches.contains(r)) {
			matches.clear();
			matches.add(r);
		} else {
			matches.clear();
		}
		return matches;
	}
}
