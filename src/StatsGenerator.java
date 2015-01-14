import graph.WTPGraph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;

import utils.PowerSetGenerator;


public class StatsGenerator extends MainClass {
	// Automated Evaluation
	public final static String outputFolder = "C:\\Users\\Chris\\Desktop\\test\\";
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			doTests();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @throws FileNotFoundException 
	 * 
	 */
	protected static void doTests() throws FileNotFoundException {
		// parameters
		final int keywordListIdx = 4;
		final int maxNumKeywords = 8;
		final int minNumKeywords = 4;
		final boolean skipIfExists = true;
		
		// start
		final String fileName = outputFolder + "evaluation_list_" + keywordListIdx + ".txt";
		if (skipIfExists && new File(fileName).exists()) {
			System.out.println("File already exists: " + fileName);
		}
		
		// initalize
		final LinkedList<String> keywords = keywordList.get(keywordListIdx);
		final String[] arr = keywords.toArray(new String[keywords.size()]);
		// iterate power set
		final int maxNumKWs = Math.min(maxNumKeywords, keywords.size());
		final int minNumKWs = Math.min(minNumKeywords, keywords.size());
		
		// calculate the graph with all keywords
		WTPGraph maxGraph = processKeyWords(keywords);
		
		PrintWriter out = new PrintWriter(fileName);
		
		try {
			// write information about the full graph
			out.printf("-- Format: #keywords, keywords, #nodes, #edges, #intersectedNodes, #intersectedEdges, #missingNodes, #missingEdges (if the two last are negative the certain graph is bigger than the graph generated with the fill keywordlist)%n");
			out.printf("-- The maximal graph gives the following output:%n");
			out.write(Integer.toString(keywords.size()));
			out.write('\t');
			out.write(keywords.toString());
			out.write('\t');
			out.write(Integer.toString(maxGraph.getNodeCount()));
			out.write('\t');
			out.write(Integer.toString(maxGraph.getEdgeCount()));
			out.printf("%n----------- The subsets of the keywordlist generate the following ---------- %n");
			out.printf("#keywords\tkeywords\t#nodes\t#edges\t#intersectedNodes\t#intersectedEdges\t#missingNodes\t#missingEdges%n");
			// run all combination
			for (int i = maxNumKWs; i >= minNumKWs; i--) {
				PowerSetGenerator<String> psg = new PowerSetGenerator<String>(i, arr);
				// iterate all powersets with k = i
				for(String[] keywordArr : psg) {
					// generate fingerprint
					WTPGraph g = processKeyWords(new LinkedList<String>(Arrays.asList(keywordArr)));
					// write stats
					out.write(Integer.toString(keywordArr.length));
					out.write('\t');
					out.write(Arrays.toString(keywordArr));
					out.write('\t');
					out.write(Integer.toString(g.getNodeCount()));
					out.write('\t');
					out.write(Integer.toString(g.getEdgeCount()));
					// add intersection
					out.write('\t');
					final int numNodeInter = getNumIntersectingNodes(maxGraph, g);
					out.write(Integer.toString(numNodeInter));
					out.write('\t');
					final int numEdgeInter = getNumIntersectingEdges(maxGraph, g);
					out.write(Integer.toString(numEdgeInter));
					// how many edges are missing to be equal to the max graph (negative value => we have more nodes/egdes in the small keywordlist)
					out.write('\t');
					out.write(Integer.toString(maxGraph.getNodeCount() - numNodeInter));
					out.write('\t');
					out.write(Integer.toString(maxGraph.getEdgeCount() - numEdgeInter));
					// TODO: add more to the output
					//out.write('\n');
					out.printf("%n");
					out.flush();
				}
			}
		} finally {
			out.close();
		}
		System.out.println("Finished test run.");
		System.exit(0);
	}
	
	
	/**
	 * 
	 * @param g1
	 * @param g2
	 * @return 
	 */
	public static int getNumIntersectingNodes(WTPGraph g1, WTPGraph g2) {
		int num = 0;
		for (Node n : g1.getGraph().getNodeSet()) {
			for (Node n2 : g2.getGraph().getNodeSet()) {
				if (n.getId().equals(n2.getId())) {
					num++;
				}
			}
		}
		return num;
	}
	
	/**
	 * 
	 * @param g1
	 * @param g2
	 * @return
	 */
	public static int getNumIntersectingEdges(WTPGraph g1, WTPGraph g2) {
		int num = 0;
		for (Edge e : g1.getGraph().getEdgeSet()) {
			for (Edge e2 : g2.getGraph().getEdgeSet()) {
				if (e.getSourceNode().getId().equals(e2.getSourceNode().getId()) &&
					e.getTargetNode().getId().equals(e2.getTargetNode().getId())) {
					num++;
				}
			}
		}
		return num;
	}
}
