package graph;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
