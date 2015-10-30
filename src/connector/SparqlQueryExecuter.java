package connector;

import java.util.List;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;

/**
 * Implements a SparQL Query connector.
 * https://github.com/dbpedia-spotlight/dbpedia-spotlight/blob/master/core/src/main/java/org/dbpedia/spotlight/sparql/SparqlQueryExecuter.java
 * 
 * @author Christian Nywelt
 *
 */
public class SparqlQueryExecuter {
	private final String endpointURL;
	
	public SparqlQueryExecuter(String endpointURL) {
		this.endpointURL = endpointURL;
	}
	/**
	 * Executes a SPARQL-Query and executes it
	 * @param queryString
	 * @return
	 */
	public List<QuerySolution> executeQuery(String queryString) {
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointURL, query);
		System.out.println("sparql endpoint: "+endpointURL);
		ResultSet results = qexec.execSelect();
		
		// collect results
		List<QuerySolution> res = ResultSetFormatter.toList(results);
		
		// print result
		//ResultSetFormatter.out(System.out, results, query);
		
		qexec.close();
		
		return res;
	}
	
	public String getEndpointURL(){
		return this.endpointURL;
	}
}
