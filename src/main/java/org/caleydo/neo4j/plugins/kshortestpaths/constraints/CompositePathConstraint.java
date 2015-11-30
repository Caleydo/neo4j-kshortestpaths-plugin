package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

public class CompositePathConstraint implements ICompositePathContraint, IConstraint {
	private final Collection<? extends IPathConstraint> constraints;
	public final boolean isAnd;

	public CompositePathConstraint(boolean isAnd, Collection<? extends IPathConstraint> constraints) {
		this.isAnd = isAnd;
		this.constraints = constraints;
	}

	@Override
	public String toString() {
		return "CompositePathConstraint [constraints=" + constraints + ", isAnd=" + isAnd + "]";
	}

	@Override
	public Iterable<? extends IPathConstraint> children() {
		return constraints;
	}

	@Override
	public boolean accept(Path path) {
		for(IPathConstraint c : constraints) {
			if (isAnd != c.accept(path)) {
				return !isAnd;
			}
		}
		return isAnd;
	}

	@Override
	public SortedSet<MatchRegion> matches(Path path) {
		BitSet total = new BitSet();
		if (this.isAnd) {
			//just the intersection of the region
			total.set(0, path.length());
			for(IPathConstraint p: constraints) {
				total.and(MatchRegion.toSet(p.matches(path), path.length()));
			}
		} else {
			//combine all regions to a large one
			for(IPathConstraint p: constraints) {
				for(MatchRegion r : p.matches(path)) {
					r.toBits(total, path.length());
				}
			}
		}
		return MatchRegion.from(total);
	}

	@Override
	public boolean accept(Node node, Relationship rel) {
		for(IPathConstraint c : constraints) {
			if (!(c instanceof IConstraint)) {
				continue;
			}
			if (isAnd != ((IConstraint)c).accept(node, rel)) {
				return !isAnd;
			}
		}
		return isAnd;
	}


	@Override
	public void toCypher(StringBuilder b, String var) {
		b.append("(");
		String in = isAnd ? " and " : " or ";
		List<String> l = new ArrayList<>();
		for(IPathConstraint c : constraints) {
			if (!(c instanceof IConstraint)) {
				continue;
			}
			IConstraint cc = (IConstraint)c;

			StringBuilder binner = new StringBuilder();
			cc.toCypher(binner, var);
			if (binner.length() > 0) {
				l.add(binner.toString());
			}
		}
		b.append(StringUtils.join(l, in));
		b.append(") ");
	}
}
