package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.Predicates;

public class NodeConstraints {
	private final INodeConstraint[] constraints;
	public NodeConstraints(List<Map<String, Object>> nodeContraints) {
		this(parse(nodeContraints));
	}

	private NodeConstraints(INodeConstraint[] constraints) {
		this.constraints = constraints;
	}
	
	public static NodeConstraints of(INodeConstraint ...constraints) {
		return new NodeConstraints(constraints);
	}

	private static INodeConstraint[] parse(List<Map<String, Object>> nodeContraints) {
		if (nodeContraints == null) {
			return new INodeConstraint[0];
		}
		INodeConstraint[] r = new INodeConstraint[nodeContraints.size()];
		for(int i = 0; i < r.length; ++i) {
			r[i] = parse(nodeContraints.get(i));
		}
		return r;
	}

	private static INodeConstraint parse(Map<String, Object> desc) {
		if (desc.containsKey("prop")) {
			return PropertyNodeConstraint.parse(desc.get("prop").toString(), desc);
		}
		return LabelConstraint.parse(desc);
	}
	
	public boolean isValid(Iterable<Node> nodes) {
		final int[] hits = new int[this.constraints.length];
		Arrays.fill(hits, 0);
		int l = 0;
		for(Node node : nodes) {
			l++;
			for(int i = 0; i < hits.length; ++i)
				if (this.constraints[i].accept(node))
					hits[i]++;
		}
		
		for(int i = 0; i < hits.length; ++i) {
			int hit = hits[i];
			if(!constraints[i].timesFinal(hit, l)) {
				//System.out.println("no: "+item);
				return false;
			}
		}
		return true;
	}

	public Predicate<Node> prepare(Iterable<Node> nodes) {
		if (this.constraints.length == 0) {
			return Predicates.TRUE();
		}
		final int[] hits = new int[this.constraints.length];
		Arrays.fill(hits, 0);
		int l = 0;
		for(Node node : nodes) {
			l++;
			for(int i = 0; i < hits.length; ++i)
				if (this.constraints[i].accept(node))
					hits[i]++;
		}
		final int length = l+1; //added one new
		//System.out.println("node: "+nodes+" "+Arrays.toString(hits));
		return new Predicate<Node>() {
			@Override
			public boolean accept(Node item) {
				for(int i = 0; i < hits.length; ++i) {
					int hit = hits[i];
					if (constraints[i].accept(item))
						hit += 1;
					if (!constraints[i].timesIntermediate(hit, length)) {
						//System.out.println("no: "+item);
						return false;
					}
				}
				return true;
			}
		};
	}
	
	public static interface INodeConstraint extends Predicate<Node> {
		boolean timesIntermediate(int hits, int pathLength);
		boolean timesFinal(int hits, int pathLength);
	}
	

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("[");
		b.append(StringUtils.join(this.constraints, ","));
		b.append("]");
		return b.toString();
	}

}
