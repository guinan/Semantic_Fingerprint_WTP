import graph.WTPGraph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;

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
		final int keywordListIdx = 3;
		final int maxNumKeywords = 6;
		final int minNumKeywords = 3;
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
		
		// run all combinations
		PrintWriter out = new PrintWriter(fileName);
		try {
			for (int i = maxNumKWs; i > minNumKWs; i--) {
				PowerSetGenerator<String> psg = new PowerSetGenerator<String>(i, arr);
				// iterate all powersets with k = i
				for(String[] keywordArr : psg) {
					// generate fingerprint
					WTPGraph g = processKeyWords(new LinkedList<String>(Arrays.asList(keywordArr)));
					// write stats
					out.write(Arrays.toString(keywordArr));
					out.write("\t");
					out.write(g.getNodeCount());
					out.write("\t");
					out.write(g.getEdgeCount());
					// TODO: add more to the output
					out.write("\n");
				}
			}
		} finally {
			out.close();
		}
		System.out.println("Finished test run.");
		System.exit(0);
	}
}
