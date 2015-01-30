package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Transaction;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;

// START SNIPPET: ShortestPath
public class KShortestPaths extends ServerPlugin {

	@Description("get the k shortest paths from source to target")
	@PluginTarget(GraphDatabaseService.class)
	public Iterable<WeightedPath> kShortestPaths(
			@Source GraphDatabaseService graphDb,
			@Description("Source node of the path") @Parameter(name = "source") Node source,
			@Description("Target node of the path") @Parameter(name = "target") Node target,
			@Description("The max number of paths to retrieve") @Parameter(name = "k") Integer k,
			@Description("Javascript cost function to determine the cost of an edge") @Parameter(name = "costFunction", optional = true) String costFunction,
			// @Description("Cost for an edge (>0)") @Parameter(name = "baseCost", optional = true) Double baseCost,
			// @Description("Map of property costs (property name : cost)") @Parameter(name = "propertyCosts", optional
			// = true) Map<String, Double> propertyCosts,
			@Description("Determines, whether the edge direction is considered") @Parameter(name = "ignoreDirection", optional = true) Boolean ignoreDirection) {

		PathExpander<?> expander;
		List<WeightedPath> paths = new ArrayList<>();
		if (ignoreDirection == null || !ignoreDirection) {
			expander = PathExpanders.forDirection(Direction.OUTGOING);
		} else {
			expander = PathExpanders.allTypesAndDirections();
			//
			// PathExpanderBuilder expanderBuilder = PathExpanderBuilder.empty();
			// for (int i = 0; i < types.length; i++) {
			// expanderBuilder = expanderBuilder.add(DynamicRelationshipType.withName(types[i]));
			// }
			// expander = expanderBuilder.build();
		}

		try (Transaction tx = graphDb.beginTx()) {

			CostEvaluator<Double> costEvaluator = new EdgePropertyCostEvaluator(costFunction);

			KShortestPathsAlgo algo = new KShortestPathsAlgo(expander, costEvaluator);

			paths = algo.run(source, target, k);

			tx.success();
		}
		return paths;
	}
}
