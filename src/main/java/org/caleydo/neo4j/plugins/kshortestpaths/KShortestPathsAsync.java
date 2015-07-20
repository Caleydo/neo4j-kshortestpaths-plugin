package org.caleydo.neo4j.plugins.kshortestpaths;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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
import org.neo4j.function.Function;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.Pair;
import org.neo4j.helpers.collection.Iterables;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

@Path("/kShortestPaths")
public class KShortestPathsAsync {
	private final GraphDatabaseService graphDb;

	public KShortestPathsAsync(@Context GraphDatabaseService database) {
		this.graphDb = database;
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
					CustomPathExpander expander = KShortestPaths.toExpander(contraints, db ,Collections.<FakeNode>emptyList());
					expander.setDebug(debug);

					Pair<FakeNode, FakeNode> st = resolveNodes(from, to, expander.getConstraints(), db);
					if (st == null || st.first() == null || st.other() == null) {
						writer.value("missing start or end");
						return;
					}

					expander.setExtraNodes(Iterables.iterable(st.first(), st.other()));

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
								System.out.println("connection closed");
								e.printStackTrace();
								throw new ConnectionClosedException();
							}
						}
					};

					runImpl(k, maxDepth, algorithm, costFunction, debug, st.first(), st.other(), listener, db, expander);
				} catch(ConnectionClosedException e) {
					System.out.println("connection closed"+e);
					e.printStackTrace();
					e.printStackTrace(System.out);
				} catch(RuntimeException e) {
					System.out.println("exception"+e);
					e.printStackTrace();
					e.printStackTrace(System.out);
				}finally {
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

	public static Pair<FakeNode, FakeNode> resolveNodes(Long from, Long to, IPathConstraint constraints,
			FakeGraphDatabase db) {
		Pair<IConstraint,IConstraint> c = (from == null || to == null) ? PathConstraints.getStartEndConstraints(constraints) : null;

		FakeNode source = resolveNode(from, c == null ? null : c.first(), Direction.OUTGOING, db);
		if (source == null) {
			return null;
		}
		FakeNode target = resolveNode(to, c == null ? null : c.other(), Direction.INCOMING, db);
		if (target == null) {
			return null;
		}
		return Pair.of(source, target);
	}

	public static FakeNode resolveNode(Long id, IConstraint constraint, Direction dir, FakeGraphDatabase db) {
		Iterator<Node> r = null;
		if (id != null) {
			r = Iterables.iterable(db.getNodeById(id.longValue())).iterator();
		} else {
			StringBuilder b = new StringBuilder();
			List<String> labels = PathConstraints.findAndLabels(constraint);
			b.append("MATCH (n");
			if (!labels.isEmpty()) {
				b.append(':').append(StringUtils.join(labels,':'));
			}
			b.append(") WHERE ");
			constraint.toCypher(b, "n");
			b.append(" RETURN n");
			System.out.println(constraint+" "+b.toString());
			r = db.execute(b.toString()).columnAs("n");
		}
		return new FakeNode(dir == Direction.OUTGOING ? 1 << 20 : 3 << 20, db, dir, r);
	}


	public static void runImpl(final Integer k, final Integer maxDepth, final String algorithm, final String costFunction, final boolean debug, FakeNode source,
			FakeNode target, IPathReadyListener listener, FakeGraphDatabase db, CustomPathExpander expander) {

		Function<org.neo4j.graphdb.Path, org.neo4j.graphdb.Path> mapper = toMapper();

		int k_ = (k == null ? 1 : k.intValue());
		int maxDepth_ = 2+ (maxDepth == null ? 100 : maxDepth.intValue());

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

	private static Function<org.neo4j.graphdb.Path, org.neo4j.graphdb.Path> toMapper() {
		return new Function<org.neo4j.graphdb.Path, org.neo4j.graphdb.Path>() {
			@Override
			public org.neo4j.graphdb.Path apply(org.neo4j.graphdb.Path from) {
				return KShortestPaths.slice(from, 1,-2);
			}
		};
	}

	@GET
	@Path("/neighborsOf/{node}")
	public Response find(@PathParam("node") final Long node, final @QueryParam("constraints") String contraints,
			@QueryParam("debug") Boolean debugD) {
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
					CustomPathExpander expander = KShortestPaths.toExpander(contraints, db,
							Collections.<FakeNode> emptyList());
					expander.setDebug(debug);

					Node n = db.getNodeById(node.longValue());
					if (n == null) {
						writer.value("missing start or end");
						return;
					}
					final Gson gson = new Gson();
					for (Relationship r : expander.getRelationships(n)) {
						Map<String, Object> repr = KShortestPaths.getNodeAsMap(r.getOtherNode(n));
						try {
							gson.toJson(repr, Map.class, writer);
							writer.flush();
						} catch (IOException e) {
							// can't write the connection was closed -> abort
							System.out.println("connection closed");
							e.printStackTrace();
							throw new ConnectionClosedException();
						}
					}
				} catch (ConnectionClosedException e) {
					System.out.println("connection closed" + e);
					e.printStackTrace();
					e.printStackTrace(System.out);
				} catch (RuntimeException e) {
					System.out.println("exception" + e);
					e.printStackTrace();
					e.printStackTrace(System.out);
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
}