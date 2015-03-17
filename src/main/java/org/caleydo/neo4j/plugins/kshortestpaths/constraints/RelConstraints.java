package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.Predicates;

public class RelConstraints {
	private final IRelConstraint[] constraints;
	public RelConstraints(List<Map<String, Object>> nodeContraints) {
		this(parse(nodeContraints));
	}

	private RelConstraints(IRelConstraint[] constraints) {
		this.constraints = constraints;
	}
	
	public static RelConstraints of(IRelConstraint ...constraints) {
		return new RelConstraints(constraints);
	}

	private static IRelConstraint[] parse(List<Map<String, Object>> relConstraints) {
		if (relConstraints == null) {
			return new IRelConstraint[0];
		}
		IRelConstraint[] r = new IRelConstraint[relConstraints.size()];
		for(int i = 0; i < r.length; ++i) {
			r[i] = parse(relConstraints.get(i));
		}
		return r;
	}

	private static IRelConstraint parse(Map<String, Object> desc) {
		if (desc.containsKey("prop")) {
			return PropertyRelConstraint.parse(desc.get("prop").toString(), desc);
		}
		return PropertyLabelConstraint.parse(desc);
	}
	

	
	public boolean isValid(Iterable<Relationship> rels) {
		final int[] hits = new int[this.constraints.length];
		Arrays.fill(hits, 0);
		int l = 0;
		for(Relationship rel : rels) {
			l++;
			for(int i = 0; i < hits.length; ++i)
				if (this.constraints[i].accept(rel))
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

	public Predicate<Relationship> prepare(Iterable<Relationship> rels) {
		if (constraints.length == 0) {
			return Predicates.TRUE();
		}
		final int[] hits = new int[this.constraints.length];
		Arrays.fill(hits, 0);
		int l = 0;
		for(Relationship node : rels) {
			l++;
			for(int i = 0; i < hits.length; ++i)
				if (this.constraints[i].accept(node))
					hits[i]++;
		}
		final int length = l+1; //added one new
		//System.out.println("node: "+rels+" "+Arrays.toString(hits));
		return new Predicate<Relationship>() {
			@Override
			public boolean accept(Relationship item) {
				for(int i = 0; i < hits.length; ++i) {
					int hit = hits[i];
					if (constraints[i].accept(item))
						hit += 1;
					if (!constraints[i].timesIntermediate(hit, length))
						return false;
				}
				return true;
			}
		};
	}
	
	public static interface IRelConstraint extends Predicate<Relationship> {
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
