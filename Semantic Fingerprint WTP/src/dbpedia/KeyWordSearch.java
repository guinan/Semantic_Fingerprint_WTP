package dbpedia;

import java.util.LinkedList;
import java.util.List;

/**
 * Make a Keywordsearch to the dbpedia lookup located at:
 * http://lookup.dbpedia.org/api/search.asmx/KeywordSearch?QueryString=query_string
 * 
 * @author Chris
 *
 */
public class KeyWordSearch {
	public static class Concept {
		String label;
		String uri;
	}
	
	/**
	 * 
	 * @param keyword
	 * @return
	 */
	public List<KeywordSearchResult> search(String keyword) {
		return new LinkedList<KeywordSearchResult>();
	}
	
	
	/**
	 * 
	 * @author Chris
	 *
	 */
	public static class KeywordSearchResult extends Concept {
		List<Concept> classes;
		List<Concept> categories;
		//List<Concept> templates;
		//List<Concept> redirects;
		int refcount;
	}
}
