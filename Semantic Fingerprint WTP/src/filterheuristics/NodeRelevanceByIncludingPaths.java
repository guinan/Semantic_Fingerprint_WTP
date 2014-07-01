package filterheuristics;

import graph.WTPGraph;
import graph.GraphCleaner.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.graphstream.graph.Node;

public class NodeRelevanceByIncludingPaths {
	
	private Map<String, Integer> numberOfIncludingPaths;

	
	
	public NodeRelevanceByIncludingPaths(){
		
	}
	
	public void filterByNumberOfPaths(WTPGraph graph, List<Path> paths, int relevanceThreshold){		
		generateIncludingPathMap(paths);
		removeUnrelevantNodes(graph, relevanceThreshold);
	}
	
	public void filterTheNMostVisited(WTPGraph graph, List<Path> paths, int maxNumber){
		generateIncludingPathMap(paths);
		findNMostRelevant(graph, maxNumber);
	}

	private void findNMostRelevant(WTPGraph graph, int maxNumber) {
		List<Integer> ints = new ArrayList<Integer>();
		for(String key : numberOfIncludingPaths.keySet()){
			ints.add(numberOfIncludingPaths.get(key));	
		}
		Collections.sort(ints,Collections.reverseOrder());
		int count = 0;
		for(Integer in : ints){
			count++;
			if(count > maxNumber){
				removeUnrelevantNodes(graph, in);
				return;
			}
		}
		
	}

	private void removeUnrelevantNodes(WTPGraph graph, int relevanceThreshold) {
		if(numberOfIncludingPaths != null){
			for(String key : numberOfIncludingPaths.keySet()){
				if(numberOfIncludingPaths.get(key) <= relevanceThreshold)
					graph.getGraph().removeNode(key);
					
			}
			System.out.println("nodes filtered by number of including paths heuristic");
		}
	}

	private void generateIncludingPathMap(List<Path> paths) {
		numberOfIncludingPaths = new HashMap<String,Integer>();
		for(Path p : paths) {
			for(Node n : p){
				Integer numberOfVisits = numberOfIncludingPaths.get(n.getId());
				//already visited
				if(numberOfVisits != null)
					numberOfIncludingPaths.put(n.getId(), numberOfVisits+1);
				//first visit
				else
					numberOfIncludingPaths.put(n.getId(), 1);
			}
			
		}		
	}
}
