package connector;

/**
 * Implements the connection to the DBPedia Endpoint.
 * @author Christian Nywelt
 *
 */
public final class DBPediaEndpoint extends SparqlQueryExecuter {

	public DBPediaEndpoint() {
		super("http://dbpedia.org/sparql");
		//super("http://localhost:8890/sparql");
		
	}
	
	public DBPediaEndpoint(String uri){
		super(uri);
	}
	
	

}
