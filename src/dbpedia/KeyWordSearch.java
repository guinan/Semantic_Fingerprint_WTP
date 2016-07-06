package dbpedia;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import utils.FileCache;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;

import connector.DBPediaEndpoint;
import connector.SparqlQueryExecuter;

/**
 * Make a Keywordsearch to the dbpedia lookup located at:
 * http://lookup.dbpedia.org/api/search.asmx/KeywordSearch?QueryString=query_string
 * Note that this class uses caching to store search results.
 * 
 * @author Christian Nywelt
 */
public class KeyWordSearch {
	private final int defaultMaxResultSize = 10;
	
	protected final SparqlQueryExecuter queryExecuter = new DBPediaEndpoint();
	public boolean useCaching = true;
	
	FileCache<SerializedResult> cache = new FileCache<SerializedResult>("KeyWordSearch");
	
	/**
	 * 
	 */
	protected static class SerializedResult implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private final ArrayList<SearchResult> searchResult;
		protected final int searchLimit;
		
		public SerializedResult(ArrayList<SearchResult> searchResult, int searchLimit) {
			this.searchLimit = searchLimit;
			this.searchResult = searchResult;
		}

		@SuppressWarnings("unchecked")
		public ArrayList<SearchResult> getSearchResult() {
			return (ArrayList<SearchResult>) searchResult.clone(); // make sure no one accidently manipulates the cache
		}

		public int getSearchLimit() {
			return searchLimit;
		}

		public List<SearchResult> getShrinkedResult(int limit) {
			ArrayList<SearchResult> res = getSearchResult();
			int maxElems = Math.min(res.size(), limit);
			return res.subList(0, maxElems);
		}
	}
	
	
	/**
	 * 
	 */
	public static class SearchResult implements Comparable<SearchResult>, Serializable {
		private static final long serialVersionUID = 1L;
		public final String label;
		public final String uri;
		public final int count;
		
		/**
		 * 
		 * @param uri
		 * @param label
		 * @param count
		 */
		public SearchResult(String uri, String label, int count) {
			this.label = label;
			this.uri = uri;
			this.count = count;
		}
		
		@Override
		public String toString() {
			return "\"" + label + "\" (" + count + ")";
		}

		@Override
		public int compareTo(SearchResult o) {
			return uri.compareTo(o.uri);
		}
		
		
	}
	
	
	/**
	 * 
	 * @param keywords
	 * @return
	 */
	public List<SearchResult> search(Collection<String> keywords) {
		return search(keywords, defaultMaxResultSize, null);
	}
	
	/**?? wird nur diese suchmethode genutzt? was ist mit den anderen?
	 * 
	 * @param keywords List of keywords for which concepts are searched in the DBPedia
	 * @param limit Maximum number of concepts that will be returned.
	 * @param correspondingKeywords and empty map, which should be filled with the semantic concepts foun
	 *  in the ontology and their corresponding keyword, used for searching them
	 * @return
	 */
	public List<SearchResult> search(Collection<String> keywords, int limit, Map<String, String> correspondingKeywords) {
		LinkedList<SearchResult> res = new LinkedList<SearchResult>();
		for(String keyword : keywords) {
			List<SearchResult> tmp = search(keyword, defaultMaxResultSize);
			for(SearchResult sr : tmp) {
				if (!res.contains(sr)) {
					if(correspondingKeywords != null)
						correspondingKeywords.put(sr.label, keyword);
					res.add(sr);
				}
			}
		}
		return res;
	}
	
	/**
	 * 
	 * @param keyword
	 * @return
	 */
	public List<SearchResult> search(String keyword) {
		return search(keyword, defaultMaxResultSize);
	}
	
	/**
	 * 
	 * @param keyword
	 * @param limit
	 * @return
	 */
	public List<SearchResult> search(String keyword, int limit) {
		SerializedResult cacheHit = cache.get(keyword);
		if (useCaching && cacheHit != null && cacheHit.searchLimit >= limit) {
			List<SearchResult> res = cacheHit.getShrinkedResult(limit);
			return res;
		}
		// execute query
		String query = createQueryString(keyword, limit);
		List<QuerySolution> queryResult = queryExecuter.executeQuery(query);
		// create result
		ArrayList<SearchResult> resultList = new ArrayList<SearchResult>(queryResult.size());
		for(QuerySolution sol : queryResult) {
			RDFNode uri = sol.get("?s");
			RDFNode name = sol.get("?l");
			String nameStr = name.toString();
			nameStr = nameStr.substring(0, nameStr.length()-3);
			RDFNode count = sol.get("?count");
			String countStr = count.toString();
			countStr = countStr.substring(0, countStr.indexOf("^"));
			SearchResult res = new SearchResult(uri.toString(), nameStr, Integer.parseInt(countStr));
			resultList.add(res);
		}
		cache.put(keyword, new SerializedResult(resultList, limit));
		return resultList;
	}
	
	/**
	 * 
	 * @param keyword
	 * @param limit
	 * @return
	 */
	protected String createQueryString(String keyword, int limit) {
		StringBuilder st = new StringBuilder();
		st.append("SELECT ?s ?l (count(?s) as ?count) ");
		//st.append("GRAPH :http://dbpedia.org ");
		st.append("WHERE { ?someobj ?p ?s . ");
		st.append("?s <http://www.w3.org/2000/01/rdf-schema#label> ?l . ");
		st.append("?l <bif:contains> \"'");
		st.append(keyword);
		st.append("'\" . ");
		//st.append("FILTER (!regex(str(?s), '^http://dbpedia.org/resource/Category:')). ");
		st.append("FILTER (!STRSTARTS(STR(?s), 'http://dbpedia.org/resource/Category:')). ");
		//st.append("FILTER (!regex(str(?s), '^http://dbpedia.org/resource/List')). ");
		st.append("FILTER (!STRSTARTS(STR(?s), 'http://dbpedia.org/resource/List')). ");
		//st.append("FILTER (!regex(str(?s), '^http://sw.opencyc.org/')). ");
		st.append("FILTER (!STRSTARTS(STR(?s), 'http://sw.opencyc.org/')). ");
		st.append("FILTER (lang(?l) = '' || langMatches(lang(?l), 'en')). ");
		st.append("FILTER (!isLiteral(?someobj)). } ");
		st.append("GROUP BY ?s ?l ");
		st.append("ORDER BY DESC(?count) LIMIT ");
		st.append(limit);
		return st.toString();
	}

	/**
	 * 
	 * @param resultList
	 * @return
	 */
	public static List<String> toUriList(List<SearchResult> resultList) {
		ArrayList<String> res = new ArrayList<String>(resultList.size());
		for(SearchResult sr : resultList) {
			res.add(sr.uri);
		}
		return res;
	}
	
	
}
