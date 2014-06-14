package graph;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.SingleGraph;

public class WTPGraph {
	private Graph graph;
	
	public WTPGraph(String id){
		graph = new SingleGraph(id);
		
	}
	
	public void addNode(String nodeId){
		Node nodeTemp = graph.addNode(nodeId);
		nodeTemp.addAttribute("ui.label", nodeId);
	}
	
	public void addNode(String nodeId, String nodeLabel){
		Node nodeTemp = graph.addNode(nodeId);
		nodeTemp.addAttribute("ui.label", nodeLabel);
	}
	
	public void addNode(Node node){
		Node nodeTemp = graph.addNode(node.getId());
		String label = node.getAttribute("ui.label");
		if(label != null){
			nodeTemp.addAttribute("ui.label", label);
		}
		else{
			nodeTemp.addAttribute("ui.label", node.getId());
		}
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
		graph.display();
	}
	
	public Graph getGraph(){
		return graph;
	}
	
	
	
}
