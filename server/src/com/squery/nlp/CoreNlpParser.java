package com.squery.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.squery.servlet.QueryServlet;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * @author 	Josh Williams
 * @date	10Jun2013
 */
public class CoreNlpParser {

	private static Logger log = Logger.getLogger(QueryServlet.class);
	Properties props;
	StanfordCoreNLP pipeline;
	
	/**
	 * Default constructor
	 * 
	 * Creates a generic CoreNLP instance to annotate with the tokenize, split
	 * and parse options.
	 */
	public CoreNlpParser() {
		props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, depparse, ner, parse");
		props.setProperty("depparse.extradependencies", "MAXIMAL");
		pipeline = new StanfordCoreNLP(props);
	}
	
	
	/**
	 * Creates a CoreNLP instance to annotate with the specified annotators
	 * 
	 * @param annotatorsString String - comma separated list of annotators to
	 * 		perform when parsing. 
	 */
	public CoreNlpParser(String annotatorsString) {
		props = new Properties();
		props.put("annotators", annotatorsString);
		pipeline = new StanfordCoreNLP(props);
	}
    
	public List<Tree> getTextAnnotatedTree(String text) {
		ArrayList<Tree> results = new ArrayList<Tree>();
		List<CoreMap> annotationResults = annotate(text);
		for(CoreMap sentence : annotationResults) {
			results.add(sentence.get(TreeAnnotation.class));
		}
		return results;
	};
	
	public List<SemanticGraph> getTextDependencyTree(String text) {
		List<CoreMap> annotateResults = annotate(text);
		ArrayList<SemanticGraph> results = new ArrayList<SemanticGraph>(); 
		for(CoreMap sentence : annotateResults) {
			results.add((SemanticGraph) sentence.get(CollapsedCCProcessedDependenciesAnnotation.class));
		}
		return results;
	}
	
	public List<String> getRootChildrenAsString(String text) {
		List<SemanticGraph> dependencies = getTextDependencyTree(text);
		ArrayList<String> result = new ArrayList<String>();
		
		String resultString = "";
		for(SemanticGraph graph : dependencies) {
			Collection<IndexedWord> roots = graph.getRoots();
			
			for(IndexedWord r : roots) {
				resultString += ("Root:\n");
				resultString += (r.toString() + "\n");
				resultString += ("Child Pairs:\n");
				try {
					resultString += (graph.childPairs(r).toString() + "\n");
				} catch (IllegalArgumentException e) {
					resultString += ("N/A \n");
				}

				resultString += ("Child Relations:\n");
				try {
					resultString += (graph.childRelns(r).toString() + "\n");
				} catch (IllegalArgumentException e) {
					resultString += ("N/A \n");
				}
			}
			
			result.add(resultString);
			resultString = "";
		}
		
		return result;
	};
	
	public List<String> getDotNotation(String text) {
		List<SemanticGraph> dependencies = getTextDependencyTree(text);
		ArrayList<String> result = new ArrayList<String>();
				
		for(SemanticGraph graph : dependencies) {
			result.add(graph.toDotFormat());
		}
		return result;
	}
	
	public List<String> getEdgeVertexNotationAsString(String text) {
		List<SemanticGraph> dependencies = getTextDependencyTree(text);
		ArrayList<String> result = new ArrayList<String>();
		
		String rstring = "";		
		for(SemanticGraph graph : dependencies) {
			rstring += ("Edges:\n");
			rstring += (graph.edgeIterable());
			rstring += ("\nVertices:\n");
			rstring += (graph.vertexSet());
			
			result.add(rstring);
			rstring = "";
		}
		return result;
	}
	
	/**
	 * Perform the CoreNLP annotation on the entry string, returning the results
	 * as a List of {@link CoreMap} objects corresponding to each sentence. </br>
	 * </br>
	 * <i>Basically just breaks the text into sentences.</i>
	 *  
	 * @param entryString String - The string to be annotated
	 * @return List - List of {@link CoreMap} objects, each corresponding to
	 * 		one sentence of the entryString parameter.
	 * @see edu.stanford.nlp.util.CoreMap
	 */
	protected List<CoreMap> annotate(String entryString) {
		Annotation document = new Annotation(entryString);
	    
	    long startMilli = new Date().getTime();
	    pipeline.annotate(document);
	    long diffTime = new Date().getTime() - startMilli;
	    log.debug("Parsing took " + diffTime + "ms");
	    
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    
	    return sentences;
	}
	
	/**
	 * Return the CoreNLP annotated and parsed result of the entryString as a
	 * String
	 * 
	 * @param entryString String - The string to be processed
	 * @return String - the flattened tree returned by {@link #annotate(String) annotate}
	 */
	public String parseToString(String entryString) {
		List<CoreMap> sentences = annotate(entryString);
		
		String ret = "";
	     
	    for(CoreMap sentence: sentences) {
	    	Tree tree = sentence.get(TreeAnnotation.class);
	    	ret = ret + tree.toString() + " ";
	    }
	    
	    return ret;
	}
	
	/**
	 * Return the CoreNLP annotated and parsed result of the entryString as a
	 * ArrayList of {@link Tree} objects
	 * 
	 * @param entryString String - The string to be processed
	 * @return ArrayList - ArrayList containing {@link Tree} objects
	 * 		representing each string
	 */
	public ArrayList<Tree> parse(String entryString) {
		List<CoreMap> sentences = annotate(entryString);
	    
	    ArrayList<Tree> ret = new ArrayList<Tree>();
	     
	    for(CoreMap sentence: sentences) {
	    	Tree tree = sentence.get(TreeAnnotation.class);
	    	ret.add(tree);
	    }
	    
	    return ret;
	}

	/**
	* Return the CoreNLP annotated and parsed result of the entryString as a
	* Array of {@link Tree} objects formated to remove periods.
	 * @param entryString String - The string to be processed
	 * @return Array - Array containing {@link Tree} objects
	 * 		representing each string
	**/
	public Tree[] parseAndRemovePeriods(String entryString) {
		List<CoreMap> sentences = annotate(entryString);
	    
	    ArrayList<Tree> ret = new ArrayList<Tree>();
	     
	    for(CoreMap sentence: sentences) {
	    	Tree tree = sentence.get(TreeAnnotation.class);
	    	int numKids = tree.lastChild().numChildren();
	    	String last = tree.lastChild().lastChild().toString();
	    	if(last.indexOf("(. ") == 0) {
	    		tree.lastChild().removeChild(numKids -1);
	    	}
	    	ret.add(tree);
	    }
	    Tree[] arr = ret.toArray(new Tree[ret.size()]);
	    
	    return arr;
	}

	/**
	 * 
	 * @param textBlock String - The block of text to parse and extract
	 * @return ArrayList<{@link Triplet}> - The extracted tuples
	**/


}
