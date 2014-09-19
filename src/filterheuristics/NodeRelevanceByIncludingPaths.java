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
		generateCountingIncludingPathMap(paths);
		removeUnrelevantNodes(graph, relevanceThreshold);
	}
	
	public void filterTheNMostVisited(WTPGraph graph, List<Path> paths, int maxNumber, Map<String, String> correspondingKeywords){
		generateCountingIncludingPathMap(paths);
		findNMostRelevant(graph, maxNumber);
		//filerBiggestCluster(graph);
		filterClusterWithMostCorrespondingKeywords(graph, correspondingKeywords);
		findNMostRelevant2(graph, 10);

	}
	
	private void findNMostRelevant2(WTPGraph graph, int maxNumber) {
		List<Integer> ints = new ArrayList<Integer>();
		
		for(Node n : graph.getGraph().getNodeSet()){
			ints.add(numberOfIncludingPaths.get(n.getId()));
		}
		
		Collections.sort(ints,Collections.reverseOrder());
		int count = 0;
		for(Integer in : ints){
			count++;
			if(count > maxNumber){
				removeUnrelevantNodes2(graph, in);
				return;
			}
		}
		
	}
	
	private void removeUnrelevantNodes2(WTPGraph graph, int relevanceThreshold) {
		Queue<String> delQ = new LinkedList<String>();
		if(graph.getGraph().getNodeSet() != null){
			for(Node n : graph.getGraph().getNodeSet()){
				if(numberOfIncludingPaths.get(n.getId()) <= relevanceThreshold)
					delQ.add(n.getId());
					
			}
			while(!delQ.isEmpty())
				graph.getGraph().removeNode(delQ.poll());
			System.out.println("nodes filtered by number of including paths heuristic");
		}
	}
	
	

	/**
	 * 
	 * @param graph
	 * @param correspondingKeywords
	 */
	private void filterClusterWithMostCorrespondingKeywords(WTPGraph graph, Map<String, String> correspondingKeywords){
		visitedWhileClustering = new HashMap<String, String>();
		List<List<String>> clusterList = new LinkedList<List<String>>();
		
		// iterate over each graph node
		for(Node n : graph.getGraph().getNodeSet()){
			String id = n.getId();
			// if  not already mapped to a cluster
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
		
		
		//get biggest number of corresponding keywords of one cluster
		int maxCors = 0;
		for(List<String> list : clusterList){
			int numberOfCors = getCorsKeywords(list, correspondingKeywords);

			System.out.println("JJJJJ; "+numberOfCors);
			if(numberOfCors > maxCors)
				maxCors = numberOfCors;
		}
		
		
		
		// searches all nodes that are in a cluster whiches nodes can be mapped to maxCors keywords
		Map<String,String> survivingNodeIds = new HashMap<String, String>();
		for(List<String> list : clusterList){
			if(getCorsKeywords(list, correspondingKeywords) == maxCors){
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
	
	private int getCorsKeywords(List<String> list,
			Map<String, String> correspondingKeywords) {
		String ws = new String();
		
		int count = 0;
		Map<String, String> alreadyContained = new HashMap<String, String>();
		for(String label: list){
			String kw = correspondingKeywords.get(label);
			if(kw != null && alreadyContained.get(kw) == null){
				ws += kw;
				count++;
				alreadyContained.put(kw, kw);
			}
		}
		return count;
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

	
	/**
	 * 
	 * @param graph
	 * @param id of one node of the cluster
	 * @return a list of node ids, which are all in the same cluster
	 */
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

	/**
	 * 
	 * @param graph the graph
	 * @param maxNumber the x most relevant semantic concepts
	 */
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
	
	/**
	 * 
	 * @param graph the graph
	 * @param relevanceThreshold atleast number of including paths to be relevant
	 */
	private void removeUnrelevantNodes(WTPGraph graph, int relevanceThreshold) {
		if(numberOfIncludingPaths != null){
			for(String key : numberOfIncludingPaths.keySet()){
				if(numberOfIncludingPaths.get(key) <= relevanceThreshold)
					graph.getGraph().removeNode(key);
					
			}
			System.out.println("nodes filtered by number of including paths heuristic");
		}
	}

	private void generateCountingIncludingPathMap(List<Path> paths) {
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
