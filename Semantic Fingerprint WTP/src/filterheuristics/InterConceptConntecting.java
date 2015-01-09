package filterheuristics;

import graph.GraphCleaner.Path;
import graph.WTPGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.graphstream.graph.Node;

import utils.OccurenceCounter;

/**
 * Creates a semantic fingerprint based on a subgraph that has already been cleaned.
 * @author Jens Schneider
 *
 */
public class InterConceptConntecting {
	
	private Map<String, Integer> countOfNodeOccurrencesInPaths;
	private Set<String> visitedWhileClustering;
	
	
	public InterConceptConntecting(){
	}
		

	
	/**
	 * chooses the cluster with the highest number of semantic concepts that correspond to different search keywords
	 * @param graph the graph
	 * @param correspondingKeywords Mapping of semantic concepts (nodeIDs) to search keywords
	 */
	public void filterClusterByInterconnectionLevel(WTPGraph graph, Map<String, String> correspondingKeywords){
		Set<Set<String>> clusters = getClusters(graph);
		
		printClusters(clusters);
		Set<Set<String>> resultClusters = new HashSet<Set<String>>();
		
		//get biggest number of corresponding keywords of one cluster
		int maxNumberOfCorrespondingKeywords = 0;
		for(Set<String> cluster : clusters){
			int numOfDifferentCorrespondingKeywords = getNumberOfCorrespondingKeywords(cluster, correspondingKeywords);
			System.out.println("Number of different corresponding keywords:  "+numOfDifferentCorrespondingKeywords);
			if(numOfDifferentCorrespondingKeywords > maxNumberOfCorrespondingKeywords){
				maxNumberOfCorrespondingKeywords = numOfDifferentCorrespondingKeywords;
				resultClusters = new HashSet<Set<String>>();
				resultClusters.add(cluster);
			}
			else if(numOfDifferentCorrespondingKeywords == maxNumberOfCorrespondingKeywords){
				resultClusters.add(cluster);
			}
				
		}
		
		Set<String> survivingNodes = new HashSet<String>();
		for(Set<String> cluster : resultClusters){
			survivingNodes.addAll(cluster);
		}
		Set<Node> dyingNodes = new HashSet<Node>();
		for(Node n : graph.getGraph().getNodeSet()){
			if(!survivingNodes.contains(n.getId()))
				dyingNodes.add(n);
		}
		graph.getGraph().getNodeSet().removeAll(dyingNodes);
	}
	
	public void filterClusterBySize(WTPGraph graph) {
		Set<Set<String>> clusters = getClusters(graph);
		printClusters(clusters);
		
		Set<String> survivingNodes = new HashSet<String>();
		int maxClusterSize = 0;
		for(Set<String> cluster : clusters){
			System.out.println("Size of Cluster: "+cluster.size());
			if(cluster.size()>maxClusterSize){
				maxClusterSize = cluster.size();
				survivingNodes = new HashSet<String>();
				survivingNodes.addAll(cluster);
			}
			else if(cluster.size() == maxClusterSize){
				survivingNodes.addAll(cluster);
			}	
		}
		Set<Node> dyingNodes = new HashSet<Node>();
		for(Node n : graph.getGraph().getNodeSet()){
			if(!survivingNodes.contains(n.getId()))
				dyingNodes.add(n);
		}
		graph.getGraph().getNodeSet().removeAll(dyingNodes);

	}
	
	public void filterClusterByNodeOccurrencesInPaths(WTPGraph graph, List<Path> paths) {
		Set<Set<String>> clusters = getClusters(graph);
		printClusters(clusters);
		
		Set<String> survivingNodes = new HashSet<String>();
		int maxOccurrance = 0;
		for(Set<String> cluster : clusters){
			int count = getNumberOfClusterOccurrencesInPaths(cluster, paths);
			System.out.println("Number of cluster occurrences in paths: "+count);
			if(count > maxOccurrance){
				maxOccurrance = count;
				survivingNodes = new HashSet<String>();
				survivingNodes.addAll(cluster);
			}
			else if(count == maxOccurrance){
				survivingNodes.addAll(cluster);
			}	
		}
		Set<Node> dyingNodes = new HashSet<Node>();
		for(Node n : graph.getGraph().getNodeSet()) {
			if(!survivingNodes.contains(n.getId()))
				dyingNodes.add(n);
		}
		graph.getGraph().getNodeSet().removeAll(dyingNodes);

	}
	
	
	/**
	 * this method creates a set of all nodes that are connected by at least one path (cluster)
	 * it starts with on nodeID and uses a breadth first search
	 * @param graph the graph
	 * @param id of one node of the cluster
	 * @return a set of node ids, which are all in the same cluster
	 */
	private Set<String> getCluster(WTPGraph graph, String id) {
		Queue<String> queue = new LinkedList<String>();
		Set<String> cluster = new HashSet<String>();
		queue.add(id);
		while(!queue.isEmpty()){
			String temp = queue.poll();
			if(!visitedWhileClustering.contains(temp)){
				visitedWhileClustering.add(temp);
				cluster.add(temp);
				Iterator<Node> neighbourNodesIterator = graph.getGraph().getNode(temp).getNeighborNodeIterator();
				while(neighbourNodesIterator.hasNext()){
					queue.add(neighbourNodesIterator.next().getId());
				}
			}
		}
		return cluster;
	}
	
	private int getNumberOfCorrespondingKeywords(Set<String> cluster,
			Map<String, String> correspondingKeywords) {
		
		int count = 0;
		Set<String> alreadyContained = new HashSet<String>();
		for(String nodeID: cluster){
			String keyword = correspondingKeywords.get(nodeID);
			if(keyword != null && !alreadyContained.contains(keyword)){
				count++;
				alreadyContained.add(keyword);
			}
		}
		return count;
	}
	
	
	private int getNumberOfClusterOccurrencesInPaths(Set<String> cluster, List<Path> paths) {
		int sumOccurrences = 0;
		countOccurrencesInPaths(paths);
		for(String nodeid : cluster){
			Integer count = countOfNodeOccurrencesInPaths.get(nodeid);
			if(count != null){
				sumOccurrences += count;
			}
		}
		return sumOccurrences;
	}
	
	
	


	
	
	/**
	 * filters the paths and nodes concerning their interconnection property
	 * @param graph
	 * @param paths
	 * @param correspondingKeywords
	 */
	public void filterInterconntection(WTPGraph graph, List<Path> paths,
			Map<String, String> correspondingKeywords) {
		filterInterconnectingPaths(paths, correspondingKeywords);
		filterInterconnectedNodes(paths, graph);
	}
	
	

	/**
	 * This methods deletes all nodes of the graph that are not part of a path
	 * @param paths all paths that are interconnecting nodes, which correspond to different keywords
	 * @param graph the graph
	 */
	private void filterInterconnectedNodes(List<Path> paths, WTPGraph graph) {
		// NodeIDs of the nodes that are part of at least one of the path
		Set<String> interconnectedNodeIDs = new HashSet<String>();
		// Write all nodeIDs included in at least one of the path into the interconnectedNodeIDs Set
		for(Path p : paths){
			for(Node n : p){
				interconnectedNodeIDs.add(n.getId());
			}
		}
		System.out.println("Count of Nodes before interconnectionFiltering: "+graph.getGraph().getNodeCount() );
		
		// Nodes whose nodeID is not included in interconnectedNodeIDs and which should be deleted, because they are not interconnected
		List<Node> notInterconnectedNodes = new LinkedList<Node>();
		for(Node n : graph.getGraph().getNodeSet()){
			if(!interconnectedNodeIDs.contains(n.getId()))
				notInterconnectedNodes.add(n);
		}
		// remove all of the not interconnected Nodes
		graph.getGraph().getNodeSet().removeAll(notInterconnectedNodes);
		
		System.out.println("Count of Nodes after interconnectionFiltering:"+graph.getGraph().getNodeCount() );
		
	}

	/**
	 * This methods deletes all paths of paths that are not interconnection between two nodes, which correspond to different keywords
	 * @param paths all possible paths of the graph
	 * @param correspondingKeywords a map which delivers the corresponding search keyword for each semantic concept (nodeID)
	 */
	private static void filterInterconnectingPaths(List<Path> paths,
			Map<String, String> correspondingKeywords) {
		
		// Paths that are not interconnecting two nodes of different search keywords
		LinkedList<Path> notInterconnectingPaths = new LinkedList<Path>();
				
		// counts for each path how often semantic concepts by the same corresponding keyword occur 
		for(Path path : paths){
			
			// counts the occurrence of the keywords
			OccurenceCounter<String> occurrenceCounter = new OccurenceCounter<String>();
			//counts occurrence of the corresponding keyword for each nodes
			for(Node n : path){
				occurrenceCounter.inc(correspondingKeywords.get(n.getId()));
			}
			
			// to stop if their are at least two nodes that correspond to the same keyword on the same path
			boolean alReadyadded = false;
			
			for(Entry<String, Integer> e : occurrenceCounter.entrySet()){
				
				// if the occurrence counter is bigger than 1 --> the Path is not interconnecting
				if(e.getValue() > 1 && !alReadyadded){
					notInterconnectingPaths.add(path);
					alReadyadded = true;
				}
			}
		}
		
		System.out.println("Count of all paths: "+paths.size());
		System.out.println("Count of paths that are not interconnecting: "+notInterconnectingPaths.size());
		
		paths.removeAll(notInterconnectingPaths);
		
		System.out.println("Count of remaining paths: "+paths.size());
	}
	
	/**
	 * this method filters the n nodes most frequently included in paths 
	 * @param graph
	 * @param paths
	 * @param maxNumber
	 * @param correspondingKeywords
	 */
	public void filterNMostFrequentlyOccuring(WTPGraph graph, List<Path> paths, int maxNumber, Map<String, String> correspondingKeywords){
		
		// filters the n nodes most frequently included in paths 
		filterNByOccurencesInPaths(graph, paths, maxNumber, correspondingKeywords);
	}
	
	/**
	 * filters the n nodes most frequently included in paths 
	 * @param graph
	 * @param paths
	 * @param maxNumInclusions the max count n
	 * @param correspondingKeywords
	 */
	private void filterNByOccurencesInPaths(WTPGraph graph, List<Path> paths, int maxNumInclusions, Map<String, String> correspondingKeywords) {
		
		// counts in how many paths of paths each node is included
		countOccurrencesInPaths(paths);
		
		// filters the maxNumInclusions Nodes with the most occurrences paths
		filterNMostOccurringNodes(graph, maxNumInclusions);
	}
	
	/**
	 * this method finds out the minOccurrenc count to be one of the n best and deletes nodes with lower support
	 * @param graph the graph
	 * @param maxNumber the x most relevant semantic concepts
	 */
	private void filterNMostOccurringNodes(WTPGraph graph, int maxNumber) {
		
		// to find out the minimal occurrence count to be under the n most occurring nodes (needed for sorting)
		List<Integer> occurrencesCounts = new ArrayList<Integer>();
		
		// add each of the occurrences counters in the occurrencesCounts list
		for(String key : countOfNodeOccurrencesInPaths.keySet()){
			Integer count = countOfNodeOccurrencesInPaths.get(key);
			if(count != null)
				occurrencesCounts.add(count);	
		}
		
		// sorting
		Collections.sort(occurrencesCounts,Collections.reverseOrder());
	
		
		//int count = occurrencesCounts.size();
		
		// if something went wrong
		if(occurrencesCounts == null || occurrencesCounts.isEmpty())
			return;
		// if there are already less than the n nodes that have to be filtered
		if(occurrencesCounts.size() <= maxNumber){
			return;
		}
		else{
			Integer minOccurrences = occurrencesCounts.get(maxNumber-1);
			removeNodesWithLessThanNOccurrences(graph, minOccurrences);
		}		
	}
	
	/**
	 * 
	 * @param graph the graph
	 * @param relevanceThreshold atleast number of including paths to be relevant
	 */
	private void removeNodesWithLessThanNOccurrences(WTPGraph graph, int minNumOfOccurrences) {
		if(countOfNodeOccurrencesInPaths != null){
			LinkedList<Node> nodesToDelete = new LinkedList<Node>();
			for(Node node : graph.getGraph().getNodeSet()){
				Integer occurrenceCount = countOfNodeOccurrencesInPaths.get(node.getId());
				if(occurrenceCount == null || occurrenceCount < minNumOfOccurrences)
					nodesToDelete.add(node);
					
			}
			graph.getGraph().getNodeSet().removeAll(nodesToDelete);
			//System.out.println("nodes filtered by number of including paths heuristic");
		}		

	}
	
	/**
	 * this method counts in how many paths of paths each node is included and generates the corresponding map
	 * @param paths all remaining paths of the graph
	 */
	private void countOccurrencesInPaths(List<Path> paths) {
		countOfNodeOccurrencesInPaths = new HashMap<String,Integer>();
		
		for(Path p : paths) {
			for(Node n : p){
				Integer occurrenceCount = countOfNodeOccurrencesInPaths.get(n.getId());
				
				// if already occurred in another path
				if(occurrenceCount != null)
					countOfNodeOccurrencesInPaths.put(n.getId(), occurrenceCount+1);
				// else first occurrence in a path
				else
					countOfNodeOccurrencesInPaths.put(n.getId(), 1);
			}
			
		}		
	}
	
	private Set<Set<String>> getClusters(WTPGraph graph){
		visitedWhileClustering = new HashSet<String>();
		Set<Set<String>> clusters = new HashSet<Set<String>>();
		
		// for each node of the graph: if not yet in a cluster, try to generate the cluster starting with this node
		for(Node n : graph.getGraph().getNodeSet()){
			String id = n.getId();
			if(!visitedWhileClustering.contains(id)){
				clusters.add(getCluster(graph, id));
			}
		}
		return clusters;
	}
	
	private void printClusters(Set<Set<String>> clusters){
		System.out.println();
		System.out.println("Clusters");
		Iterator<Set<String>> clustersIterator = clusters.iterator();
		for(int i = 0; i<clusters.size(); i++){
			System.out.println("|| Cluster#:"+i+1);
			for(String nodeid : clustersIterator.next()){
				System.out.println(nodeid);
			}
			System.out.println();
			System.out.println();
		}
	}

}
