package connector;

import java.util.List;

import utils.FileCache;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;

/**
 * https://github.com/dbpedia-spotlight/dbpedia-spotlight/blob/master/core/src/main/java/org/dbpedia/spotlight/sparql/SparqlQueryExecuter.java
 * 
 * @author Chris
 *
 */
public class SparqlQueryExecuter {
	// just for testing porpuses
	protected static final String allPrefix = "PREFIX owl: <http://www.w3.org/2002/07/owl#>"
						+"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>"
						+"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
						+"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
						+"PREFIX foaf: <http://xmlns.com/foaf/0.1/>"
						+"PREFIX dc: <http://purl.org/dc/elements/1.1/>"
						+"PREFIX : <http://dbpedia.org/resource/>"
						+"PREFIX dbpedia2: <http://dbpedia.org/property/>"
						+"PREFIX dbpedia: <http://dbpedia.org/>"
						+"PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"
						;
	
	/**
	 * Executes a SPARQL-Query and executes it
	 * @param queryString
	 * @return
	 */
	public static List<QuerySolution> executeQuery(String queryString) {
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
		
		ResultSet results = qexec.execSelect();
		
		// collect results
		List<QuerySolution> res = ResultSetFormatter.toList(results);
		
		qexec.close();
		
		return res;
	}
	
	// ------------------------------------------ tests
	
	/**
	 * 
	 */
	public void test() {
		String sparqlQueryString1= allPrefix
									+"SELECT ?con ?res "
									+"WHERE {"
									+"<http://dbpedia.org/resource/Haskell_(programming_language)> ?con ?res ."
									+"FILTER(STRSTARTS(STR(?res), \"http://dbpedia.org/resource\")) ."
									+"} LIMIT 1000";

		
		Query query = QueryFactory.create(sparqlQueryString1);
		QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
		
		ResultSet results = qexec.execSelect();
		
		ResultSetFormatter.out(System.out, results, query);
		
		// collect output
		/*while(results.hasNext()) {
			QuerySolution sol = (QuerySolution) results.next();

            System.out.println(sol.get("?place"));
		}*/
		
		// close connection
		qexec.close();
	}
	
	public void test2() {
		String sparqlQueryString1= "SELECT ?p ?o {"
									+"<http://nasa.dataincubator.org/spacecraft/1968-089A> ?p ?o"
									+ "}";

		
		Query query = QueryFactory.create(sparqlQueryString1);
		QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
		
		ResultSet results = qexec.execSelect();
		
		ResultSetFormatter.out(System.out, results, query);
		
		
		// close connection
		qexec.close();
	}
}
