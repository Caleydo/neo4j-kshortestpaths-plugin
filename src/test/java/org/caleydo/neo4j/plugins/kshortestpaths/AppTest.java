package org.caleydo.neo4j.plugins.kshortestpaths;

import static org.caleydo.neo4j.plugins.kshortestpaths.KShortestPathsAsync.resolveNodes;
import static org.caleydo.neo4j.plugins.kshortestpaths.KShortestPathsAsync.runImpl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.io.FileUtils;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.helpers.Pair;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.tooling.GlobalGraphOperations;
/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {

	protected GraphDatabaseService graphDb;
	public Node s = null;
	public Node u = null;
	public Node t = null;
	public Node _0 = null;
	public Node _1 = null;
	public Node _2 = null;
	public Node _3 = null;
	public Node _4 = null;
	public Node _5 = null;
	public Node _6 = null;
	public Node _7 = null;
	

	RelationshipType consistsOf = DynamicRelationshipType.withName("consistsOf");
	RelationshipType to = DynamicRelationshipType.withName("to");
	
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
			_0 = createNode("0");
			_1 = createNode("1");
			_2 = createNode("2");
			_3 = createNode("3");
			_4 = createNode("4");
			_5 = createNode("5");
			_6 = createNode("6");
			_7 = createNode("7");

			s = createSetNode("S");
			u = createSetNode("U");
			t = createSetNode("T");
			
			
			
			partOf(s,consistsOf,_0,_1,_2,_7);
			
			
			partOf(u,consistsOf,_5,_6);
			
			partOf(t,consistsOf,_3,_4,_7);
			_7.setProperty("sets", new String[] { "S", "T" });

			_3.createRelationshipTo(_6,to).setProperty("isNet", true);
			_5.createRelationshipTo(_4,to).setProperty("isNet", true);
			_0.createRelationshipTo(_5,to).setProperty("isNet", true);
			_2.createRelationshipTo(_4,to).setProperty("isNet", true);
			
			//double meaning isNet and isSet
			Relationship r= _0.createRelationshipTo(_1,to);
			r.setProperty("isNet", true);
			r.setProperty("isSet", true);
			r.setProperty("sets",new String[]{s.getProperty("name").toString()});
			
//			product(s, to, _0,_2,_7);
//			product(u, to, _5,_6);
//			product(t, to, _3,_4,_7);
//			
//			for(Node n:  Arrays.asList(_0,_2,_7)) {
//				r = _1.createRelationshipTo(n,to);
//				r.setProperty("isSet", true);
//				r.setProperty("sets",new String[]{s.getProperty("name").toString()});
//			}
//			for(Node n:  Arrays.asList(_2,_7)) {
//				n.createRelationshipTo(_1,to);
//				r.setProperty("isSet", true);
//				r.setProperty("sets",new String[]{s.getProperty("name").toString()});
//			}
			
			tx_create_graph.success();
		} catch (Exception e) {
			tx_create_graph.failure();
			Assert.assertTrue("Error while create db (test db)", false);
		}
		tx_create_graph.close();
	}

	private void partOf(Node s, RelationshipType rel, Node ...nodes) {
		for(Node n : nodes) {
			s.createRelationshipTo(n, rel);
			n.setProperty("sets", new String[]{s.getProperty("name").toString()});
		}
	}

	private Node createSetNode(String name) {
		Node n = graphDb.createNode(DynamicLabel.label("SetNode"));
		n.setProperty("name", name);
		return n;
	}
	private Node createNode(String name) {
		Node n = graphDb.createNode(DynamicLabel.label("NetworkNode"));
		n.setProperty("name", name);
		n.setProperty("sets", new String[0]);
		return n;
	}
	
	private void product(Node set, RelationshipType type, Node ...nodes) {
		for(Node n : nodes) {
			for (Node n2 : nodes) {
				if (n == n2)
					continue;
				Relationship r = n.createRelationshipTo(n2, type);
				r.setProperty("isSet", true);
				r.setProperty("sets",new String[]{set.getProperty("name").toString()});
			}
		}
	}

	@Override
	protected void tearDown() throws Exception {
		if (graphDb != null) {
			graphDb.shutdown();
		}
	}

	private void run(Node source, Node target, int k, String contraints) {

		Transaction tx = graphDb.beginTx();

		FakeGraphDatabase db = new FakeGraphDatabase(graphDb);
		CustomPathExpander expander = KShortestPaths.toExpander(contraints.replace('\'', '"'), db ,Collections.<FakeNode>emptyList());
		expander.setDebug(false);
		ExecutionEngine engine = new ExecutionEngine(graphDb);
		Pair<FakeNode,FakeNode> st = resolveNodes(source.getId(), target.getId(), expander.getConstraints(), db, engine);
		if (st == null || st.first() == null || st.other() == null) {
			return;
		}
		
		expander.setExtraNodes(Iterables.iterable(st.first(), st.other()));
		IPathReadyListener listener = new IPathReadyListener() {
			@Override
			public void onPathReady(WeightedPath path) {
				System.out.println(path);
			}
		};
		try {
			runImpl(k, 100, null,null,true, st.first(), st.other(), listener, db, expander);
			System.out.println("END");
		} catch (RuntimeException e ) {
			e.printStackTrace();
		}
		tx.success();
		tx.close();
		
	}
	
	/**
	 * Rigourous Test :-)
	 */
	public void test0_2() {
		FakeGraphDatabase db = new FakeGraphDatabase(graphDb);
		run(_0, _2, 100, "{'inline': {'flag': 'isSet', 'undirectional': false, 'toaggregate': 'name', 'aggregate': 'sets', 'inline': 'consistsOf', 'type': 'to'}, 'dir': {'to': 'out', 'consistsOf': 'in'}}");
		run(_0, _4, 100, "{'inline': {'flag': 'isSet', 'undirectional': false, 'toaggregate': 'name', 'aggregate': 'sets', 'inline': 'consistsOf', 'type': 'to'}, 'dir': {'to': 'out', 'consistsOf': 'in'}}");
		System.out.println("just NetworkNodes");
		//run(_0, _2, 100, new CustomPathExpander(DirectionContraints.of("to", Direction.OUTGOING, "consistsOf", Direction.INCOMING), PathConstraints.parse(null),inline, new ArrayList<FakeNode>()));
		System.out.println("just NetworkNodes and real isNet edges");
		//run(_0, _2, 100, new CustomPathExpander(DirectionContraints.of("to", Direction.OUTGOING), NodeConstraints.of(), RelConstraints.of(new PropertyRelConstraint(null, null, "isNet", ValueConstraint.eq(true))),inline));
	}
}
