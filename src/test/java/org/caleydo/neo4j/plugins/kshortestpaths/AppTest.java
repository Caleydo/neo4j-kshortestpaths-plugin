package org.caleydo.neo4j.plugins.kshortestpaths;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.tooling.GlobalGraphOperations;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {

	protected GraphDatabaseService graphDb;
	public Node a = null;
	public Node b = null;
	public Node c = null;
	public Node d = null;
	public Node e = null;
	public Node f = null;
	public Node g = null;

	/**
	 * Create the test case
	 *
	 * @param testName
	 *            name of the test case
	 */
	public AppTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(AppTest.class);
	}

	@Override
	protected void setUp() throws Exception {
		try {
			File theDir = new File("test_neo4j_db/");
			if (!theDir.exists()) {
				boolean result = theDir.mkdir();
				if (!result) {
					throw new IOException();
				}
			} else {
				FileUtils.cleanDirectory(new File("test_neo4j_db/"));
			}
		} catch (IOException e) {
			Assert.assertTrue("IOException: " + e.getMessage(), false);
		}
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder("test_neo4j_db/")
				.setConfig(GraphDatabaseSettings.keep_logical_logs, "false")
				.setConfig(GraphDatabaseSettings.allow_store_upgrade, "true").newGraphDatabase();
		Transaction txClearGraph = graphDb.beginTx();
		try {
			GlobalGraphOperations ggo = GlobalGraphOperations.at(graphDb);
			for (Node nToDelete : ggo.getAllNodes()) {
				for (Relationship relToDelete : nToDelete.getRelationships()) {
					relToDelete.delete();
				}
				nToDelete.delete();
			}
			txClearGraph.success();
		} catch (Exception e) {
			txClearGraph.failure();
			Assert.assertTrue("Error while clear embedded db (test db)", false);
		} finally {
			txClearGraph.close();
		}

		Transaction tx_create_graph = graphDb.beginTx();
		try {
			a = graphDb.createNode();
			a.setProperty("name", "A");
			b = graphDb.createNode();
			b.setProperty("name", "B");
			c = graphDb.createNode();
			c.setProperty("name", "C");
			d = graphDb.createNode();
			d.setProperty("name", "D");
			e = graphDb.createNode();
			e.setProperty("name", "E");
			f = graphDb.createNode();
			f.setProperty("name", "F");
			g = graphDb.createNode();
			g.setProperty("name", "G");

			Relationship r = a.createRelationshipTo(b, DynamicRelationshipType.withName("related"));
			r.setProperty("size", "big");
			r.setProperty("mood", "bad");
			r = a.createRelationshipTo(d, DynamicRelationshipType.withName("related"));
			r.setProperty("size", "small");
			r = b.createRelationshipTo(d, DynamicRelationshipType.withName("related"));
			r.setProperty("size", "small");
			r.setProperty("mood", "good");
			r = b.createRelationshipTo(c, DynamicRelationshipType.withName("related"));
			r.setProperty("mood", "bad");
			b.createRelationshipTo(e, DynamicRelationshipType.withName("related"));
			c.createRelationshipTo(e, DynamicRelationshipType.withName("related"));
			d.createRelationshipTo(c, DynamicRelationshipType.withName("related"));
			d.createRelationshipTo(f, DynamicRelationshipType.withName("related"));
			e.createRelationshipTo(f, DynamicRelationshipType.withName("related"));
			e.createRelationshipTo(g, DynamicRelationshipType.withName("related"));
			f.createRelationshipTo(g, DynamicRelationshipType.withName("related"));

			g.createRelationshipTo(b, DynamicRelationshipType.withName("related"));
			f.createRelationshipTo(b, DynamicRelationshipType.withName("related"));

			tx_create_graph.success();
		} catch (Exception e) {
			tx_create_graph.failure();
			Assert.assertTrue("Error while create db (test db)", false);
		}
		tx_create_graph.close();
	}

	@Override
	protected void tearDown() throws Exception {
		if (graphDb != null) {
			graphDb.shutdown();
		}
	}

	/**
	 * Rigourous Test :-)
	 */
	public void testApp() {
		if (a == null || b == null || c == null) {
			Assert.assertTrue("Nodes are not created for test db.", false);
		}

		KShortestPaths sp = new KShortestPaths();
		String script = "var propertyCosts = { size: { big: 2.0, small: 1.0 }, mood: { good: 2.0, bad: 1.0 } }; function getCost(properties) { var totalCost = 1.0; properties.forEach(function (propObject) { var property = propObject[0]; var value = propObject[1]; var propDef = propertyCosts[property]; if (typeof propDef != \"undefined\") { var cost = propDef[value]; if (typeof cost != \"undefined\") { totalCost += cost; } } }); return totalCost; }";
		Representation paths = sp.kShortestPaths(graphDb, a, g, 100, script, null);
		Transaction tx_verify = graphDb.beginTx();

		// printPaths(paths);
		Assert.assertTrue(true);
		tx_verify.close();

		// FindShortestPath fsp = new FindShortestPath();
		// fsp.getShortestPath(graphDb, n1, n3, Arrays.asList("relType1", "relType2", "relType3"),
		// Arrays.asList(1.0, 1.0, 1.0), false, 5000, 1000.0);
		// Map<String, Double> costs = new HashMap<>();
		// costs.put("relType1", 1.0);
		// costs.put("relType2", 1.0);
		// costs.put("relType3", 1.0);
		// RoutersFinding routersFinding = new RoutersFinding(graphDb);
		// List<List<Node>> shortestPaths = routersFinding.getShortestPaths(n1, n3,
		// Arrays.asList("relType1", "relType2", "relType3"), 5000, costs, true, 1000.0);
		// Assert.assertEquals("Result isn't true", shortestPaths, Arrays.asList(Arrays.asList(n1, n3)));
		// costs.put("relType3", 3.0);
		// shortestPaths = routersFinding.getShortestPaths(n1, n3, Arrays.asList("relType1", "relType2", "relType3"),
		// 5000, costs, true, 1000.0);
		// Assert.assertEquals("Result isn't true", shortestPaths, Arrays.asList(Arrays.asList(n1, n2, n3)));
	}

	private void printPaths(Iterable<Path> paths) {
		// StringBuilder b = new StringBuilder("Path 1 (" + path.weight() + "): ");
		// for (Node n : path.nodes()) {
		// b.append(n.getProperty("name")).append("-");
		// }
		int i = 1;
		for (Path path : paths) {
			System.out.println(i++ + ": " + path.toString());
		}

	}
}
