package graph;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
//import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.file.FileSink;
import org.graphstream.stream.file.FileSinkSVG2;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.graphstream.ui.swingViewer.View;
import org.graphstream.ui.swingViewer.Viewer;
import org.graphstream.ui.swingViewer.Viewer.CloseFramePolicy;

import utils.OccurenceCounter;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import dbpedia.BreadthFirstSearch.ResultSet;

/**
 * Wrapper class for a graph.
 * 
 * @author Jens Schneider
 *
 */
public class WTPGraph {
	private Graph graph;
	private Map<String, String> requestNodes;

	protected final String styleSheet = "node {" + "   fill-color: black;"
			+ "}" + "node.request {" + "   fill-color: red;"
			+ "   text-color: red;" + "}";

	public WTPGraph(String id) {
		//graph = new SingleGraph(id);
		graph = new MultiGraph(id);
		graph.addAttribute("ui.quality");
		graph.addAttribute("ui.antialias");
		String path = getClass().getClassLoader().getResource(".").getPath();
		graph.addAttribute("ui.stylesheet", "url('file://" + path
				+ "../css/style.css')");
	}

	public Node addNode(String nodeId) {
		Node nodeTemp = graph.addNode(nodeId);
		nodeTemp.addAttribute("ui.label", nodeId);
		return nodeTemp;
	}

	public Node addNode(String nodeId, String nodeLabel) {
		Node nodeTemp = graph.addNode(nodeId);
		nodeTemp.addAttribute("ui.label", nodeLabel);
		return nodeTemp;
	}

	public Node addNode(Node node) {
		Node nodeTemp = graph.addNode(node.getId());
		String label = node.getAttribute("ui.label");
		if (label != null) {
			nodeTemp.addAttribute("ui.label", label);
		} else {
			nodeTemp.addAttribute("ui.label", node.getId());
		}
		return nodeTemp;
	}

	public void addEdge(String edgeId, String nodeId1, String nodeId2) {
		graph.addEdge(edgeId, nodeId1, nodeId1);
	}

	public void addEdge(String edgeId, String nodeId1, String nodeId2,
			String label) {
		Edge edgeTemp = graph.addEdge(edgeId, nodeId1, nodeId1);
		edgeTemp.addAttribute("ui.label", label);
	}

	/**
	 * 
	 * @param nodeId1
	 * @param nodeId2
	 */
	public void addEdge(String nodeId1, String nodeId2) {
		graph.addEdge(nodeId1 + "-" + nodeId2, nodeId1, nodeId1);
	}

	/**
	 * 
	 * @param graphD
	 */
	public static void display(WTPGraph graphD) {
		graphD.display();
	}

	public void display() {
		this.display(true);
	}

	public int getNodeCount() {
		return graph.getNodeCount();
	}

	public int getEdgeCount() {
		return graph.getEdgeCount();
	}

	/**
	 * 
	 */
	public void display(boolean exitOnClose) {
		Viewer viewer = graph.display(true);
		if (!exitOnClose)
			viewer.setCloseFramePolicy(CloseFramePolicy.CLOSE_VIEWER);
		View view = viewer.getDefaultView();
		view.resizeFrame(1024, 800);

		// view.setViewCenter(440000, 2503000, 0);
		// view.setViewPercent(0.25);

		// graph.display();
	}

	/**
	 * Workaround for the not working saveToSVG method
	 */
	public void displaySaveClose(String file) {
		Viewer viewer = graph.display(true);
		viewer.setCloseFramePolicy(CloseFramePolicy.CLOSE_VIEWER);
		View view = viewer.getDefaultView();
		view.resizeFrame(1024, 800);

		try {
			Thread.sleep(5000); // wait until nodes are positioned correctly (in
								// another thread)

			BufferedImage image = new BufferedImage(view.getWidth(),
					view.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics2D = image.createGraphics();
			view.paint(graphics2D);

			File outFile = new File(file);
			outFile.mkdirs();
			ImageIO.write(image, "png", outFile);
		} catch (FileNotFoundException | NullPointerException e) {
			e.printStackTrace();
			System.out
					.println("Probably you can handle that error just by rerunning the application (with the same parameters). Don't know why but it helped me ;)");
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		// viewer.close();
	}

	/**
	 * DOES NOT WORK AS IT SHOULD. Saves the graph to an svg image file.
	 * 
	 * @param file
	 *            The absolute path to the file. For example "C:\\test.svg"
	 * @throws IOException
	 */
	public void saveToSVG(String file) throws IOException {
		SpringBox sb = new SpringBox();
		// add inital positions
		int i = 0;
		for (Node n : graph.getNodeSet()) {
			n.setAttribute("x", i);
			n.setAttribute("y", i);
		}

		// then apply the positioning algorithm
		graph.addSink(sb);
		sb.addAttributeSink(graph);

		sb.shake();
		// compute the layout
		do {
			sb.compute();
		} while (sb.getStabilization() < 0.9);

		// remove layout sink
		graph.removeSink(sb);
		sb.removeAttributeSink(graph);
		// save to file
		FileSink out = new FileSinkSVG2();
		out.writeAll(graph, file);
	}

	/**
	 * 
	 * @return
	 */
	public Graph getGraph() {
		return graph;
	}

	/**
	 * 
	 * @param requestNodes
	 */
	public void deleteUnrelevantEdgesDFS(
			List<dbpedia.BreadthFirstSearch.Node> requestNodes, int searchDepth) {
		this.requestNodes = new HashMap<String, String>();
		// put request nodes into hashmap
		for (dbpedia.BreadthFirstSearch.Node temp : requestNodes) {
			this.requestNodes.put(temp.resourceName(), temp.resourceName());
		}
		// and start cleaning the graph
		for (dbpedia.BreadthFirstSearch.Node temp : requestNodes) {
			dfs(graph.getNode(temp.resourceName()), new LinkedList<Node>());
		}
	}

	/**
	 * 
	 * @param actual
	 * @param previous
	 * @return
	 */
	private boolean dfs(Node actual, LinkedList<Node> previous) {
		List<Edge> eToDelete = new LinkedList<Edge>();
		List<Node> nToDelete = new LinkedList<Node>();
		// reached one of the request nodes
		if (isRequestNode(actual) && !previous.isEmpty())
			return true;
		// check if the node connects two or more requested nodes
		boolean isConnector = false;
		for (Edge edgeTemp : actual.getEachEdge()) {
			if (edgeTemp == null)
				continue;
			// get adjacent node
			Node adjacentNode;
			if (edgeTemp.getNode0() == actual)
				adjacentNode = edgeTemp.getNode1();
			else
				adjacentNode = edgeTemp.getNode0();
			// never go backwards
			if (previous.contains(adjacentNode))
				continue;
			// try to find a way from this adjacent node to a request node
			previous.addLast(actual);
			if (!dfs(adjacentNode, previous)) {
				eToDelete.add(edgeTemp);
				nToDelete.add(adjacentNode);
			} else {
				isConnector = true;
			}
			previous.removeLast();
		}
		// delete from the graph
		for (Edge e : eToDelete)
			graph.removeEdge(e);
		for (Node n : nToDelete)
			graph.removeNode(n);
		return isConnector;
	}

	/**
	 * 
	 * @param node
	 * @return
	 */
	private boolean isRequestNode(Node node) {
		if (requestNodes.get(node.getId()) == null)
			return false;
		return true;
	}

	/**
	 * 
	 * @return
	 */
	public OccurenceCounter<String> getEdgeOccurences() {
		OccurenceCounter<String> counter = new OccurenceCounter<String>();

		Iterator<Edge> eIt = graph.getEdgeIterator();
		while (eIt.hasNext()) {
			Edge e = eIt.next();
			String key = e.getAttribute("ui.label");
			counter.inc(key);
		}
		return counter;
	}

	/**
	 * 
	 * @param name
	 */
	public void removeEdgesByName(String name) {
		LinkedList<Edge> eToDelete = new LinkedList<Edge>();
		Iterator<Edge> eIt = graph.getEdgeIterator();
		while (eIt.hasNext()) {
			Edge e = eIt.next();
			String key = e.getAttribute("ui.label");
			if (key.equals(name)) {
				eToDelete.add(e);
			}
		}
		// remove edges
		for (Edge e : eToDelete) {
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
		for (dbpedia.BreadthFirstSearch.Node temp : requestNodes) {
			this.requestNodes.put(temp.resourceName(), temp.resourceName());
		}
		// and colorizing the graph
		// works yet just for 1 start Node
		// for(dbpedia.BreadthFirstSearch.Node temp : requestNodes){
		// dfs(graph.getNode(temp.resourceName()), new LinkedList<Node>());
		// }
		dfs2(graph.getNode(requestNodes.get(0).resourceName()),
				new HashMap<String, String>(), 0);
	}

	private int dfs2(Node actual, HashMap<String, String> previous, int step) {

		// reached one of the request nodes
		if (isRequestNode(actual) && !previous.isEmpty())
			// return the distance
			return step;
		// setting shortest Distance to "infinity"
		int shortestDist = 100;

		for (Edge edgeTemp : actual.getEachEdge()) {
			if (edgeTemp == null)
				continue;
			// get adjacent node
			Node adjacentNode;
			if (edgeTemp.getNode0() == actual)
				adjacentNode = edgeTemp.getNode1();
			else
				adjacentNode = edgeTemp.getNode0();
			// never go backwards
			if (previous.get(adjacentNode.getId()) != null)
				continue;
			previous.put(actual.getId(), actual.getId());
			// try to find a way from this adjacent node to a request node
			int dist = dfs2(adjacentNode, previous, step + 1);
			previous.remove(actual.getId());
			// check if already colored
			String prevC = edgeTemp.getAttribute("col");
			if (prevC != null) {
				// check which path was shorter and adjust color
				if (Integer.parseInt(prevC) > dist) {
					edgeTemp.setAttribute("col", "" + dist);
					edgeTemp.setAttribute("ui.class", getColor(dist));
				}
			}
			// initial coloring for this edge
			else {
				edgeTemp.addAttribute("col", "" + dist);
				edgeTemp.addAttribute("ui.class", getColor(dist));
			}

			// return shortest distance from this node to the requested node
			if (dist < shortestDist) {
				shortestDist = dist;
			}

		}

		return shortestDist;
	}

	private String getColor(int color) {
		switch (color) {
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

	/**
	 * 
	 * @param res
	 * @param name
	 * @return
	 */
	public static WTPGraph createFromResultSet(ResultSet res, String name) {
		WTPGraph graph = new WTPGraph(name);
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
				Edge edge = graph.getGraph().addEdge(e.getID() + "",
						e.source.resourceName(), e.dest.resourceName(),
						useDirectedEdges);
				edge.setAttribute("ui.label", e.getName());
			} catch (org.graphstream.graph.EdgeRejectedException err) {
				// System.out.println("Error: " + err.getMessage());
				// System.out.println("Node: " + e.toString());
			}
		}
		return graph;
	}

	public static Model getRDFGraph(WTPGraph graph) {
		Model rdfmodel = ModelFactory.createDefaultModel();
		rdfmodel.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
		rdfmodel.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
		rdfmodel.setNsPrefix("dbpprop", "http://dbpedia.org/property/");
		rdfmodel.setNsPrefix("dbpedia-owl", "http://dbpedia.org/ontology/");
		// iterate over all nodes and extract triples
		System.out.println("original nodes: "+graph.getGraph().getNodeSet().toString());
		System.out.println("original edges: "+graph.getGraph().getEdgeSet().toString());
		System.out.println("gen rdf graph");

		for (Edge e : graph.getGraph().getEdgeSet()) {

			// determining edge label
			String edgelabel = e.getAttribute("ui.label");

			System.out.println("Adding triple S[" + e.getNode0().getId()
					+ "] P[" + edgelabel + "] O[" + e.getNode1().getId());

			Resource subj = rdfmodel.createResource(e.getNode0().getId());
			Property pred = rdfmodel.createProperty(edgelabel);
			Resource obj = rdfmodel.createResource(e.getNode1().getId());
			rdfmodel.add(subj, pred, obj);
		}

		return rdfmodel;
	}

	public static WTPGraph fromXML(String xmlsrc) {
		int idPool = 0;

		WTPGraph graph = new WTPGraph(null);

		InputStream is = new ByteArrayInputStream(xmlsrc.getBytes());
		Model rdfmodel = ModelFactory.createDefaultModel();
		rdfmodel.read(is, null, "RDF/XML");
		StmtIterator iter = rdfmodel.listStatements();
		while (iter.hasNext()) {
			if (idPool >= Integer.MAX_VALUE)
				idPool = 0;

			Statement stmt = iter.next();
			String subject = stmt.getSubject().toString();
			String predicate = stmt.getPredicate().toString();
			String unique_predicate = predicate + (idPool++)
					+ predicate.hashCode();
			String obj = stmt.getObject().toString();

		
			if (graph.getGraph().getNode(subject) == null)
				graph.addNode(subject);
			if (graph.getGraph().getNode(obj) == null)
				graph.addNode(obj);
			System.out.println("Inserting edge " + subject + " -- "
					+ unique_predicate + " -- " + obj);
			graph.addEdge(unique_predicate, subject, obj,predicate);
		}

		return graph;
	}
}