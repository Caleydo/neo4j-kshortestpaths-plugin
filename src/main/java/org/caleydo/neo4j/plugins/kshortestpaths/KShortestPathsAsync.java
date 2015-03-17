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
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

@Path("/kShortestPaths")
public class KShortestPathsAsync {
	private final GraphDatabaseService graphDb;

	public KShortestPathsAsync(@Context GraphDatabaseService database,
			@Context ExecutionEngine cypher) {
		this.graphDb = database;
	}

	@GET
    @Path("/{from}/{to}")
    public Response findColleagues(@PathParam("from") final Long from, @PathParam("to") final Long to, final @QueryParam("k") Integer k, final @QueryParam("maxDepth") Integer maxDepth, 
    		final @QueryParam("constraints") String contraints, @QueryParam("algorithm") final String algorithm, @QueryParam("costFunction") final String costFunction, @QueryParam("debug") Boolean debugD)
    {
		final boolean debug = debugD == Boolean.TRUE;
        StreamingOutput stream = new StreamingOutput()
        {
            @Override
            public void write( OutputStream os ) throws IOException, WebApplicationException
            {
            	final JsonWriter writer = new JsonWriter(new OutputStreamWriter(os));
            	writer.beginArray();
            	

        		FakeGraphDatabase db = new FakeGraphDatabase(graphDb);
            	CustomPathExpander expander = KShortestPaths.toExpander(contraints, db);
            	expander.setDebug(debug);
        		
        		Transaction tx = null;
        		try {
        			tx = graphDb.beginTx();
        		
        		
	        		Node source = graphDb.getNodeById(from); //findById(from,tx);
	        		if (source == null) {
	        			writer.value("invalid source id "+from);
	        			return;
	        		}
	        		Node target = graphDb.getNodeById(to); //findById(to, tx);

	        		if (target == null) {
	        			writer.value("invalid target id "+to);
	        			return;
	        		}	   
	        		
	        		source = db.inject(source);
	        		target = db.inject(target);
	        		
	        		int k_ = k == null ? 1 : k.intValue();
	        		int maxDepth_ = maxDepth == null ? 100 : maxDepth.intValue();
	        		
	       
	        		final Gson gson = new Gson();
	        		IPathReadyListener listener = new IPathReadyListener() {
	
						@Override
						public void onPathReady(WeightedPath path) {
							//System.out.println(path);
							//System.out.println(path.relationships());
							Map<String, Object> repr = KShortestPaths.getPathAsMap(path);
							gson.toJson(repr, Map.class, writer); 
							try {
								writer.flush();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
	        		};
	        		
	        		boolean runShortestPath = StringUtils.contains(algorithm,"shortestPath");
	        		boolean runDijsktra = StringUtils.contains(algorithm, "dijkstra");
	        		if (!runShortestPath && !runDijsktra) { //by default the shortest path only
	        			runShortestPath = true;
	        		}
	        		if (runShortestPath) {
		        		KShortestPathsAlgo2 algo = new KShortestPathsAlgo2(expander, expander, debug);
						algo.run(source, target, k_, maxDepth_, listener);
	        		}
	        		if (runDijsktra) {
		        		CostEvaluator<Double> costEvaluator = new EdgePropertyCostEvaluator(costFunction);
		        		KShortestPathsAlgo algo = new KShortestPathsAlgo(expander, costEvaluator);
		        		
		        		algo.run(source, target, k_, listener);
	        		}
	
	        		tx.success();
	        		tx.close();
	        		tx = null;
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

        return Response.ok().entity( stream ).type( MediaType.APPLICATION_JSON ).build();
    }
}