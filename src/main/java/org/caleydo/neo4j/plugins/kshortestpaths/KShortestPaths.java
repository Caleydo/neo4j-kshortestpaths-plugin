package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.caleydo.neo4j.plugins.kshortestpaths.constraints.DirectionContraints;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.IPathConstraint;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.InlineRelationships;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.PathConstraints;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.server.rest.repr.ValueRepresentation;
import org.parboiled.common.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

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
			@Description("constraints") @Parameter(name = "constraints", optional = true) String constraints
			) {

		FakeGraphDatabase db = new FakeGraphDatabase(graphDb);
		CustomPathExpander expander = toExpander(constraints, db, Collections.<FakeNode>emptyList());

		Transaction tx = graphDb.beginTx();

		CostEvaluator<Double> costEvaluator = new EdgePropertyCostEvaluator(costFunction);

		KShortestPathsAlgo algo = new KShortestPathsAlgo(expander, costEvaluator);

		List<WeightedPath> paths = algo.run(db.inject(source), db.inject(target), k);

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


	static CustomPathExpander toExpander(String constraints, FakeGraphDatabase db, Iterable<FakeNode> extraNodes) {
		return toExpander(toMap(constraints), db, extraNodes);
	}
	@SuppressWarnings("unchecked")
	static CustomPathExpander toExpander(Map<String,Object> c, FakeGraphDatabase db, Iterable<FakeNode> extraNodes) {
		Map<String,String> directions = (Map<String, String>) (c == null ? null : c.get("dir"));
		Map<String,Object> constraints = (Map<String, Object>) (c == null ? null : c.get("c"));
		Map<String,Object> inline = (Map<String,Object>)(c == null ? null : c.get("inline"));

		DirectionContraints d = new DirectionContraints(directions);
		IPathConstraint path = PathConstraints.parse(constraints);
		InlineRelationships rel = InlineRelationships.of(inline, db);

		return new CustomPathExpander(d, path, rel, extraNodes, c != null && c.get("acyclic") == Boolean.TRUE);
	}

	static Path slice(Path path, int start, int end) {
		if (start ==0 && end == -1) {
			return path;
		}
		List<Node> nodes = Iterables.toList(path.nodes());
		List<Relationship> rels = Iterables.toList(path.relationships());
		return new MyPath(nodes.subList(start, end < 0 ? nodes.size()+end+1: end),rels.subList(start, end < 0 ? rels.size()+end+1 : end));
	}


	private static Map<String, Object> toMap(String filter) {
		if (StringUtils.isEmpty(filter)) {
			return null;
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String,Object> r = new Gson().fromJson(filter, Map.class);
			return r;
		} catch(JsonSyntaxException e) {
			e.printStackTrace();
			System.err.println("cant convert given filter to a json map: "+filter);
			return null;
		}
	}


	static  Map<String, Object> getPathAsMap(Path path) {
		Map<String, Object> p = new HashMap<>();
		p.put("weight", path.length());

		return toMap(path, p);
	}

	static  Map<String, Object> getPathAsMap(WeightedPath path) {
		Map<String, Object> p = new HashMap<>();
		p.put("weight", path.weight());

		return toMap(path, p);
	}


	public static Map<String, Object> toMap(Path path, Map<String, Object> p) {
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
