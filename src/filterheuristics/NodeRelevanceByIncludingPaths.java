package filterheuristics;

import graph.WTPGraph;
import graph.GraphCleaner.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.graphstream.graph.Node;

public class NodeRelevanceByIncludingPaths {
	
	private Map<String, Integer> numberOfIncludingPaths;
	private Map<String,String> visitedWhileClustering;
	
	
	public NodeRelevanceByIncludingPaths(){
		
	}
	
	public void filterByNumberOfPaths(WTPGraph graph, List<Path> paths, int relevanceThreshold){		
		generateIncludingPathMap(paths);
		removeUnrelevantNodes(graph, relevanceThreshold);
	}
	
	public void filterTheNMostVisited(WTPGraph graph, List<Path> paths, int maxNumber){
		generateIncludingPathMap(paths);
		findNMostRelevant(graph, maxNumber);
		//filerBiggestCluster(graph);
	}

	private void filerBiggestCluster(WTPGraph graph) {
		visitedWhileClustering = new HashMap<String, String>();
		List<List<String>> clusterList = new LinkedList<List<String>>();
		for(Node n : graph.getGraph().getNodeSet()){
			String id = n.getId();
			if(visitedWhileClustering.get(id) == null){
				clusterList.add(getCluster(graph, id));
			}
		}
		//print Clusters
		int number = 1;
		System.out.println();
		System.out.println("Clusters");
		for(List<String> lll : clusterList){
			System.out.println("|||| Cluster#:"+number++);
			for(String st : lll){
				System.out.println(st);
			}
			System.out.println();
			System.out.println();
		}
		
		
		//get size of biggest cluster;
		int biggestSize = 0;
		for(List<String> list : clusterList){
			if(list.size() > biggestSize)
				biggestSize = list.size();
		}
		Map<String,String> survivingNodeIds = new HashMap<String, String>();
		for(List<String> list : clusterList){
			if(list.size() == biggestSize){
				for(String idL : list)
					survivingNodeIds.put(idL, idL);
			}
		}

		//generate deleting queue
		Queue<String> deleteQueue = new LinkedList<String>();

		for(Node node : graph.getGraph().getNodeSet()){
			if(survivingNodeIds.get(node.getId())==null){
				deleteQueue.add(node.getId());
			}
		}
		
		//remove dead Nodes
		while(!deleteQueue.isEmpty()){
			graph.getGraph().removeNode(deleteQueue.poll());
		}
	}

	private List<String> getCluster(WTPGraph graph, String id) {
		Queue<String> q = new LinkedList<String>();
		List<String> list = new LinkedList<String>();
		q.add(id);
		while(!q.isEmpty()){
			String temp = q.poll();
			if(visitedWhileClustering.get(temp)==null){
				visitedWhileClustering.put(temp,temp);
				list.add(temp);
				Iterator<Node> neighbourNodesIterator = graph.getGraph().getNode(temp).getNeighborNodeIterator();
				while(neighbourNodesIterator.hasNext()){
					q.add(neighbourNodesIterator.next().getId());
				}
			}
		}
		return list;
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
