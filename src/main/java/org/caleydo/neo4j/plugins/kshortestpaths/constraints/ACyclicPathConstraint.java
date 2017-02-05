package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;


public class ACyclicPathConstraint implements IPathConstraint {

	@Override
	public boolean test(Path item) {
		Set<Long> used = new HashSet<>();
		for(Node node : item.nodes()) {
			long id = node.getId();
			if (used.contains(id)) {
				return false; // loop
			}
			used.add(id);
		}
		return true;
	}

	@Override
	public SortedSet<MatchRegion> matches(Path path) {
		TreeSet<MatchRegion> r = new TreeSet<>();
		if (test(path)) {
			r.add(new MatchRegion(0, path.length()));
		}
		return r;
	}

	@Override
	public String toString() {
		return "acyclic";
	}

}
