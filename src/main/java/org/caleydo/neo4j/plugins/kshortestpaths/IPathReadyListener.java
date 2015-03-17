package org.caleydo.neo4j.plugins.kshortestpaths;

import org.neo4j.graphalgo.WeightedPath;


public interface IPathReadyListener {
	void onPathReady(WeightedPath path);
}