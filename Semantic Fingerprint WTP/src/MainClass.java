import filterheuristics.NodeRelevanceByIncludingPaths;
import graph.GraphCleaner;
import graph.GraphCleaner.ExtendedPath;
import graph.GraphCleaner.ImplicitPath;
import graph.GraphCleaner.Path;
import graph.WTPGraph;

import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

import utils.OccurenceCounter;
import dbpedia.BreadthFirstSearch;
import dbpedia.KeyWordSearch;
import dbpedia.BreadthFirstSearch.ResultSet;
import dbpedia.KeyWordSearch.SearchResult;


public class MainClass {
	// Request to DBPedia
	public static final int maxSearchResults = 10;
	public static final int maxSearchDepth = 3;
	// Initial cleaning of the graph
	public static final int maxPathLength = maxSearchDepth;
	public static final int maxPathExtensionLength = 1;
	// Heuristics
	public static final int numRelevantNodesFilter = 20;
	public static final int minSupportNodesFilter = 300;
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// a) Serach for concepts
		LinkedList<String> keywords = new LinkedList<String>();
		keywords.add("Haskell");
		keywords.add("C++");
		keywords.add("Java");
		
		KeyWordSearch s = new KeyWordSearch();
		List<SearchResult> res = s.search(keywords, maxSearchResults); 
		System.out.println(res);
		List<String> request = KeyWordSearch.toUriList(res); // TODO: use them as input for the next algorithm
		// b) Create the Graph
		/*List<String> request = new LinkedList<String>();
		request.add("http://dbpedia.org/resource/Haskell_(programming_language)");
		request.add("http://dbpedia.org/resource/C++");
		request.add("http://dbpedia.org/resource/Java_(programming_language)");*/
		//request.add("http://dbpedia.org/page/ML_(programming_language)"); //According to RelFinder there should be a connection!
		
		generateGraph(request, maxSearchDepth);
		
		
	}
	
	/**
	 * 
	 * @param request
	 * @throws FileNotFoundException 
	 */
	public static void generateGraph(List<String> request, int searchDepth) {
		// -- 1) get connections
		System.out.println("Starting BFS...");
		BreadthFirstSearch lc = new BreadthFirstSearch();
		ResultSet res = lc.getConnections(request, searchDepth);
		System.out.println("...Done");
		
		// -- 2) create the graph
		System.out.println("Creating the initial graph...");
		WTPGraph graph = WTPGraph.createFromResultSet(res, "Testgraph");
		System.out.println("...Done");
		
		// -- 3) remove specific edges
//		graph.removeEdgesByName("ject");
//		graph.removeEdgesByName("paradigm");
//		graph.removeEdgesByName("influencedBy");
//		graph.removeEdgesByName("influenced");
//		graph.removeEdgesByName("typing");
//		graph.removeEdgesByName("license");
		
		
		// -- 4) tidy graph
		System.out.print("Tidying graph (" + graph.getGraph().getNodeCount() + " Nodes, " + graph.getGraph().getEdgeCount()+" Edges) ...");
		GraphCleaner c = new GraphCleaner(graph.getGraph(), res.requestNodes);
		LinkedList<Path> paths = c.clean(maxPathLength, maxPathExtensionLength);
		System.out.println(" Done (" + graph.getGraph().getNodeCount() + " Nodes, " + graph.getGraph().getEdgeCount()+" Edges, "+ paths.size() +" Paths)");
		
		// --4.2) tidy the second
		
		NodeRelevanceByIncludingPaths heuristic = new NodeRelevanceByIncludingPaths();
		
		heuristic.filterTheNMostVisited(graph, paths, numRelevantNodesFilter);
		//heuristic.filterByNumberOfPaths(graph, paths, minSupportNodesFilter);
		
	
		
		
		// --4.3) colorize Graph
		
		//graph.colorizeDFS(res.requestNodes);
		
		
		// --6) Get Stats
		System.out.println("-- Displaying path statistics");
		int[] types = new int[3];
		OccurenceCounter<Integer> counter = new OccurenceCounter<Integer>();
		for(Path p : paths) {
			// count types
			if (p instanceof ExtendedPath) types[1]++;
			else if (p instanceof ImplicitPath) types[2]++;
			else types[0]++;
			// count length
			counter.inc(p.size()-2);
		}
		System.out.println("Kurze Pfade: " + types[0]);
		System.out.println("Erweiterte Pfade: " + types[1]);
		System.out.println("Implizite Pfade: " + types[2]);
		System.out.println("Pfadl�ngen: " + counter);
		
		System.out.println("-- Displaying edge statistics");
		OccurenceCounter<String> edgeStats = graph.getEdgeOccurences();
		System.out.println(edgeStats);

		// -- 7) display graph
		System.out.println("-- Displaying graph...");
		graph.display();
		
	}
	
	
	/**
	 * Testgraph #1
	 */
	private static void generateTestGraph() {
		WTPGraph graph = new WTPGraph("Test");
		
		String[] arr = new String[] {
				"A", "4",
				"A", "2",
				"A", "15",
				"15", "14",
				"14", "13",
				"15", "13",
				"4", "5",
				"5", "6",
				"2", "3",
				"A", "1",
				"B", "1",
				"1", "11",
				"11", "12",
				"12", "1",
				"A", "7",
				"7", "10",
				"7", "8",
				"8", "9",
				"9", "B",
				"B", "7",
				"B", "16",
				"16", "17",
				"18", "16",
				"6", "B",
				"B", "3",
				"19", "20",
				"11", "19",
				"12", "20",
				"7", "21",
				"21", "22",
				"22", "9",
				"2", "23",
				"23", "3",
		};
		Graph g = graph.getGraph();
		for(int i = 0; i < arr.length; i += 2) {
			String src = arr[i];
			String dest = arr[i+1];
			if (g.getNode(src) == null) graph.addNode(src);
			if (g.getNode(dest) == null) graph.addNode(dest);
			g.addEdge(""+ i, src, dest, false);
		}
		
		g.getNode("A").setAttribute("ui.class", "request");
		g.getNode("B").setAttribute("ui.class", "request");
		
		List<dbpedia.BreadthFirstSearch.Node> start = new LinkedList<dbpedia.BreadthFirstSearch.Node>();
		start.add(new dbpedia.BreadthFirstSearch.Node("http://dbpedia.org/resource/A"));
		start.add(new dbpedia.BreadthFirstSearch.Node("http://dbpedia.org/resource/B"));
		
		// clean the graph
		GraphCleaner c = new GraphCleaner(graph.getGraph(), start);
		LinkedList<Path> paths = c.clean(4, 2);
		
		System.out.println(paths);
		graph.display();
	}
	
	/**
	 * Testgraph #2
	 */
	private static void generateTestGraph_2() {
		WTPGraph graph = new WTPGraph("Test");
		
		String[] arr = new String[] {
				"A", "2",
				"2", "C",
				"A", "1",
				"1", "B",
				"B", "3",
				"3", "C",
				"1", "4",
				"4", "2",
				"4", "3",
				
		};
		Graph g = graph.getGraph();
		for(int i = 0; i < arr.length; i += 2) {
			String src = arr[i];
			String dest = arr[i+1];
			if (g.getNode(src) == null) graph.addNode(src);
			if (g.getNode(dest) == null) graph.addNode(dest);
			g.addEdge(""+ i, src, dest, false);
		}
		
		g.getNode("A").setAttribute("ui.class", "request");
		g.getNode("B").setAttribute("ui.class", "request");
		g.getNode("C").setAttribute("ui.class", "request");
		
		List<dbpedia.BreadthFirstSearch.Node> start = new LinkedList<dbpedia.BreadthFirstSearch.Node>();
		start.add(new dbpedia.BreadthFirstSearch.Node("http://dbpedia.org/resource/A"));
		start.add(new dbpedia.BreadthFirstSearch.Node("http://dbpedia.org/resource/B"));
		start.add(new dbpedia.BreadthFirstSearch.Node("http://dbpedia.org/resource/C"));
		
		// clean the graph
		GraphCleaner c = new GraphCleaner(graph.getGraph(), start);
		LinkedList<Path> paths = c.clean(3, 2);
		
//		System.out.println(paths);
		graph.display();
	}
}