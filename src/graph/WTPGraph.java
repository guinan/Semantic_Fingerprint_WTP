package graph;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
		//edgeTemp.addAttribute("ui.label", label);
	}
	
	public void addEdge(String nodeId1, String nodeId2){
		graph.addEdge(nodeId1+"-"+nodeId2, nodeId1, nodeId1);
	}
	
	public static void display(WTPGraph graphD){
		graphD.display();
	}
	
	public void display(){
		Viewer viewer = graph.display(true);
		View view = viewer.getDefaultView();
		view.resizeFrame(800, 600);
		
		
		
		
		//view.setViewCenter(440000, 2503000, 0);
		//view.setViewPercent(0.25);
		
		//graph.display();
		
	}
	
	public Graph getGraph(){
		return graph;
	}

	public void deleteUnrelevantEdgesDFS(List<dbpedia.BreadthFirstSearch.Node> requestNodes) {
		this.requestNodes = new HashMap<String, String>();
		for(dbpedia.BreadthFirstSearch.Node temp : requestNodes){
			this.requestNodes.put(temp.resourceName(), temp.resourceName());
		}
		for(dbpedia.BreadthFirstSearch.Node temp : requestNodes){
			dfs(graph.getNode(temp.resourceName()), null);
		}
	}
	private boolean dfs(Node actual, Node previous){
		List<Edge> eToDelete = new LinkedList<Edge>();
		List<Node> nToDelete = new LinkedList<Node>();
		if(isRequestNode(actual) && previous != null)
			return true;
		boolean isConnector = false;
		for (Edge edgeTemp : actual.getEachEdge()){
			if(edgeTemp == null)
				continue;
			Node adjacentNode;	
			if(edgeTemp.getNode0() == actual)
				adjacentNode = edgeTemp.getNode1();
			else
				adjacentNode = edgeTemp.getNode0();
			if(adjacentNode == previous)
				continue;
			if (!dfs(adjacentNode, actual)){
				eToDelete.add(edgeTemp);
				nToDelete.add(adjacentNode);
				
			}
			else
				isConnector = true;
		}
		for(Edge e : eToDelete)
			graph.removeEdge(e);
		for(Node n : nToDelete)
			graph.removeNode(n);
		return isConnector;
	}
	
	private boolean isRequestNode(Node node){
		if(requestNodes.get(node.getId()) == null)
			return false;
		return true;
	}
	
	
	
}
