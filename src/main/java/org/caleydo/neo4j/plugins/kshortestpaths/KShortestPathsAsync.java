package org.caleydo.neo4j.plugins.kshortestpaths;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang.StringUtils;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.IConstraint;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.IPathConstraint;
import org.caleydo.neo4j.plugins.kshortestpaths.constraints.PathConstraints;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.Function;
import org.neo4j.helpers.Pair;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

@Path("/kShortestPaths")
public class KShortestPathsAsync {
	private final GraphDatabaseService graphDb;
	private final ExecutionEngine engine;

	public KShortestPathsAsync(@Context GraphDatabaseService database, @Context ExecutionEngine cypher) {
		this.graphDb = database;
		this.engine = new ExecutionEngine(graphDb);
	}
	
	@GET
	@Path("/")
	public Response find(final @QueryParam("k") Integer k,
			final @QueryParam("maxDepth") Integer maxDepth, final @QueryParam("constraints") String contraints,
			@QueryParam("algorithm") final String algorithm,
			@QueryParam("costFunction") final String costFunction, @QueryParam("debug") Boolean debugD) {
		return findGiven(null,null, k, maxDepth, contraints, algorithm, costFunction, debugD);
	}

	@GET
	@Path("/{from}/{to}")
	public Response findGiven(@PathParam("from") final Long from, @PathParam("to") final Long to, final @QueryParam("k") Integer k,
			final @QueryParam("maxDepth") Integer maxDepth, final @QueryParam("constraints") String contraints,
			@QueryParam("algorithm") final String algorithm,
			@QueryParam("costFunction") final String costFunction, @QueryParam("debug") Boolean debugD) {
		final boolean debug = debugD == Boolean.TRUE;
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				final JsonWriter writer = new JsonWriter(new OutputStreamWriter(os));
				writer.beginArray();

				Transaction tx = null;
				try {
					tx = graphDb.beginTx();
					
					FakeGraphDatabase db = new FakeGraphDatabase(graphDb);
					CustomPathExpander expander = KShortestPaths.toExpander(contraints, db);
					expander.setDebug(debug);

					Pair<Node,Node> st = resolveNodes(from, to, expander.getConstraints(), db);
					if (st == null || st.first() == null || st.other() == null) {
						writer.value("missing start or end");
						return;
					}

					final Gson gson = new Gson();
					IPathReadyListener listener = new IPathReadyListener() {

						@Override
						public void onPathReady(WeightedPath path) {
							// System.out.println(path);
							// System.out.println(path.relationships());
							Map<String, Object> repr = KShortestPaths.getPathAsMap(path);
							try {
								gson.toJson(repr, Map.class, writer);
								writer.flush();
							} catch (IOException e) {
								//can't write the connection was closed -> abort
								System.err.println("connection closed");
								e.printStackTrace();
								throw new ConnectionClosedException();
							}
						}
					};
					
					runImpl(k, maxDepth, algorithm, costFunction, debug, st.first(), st.other(), listener, db, expander);
				} catch(ConnectionClosedException e) {
					
				} finally {
					if (tx != null) {
						tx.failure();
						tx.close();
					}
					writer.endArray();
					writer.flush();
					writer.close();
				}
			}

		};

		return Response.ok().entity(stream).type(MediaType.APPLICATION_JSON).build();
	}

	protected Pair<Node, Node> resolveNodes(Long from, Long to, IPathConstraint constraints, FakeGraphDatabase db) {
		Pair<IConstraint,IConstraint> c = (from == null || to == null) ? PathConstraints.getStartEndConstraints(constraints) : null;
		
		Node source = resolveNode(from, c.first(), Direction.OUTGOING, db);
		if (source == null) {
			return null;
		}
		Node target = resolveNode(from, c.other(), Direction.INCOMING, db);
		if (target == null) {
			return null;
		}
		return Pair.of(source, target);
	}

	private Node resolveNode(Long id, IConstraint constraint, Direction dir, FakeGraphDatabase db) {
		Node r = null;
		if (id != null) {
			r = graphDb.getNodeById(id.longValue());
		}
		if (r != null || constraint == null) {
			return r;
		}
		StringBuilder b = new StringBuilder();
		b.append("MATCH (n) WHERE ");
		constraint.toCypher(b, "n");
		b.append(" RETURN n");
		System.out.println(b.toString());
		ResourceIterator<Node> nodes = engine.execute(b.toString()).columnAs("n");
		return new FakeNode(dir == Direction.OUTGOING ? -1 : -2, db, dir, nodes);
	}

	public void runImpl(final Integer k, final Integer maxDepth, final String algorithm, final String costFunction, final boolean debug, Node source,
			Node target, IPathReadyListener listener, FakeGraphDatabase db, CustomPathExpander expander) {
		source = db.inject(source);
		target = db.inject(target);
		
		Function<org.neo4j.graphdb.Path, org.neo4j.graphdb.Path> mapper = toMapper(source instanceof FakeNode, target instanceof FakeNode);

		int k_ = k == null ? 1 : k.intValue();
		int maxDepth_ = maxDepth == null ? 100 : maxDepth.intValue();

		boolean runShortestPath = StringUtils.contains(algorithm, "shortestPath");
		boolean runDijsktra = StringUtils.contains(algorithm, "dijkstra");
		if (!runShortestPath && !runDijsktra) { // by default the shortest path
												// only
			runShortestPath = true;
		}
		if (runShortestPath) {
			KShortestPathsAlgo2 algo = new KShortestPathsAlgo2(expander, expander, debug);
			algo.run(source, target, k_, maxDepth_, listener, mapper);
		}
		if (runDijsktra) {
			CostEvaluator<Double> costEvaluator = new EdgePropertyCostEvaluator(costFunction);
			KShortestPathsAlgo algo = new KShortestPathsAlgo(expander, costEvaluator);

			algo.run(source, target, k_, listener);
		}
	}

	private Function<org.neo4j.graphdb.Path, org.neo4j.graphdb.Path> toMapper(final boolean source_fake, final boolean target_fake) {
		return new Function<org.neo4j.graphdb.Path, org.neo4j.graphdb.Path>() {
			@Override
			public org.neo4j.graphdb.Path apply(org.neo4j.graphdb.Path from) {
				return KShortestPaths.slice(from, source_fake ? 1 : 0, target_fake ? -2 : -1);
			}
		};
	}
}