package com.squery.nlp;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.IndexedWord;

public class FormattedTriple {
	private List<IndexedWord> subj;
	private List<IndexedWord> reln;
	private List<List<IndexedWord>> obj;
	private int depth;

	public List<IndexedWord> getSubj() {
		return subj;
	}

	public void setSubj(List<IndexedWord> subj) {
		this.subj = subj;
	}

	public List<IndexedWord> getReln() {
		return reln;
	}

	public void setReln(List<IndexedWord> reln) {
		this.reln = reln;
	}

	public List<List<IndexedWord>> getObj() {
		return obj;
	}

	public void setObj(List<List<IndexedWord>> obj) {
		this.obj = obj;
	}

	public FormattedTriple(List<IndexedWord> subj, List<IndexedWord> reln, List<List<IndexedWord>> obj) {
		this.subj = subj;
		this.reln = reln;
		this.obj = obj;
	};

	public FormattedTriple() {
		this.subj = new ArrayList<IndexedWord>();
		this.reln = new ArrayList<IndexedWord>();
		this.obj = new ArrayList<List<IndexedWord>>();
	}

	public String hasWord(IndexedWord word) {
		for (IndexedWord current : subj) {
			if (current.equals(word))
				return "subj";
		}
		for (IndexedWord current : reln) {
			if (current.equals(word))
				return "reln";
		}
		for (List<IndexedWord> currentList : obj) {
			for (IndexedWord current : currentList) {
				if (current.equals(word))
					return "obj";
			}
		}
		return null;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

}
