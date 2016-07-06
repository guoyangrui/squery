package com.squery.solr;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.IntervalFacet;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import com.squery.nlp.FormattedTriple;
import com.squery.nlp.TripleBuilder;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;

public class SolrBuilder {
    static final String zkHostString = "http://50.97.229.36:9983";

	public static SolrQuery buildQuery(SemanticGraph graph, String[] filters, String subfacet) {
	    List<FormattedTriple> triples = TripleBuilder.traverse(graph, CoreLabel.OutputFormat.VALUE_TAG);
	    SolrQuery query = buildSolrQuery(triples, filters, subfacet);
		return query;
	}
		
	public static void query(SemanticGraph graph, String[] filters, String subfacet) {
		SolrQuery query = buildQuery(graph, filters, subfacet);
	    CloudSolrClient solr = new CloudSolrClient(zkHostString);
	    solr.setDefaultCollection("traffic");

		try {
			QueryResponse response = solr.query(query);
			SolrDocumentList list = response.getResults();
			System.out.println(list);
			System.out.println(response);

		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static SolrQuery buildSolrQuery(List<FormattedTriple> triples, String[] filters, String subfacet) {
	    Set<List<IndexedWord>> used = new HashSet<List<IndexedWord>>();
	    SolrQuery query = new SolrQuery();

		for (FormattedTriple triple : triples) {

			if (!used.contains(triple.getSubj())) {
				StringBuilder subject = new StringBuilder();
				for (int i = 0; i < triple.getSubj().size(); i ++) {
					String word = triple.getSubj().get(i).backingLabel().value();
					subject.append("(name1:");
					subject.append(word);
					subject.append(" OR class1:");
					subject.append(word);
					subject.append(")");
					if (i < triple.getSubj().size() - 1) subject.append(" AND ");
					
				}
				used.add(triple.getSubj());
				System.out.println(subject.toString());
			    query.setQuery("{!parent which=\"content_type:p\"}+("
			    		+ subject +
			    ")+content_type:c");
			    //query.setParam("expand", true);
			    //query.setParam("expand.field", "_root_");
			    //query.setParam("expand.q", "*:*");
			    //query.setParam("expand.fq", "*:*");

				for (List<IndexedWord> phrase: triple.getObj()) {
					String object = indexedWordToString(phrase);
					System.out.println(object);
				    query.addFilterQuery("{!parent which=\"content_type:p\"}+("
				    		+ object +
				    ")+content_type:c");
				}
				
				if (filters.length > 0) {
					for (String filter : filters) {
					    query.addFilterQuery("{!parent which=\"content_type:p\"}+("
					    		+ filter.trim().replaceAll(" ", " AND ") +
					    ")+content_type:c");
					}
				}
				
			    query.setParam("fl", "id,[explain style=nl],[child parentFilter=content_type:p childFilter=content_type:c limit=100]");
			    //query.setHighlight(true);
			    //query.setParam("hl.fl", "id,class1[child parentFilter=content_type:p childFilter=class2:fatality limit=100]"); 
				query.setParam("json.facet", "{class2 : {type: terms,field: class2_f, facet:{name2: {type: query,q: \"" + subfacet + "\", facet:{ name2: {type: terms, field:name2_f}}}},domain: { blockChildren : \"content_type:p\" } } }");
				query.setParam("collection", "traffic,job,car");
			}
		}
		return query;
	}
	
	public static String indexedWordToString(List<IndexedWord> phrase) {
		StringBuilder sb = new StringBuilder();
		for (int k = 0; k < phrase.size(); k++) {
			IndexedWord word = phrase.get(k);
			sb.append(word.backingLabel().value());
			
			if (k < phrase.size() - 1) sb.append(" AND ");
		}
		return sb.toString().trim();
	}
	
}
