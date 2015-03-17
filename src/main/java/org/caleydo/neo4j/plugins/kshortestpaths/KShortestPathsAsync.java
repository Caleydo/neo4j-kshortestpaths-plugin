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

import org.caleydo.neo4j.plugins.kshortestpaths.KShortestPathsAlgo2.IPathReadyListener2;
import org.neo4j.cypher.javacompat.ExecutionEngine;
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
    		final @QueryParam("constraints") String contraints, @QueryParam("debug") Boolean debugD)
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
        		
        		
	        		final Node source = graphDb.getNodeById(from); //findById(from,tx);
	        		if (source == null) {
	        			writer.value("invalid source id "+from);
	        			return;
	        		}
	        		final Node target = graphDb.getNodeById(to); //findById(to, tx);

	        		if (target == null) {
	        			writer.value("invalid target id "+to);
	        			return;
	        		}	        		
	        		
	        		/*
	        		CostEvaluator<Double> costEvaluator = new EdgePropertyCostEvaluator(costFunction);
	        		KShortestPathsAlgo algo = new KShortestPathsAlgo(expander, costEvaluator);
	        		
	        		final Gson gson = new Gson();
	        		algo.run(source, target, k == null ? 1 : k.intValue(), new IPathReadyListener() {
	
						@Override
						public void onPathReady(WeightedPath path) {
							Map<String, Object> repr = KShortestPaths.getPathAsMap(path);
							gson.toJson(repr, Map.class, writer);
							try {
								writer.flush();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
	        		});
	        		*/
	        		KShortestPathsAlgo2 algo = new KShortestPathsAlgo2(expander, expander, debug);
	        		
	        		final Gson gson = new Gson();
	        		algo.run(db.inject(source), db.inject(target), k == null ? 1 : k.intValue(), maxDepth == null ? 100 : maxDepth.intValue(), new IPathReadyListener2() {
	
						@Override
						public void onPathReady(org.neo4j.graphdb.Path path) {
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
	        		});
	
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