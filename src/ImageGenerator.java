import graph.WTPGraph;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;

import utils.MainClass;
import utils.PowerSetGenerator;


public class ImageGenerator extends MainClass {
	// Automated Evaluation
	public final static String outputFolder = "C:\\Users\\Chris\\Desktop\\test\\";
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		doTests();
	}
	
	/**
	 * 
	 */
	protected static void doTests() {
		// parameters
		final int keywordListIdx = 3;
		final int maxNumKeywords = 6;
		final int minNumKeywords = 3;
		final boolean skipIfExists = true;
		
		// initalize
		final LinkedList<String> keywords = keywordList.get(keywordListIdx);
		final String[] arr = keywords.toArray(new String[keywords.size()]);
		// iterate power set
		final int maxNumKWs = Math.min(maxNumKeywords, keywords.size());
		final int minNumKWs = Math.min(minNumKeywords, keywords.size());
		
		// run all combinations
		for (int i = maxNumKWs; i > minNumKWs; i--) {
			PowerSetGenerator<String> psg = new PowerSetGenerator<String>(i, arr);
			// iterate all powersets with k = i
			for(String[] l : psg) {
				// create filename
				final String path = keywordListIdx + " (" + keywords.get(0) + "..." + keywords.getLast() + ")\\" +
						"Search Depth = " + maxSearchDepth + "\\" +
						i + " keywords\\" +
						Arrays.toString(l) + ".png";
				final String file = outputFolder + path;
				
				// chek if this file already exists
				if (skipIfExists && new File(file).exists()) {
					System.out.println("File already exists: " + path);
					continue;
				}
				
				// generate fingerprint
				WTPGraph g = processKeyWords(new LinkedList<String>(Arrays.asList(l)));
				
				// save to iamge
				System.out.println("Saving graph to file \"" + path + "\"");
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
}
