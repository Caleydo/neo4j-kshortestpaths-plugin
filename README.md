K-Shortest Paths Plugin for Neo4j [![Build Status][travis-image]][travis-url] [![Dependency Status][daviddm-image]][daviddm-url]
=================================
This is a plugin for the [Neo4j graph database](http://neo4j.com/) that calculates the k-shortest paths in a graph.

Installation
------------

```
git clone https://github.com/caleydo/neo4j-kshortestpaths-plugin.git
cd neo4j-kshortestpaths-plugin
```

Testing
-------

```
mvn test
```

Building
--------

```
mvn package
```

Eclipse import
--------------
Just import as Maven Project.

Setup in Neo4j
--------------
 1. Compile the project into a .jar file (in Eclipse right-click on project folder -> Run As -> Maven Install)
 2. Put the generated .jar file into the `/plugins` folder of the Neo4j installation
 3. The paths can be retrieved by sending a POST request to http://localhost:7474/db/data/ext/KShortestPaths/graphdb/kShortestPaths, sending a GET will retrieve information about how parameters need to be specified
 

***

<a href="https://caleydo.org"><img src="http://caleydo.org/assets/images/logos/caleydo.svg" align="left" width="200px" hspace="10" vspace="6"></a>
This repository is part of **[Caleydo Web](http://caleydo.org/)**, a platform for developing web-based visualization applications. 


[travis-image]: https://travis-ci.org/caleydo/clue_dummy.svg?branch=master
[travis-url]: https://travis-ci.org/caleydo/clue_dummy
[daviddm-image]: https://david-dm.org/caleydo/clue_dummy/status.svg
[daviddm-url]: https://david-dm.org/caleydo/clue_dummy
