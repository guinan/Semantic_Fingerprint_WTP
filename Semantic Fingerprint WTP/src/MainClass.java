import graph.WTPGraph;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import utils.FileCache;
import dbpedia.BreadthFirstSearch;
import dbpedia.BreadthFirstSearch.ResultSet;


public class MainClass {
	
	public static void main(String[] args) {
		LinkedList<String> request = new LinkedList<String>();
		request.add("http://dbpedia.org/resource/Haskell_(programming_language)");
		request.add("http://dbpedia.org/resource/C++");
		//request.add("http://dbpedia.org/page/ML_(programming_language)"); According to RelFinder there should be a connection!
		
		generateGraph(request, 2);
	}
	
	/**
	 * 
	 * @param request
	 * @throws FileNotFoundException 
	 */
	public static void generateGraph(LinkedList<String> request, int searchDepth) {
		// -- 1) get connections
		System.out.println("Starting BFS...");
		BreadthFirstSearch lc = new BreadthFirstSearch();
		ResultSet res = lc.getConnections(request, searchDepth);
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
		System.out.println("Creating the initial graph...");
		WTPGraph graph = new WTPGraph("Testgraph");
		// add nodes
		for (dbpedia.BreadthFirstSearch.Node n : res.nodes) {
			Node node = graph.addNode(n.resourceName());
			if (res.requestNodes.contains(n)) {
				node.setAttribute("ui.class", "request");
			}
		}
		// add edges
		boolean useDirectedEdges = false;
		for (dbpedia.BreadthFirstSearch.Edge e : res.edges) {
			try {
				Edge edge = graph.getGraph().addEdge(""+ e.hashCode(), e.source.resourceName(), e.dest.resourceName(), useDirectedEdges);
				edge.setAttribute("ui.label", e.getName());
			} catch (org.graphstream.graph.EdgeRejectedException err) {
				System.out.println("Error: " + err.getMessage());
				//System.out.println("Node: " + e.toString());
			}
		}
		/*node {
			fill-color: black;
		}*/
		
		// -- 4) remove specific edges
		graph.removeEdgesByName("ject");
		graph.removeEdgesByName("paradigm");
		graph.removeEdgesByName("influencedBy");
		graph.removeEdgesByName("influenced");
		graph.removeEdgesByName("typing");
		//graph.removeEdgesByName("license");
		
		
		// -- 5) tidy graph
		System.out.print("Tidying graph (" + graph.getGraph().getNodeCount() + " Nodes, " + graph.getGraph().getEdgeCount()+" Edges) ...");
		graph.tidyFast(res.requestNodes, res.requestDepth);
		System.out.println(" Done (" + graph.getGraph().getNodeCount() + " Nodes, " + graph.getGraph().getEdgeCount()+" Edges)");
		
		// --5.2) colorize Graph
		
		//graph.colorizeDFS(res.requestNodes);
		
		// --6) Get Stats
		System.out.println("-- Displaying edge statistics");
		HashMap<String, Integer> edgeStats = graph.getEdgeOccurenceMap();
		for(Entry<String, Integer> e : edgeStats.entrySet()) {
			System.out.println(e.getKey() + ": " + e.getValue());
		}

		// -- 7) display graph
		System.out.println("-- Displaying graph...");
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
