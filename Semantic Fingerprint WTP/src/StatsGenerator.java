import graph.WTPGraph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
		final int keywordListIdx = 1;
//		final int maxNumKeywords = 7;
//		final int minNumKeywords = 4;
		final boolean skipIfExists = true;
		
		// define the upper bounds of the buckets (including) in percent
		final int[] upperBounds = {40, 65, 90, 999999};
		
		// start
		final String fileName = outputFolder + "evaluation_list_" + keywordListIdx + ".txt";
		if (skipIfExists && new File(fileName).exists()) {
			System.out.println("File already exists: " + fileName);
		}
		
		// initalize
		
		final LinkedList<String> keywords = keywordList.get(keywordListIdx);
		final String[] arr = keywords.toArray(new String[keywords.size()]);
		// iterate power set
		final int maxNumKWs = keywords.size()-1;
		final int minNumKWs = Math.min(keywords.size()-4, 4); // down to max-4 but not lower than 4
		
		// calculate the graph with all keywords
		WTPGraph maxGraph = processKeyWords(keywords);
		final double numNodesMax = maxGraph.getNodeCount();
		
		// initialize buckets
		final int[][] bucketSizes = new int[maxNumKWs - minNumKWs + 1][];
		for (int i = 0; i < bucketSizes.length; i++) {
			bucketSizes[i] = new int[upperBounds.length];
		}
		
		// start calculating
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
			out.write('\t');
			out.write(getNodeNames(maxGraph).toString());
			out.printf("%n----------- The subsets of the keywordlist generate the following ---------- %n");
			out.printf("#keywords\tkeywords\t#nodes\t#edges\t#intersectedNodes\t#intersectedEdges\t#missingNodes\t#missingEdges\tnodesOfResultingGraph%n");
			// run all combination
			for (int i = maxNumKWs; i >= minNumKWs; i--) {
				PowerSetGenerator<String> psg = new PowerSetGenerator<String>(i, arr);
				// iterate all powersets with k = i
				for(String[] keywordArr : psg) {
					// generate fingerprint
					WTPGraph g = processKeyWords(new LinkedList<String>(Arrays.asList(keywordArr)));
					
					// sort into bucket
					int bucketIdx = 0;
					final double nodeCount = g.getNodeCount();
					final double percent = nodeCount / numNodesMax * 100.0;
					while (percent >= upperBounds[bucketIdx]) {
						bucketIdx++;
					}
					bucketSizes[i-minNumKWs][bucketIdx]++;
					
					// write stats
					out.write(Integer.toString(keywordArr.length));
					out.write('\t');
					out.write(Arrays.toString(keywordArr));
					out.write('\t');
					out.write(Integer.toString((int)nodeCount));
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
					out.write('\t');
					out.write(getNodeNames(g).toString());
					// TODO: add more to the output
					//out.write('\n');
					out.printf("%n");
					//out.flush();
				}
			}
		} finally {
			out.close();
		}
		System.out.println("Finished test run.");
		
		// print bucket sizes
		System.out.println("\nBuckets up to percent:");
		for(int i = upperBounds.length-1; i >= 0; i--) {
			System.out.print(upperBounds[i] + "\t");
		}
		System.out.println("\n");
		
		for (int kwSize = bucketSizes.length-1; kwSize >= 0; kwSize--) {
			System.out.println("Bucket distribution for #keywords = " + (kwSize + minNumKWs));
			for(int i = bucketSizes[kwSize].length-1; i >= 0; i--) {
				System.out.print(bucketSizes[kwSize][i] + "\t");
			}
			System.out.println();
		}
		System.out.println();
		
		
		System.exit(0);
	}
	
	/**
	 * 
	 * @param g
	 * @return
	 */
	public static List<String> getNodeNames(WTPGraph g) {
		List<String> nodes = new LinkedList<String>();
		for (Node n : g.getGraph().getNodeSet()) {
			nodes.add(n.getId());
		}
		return nodes;	
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
