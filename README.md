K-Shortest Paths Plugin for Neo4j
=================================
This is a plugin for the [Neo4j graph database](http://neo4j.com/) that calculates the k-shortest paths in a graph.

##Eclipse import
Just import as Maven Project.

##Setup in Neo4j
 1. Compile the project into a .jar file (in Eclipse right-click on project folder -> Run As -> Maven Install)
 2. Put the generated .jar file into the /plugins folder of the Neo4j installation
 3. The paths can be retrieved by sending a POST request to http://localhost:7474/db/data/ext/KShortestPaths/graphdb/kShortestPaths, sending a GET will retrieve information about how parameters need to be specified