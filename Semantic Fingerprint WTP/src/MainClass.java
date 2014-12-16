import filterheuristics.InterConceptConntecting;
import graph.GraphCleaner;
import graph.GraphCleaner.ExtendedPath;
import graph.GraphCleaner.ImplicitPath;
import graph.GraphCleaner.Path;
import graph.WTPGraph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.graphstream.graph.Graph;

import utils.OccurenceCounter;
import utils.PowerSetGenerator;
import utils.PowerSetGenerator.PowerSetIterator;
import dbpedia.BreadthFirstSearch;
import dbpedia.BreadthFirstSearch.ResultSet;
import dbpedia.KeyWordSearch;
import dbpedia.KeyWordSearch.SearchResult;


public class MainClass {
	// Request to DBPedia
	public static final int maxSearchResults = 10;
	public static final int maxSearchDepth = 3;
	// Initial cleaning of the graph
	public static final int maxPathLength = maxSearchDepth;
	public static final int maxPathExtensionLength = 1;
	// Heuristics
	public static final int numRelevantNodesFilter = 10;
	public static final int minSupportNodesFilter = 5;
	// keyWords
	public static final ArrayList<LinkedList<String>> keywordList = new ArrayList<LinkedList<String>>();
	
	static {
		// keywords 0
		addKeywords(new String[] {
				"Haskell",
				"induction",
				"foldr",
				"fold",
				"higher order function",
				"prove"
			});
		
		// keywords 1
		addKeywords(new String[] {
				"Götz_Alsmann",
				"University_of_Münster",
				"Jazz",
				"Brandenburger-Tor",
				"Hauptstadt"
			});
	
		// keywords 2
		addKeywords(new String[] {
				"NP complete",
				"NP",
				"computer science",
				"theory"
		});
		
		// keywords 3
		addKeywords(new String[] {
				"Haskell",
				"span",
				"takewhile",
				"dropwhile",
				"proof",
				"induction",
				"program",
				"properties"
		});
		
		// keywords 4
		addKeywords(new String[] {
				"scheduling",
				"simulated annealing",
				"tabu search",
				"optimization",
				"jobshop scheduling",
				"heuristics",
				"local search",
				"hybrid algorithms"
		});
	}
	
	
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// a) Search for concepts
//		LinkedList<String> keywords = keywordList.get(0);
//		WTPGraph g = processKeyWords(keywords);
//		System.out.println(keywords);
//		g.display();
//		System.out.println("Done");
		
		// a) testing
		doTests();
	}
	
	protected static void doTests() {
		LinkedList<String> keywords = keywordList.get(0);
		// iterate power set
		int maxNumKWs = Math.min(6, keywords.size());
		int minNumKWs = Math.min(3, keywords.size());
		
		// run all combinations
		for (int i = maxNumKWs; i > minNumKWs; i--) {
			String[] arr = keywords.toArray(new String[keywords.size()]);
			PowerSetGenerator<String> psg = new PowerSetGenerator<String>(i, arr);
			// iterate all powersets with k = i
			for(String[] l : psg) {
				WTPGraph g = processKeyWords(new LinkedList<String>(Arrays.asList(l)));
				
				// save to svg image
				final String outputFolder = "C:\\Users\\Chris\\Desktop\\test\\";
				final String path = outputFolder + "Search Depth = " + maxSearchDepth +"\\"+ keywords.size() + " keywords\\";
				String file = path + Arrays.toString(l) + ".png";
				System.out.println("Saving graph to file \"" + file + "\"");
				g.displaySaveClose(file);
//				try {
//					g.saveToSVG(file);
//				} catch (IOException e) {
//					e.printStackTrace();
//					return;
//				}
				
			}
		}
		System.out.println("Finished test run.");
		System.exit(0);
	}
	
	/**
	 * 
	 * @param keywords
	 * @return 
	 */
	protected static WTPGraph processKeyWords(LinkedList<String> keywords) {
		// map for the semantic concepts found in the ontology and their corresponding keyword, used for searching them
		Map<String, String> correspondingKeywords = new HashMap<String, String>();
		
		KeyWordSearch s = new KeyWordSearch();
		List<SearchResult> res = s.search(keywords, maxSearchResults, correspondingKeywords); 
 		System.out.println(res);
		List<String> request = KeyWordSearch.toUriList(res);
		
		// b) Create the Graph
		return generateGraph(request, maxSearchDepth, correspondingKeywords);
	}
	
	/**
	 * 
	 * @param request
	 * @throws FileNotFoundException 
	 */
	public static WTPGraph generateGraph(List<String> request, int searchDepth, Map<String, String> correspondingKeywords) {
		// -- 1) get connections
		System.out.println("Starting BFS...");
		BreadthFirstSearch lc = new BreadthFirstSearch();
		ResultSet res = lc.getConnections(request, searchDepth);
		System.out.println("...Done");
		
		// -- 2) create the graph
		System.out.println("Creating the initial graph...");
		WTPGraph graph = WTPGraph.createFromResultSet(res, "Semantic Fingerprint");
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
		
		// --4.2) heuristics finger print selection
		InterConceptConntecting heuristic = new InterConceptConntecting();

		/**
		 * Filters all Nodes that have paths to other Nodes which correspond to a different keyword
		 */
		heuristic.filterInterconntection(graph, paths, correspondingKeywords);
		
		/**
		 * Filters the n Nodes which occur most frequently in the paths
		 */
		//heuristic.filterNMostFrequentlyOccuring(graph, paths, numRelevantNodesFilter, correspondingKeywords);
		
		/**
		 *  Selects the cluster which corresponds to the most different keywords
		 */
		heuristic.filterClusterByInterconnectionLevel(graph, correspondingKeywords);
		
		/**
		 * Selects the biggest cluster
		 */
		heuristic.filterClusterBySize(graph);
		
		/**
		 * Selects the cluster whose nodes occur most frequently in the paths
		 */
		//heuristic.filterClusterByNodeOccurrencesInPaths(graph, paths);
		
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
		return graph;
	}
	
	/**
	 * Add keywords to the keywordList
	 * @param arr
	 */
	private static void addKeywords(String arr[]) {
		keywordList.add(new LinkedList<String>(Arrays.asList(arr)));
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
