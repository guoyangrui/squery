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
import com.squery.nlp.TripleBuilder;
import com.squery.solr.SolrBuilder;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.StringUtils;

/**
 * Servlet implementation class QueryServlet
 */
@WebServlet("/lumiere")
public class LumiereServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public LumiereServlet() {
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
			String results = query(sentence);
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

	protected String query(String sentence) {
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
			
			TripleBuilder.traverse(graph, CoreLabel.OutputFormat.VALUE_TAG);

			// add request header
			
		}
		return null;
	}

}
