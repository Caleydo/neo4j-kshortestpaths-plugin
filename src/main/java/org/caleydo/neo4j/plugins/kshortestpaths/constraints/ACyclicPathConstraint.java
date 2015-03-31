package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;


public class ACyclicPathConstraint implements IPathConstraint {

	@Override
	public boolean accept(Path item) {
		Set<Long> used = new HashSet<>();
		int c = 0;
		for(Node node : item.nodes()) { 
			used.add(node.getId());
			c++;
		}
		return used.size() == c;
	}

	@Override
	public SortedSet<MatchRegion> matches(Path path) {
		TreeSet<MatchRegion> r = new TreeSet<>();
		if (accept(path)) {
			r.add(new MatchRegion(0, path.length()));
		}
		return r;
	}
	
	@Override
	public String toString() {
		return "acyclic";
	}

}
