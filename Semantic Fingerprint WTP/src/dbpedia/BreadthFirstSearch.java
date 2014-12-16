package dbpedia;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import utils.FileCache;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.RDFNode;

import connector.DBPediaEndpoint;
import connector.SparqlQueryExecuter;

/**
 * Performs a BreadthFirstSearch on DBPedia concepts.
 * Notice: Stores request data to application folder
 * @author Christian Nywelt
 *
 */
public class BreadthFirstSearch {
	
	// To get all resources
	private static final String allPrefix = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
			+"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
			+"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
			+"PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
			+"PREFIX : <http://dbpedia.org/resource/>\n"
			+"PREFIX dbpedia2: <http://dbpedia.org/property/>\n"
			+"PREFIX dbpedia: <http://dbpedia.org/>\n"
			+"PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
			;
	
	private final SparqlQueryExecuter queryExecuter = new DBPediaEndpoint();
	
	public boolean useCaching = true;
	
	/**
	 * 
	 * @author Chris
	 *
	 */
	public static class Node implements Comparable<Node>, Serializable {
		String resourceURI;
		byte depth;
		
		public Node(String resourceURI) {
			this.resourceURI = resourceURI;
			depth = 0;
		}
		
		public Node(String resourceURI, byte depth) {
			this.resourceURI = resourceURI;
			this.depth = depth;
		}
		
		@Override
		public int compareTo(Node o) {
			return resourceURI.compareTo(o.resourceURI);
		}
		
		@Override
	    public int hashCode() {
	        return resourceURI.hashCode();
	    }
		
		@Override
		public String toString() {
			return resourceURI;
		}

		/**
		 * Returns the name of the entity (not the resource string)
		 * @return
		 */
		public String resourceName() {
			final int idx = "http://dbpedia.org/resource/".length();
			return resourceURI.substring(idx).replace("_", " ");
		}
	}
	
	/**
	 * 
	 * @author Chris
	 *
	 */
	public static class Edge implements Serializable {
		public final Node source;
		public final Node dest;
		public final String connectionName;
		
		public Edge(Node source, String connection, Node dest) {
			this.source = source;
			this.connectionName = connection;
			this.dest = dest;
		}
		
		@Override
		public String toString() {
			return source + "	" + connectionName + "	" + dest;
		}

		public Object getName() {
			final int idx = "http://dbpedia.org/property/".length();
			return connectionName.substring(idx).replace("_", " ");
		}
		
		public String getID() {
			if (idPool >= Integer.MAX_VALUE) idPool = 0;
			return "" + getName() + (idPool++) + hashCode();
		}
		
		public static int idPool = 0;
	}
	
	/**
	 * 
	 * @author Chris
	 *
	 */
	public static class ResultSet implements Serializable {
		public final List<Node> nodes;
		public final List<Edge> edges;
		public final List<Node> requestNodes;
		public final byte requestDepth;
		
		protected ResultSet(List<Node> nodes, List<Edge> edges, List<Node> requestNodes, byte requestDepth) {
			this.nodes = nodes;
			this.edges = edges;
			this.requestNodes = requestNodes;
			this.requestDepth = requestDepth;
		}

		public void printTo(PrintStream out) {
			// order edges by source node level
			Collections.sort(edges, new Comparator<Edge>() {
				@Override
				public int compare(Edge o1, Edge o2) {
					byte b1 = o1.source.depth;
					byte b2 = o2.source.depth;
					if (b1 == b2)
						return 0;
					else if (b1 < b2)
						return -1;
					else
						return 1;
				}
				
			});
			// print them
			for(Edge e : edges) {
				out.println("(" + e.source.depth + ") " + e + " (" + e.dest.depth + ") ");
			}
		}
	}
	
	// working data
	private HashMap<String, Node> seenNodes = new HashMap<String, Node>();
	private LinkedList<Node> unseenNodes = new LinkedList<Node>();
	private LinkedList<Edge> edges = new LinkedList<Edge>();
	
	// statistics
	private int readFromCache = 0;
	private int requestedOnline = 0;
	
	// cache
	protected final FileCache<AnalysedNode> cache = new FileCache<AnalysedNode>("DBPedia.BreadthFirstSearch");
	
	
	/**
	 * Gets all concepts/links that interconnect the given concepts
	 * Notice: the output gets cached to disk
	 * @param resourceURIs
	 * @return
	 */
	public ResultSet getConnections(List<String> resourceURIs, int maxDepth) {
		return getConnections(resourceURIs, (byte) maxDepth);
	}
	
	/**
	 * Gets all concepts/links that interconnect the given concepts
	 * Notice: the output gets cached to disk
	 * @param resourceURIs
	 * @return
	 */
	public ResultSet getConnections(List<String> resourceURIs) {
		return getConnections(resourceURIs, (byte) 2);
	}
	
	/**
	 * Gets all concepts/links that interconnect the given concepts
	 * Notice: the output gets cached to disk 
	 * @param resourceURIs
	 * @param maxDepth
	 * @return
	 */
	public ResultSet getConnections(List<String> resourceURIs, byte maxDepth) {
		long startTime = System.currentTimeMillis();
		
		// add start nodes
		for(String res : resourceURIs) {
			unseenNodes.add(new Node(res));
		}
		// save start nodes
		ArrayList<Node> requestNodes = new ArrayList<Node>(unseenNodes);
		
		// start searching every node for adjacent nodes
		while(!unseenNodes.isEmpty()) {
			Node n = unseenNodes.pop();
			if (n.depth < maxDepth) {
				if (!seenNodes.containsKey(n.resourceURI)) {
					seenNodes.put(n.resourceURI, n);
					analyseNode(n);
				}
			} else if(!seenNodes.containsKey(n.resourceURI)){
				seenNodes.put(n.resourceURI, n);
			}
		}

		// create output (and add all unseennodes to seen nodes)
		for(Node n : unseenNodes) {
			seenNodes.put(n.resourceURI, n);
		}
		ArrayList<Node> finalNodes = new ArrayList<Node>(seenNodes.values());
		ResultSet res = new ResultSet(finalNodes, edges, requestNodes, maxDepth);
		
		// output statistics
		double pastTime = System.currentTimeMillis() - startTime;
		double timeSpent = pastTime / 1000.D;
		timeSpent = Math.round(timeSpent*100.0)/100.0;
		System.out.println("BFS answered request in " + timeSpent + " seconds. Cache: " + readFromCache + "; Online: " + requestedOnline + " (Total: " + (readFromCache+requestedOnline) + ")" );
		
		// tidy
		seenNodes = new HashMap<String, Node>();
		unseenNodes = new LinkedList<Node>();
		edges = new LinkedList<Edge>();
		readFromCache = 0;
		requestedOnline = 0;
		return res;
	}

	/**
	 * Creates the query string to search for all associated concepts
	 * @param resourceURI
	 * @return
	 */
	protected String createQueryString(String resourceURI) {
		StringBuilder sb = new StringBuilder(allPrefix);
		sb.append("SELECT ?con ?res\n");
		sb.append("WHERE {\n<");
		sb.append(resourceURI);
		sb.append("> ?con ?res .\n");
		sb.append("FILTER(STRSTARTS(STR(?res), \"http://dbpedia.org/resource\")) .\n");
		sb.append("} LIMIT 500");

		return sb.toString();
	}
	
	/**
	 * 
	 * @author Chris
	 *
	 */
	protected static class AnalysedNode implements Serializable {
		public final ArrayList<String> edges;
		public final ArrayList<String> targetResourceNames;
		
		public AnalysedNode() {
			edges = new ArrayList<String>();
			targetResourceNames = new ArrayList<String>();
		}
	}
	
	/**
	 * Get all adjacent nodes of the given node
	 * @param resourceURI
	 * @return
	 */
	protected void analyseNode(Node resource) {		
		// check cache
		AnalysedNode cachedNode = useCaching ? cache.get(resource.resourceURI) : null;
		if (cachedNode != null) {
			for(int i = 0; i < cachedNode.edges.size(); i++) {
				String resName = cachedNode.targetResourceNames.get(i);
				Node dest = seenNodes.get(resName);
				if (dest == null) {
					dest = new Node(resName, (byte) (resource.depth + 1));
					unseenNodes.addLast(dest);
				}
				Edge edge = new Edge(resource, cachedNode.edges.get(i), dest);
				edges.add(edge);
			}
			//System.out.println("Loaded data from cache.");
			readFromCache++;
		} else { // load from DBPedia online
			requestedOnline++;
			System.out.println("Analysing node: " + resource.resourceURI);
			//System.out.print("Loading data from DBPedia...");
			cachedNode = new AnalysedNode();
			
			// request with sparql
			String query = createQueryString(resource.resourceURI);
			//System.out.println(query);
			List<QuerySolution> resultSet = queryExecuter.executeQuery(query);
			
			// search for missing uri's (don't know why but some resources are named :_concept instead of <http://dbpedia.org/resource/concept>
			for(QuerySolution sol : resultSet) {
				// get columns
				RDFNode con = sol.get("?con");
				RDFNode res = sol.get("?res");
				
				// convert res to valid resource string
				String destURI = res.toString();
				if (destURI.startsWith(":")) {
					destURI = "<http://dbpedia.org/resource/" + destURI.substring(1) + ">";
				}
				
				// add dest node
				cachedNode.targetResourceNames.add(destURI);
				Node dest = seenNodes.get(destURI);
				if (dest == null) {
					dest = new Node(destURI, (byte) (resource.depth + 1));
					unseenNodes.addLast(dest); // could lead to double insertion in unseenNodes but we handle that somewhereelse for performance benefits
				}
				
				// add edge
				String conStr = con.toString();
				Edge edge = new Edge(resource, conStr, dest);
				edges.add(edge);
				cachedNode.edges.add(conStr);
			}
			//System.out.println("Done");
			cache.put(resource.resourceURI, cachedNode);
		}
	}
}
