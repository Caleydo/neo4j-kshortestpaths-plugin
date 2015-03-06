package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanderBuilder;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.server.rest.repr.ValueRepresentation;

import com.google.gson.Gson;

// START SNIPPET: ShortestPath
public class KShortestPaths extends ServerPlugin {

	@Description("get the k shortest paths from source to target")
	@PluginTarget(GraphDatabaseService.class)
	public Representation kShortestPaths(
			@Source GraphDatabaseService graphDb,
			@Description("Source node of the path") @Parameter(name = "source") Node source,
			@Description("Target node of the path") @Parameter(name = "target") Node target,
			@Description("The max number of paths to retrieve") @Parameter(name = "k") Integer k,
			@Description("Javascript cost function to determine the cost of an edge") @Parameter(name = "costFunction", optional = true) String costFunction,
			// @Description("Cost for an edge (>0)") @Parameter(name = "baseCost", optional = true) Double baseCost,
			// @Description("Map of property costs (property name : cost)") @Parameter(name = "propertyCosts", optional
			// = true) Map<String, Double> propertyCosts,
			@Description("Determines, whether the edge direction is considered") @Parameter(name = "ignoreDirection", optional = true) Boolean ignoreDirection,
			@Description("The edge types to consider") @Parameter(name = "edgeTypes", optional = true) String[] edgeTypes
			) {

		PathExpander<?> expander = toExpander(ignoreDirection, edgeTypes);

		Transaction tx = graphDb.beginTx();

		CostEvaluator<Double> costEvaluator = new EdgePropertyCostEvaluator(costFunction);

		KShortestPathsAlgo algo = new KShortestPathsAlgo(expander, costEvaluator);

		List<WeightedPath> paths = algo.run(source, target, k);

		List<Map<String, Object>> pathList = new ArrayList<Map<String, Object>>(paths.size());

		for (WeightedPath path : paths) {
			pathList.add(getPathAsMap(path));
		}

		tx.success();
		tx.close();

		Gson gson = new Gson();

		String resJSON = gson.toJson(pathList, pathList.getClass());
		return ValueRepresentation.string(resJSON);
	}

	static PathExpander<?> toExpander(Boolean ignoreDirection, String[] edgeTypes) {
		PathExpander<?> expander;
		boolean ignore = ignoreDirection != null && ignoreDirection.booleanValue();
		Direction dir = ignore ? Direction.BOTH : Direction.OUTGOING;
		if (edgeTypes != null && edgeTypes.length > 0) {			
			PathExpanderBuilder expanderBuilder = PathExpanderBuilder.empty();
			for(String type : edgeTypes) {
				expanderBuilder.add(DynamicRelationshipType.withName(type), dir);
			}
			expander = expanderBuilder.build();
		} else{
			expander = PathExpanders.forDirection(dir);
		}
		return expander;
	}

	static  Map<String, Object> getPathAsMap(WeightedPath path) {
		Map<String, Object> p = new HashMap<>();
		p.put("weight", path.weight());

		List<Map<String, Object>> nodes = new ArrayList<>();
		for (Node node : path.nodes()) {
			nodes.add(getNodeAsMap(node));
		}
		p.put("nodes", nodes);

		List<Map<String, Object>> edges = new ArrayList<>();
		for (Relationship rel : path.relationships()) {
			edges.add(getRelationshipAsMap(rel));
		}
		p.put("edges", edges);
		return p;
	}

	static  Map<String, Object> getNodeAsMap(Node node) {
		Map<String, Object> n = new HashMap<>();
		n.put("id", node.getId());
		n.put("labels", getNodeLabels(node));
		n.put("properties", getPropertyMap(node));
		return n;
	}

	static  Map<String, Object> getRelationshipAsMap(Relationship relationship) {
		Map<String, Object> r = new HashMap<>();
		r.put("id", relationship.getId());
		r.put("type", relationship.getType().name());
		r.put("sourceNodeId", relationship.getStartNode().getId());
		r.put("targetNodeId", relationship.getEndNode().getId());
		r.put("properties", getPropertyMap(relationship));

		return r;
	}

	static  List<String> getNodeLabels(Node node) {
		List<String> list = new ArrayList<>();
		for (Label label : node.getLabels()) {
			list.add(label.name());
		}
		return list;
	}

	static Map<String, Object> getPropertyMap(PropertyContainer container) {
		Map<String, Object> properties = new HashMap<>();
		for (String key : container.getPropertyKeys()) {
			properties.put(key, container.getProperty(key));
		}
		return properties;
	}
}
