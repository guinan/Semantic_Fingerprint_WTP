package graph;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swingViewer.View;
import org.graphstream.ui.swingViewer.Viewer;

public class WTPGraph {
	private Graph graph;
	private Map<String,String> requestNodes;
	
	protected final String styleSheet =
	        "node {" +
	        "   fill-color: black;" +
	        "}" +
	        "node.request {" +
	        "   fill-color: red;" +
	        "   text-color: red;" +
	        "}";
	
	
	public WTPGraph(String id){
		graph = new SingleGraph(id);
		graph.addAttribute("ui.quality");
		graph.addAttribute("ui.antialias");
		String path = getClass().getClassLoader().getResource(".").getPath();
		graph.addAttribute("ui.stylesheet", "url('file://"+path+"../css/style.css')");
	}
	
	public Node addNode(String nodeId){
		Node nodeTemp = graph.addNode(nodeId);
		nodeTemp.addAttribute("ui.label", nodeId);
		return nodeTemp;
	}
	
	public Node addNode(String nodeId, String nodeLabel){
		Node nodeTemp = graph.addNode(nodeId);
		nodeTemp.addAttribute("ui.label", nodeLabel);
		return nodeTemp;
	}
	
	public Node addNode(Node node){
		Node nodeTemp = graph.addNode(node.getId());
		String label = node.getAttribute("ui.label");
		if(label != null){
			nodeTemp.addAttribute("ui.label", label);
		}
		else{
			nodeTemp.addAttribute("ui.label", node.getId());
		}
		return nodeTemp;
	}
	
	public void addEdge(String edgeId,String nodeId1, String nodeId2){
		graph.addEdge(edgeId, nodeId1, nodeId1);
	}
	
	public void addEdge(String edgeId,String nodeId1, String nodeId2, String label){
		Edge edgeTemp = graph.addEdge(edgeId, nodeId1, nodeId1);
		edgeTemp.addAttribute("ui.label", label);
	}
	
	
	/**
	 * 
	 * @param nodeId1
	 * @param nodeId2
	 */
	public void addEdge(String nodeId1, String nodeId2){
		graph.addEdge(nodeId1+"-"+nodeId2, nodeId1, nodeId1);
	}
	
	
	/**
	 * 
	 * @param graphD
	 */
	public static void display(WTPGraph graphD){
		graphD.display();
	}
	
	
	/**
	 * 
	 */
	public void display(){
		Viewer viewer = graph.display(true);
		View view = viewer.getDefaultView();
		view.resizeFrame(800, 600);
		
		//view.setViewCenter(440000, 2503000, 0);
		//view.setViewPercent(0.25);
		
		//graph.display();
		
	}
	
	
	/**
	 * 
	 * @return
	 */
	public Graph getGraph(){
		return graph;
	}

	/**
	 * 
	 * @param requestNodes
	 */
	public void deleteUnrelevantEdgesDFS(List<dbpedia.BreadthFirstSearch.Node> requestNodes, int searchDepth) {
		this.requestNodes = new HashMap<String, String>();
		// put request nodes into hashmap
		for(dbpedia.BreadthFirstSearch.Node temp : requestNodes){
			this.requestNodes.put(temp.resourceName(), temp.resourceName());
		}
		// and start cleaning the graph
		for(dbpedia.BreadthFirstSearch.Node temp : requestNodes){
			dfs(graph.getNode(temp.resourceName()), new LinkedList<Node>());
		}
	}
	
	/**
	 * Tidys the graph using BreadthFirstSearch
	 * @param requestNodes
	 * @param visited nodes that have already been analysed (saves weather the node is a connector or not)
	 */
	public void tidyFast(List<dbpedia.BreadthFirstSearch.Node> requestedNodes, int maxDepth) {
		final boolean onlyShortestPaths = true;
		
		HashSet<Node> requestNodes = new HashSet<Node>();
		// put request nodes into hashmap
		for(dbpedia.BreadthFirstSearch.Node temp : requestedNodes) {
			requestNodes.add(graph.getNode(temp.resourceName()));
		}
		
		// 1) init vars
		int numRequestNodes = requestNodes.size();
		BFSMemory bfsMem = new BFSMemory(numRequestNodes, maxDepth);
		
		// 2) fill the level 0 lists
		System.out.println("\n--- Start Nodes");
		int k = 0;
		for(Node n : requestNodes) {
			bfsMem.getList(k, 0).add(n);
			bfsMem.seenNodes.put(n, true);
			k++;
			System.out.println("(" + (k-1) + ") " + n);
		}
		System.out.println("--- Linking Nodes:");
		
		// 3) fill the other lists and serach for connector nodes
		for (int level = 0; level < maxDepth; level++) {
			for (int idxNode = 0; idxNode < numRequestNodes; idxNode++) {
				// get all adjacent node for each node in this list
				HashSet<Node> levelNodes = bfsMem.getList(idxNode, level);
				HashSet<Node> nextLevelNodes = bfsMem.getList(idxNode, level+1);
				
				for(Node n : levelNodes) {
					if (onlyShortestPaths && level != 0 && bfsMem.seenNodes.get(n)) continue;
					
					Iterator<Node> neighborNodes = n.getNeighborNodeIterator();
					while(neighborNodes.hasNext()) {
						Node neighbour = neighborNodes.next();
						// 3.1) check if we are going backwards to were we came from
						if (bfsMem.seenFromNode(idxNode, neighbour)) continue; // make this constraint weaker to get longer paths
						//if (bfsMem.visitedFromNodeAtLevel(idxNode, neighbour, level-1)) continue;
						
						
						// 3.2) add it to this list
						if (!bfsMem.seenNodes.containsKey(neighbour)) {
							bfsMem.seenNodes.put(neighbour, false);
						}
						nextLevelNodes.add(neighbour);
						
						// 3.3) check if the neighbor is already in any other list of the other start nodes (than its a linking node)
						boolean hasLinked = false;
						for(int otherIdx = 0; otherIdx < numRequestNodes; otherIdx++) {
							if (otherIdx == idxNode) continue;
							boolean hasFoundLink = bfsMem.checkAndMark(otherIdx, neighbour);
							if (hasFoundLink) hasLinked = true;
						}
						
						// 3.4) if so mark it as linking node (and all other nodes from this node to the start as well)
						if (hasLinked) {
							bfsMem.markBackwarts(idxNode, level+1, neighbour);
						}
					}
					
				}
			}
		}
		
		// 4) delete all nodes that have not been marked as linking nodes
		List<Node> nToDelete = new LinkedList<Node>();
		for(Node n : graph) {
			if (!bfsMem.seenNodes.containsKey(n) || !bfsMem.seenNodes.get(n)) { // bfsMem.visitedNodes.containsKey(n) because: when we delete edges small unconnected groups could remain
				nToDelete.add(n);
			}
		}
		for(Node n : nToDelete) {
			graph.removeNode(n);
		}
		
	}
	
	/**
	 * Implements a Matrix
	 *
	 */
	private class BFSMemory {
		public final HashMap<Node, Boolean> seenNodes = new HashMap<Node, Boolean>(); // saves visited nodes and whether the node is a linking node
		public final ArrayList<HashSet<Node>> visited; // saves the nodes for each level of the bfs from each startnode
		public final int numRequestNodes;
		public final int numListsEachNode;
		public final int maxDepth;
		
		public BFSMemory(int numRequestNodes, int maxDepth) {
			this.maxDepth = maxDepth;
			this.numListsEachNode = maxDepth + 1;
			this.numRequestNodes = numRequestNodes;
			int numLists = numRequestNodes * numListsEachNode;
			visited = new ArrayList<HashSet<Node>>(numLists);
			// init empty
			for(int i = 0; i < numLists; i++) {
				visited.add(new HashSet<Node>());
			}
		}
		
		/**
		 * Returns true if the bfs already found this node (starting at the specified node)
		 * @param idx
		 * @param node
		 * @return
		 */
		public boolean seenFromNode(int idx, Node node) {
			for(int level = maxDepth; level >= 0; level--) {
				HashSet<Node> nodesForLevel = getList(idx, level);
				if (nodesForLevel.contains(node)) {
					return true;
				}
			}
			return false;
		}
		
		/**
		 * Returns true if the node was already reached from the given idx start node on the given level
		 * @param idx
		 * @param node
		 * @param level
		 * @return
		 */
		public boolean visitedFromNodeAtLevel(int idx, Node node, int level) {
			if (level < 0) return false;
			else return getList(idx, level).contains(node);
		}
		
		/**
		 * Mark
		 * @param idx
		 * @param node
		 */
		public void markBackwarts(int idx, int level, Node node) {
			seenNodes.put(node, true);
			System.out.println("Marked '" + node + " as linking node (level: "+ level +", index: "+idx+ ").");
			// mark all adjacent nodes
			if (level > 1) {
				HashSet<Node> nodesForNextLevel = getList(idx, level-1);
				
				Iterator<Node> neighborNodes = node.getNeighborNodeIterator();
				while(neighborNodes.hasNext()) {
					Node neighbour = neighborNodes.next();
					if (nodesForNextLevel.contains(neighbour)) {
						markBackwarts(idx, level-1, neighbour);
					}
				}
			}
			
		}

		/**
		 * check if a node is conatined in the set of already reached nodes
		 * @param idx
		 * @param node
		 * @return
		 */
		public boolean checkAndMark(int idx, Node node) {
			for(int level = maxDepth; level >= 1; level--) { // dont check the level 0
				HashSet<Node> nodesForLevel = getList(idx, level);
				if (nodesForLevel.contains(node)) {
					markBackwarts(idx, level, node);
					return true;
				}
			}
			return false;
		}

		/**
		 * 
		 * @param idx
		 * @param level
		 * @return
		 */
		public HashSet<Node> getList(int idx, int level) {
			return visited.get(idx * numListsEachNode + level);
		}
	}
	
	
	/**
	 * 
	 * @param actual
	 * @param previous
	 * @return
	 */
	private boolean dfs(Node actual, LinkedList<Node> previous){
		List<Edge> eToDelete = new LinkedList<Edge>();
		List<Node> nToDelete = new LinkedList<Node>();
		// reached one of the request nodes
		if(isRequestNode(actual) && !previous.isEmpty())
			return true;
		// check if the node connects two or more requested nodes
		boolean isConnector = false;
		for (Edge edgeTemp : actual.getEachEdge()){
			if(edgeTemp == null) continue;
			// get adjacent node
			Node adjacentNode;	
			if(edgeTemp.getNode0() == actual)
				adjacentNode = edgeTemp.getNode1();
			else
				adjacentNode = edgeTemp.getNode0();
			// never go backwards
			if(previous.contains(adjacentNode)) continue;
			// try to find a way from this adjacent node to a request node
			previous.addLast(actual);
			if (!dfs(adjacentNode, previous)){
				eToDelete.add(edgeTemp);
				nToDelete.add(adjacentNode);
			} else {
				isConnector = true;
			}
			previous.removeLast();
		}
		// delete from the graph
		for(Edge e : eToDelete)
			graph.removeEdge(e);
		for(Node n : nToDelete)
			graph.removeNode(n);
		return isConnector;
	}
	
	
	/**
	 * 
	 * @param node
	 * @return
	 */
	private boolean isRequestNode(Node node){
		if(requestNodes.get(node.getId()) == null)
			return false;
		return true;
	}
	
	
	/**
	 * 
	 * @return
	 */
	public HashMap<String, Integer> getEdgeOccurenceMap() {
		HashMap<String, Integer> stats = new HashMap<String, Integer>();
		Iterator<Edge> eIt = graph.getEdgeIterator();
		while(eIt.hasNext()) {
			Edge e = eIt.next();
			String key = e.getAttribute("ui.label");
			Integer num = stats.get(key);
			if (num == null) {
				num = new Integer(1);
			} else {
				num++;
			}
			stats.put(key, num);
		}
		return stats;
	}
	
	/**
	 * 
	 * @param name
	 */
	public void removeEdgesByName(String name) {
		LinkedList<Edge> eToDelete = new LinkedList<Edge>();
		Iterator<Edge> eIt = graph.getEdgeIterator();
		while(eIt.hasNext()) {
			Edge e = eIt.next();
			String key = e.getAttribute("ui.label");
			if (key.equals(name)) {
				eToDelete.add(e);
			}
		}
		// remove edges
		for(Edge e : eToDelete) {
			Node src = e.getSourceNode();
			Node dest = e.getTargetNode();
			graph.removeEdge(e);
			// remove nodes without adjacent nodes
			if (src.getDegree() == 0) {
				graph.removeNode(src);
			}
			if (dest.getDegree() == 0) {
				graph.removeNode(dest);
			}
		}
		
		
	}

	public void colorizeDFS(List<dbpedia.BreadthFirstSearch.Node> requestNodes) {
		this.requestNodes = new HashMap<String, String>();
		// put request nodes into hashmap
		for(dbpedia.BreadthFirstSearch.Node temp : requestNodes){
			this.requestNodes.put(temp.resourceName(), temp.resourceName());
		}
		// and colorizing the graph
		// works yet just for 1 start Node
		//for(dbpedia.BreadthFirstSearch.Node temp : requestNodes){
		//	dfs(graph.getNode(temp.resourceName()), new LinkedList<Node>());
		//}
		dfs2(graph.getNode(requestNodes.get(0).resourceName()), new HashMap<String,String>(),0);
	}

	private int dfs2(Node actual, HashMap<String, String> previous, int step) {

		// reached one of the request nodes
		if(isRequestNode(actual) && !previous.isEmpty())
			// return the distance
			return step;
		// setting shortest Distance to "infinity"
		int shortestDist = 100;
		
		for (Edge edgeTemp : actual.getEachEdge()){
			if(edgeTemp == null) continue;
			// get adjacent node
			Node adjacentNode;	
			if(edgeTemp.getNode0() == actual)
				adjacentNode = edgeTemp.getNode1();
			else
				adjacentNode = edgeTemp.getNode0();
			// never go backwards
			if(previous.get(adjacentNode.getId())!=null) continue;
			previous.put(actual.getId(),actual.getId());
			// try to find a way from this adjacent node to a request node
			int dist = dfs2(adjacentNode,previous,step+1);
			previous.remove(actual.getId());
			// check if already colored
			String prevC = edgeTemp.getAttribute("col");
			if(prevC != null){
				// check which path was shorter and adjust color
				if(Integer.parseInt(prevC) > dist){
					edgeTemp.setAttribute("col", ""+dist);
					edgeTemp.setAttribute("ui.class", getColor(dist));
				}
			}
			// initial coloring for this edge
			else{
				edgeTemp.addAttribute("col", ""+dist);
				edgeTemp.addAttribute("ui.class", getColor(dist));
			}
			
			// return shortest distance from this node to the requested node
			if(dist < shortestDist){
				shortestDist = dist;
			}
			
		}
		
		return shortestDist;
	}
	
	
	private String getColor(int color){
		 switch(color){ 
	        case 0: 
	            return "standard";
	        case 1: 
	            return "one";
	        case 2: 
	        	return "two";
	        case 3: 
	        	return "three";
	        case 4: 
	        	return "four";
	        case 5: 
	        	return "five";
	        default: 
	            return "standard";
	        } 
	}
}
