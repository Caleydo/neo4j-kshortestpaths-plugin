package org.caleydo.neo4j.plugins.kshortestpaths.constraints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public class PathConstraint {
	private final List<ITimedConstraint> constraints;
	

	public PathConstraint(List<ITimedConstraint> constraints) {
		this.constraints = constraints;
	}

	public static PathConstraint of(List<Map<String,Object>> descs) {
		List<ITimedConstraint> t = new ArrayList<ITimedConstraint>();
		for(Map<String,Object> desc : descs) {
			t.add(Constraint.of(desc));
		}
		return new PathConstraint(t);
	}
	
	public boolean accept(Iterable<Node> nodes, Iterable<Relationship> rels) {
		final int[] hits = new int[this.constraints.size()];
		Arrays.fill(hits, 0);
		int l = 0;
		Iterator<Relationship> it = rels.iterator();
		for(Node node : nodes) {
			Relationship rel = it.hasNext() ? it.next() : null;
			l++;
			for(int i = 0; i < hits.length; ++i)
				if (this.constraints.get(i).accept(node, rel))
					hits[i]++;
		}
		
		for(int i = 0; i < hits.length; ++i) {
			int hit = hits[i];
			if(!constraints.get(i).accept(hit, l)) {
				//System.out.println("no: "+item);
				return false;
			}
		}
		return true;
	}
	
	public List<Region> matches(Iterable<Node> nodes, Iterable<Relationship> rels, boolean getFirstOnly) {
		List<Region> r = new ArrayList<>();
		Iterator<Relationship> it = rels.iterator();
		int i = 0;
		for(Node node : nodes) {
			Relationship rel = it.hasNext() ? it.next() : null;
			boolean good = true;
			for (ITimedConstraint c : this.constraints) {
				if (!c.accept(node,rel)) {
					good = false;
					break;
				}
			}
			if (good) {
				r.add(new Region(i,i));
				if (getFirstOnly) {
					break;
				}
			}
			i++;
		}
		return r;
	}
	
	public IIntermediate prepare(Iterable<Node> nodes, Iterable<Relationship> rels) {
		if (this.constraints.isEmpty()) {
			return TRUE;
		}
		final int[] hits = new int[this.constraints.size()];
		int l = 0;
		Arrays.fill(hits, 0);
		Iterator<Relationship> it = rels.iterator();
		Node last = null;
		for(Node node : nodes) {
			last = node;
			l++;
			Relationship rel = it.hasNext() ? it.next() : null;
			if (rel == null) {
				break;
			}
			for(int i = 0; i < hits.length; ++i)
				if (this.constraints.get(i).accept(node, rel))
					hits[i]++;
		}
		final int length = l+1; //added one new
		final Node prev = last;
		//System.out.println("node: "+nodes+" "+Arrays.toString(hits));
		return new IIntermediate() {
			@Override
			public boolean accept(Relationship rel, Node node) {
				for(int i = 0; i < hits.length; ++i) {
					int hit = hits[i];
					if (constraints.get(i).accept(prev, rel))
						hit += 1;
					if (constraints.get(i).accept(node,null))
						hit += 1;
					if (!constraints.get(i).acceptIntermediate(hit, length-hit)) {
						//System.out.println("no: "+item);
						return false;
					}
				}
				return true;
			}
		};
	}
	
	public interface IIntermediate {
		boolean accept(Relationship rel, Node to);
	}
	
	public static IIntermediate TRUE = new IIntermediate() {
		
		@Override
		public boolean accept(Relationship rel, Node to) {
			return true;
		}
	};


	@Override
	public String toString() {
		return "PathConstraint [constraints=" + constraints + "]";
	}
	
}

class 
