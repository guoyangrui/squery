package com.squery.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.MapFactory;

public class TripleBuilder {

	public static List<FormattedTriple> traverse(SemanticGraph graph, CoreLabel.OutputFormat wordFormat) {
		Collection<IndexedWord> rootNodes = graph.getRoots();
		MapFactory<IndexedWord, IndexedWord> wordMapFactory = MapFactory.hashMapFactory();
		StringBuilder sb = new StringBuilder();
		Set<IndexedWord> used = wordMapFactory.newSet();

		List<FormattedTriple> triples = new ArrayList<FormattedTriple>();

		for (IndexedWord root : rootNodes) {
			sb.append("-> ").append(root.toString(wordFormat)).append(" (root)\n");
			recToString(graph, root, wordFormat, sb, 1, used, triples, null);
		}
		Set<IndexedWord> nodes = wordMapFactory.newSet();
		nodes.addAll(graph.vertexSet());
		nodes.removeAll(used);
		while (!nodes.isEmpty()) {
			IndexedWord node = nodes.iterator().next();
			sb.append(node.toString(wordFormat)).append("\n");
			recToString(graph, node, wordFormat, sb, 1, used, triples, null);
			nodes.removeAll(used);
		}
		System.out.println(sb.toString());
		System.out.println(triples.size());
		return triples;
	}

	private static void recToString(SemanticGraph graph, IndexedWord curr, CoreLabel.OutputFormat wordFormat,
			StringBuilder sb, int offset, Set<IndexedWord> used, List<FormattedTriple> triples, IndexedWord subject) {
		used.add(curr);
		List<SemanticGraphEdge> edges = graph.outgoingEdgeList(curr);
		List<SemanticGraphEdge> outgoings = edges;
		List<SemanticGraphEdge> incomings = graph.incomingEdgeList(curr);

		Collections.sort(edges);

		if (curr.backingLabel() != null && curr.backingLabel().tag().startsWith("V")) {
			for (SemanticGraphEdge outgoing : outgoings) {
				String reln = outgoing.getRelation().getShortName();
				String tag = outgoing.getTarget().backingLabel().tag();
				if (tag.startsWith("N") && reln.startsWith("nsubj")) {
					subject = outgoing.getTarget();
					System.out.println("Subject: " + outgoing.getTarget() + " Depth: " + offset);
					List<IndexedWord> phrase = getPhrase(graph.outgoingEdgeList(outgoing.getTarget()),
							outgoing.getTarget());
					System.out.println(phrase);
					FormattedTriple newTriple = new FormattedTriple();
					newTriple.setSubj(phrase);
					triples.add(newTriple);

				}
			}
		}

		if (curr.backingLabel() != null && curr.backingLabel().tag().startsWith("N")) {
			if (0 == incomings.size()) {
				subject = curr;
				List<IndexedWord> phrase = getPhrase(outgoings, curr);
				System.out.println(phrase);

				FormattedTriple newTriple = new FormattedTriple();
				newTriple.setSubj(phrase);
				triples.add(newTriple);
			}
			// System.out.println(curr);
			// System.out.println(outgoings);
			// System.out.println(incomings);
			for (SemanticGraphEdge outgoing : outgoings) {
				String tag = outgoing.getTarget().backingLabel().tag();
				String reln = outgoing.getRelation().getShortName();
				if (tag.startsWith("V") || reln.startsWith("nmod")) {
					System.out.println("Subject: " + curr + " Depth: " + offset + " focus");
					//for (FormattedTriple triple : triples) {
					//	if (triple.hasWord(curr).equals("subj"))
					//		break;
					//}
					subject = curr;
					List<IndexedWord> phrase = getPhrase(outgoings, curr);
					System.out.println(phrase);

					FormattedTriple newTriple = new FormattedTriple();
					newTriple.setSubj(phrase);
					triples.add(newTriple);
				}
			}

			boolean fromPerson = false;

			for (SemanticGraphEdge incoming : incomings) {
				String source = incoming.getSource().backingLabel().tag();
				String reln = incoming.getRelation().getShortName();
				if (reln.startsWith("nmod")) {
					System.out.println("Object: " + curr + " Depth: " + offset + " parent nmod");
					List<IndexedWord> phrase = getPhrase(outgoings, curr);
					addNew(triples, subject, phrase);
					System.out.println(phrase);
				}

				if (source.startsWith("V")) {
					//find me something|read the emails|turn on the lights
					if (graph.incomingEdgeList(incoming.getSource()).size() == 0) {
						System.out.println("Subject: " + curr + " Depth: " + offset + " from person");
						List<IndexedWord> phrase = getPhrase(outgoings, curr);
						System.out.println(phrase);
						
						subject = curr;
						FormattedTriple newTriple = new FormattedTriple();
						newTriple.setSubj(phrase);
						triples.add(newTriple);
					}
					
					for (SemanticGraphEdge parent : graph.outgoingEdgeList(incoming.getSource())) {
						if (parent.getTarget().backingLabel().tag().startsWith("P") || parent.getTarget().backingLabel().value().equals("i"))
							fromPerson = true;
					}
					
					if (fromPerson) {
						System.out.println("Subject: " + curr + " Depth: " + offset + " from person");
						List<IndexedWord> phrase = getPhrase(outgoings, curr);
						System.out.println(phrase);
						
						subject = curr;
						FormattedTriple newTriple = new FormattedTriple();
						newTriple.setSubj(phrase);
						triples.add(newTriple);
					}
					
					if (!fromPerson && reln.contains("obj")) {
						List<IndexedWord> phrase = getPhrase(outgoings, curr);
						addNew(triples, subject, phrase);
						System.out.println("Object: " + curr + " Depth: " + offset + " linked to focus");
					}
				}

				if (source.startsWith("I")) {
					for (SemanticGraphEdge parent : graph.outgoingEdgeList(incoming.getSource())) {
						if (parent.getTarget().backingLabel().tag().startsWith("N")) {
							List<IndexedWord> phrase = getPhrase(outgoings, curr);
							addNew(triples, subject, phrase);
							System.out.println("Object: " + curr + " Depth: " + offset + " linked to a prep");
							System.out.println(phrase);
						}
					}
				}
			}
		}

		for (SemanticGraphEdge edge : edges) {
			IndexedWord target = edge.getTarget();
			sb.append(space(2 * offset)).append("-> ").append(target.toString(wordFormat)).append(" (")
					.append(edge.getRelation()).append(")\n");
			if (!used.contains(target)) { // recurse
				recToString(graph, target, wordFormat, sb, offset + 1, used, triples, subject);
			}
		}
	}
	
	private String findSource() {
		findSource();
		return "person";
	}

	private static void addNew(List<FormattedTriple> triples, IndexedWord subject, List<IndexedWord> object) {
		for (FormattedTriple triple : triples) {
			for (IndexedWord subj : triple.getSubj()) {
				if (subj.equals(subject)) {
					triple.getObj().add(object);
				}
			}
		}
	}

	private static List<IndexedWord> getPhrase(List<SemanticGraphEdge> outgoings, IndexedWord rootWord) {
		List<IndexedWord> result = new ArrayList<IndexedWord>();
		for (SemanticGraphEdge outgoing : outgoings) {
			IndexedWord current = outgoing.getTarget();
			if (current.tag().startsWith("N") || current.tag().startsWith("J")) {
				GrammaticalRelation reln = outgoing.getRelation();
				if (reln.getShortName().contains("compound") || reln.getShortName().contains("amod"))
					result.add(current);
			}
		}
		result.add(rootWord);
		return result;
	}

	private String backTrace(SemanticGraph graph, IndexedWord curr) {
		List<SemanticGraphEdge> incomings = graph.incomingEdgeList(curr);
		if (incomings.size() == 0)
			return null;
		else {
			for (SemanticGraphEdge incoming : incomings) {
				IndexedWord source = incoming.getSource();
				String tag = source.tag();
				if (tag.startsWith("P")) {
					return "person";
				} else if (tag.startsWith("N")) {
					return "nonperson";
				} else
					backTrace(graph, source);
			}
		}
		return null;
	}

	private static String space(int width) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < width; i++) {
			b.append(' ');
		}
		return b.toString();
	}

}
