package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.neo4j.function.Function;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.impl.util.WeightedPathImpl;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.helpers.Predicate;

public class KShortestPathsAlgo2 {

	private final PathExpander<?> expander;
	private final Predicate<Path> pathAccepter;
	private final boolean debug;

	public KShortestPathsAlgo2(PathExpander<?> expander, Predicate<Path> pathAccepter, boolean debug) {
		this.expander = expander;
		this.pathAccepter = pathAccepter;
		this.debug = debug;

	}

	private void debug(Object ... args) {
		if (this.debug) {
			System.out.println(StringUtils.join(args,' '));
		}
	}

	public List<Path> run(Node start, Node end, int k, int maxLength, IPathReadyListener onPathReady, Function<Path,Path> mapper) {
		debug("start", start.getId(), end.getId(), "k", k, "maxLength", maxLength, this.expander);
		List<Path> result = new LinkedList<Path>();

		int checkedLength = -1;

        //first attempt: classic shortest path
		for (Path path : GraphAlgoFactory.shortestPath(expander, maxLength).findAllPaths(start, end)) {
			checkedLength = path.length(); //we have checked this length but may not accept it
			path = mapper.apply(path);
			debug("here",path)	;
			if (!pathAccepter.accept(path)) {
				debug("dimiss",path);
				continue; //dismiss result
			}
			debug("found", path);
			result.add(path);
			if (onPathReady != null) {
				onPathReady.onPathReady(new WeightedPathImpl(path.length(), path));
			}

			if (result.size() >= k) {
				break;
			}
		}
		debug("ended",checkedLength, result);

        //If there are no results, there will never be any. If there are enough, then we just return them:
        if (checkedLength < 0 || result.size() >= k) {
        	debug("abort search",checkedLength, result);
            return result;
        }

        //Now, we have some results, but not enough. All the resulting paths so far must have the same length (they are
        //the shortest paths after all). We try with longer path length until we have enough:
        for (int depth = checkedLength + 1; depth <= maxLength && result.size() < k; depth++) {
        	debug("check depth: ",depth);
			for (Path path : GraphAlgoFactory.pathsWithLength(expander, depth).findAllPaths(start, end)) {
        		path = mapper.apply(path);
        		if (!pathAccepter.accept(path)) {
    				debug("dimiss length "+depth,path);
    				continue; //dismiss result
    			}
    			debug("found length "+depth, path);
    			result.add(path);
    			if(onPathReady != null) {
    				onPathReady.onPathReady(new WeightedPathImpl(path.length(), path));
    			}
    			if (result.size() >= k) {
    				break;
    			}
    		}
        }
        debug("finally done: ",result);
        return result;
	}

	public interface IPathReadyListener2 {
		void onPathReady(Path path);
	}
}
