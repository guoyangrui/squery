package com.squery.servlet;

import java.io.IOException;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;

import com.squery.nlp.CoreNlpParser;
import com.squery.nlp.TreeGenerator;
import com.squery.solr.SolrBuilder;

import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.StringUtils;

/**
 * Servlet implementation class QueryServlet
 */
@WebServlet("/query")
public class QueryServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public QueryServlet() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		String sentence = request.getParameter("s");
		String subfacet = request.getParameter("sf");
		String[] filters = new String[0];

		if (null != request.getParameter("f") && !request.getParameter("f").isEmpty()) 
			filters = request.getParameter("f").split(",");
		if (null == subfacet || request.getParameter("sf").isEmpty())
			subfacet = "*:*"; else subfacet = subfacet.replaceAll("[^A-Za-z0-9 ]", "");
				
	    response.setContentType("application/json");
	    response.setCharacterEncoding("UTF-8");
	    response.addHeader("Access-Control-Allow-Origin", "*");
	    response.addHeader("Access-Control-Allow-Headers", "*");
	    response.addHeader("Access-Control-Request-Method","*");

		if (null == sentence || sentence.isEmpty()) {
			JsonObject jsonResponse = Json.createObjectBuilder().add("message", "How can I help you?").build();
			response.getWriter().append(jsonResponse.toString());
		}
		else {
			String results = query(sentence, filters, subfacet);
			response.getWriter().append(results);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

	protected String query(String sentence, String[] filters, String subfacet) {
		CoreNlpParser parser = new CoreNlpParser();
		List<Tree> trees = parser.getTextAnnotatedTree(sentence);
		
		for (Tree tree : trees) {

			TreeGenerator treeGenerator = new TreeGenerator("typedDependenciesCollapsed");
			treeGenerator.printTree(tree);
			List<TypedDependency> deps = treeGenerator.getDeps();
			SemanticGraph graph = new SemanticGraph();
			for (TypedDependency dep : deps) {
				SemanticGraphEdge edge = new SemanticGraphEdge(dep.gov(), dep.dep(), dep.reln(), 0, false);
				if (dep.reln().getShortName() == "root")
					graph.setRoot(dep.dep());
				else
					graph.addEdge(edge);
			}
			//SolrBuilder.query(graph);
			String queryString = SolrBuilder.buildQuery(graph, filters, subfacet).toQueryString();
			String host = "http://50.97.229.36:8983/solr/traffic/select";
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(host + queryString + "&wt=json");
			ResponseHandler<String> handler = new BasicResponseHandler();

			// add request header

			try {
				HttpResponse response = client.execute(request);
				String body = handler.handleResponse(response);
				System.out.println(body);
				return body;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "{ \"message\": \"An error occurred!\" }";
			}
		}
		return null;
	}

}
