package graph;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.graphstream.algorithm.APSP;
import org.graphstream.algorithm.APSP.APSPInfo;
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
		ArrayList<Node> requestNodess = new ArrayList<Node>();
		// put request nodes into hashmap
		for(dbpedia.BreadthFirstSearch.Node temp : requestNodes){
			this.requestNodes.put(temp.resourceName(), temp.resourceName());
			requestNodess.add(graph.getNode(temp.resourceName()));
		}
		// and start cleaning the graph
		
		for(dbpedia.BreadthFirstSearch.Node temp : requestNodes){
			dfs(graph.getNode(temp.resourceName()), new LinkedList<Node>());
		}
		
		//tidyGraph(requestNodess, searchDepth);
	}
	
	/**
	 * Does not work!
	 * @param requestNodes
	 * @param maxSearchDepth
	 */
	private void tidyGraph(ArrayList<Node> requestNodes, int maxSearchDepth) {
		APSP apsp = new APSP();
        apsp.init(graph); // registering apsp as a sink for the graph
        apsp.setDirected(false);
        apsp.compute();
        
        // start deleting
        List<Edge> eToDelete = new LinkedList<Edge>();
		List<Node> nToDelete = new LinkedList<Node>();
        for(Node n : graph) {
        	if (requestNodes.contains(n)) continue;
        	
        	// delete all nodes that do not reach at least to request nodes
        	APSPInfo info = n.getAttribute(APSPInfo.ATTRIBUTE_NAME);
        	int numHits = 0;
        	for(Node target : requestNodes) {
        		//info.getShortestPathTo(target.getId());
        		double len = info.getLengthTo(target.getId());
        		if (len <= maxSearchDepth) {
        			numHits++;
        			if (numHits >= 2) {
        				break;
        			}
        		}
        	}
        	if (numHits < 2) {
        		nToDelete.add(n);
        		// delete all edges
        	}
            
        }
        // delete what should be deleted
        /*for(Edge e : eToDelete)
			graph.removeEdge(e); */
		for(Node n : nToDelete)
			graph.removeNode(n);
	}
	
	/**
	 * 
	 * @param requestNodes
	 * @param visited nodes that have already been analysed (saves weather the node is a connector or not)
	 */
	
	private void bfs(HashSet<Node> requestNodes, int maxDepth) {
		// NOT IMPLEMENTED YET
		/*
		HashMap<Node, Integer> visitedNodes = new HashMap<Node, Integer>(); // saves which node was
		// init vars
		int numRequestNodes = requestNodes.size();
		int numListsEachNode = (maxDepth + 1);
		ArrayList<HashSet<Node>> visited = new ArrayList<HashSet<Node>>(numRequestNodes * numListsEachNode);
		int numLists = visited.size();
		for(int i = 0; i < numLists; i++) {
			visited.add(new HashSet<Node>());
		}
		
		// ------------ fill the level 0 lists
		int k = 0;
		for(Node n : requestNodes) {
			visited.get(k * numListsEachNode).add(n);
			k++;
		}
		
		// fill the other lists and serach for connector nodes
		for (int level = 0; level < numListsEachNode; level++) {
			for (int idxNode = 0; idxNode < numRequestNodes; idxNode++) {
				// get all adjacent node for each node in this list
				
			}
		}
		*/
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
	
	
	
}
