package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.LinkedList;
import java.util.List;

import org.neo4j.graphalgo.impl.path.ShortestPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.helpers.Predicate;

public class KShortestPathsAlgo2 {
	
	private final PathExpander<?> expander;
	private final Predicate<Path> pathAccepter;

	public KShortestPathsAlgo2(PathExpander<?> expander, Predicate<Path> pathAccepter) {
		this.expander = expander;
		this.pathAccepter = pathAccepter;
		
	}

	public List<Path> run(Node start, Node end, int k, int maxLength, IPathReadyListener2 onPathReady) {
		List<Path> result = new LinkedList<Path>();

        //first attempt: classic shortest path
		for(Path path : new ShortestPath(maxLength, expander).findAllPaths(start, end)) {
			if (!pathAccepter.accept(path)) {
				continue; //dismiss result
			}
			result.add(path);
			if (onPathReady != null) {
				onPathReady.onPathReady(path);
			}
		}

        //If there are no results, there will never be any. If there are enough, then we just return them:
        if (result.isEmpty() || result.size() >= k) {
            return result;
        }

        //Now, we have some results, but not enough. All the resulting paths so far must have the same length (they are
        //the shortest paths after all). We try with longer path length until we have enough:
        for (int depth = result.get(0).length() + 1; depth <= maxLength && result.size() < k; depth++) {
        	for(Path path : new ShortestPath(depth, expander, Integer.MAX_VALUE, true).findAllPaths(start, end)) {
        		if (!pathAccepter.accept(path)) {
    				continue; //dismiss result
    			}
    			result.add(path);
    			if(onPathReady != null) {
    				onPathReady.onPathReady(path);
    			}
    			if (result.size() >= k) {
    				break;
    			}
    		}
        }
        return result;
	}
	
	public interface IPathReadyListener2 {
		void onPathReady(Path path);
	}
}
