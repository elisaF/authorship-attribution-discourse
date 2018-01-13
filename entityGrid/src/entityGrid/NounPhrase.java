package entityGrid;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.IntPair;

public class NounPhrase {
	public IntPair span = null;
	public IntPair absoluteSpan = null;
	String relation=null;
	public CoreLabel headLabel=null;
	
	public NounPhrase(IntPair span, String relation, CoreLabel headLabel) {
		this.span = span;
		this.relation = relation;
		this.headLabel = headLabel;
	}

	public NounPhrase(IntPair span, IntPair absoluteSpan, CoreLabel headLabel) {
		this.span = span;
		this.absoluteSpan = absoluteSpan;
		this.headLabel = headLabel;
	}
}
