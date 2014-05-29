import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.LinkedList;

import utils.FileCache;
import dbpedia.BreadthFirstSearch;
import dbpedia.BreadthFirstSearch.ResultSet;


public class MainClass {
	
	public static void main(String[] args) {
		LinkedList<String> request = new LinkedList<String>();
		request.add("http://dbpedia.org/resource/Haskell_(programming_language)");
		//request.add("http://dbpedia.org/resource/C++");
		
		try {
			generateGraph(request);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param request
	 * @throws FileNotFoundException 
	 */
	public static void generateGraph(LinkedList<String> request) throws FileNotFoundException {
		System.out.println("Starting BFS...");
		BreadthFirstSearch lc = new BreadthFirstSearch();
		ResultSet res = lc.getConnections(request);
		System.out.println("...Done");
		
		System.out.println("Writing Result to file...");
		PrintStream outputStream = new PrintStream(new FileOutputStream("BFS_Output.txt", false));
		res.printTo(outputStream);
		System.out.println("...Done");
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
