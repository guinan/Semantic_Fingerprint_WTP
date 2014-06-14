import graph.WTPGraph;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.LinkedList;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;

import utils.FileCache;
import dbpedia.BreadthFirstSearch;
import dbpedia.BreadthFirstSearch.ResultSet;


public class MainClass {
	
	public static void main(String[] args) {
		LinkedList<String> request = new LinkedList<String>();
		request.add("http://dbpedia.org/resource/Haskell_(programming_language)");
		request.add("http://dbpedia.org/resource/C++");
		
		generateGraph(request);
	}
	
	/**
	 * 
	 * @param request
	 * @throws FileNotFoundException 
	 */
	public static void generateGraph(LinkedList<String> request) {
		// -- 1) get connections
		System.out.println("Starting BFS...");
		BreadthFirstSearch lc = new BreadthFirstSearch();
		ResultSet res = lc.getConnections(request, 1);
		System.out.println("...Done");
		
		/*
		// -- 2) write into file for debugging
		try {
			System.out.println("Writing Result to file...");
			PrintStream outputStream = new PrintStream(new FileOutputStream("BFS_Output.txt", false));
			res.printTo(outputStream);
			System.out.println("...Done");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} */
		
		// -- 3) create the graph
		WTPGraph graph = new WTPGraph("Testgraph");
		// add nodes
		for (dbpedia.BreadthFirstSearch.Node n : res.nodes) {
			Node node = graph.addNode(n.resourceName());
			if (res.requestNodes.contains(n)) {
				node.setAttribute("ui.class", "request");
			}
		}
		// add edges
		for (dbpedia.BreadthFirstSearch.Edge e : res.edges) {
			try {
				Edge edge = graph.getGraph().addEdge(""+ e.hashCode(), e.source.resourceName(), e.dest.resourceName(), true);
				edge.setAttribute("ui.label", e.getName());
			} catch (org.graphstream.graph.EdgeRejectedException err) {
				System.out.println("Error: " + err.getMessage());
				//System.out.println("Node: " + e.toString());
			}
		}

		
		// -- 4) tidy graph
		// TODO: remove all nodes and edges that don't bridge the requested nodes
		
		for(Node n : graph.getGraph()) {
//			for(Edge e : n.getEachEdge()) {
//				if (e != null)
//				graph.getGraph().removeEdge(e);
//			}
//			graph.getGraph().removeNode(n);
			
//			if (n.getEdgeSet().size() < 2) {
//				graph.getGraph().removeNode(n);
//			} else {
//				System.out.println(n.getEdgeSet().size());
//			}
//			//
		}
		

		// -- 5) display graph
		graph.display();
	}
	
	// ------------------ Cache Test
	protected static class Person implements Serializable{
		String name = "Hannes";
		int age = 25;
	}
	
	/**
	 * 
	 */
	public void cacheTest() {
		//Person p = new Person();
		FileCache<Person> cache = new FileCache<Person>("persons");
		//cache.put("h", p);
		//Person p = cache.get("h");
		//System.out.println(p.name + " = " + p.age);
	}
}
