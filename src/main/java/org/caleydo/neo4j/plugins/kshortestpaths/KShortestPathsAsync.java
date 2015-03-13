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

import org.caleydo.neo4j.plugins.kshortestpaths.KShortestPathsAlgo.IPathReadyListener;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Transaction;
import org.parboiled.common.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
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
    public Response findColleagues(@PathParam("from") final Long from, @PathParam("to") final Long to, final @QueryParam("k") Integer k, final @QueryParam("costFunction") String costFunction, 
    		final @QueryParam("ignoreDirection") Boolean ignoreDirection, final @QueryParam("nodeFilter") String nodeFilter, final @QueryParam("relationshipFilter") String edgeFilter)
    {
		
        StreamingOutput stream = new StreamingOutput()
        {
            @Override
            public void write( OutputStream os ) throws IOException, WebApplicationException
            {
            	final JsonWriter writer = new JsonWriter(new OutputStreamWriter(os));
            	writer.beginArray();
            	
            	PathExpander<?> expander = KShortestPaths.toExpander(ignoreDirection, toMap(nodeFilter), toMap(edgeFilter));
        		
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
}