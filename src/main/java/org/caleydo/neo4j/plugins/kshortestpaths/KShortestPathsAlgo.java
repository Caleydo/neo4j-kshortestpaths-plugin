/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphalgo.impl.util.PathImpl;
import org.neo4j.graphalgo.impl.util.PathImpl.Builder;
import org.neo4j.graphalgo.impl.util.WeightedPathImpl;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;

/**
 * @author Christian
 *
 */
public class KShortestPathsAlgo {

	protected final PathFinder<? extends WeightedPath> shortestPathFinder;
	protected final CostEvaluator<Double> originalCostEvaluator;
	protected final InvalidRelationshipCostEvaluator costEvaluator;

	protected static class InvalidRelationshipCostEvaluator implements CostEvaluator<Double> {

		protected final CostEvaluator<Double> decoratee;
		protected Set<Long> invalidRelationships = new HashSet<>();

		public InvalidRelationshipCostEvaluator(CostEvaluator<Double> decoratee) {
			this.decoratee = decoratee;
		}

		@Override
		public Double getCost(Relationship relationship, Direction direction) {
			if (invalidRelationships.contains(relationship.getId()))
				return Double.POSITIVE_INFINITY;
			return decoratee.getCost(relationship, direction);
		}

		public void addInvalidRelationship(Relationship relationship) {
			invalidRelationships.add(relationship.getId());
		}

		public void clearInvalidRelationships() {
			invalidRelationships.clear();
		}

	}

	public KShortestPathsAlgo(PathExpander<?> expander, CostEvaluator<Double> costEvaluator) {
		this.costEvaluator = new InvalidRelationshipCostEvaluator(costEvaluator);
		this.originalCostEvaluator = costEvaluator;
		this.shortestPathFinder = GraphAlgoFactory.dijkstra(expander, this.costEvaluator);
	}
	public List<WeightedPath> run(Node sourceNode, Node targetNode, int k) {
		return run(sourceNode, targetNode, k, null);
	}

	public List<WeightedPath> run(Node sourceNode, Node targetNode, int k, IPathReadyListener onPathReady) {

		// Calculate shortest path first
		List<WeightedPath> paths = new ArrayList<>(k);
		WeightedPath shortestPath = shortestPathFinder.findSinglePath(sourceNode, targetNode);
		if (shortestPath == null)
			return paths;

		PriorityQueue<WeightedPath> pathCandidates = new PriorityQueue<WeightedPath>(20,
				new Comparator<WeightedPath>() {
					@Override
					public int compare(WeightedPath o1, WeightedPath o2) {
						return Double.compare(o1.weight(), o2.weight());
					}
				});

		Set<Integer> pathCandidateHashes = new HashSet<>();

		if (onPathReady != null) {
			onPathReady.onPathReady(shortestPath);
		}
		paths.add(shortestPath);

		pathCandidateHashes.add(generatePathHash(shortestPath));

		for (int i = 1; i < k; i++) {

			WeightedPath prevPath = paths.get(i - 1);

			for (Node spurNode : prevPath.nodes()) {
				if (spurNode.getId() == prevPath.endNode().getId())
					break;

				WeightedPath rootPath = getSubPathTo(prevPath, spurNode);

				for (Path path : paths) {
					Iterator<Relationship> pathIterator = path.relationships().iterator();
					boolean containsRootPath = true;

					// Test if the existing shortest path starts with the root path
					for (Relationship relationship : rootPath.relationships()) {
						if (!pathIterator.hasNext()) {
							containsRootPath = false;
							break;
						}

						Relationship pathRelationship = pathIterator.next();
						if (relationship.getId() != pathRelationship.getId()) {
							containsRootPath = false;
							break;
						}
					}

					// If so, set edge weight of following edge in that path to infinity
					if (containsRootPath) {
						if (pathIterator.hasNext()) {
							costEvaluator.addInvalidRelationship(pathIterator.next());
						}
					}
				}

				// Simulate removal of root path nodes (except spur node) by setting all their edge weights to
				// infinity
				for (Node rootPathNode : rootPath.nodes()) {
					if (rootPathNode.getId() != spurNode.getId()) {
						for (Relationship relationship : rootPathNode.getRelationships()) {
							costEvaluator.addInvalidRelationship(relationship);
						}
					}
				}

				WeightedPath spurPath = shortestPathFinder.findSinglePath(spurNode, targetNode);

				if (spurPath != null && !Double.isInfinite(spurPath.weight())) {
					WeightedPath pathCandidate = concatenate(rootPath, spurPath);
					Integer pathHash = generatePathHash(pathCandidate);
					if (!pathCandidateHashes.contains(pathHash)) {
						pathCandidates.add(pathCandidate);
						pathCandidateHashes.add(pathHash);
					}
				}

				// Restore edges
				costEvaluator.clearInvalidRelationships();

			}

			if (pathCandidates.isEmpty())
				break;

			WeightedPath nextBest = pathCandidates.poll();
			if (onPathReady != null) {
				onPathReady.onPathReady(nextBest);
			}
			paths.add(nextBest);

		}

		return paths;
	}

	private WeightedPath getSubPathTo(WeightedPath sourcePath, Node endNode) {
		if (endNode.getId() == sourcePath.startNode().getId()) {
			return new WeightedPathImpl(originalCostEvaluator, PathImpl.singular(endNode));
		}

		Builder pathBuilder = new Builder(sourcePath.startNode());
		Node currentStartNode = sourcePath.startNode();
		for (Relationship relationship : sourcePath.relationships()) {
			pathBuilder = pathBuilder.push(relationship);
			Node nextNode = relationship.getOtherNode(currentStartNode);
			if (nextNode.getId() == endNode.getId())
				break;
			currentStartNode = nextNode;
		}

		return new WeightedPathImpl(originalCostEvaluator, pathBuilder.build());
	}

	private int generatePathHash(WeightedPath path) {
		List<Long> idList = new ArrayList<Long>((path.length() * 2) + 1);
		for (Node n : path.nodes()) {
			idList.add(n.getId());
		}
		for (Relationship r : path.relationships()) {
			idList.add(r.getId());
		}

		return idList.hashCode();
	}

	private WeightedPath concatenate(WeightedPath firstPath, WeightedPath secondPath) {

		if (firstPath.endNode().getId() != secondPath.startNode().getId()) {
			throw new IllegalArgumentException("Can not concatenate paths, as end node of the first path ("
					+ firstPath.endNode().getId() + ") and start node of the second path ("
					+ secondPath.startNode().getId() + ")are different!");
		}

		Builder pathBuilder = new Builder(firstPath.startNode());
		for (Relationship relationship : firstPath.relationships()) {
			pathBuilder = pathBuilder.push(relationship);
		}
		for (Relationship relationship : secondPath.relationships()) {
			pathBuilder = pathBuilder.push(relationship);
		}
		return new WeightedPathImpl(originalCostEvaluator, pathBuilder.build());

	}
	
	public static interface IPathReadyListener {
		void onPathReady(WeightedPath path);
	}
}
