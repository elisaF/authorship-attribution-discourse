package entityGrid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;

public class BuildEntityGrid {
	/** Usage: java -cp "*" StanfordCoreNlpDemo [inputFile [outputTextFile [outputXmlFile]]] */
	public static void main(String[] args) throws IOException {
		// Create a CoreNLP pipeline
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, mention, coref");
		props.setProperty("coref.algorithm", "neural");

		PrintWriter out= new PrintWriter(System.out);
		
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		String myDirectoryPath = args[0];
		String saveDirectoryPath = args[1];
		File dir = new File(myDirectoryPath);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) 
		{	
			for (File child : directoryListing) 
			{
				if (child.isFile()){
					System.out.println("Processing file: " + child);
					// Initialize an Annotation with some text to be annotated.
					Annotation annotation = new Annotation(IOUtils.slurpFileNoExceptions(child));

					// run all the selected Annotators on this text
					pipeline.annotate(annotation);

					pipeline.prettyPrint(annotation, out);

					//get num of coref chains for building entity grid
					Map<Integer, CorefChain> corefChains = annotation.get(CorefCoreAnnotations.CorefChainAnnotation.class);
					List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
					List<List<String>> entityGrid = new ArrayList<List<String>>(sentences.size());

					//pre-populate entity grid with relation of none (-)
					prepopulateGrid(corefChains, sentences, entityGrid);

					//parse NPs to get dependency and head word
					List<String> headWords = new ArrayList<String>();
					HashMap<Integer, List<NounPhrase>> sentenceToNPs = new HashMap<Integer, List<NounPhrase>>(sentences.size());
					parseNPs(sentences, sentenceToNPs);

					//loop through coref chains to build entity grid	
					HashMap<Integer, Integer> chainIdToIndex = new HashMap<Integer, Integer>();
					parseCorefChains(corefChains, entityGrid, headWords, sentenceToNPs, chainIdToIndex);
					System.out.println("Finished building grid: " + entityGrid);
					System.out.println("Chain to mention: " + headWords);

					cleanupEntities(entityGrid, headWords);
					System.out.println("After cleanup: " + entityGrid + ", " + headWords);

					//save grid to csv file
					String fileName = saveDirectoryPath+child.getName()+"_grid.csv";
					writeToCSV(entityGrid, headWords, fileName);
				}
			}
		}
	}


	private static void writeToCSV(List<List<String>> entityGrid, List<String> headWords, String fileName) throws FileNotFoundException{
		PrintWriter pw = new PrintWriter(new File(fileName));
		StringBuilder sb = new StringBuilder();
		
		//header
		sb.append(String.join(",", headWords));
		sb.append('\n');

		//rows
		for(List<String> row : entityGrid){
			sb.append(String.join(",", row));
			sb.append('\n');
		}

		pw.write(sb.toString());
		pw.close();
	}

	private static void parseCorefChains(Map<Integer, CorefChain> corefChains, List<List<String>> entityGrid,
			List<String> headWords, HashMap<Integer, List<NounPhrase>> sentenceToNPs,
			HashMap<Integer, Integer> chainIdToIndex) {
		for (Map.Entry<Integer,CorefChain> entry: corefChains.entrySet()) {
			int chainId = entry.getValue().getChainID();

			//get representative mention to add to entity grid later
			CorefMention representativeMention = entry.getValue().getRepresentativeMention();
			IntPair representativeMentionSpan = new IntPair(representativeMention.startIndex, representativeMention.endIndex-1);
			//System.out.println("representativeMention " + representativeMention + ", rep mention span: " + representativeMentionSpan + " for sent: " + (representativeMention.sentNum));
			NounPhrase matchingRepMention = getMentionNP(representativeMention.sentNum-1, sentenceToNPs, representativeMentionSpan);
			//System.out.println("Matching rep mention: " + matchingRepMention);
			for (CorefChain.CorefMention m : entry.getValue().getMentionsInTextualOrder()) {
				//System.out.println("Mention : " + m + ", chain id: " + chainId);
				//is this mention actually an NP?
				IntPair mentionSpan = new IntPair(m.startIndex, m.endIndex-1); //endIndex ends after last word
				//System.out.println("Mention span: " + mentionSpan);
				int sentNum = m.sentNum - 1; //sentence index starts at 1 while list starts at 0
				NounPhrase matchingNounPhrase = getMentionNP(sentNum, sentenceToNPs, mentionSpan);
				if(matchingNounPhrase != null){
					//System.out.println("Matching np is : " + matchingNounPhrase.relation + matchingNounPhrase.span + matchingNounPhrase.headLabel);
					//is this coref chain already in the entity grid?
					Integer chainIndex = chainIdToIndex.get(chainId);
					if (chainIndex != null) {
						String gridRelation = entityGrid.get(sentNum).get(chainIndex);
						//System.out.println("Grid relation: " + gridRelation);
						if(gridRelation.equals("s")){
							System.out.println("Relation in grid for chain id " + chainId + " is subject, so ignoring mention " + m);
						} else {
							//get mention relation
							String mentionRelation = getMentionRelation(matchingNounPhrase);
							//System.out.println("Mention relation: " + mentionRelation);
							//overwrite existing relation if
							if(gridRelation.equals("-") ||			//entity did not exist
									mentionRelation.equals("s") ||  //subject has highest rank
									(mentionRelation.equals("o") && gridRelation.equals("x"))) //object outranks other
							{ 
								entityGrid.get(sentNum).set(chainIndex, mentionRelation);
								System.out.println("Replaced mention " + chainIndex + " with relation " + mentionRelation + " for sentence " + sentNum);
							}
						}

					}
					//new coref chain
					else {
						chainIndex = chainIdToIndex.size();
						chainIdToIndex.put(chainId, chainIndex);
						//get mention relation
						String mentionRelation = getMentionRelation(matchingNounPhrase);
						entityGrid.get(sentNum).set(chainIndex, mentionRelation);	
						System.out.println("Added mention " + chainIndex + " with relation " + mentionRelation + " for sentence " + sentNum);
						//sometime representative mention is not a valid NP!
						if(matchingRepMention != null){
							headWords.add(matchingRepMention.headLabel.toString());
						} else {
							headWords.add(matchingNounPhrase.headLabel.toString());
						}

					}
				}
				else {
					System.out.println("Mention is not an actual NP: " + m);
				}
			}
		}
	}


	private static void parseNPs(List<CoreMap> sentences, HashMap<Integer, List<NounPhrase>> sentenceToNPs) {
		for (CoreMap sentence : sentences) {
			//add new row for this sentence
			sentenceToNPs.put(sentences.indexOf(sentence), new ArrayList<NounPhrase>());

			//get typed dependencies
			SemanticGraph basicDeps = sentence.get(BasicDependenciesAnnotation.class);
			Collection<TypedDependency> typedDeps = basicDeps.typedDependencies();

			//get all NPs in this sentence to eliminate invalid coref mentions
			Tree sentenceTree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
			TregexPattern pat = TregexPattern.compile("@NP|WHNP"); //@ will match any node whose basicCategory is NP or WHNP
			TregexMatcher matcher = pat.matcher(sentenceTree);
			while (matcher.find()) {
				Tree nounPhraseTree = matcher.getMatch();

				//get head
				HeadFinder headFinder = new PennTreebankLanguagePack().headFinder();
				nounPhraseTree.percolateHeads(headFinder);
				Tree head = nounPhraseTree.headTerminal(headFinder,sentenceTree);
				CoreLabel headLabel = (CoreLabel) head.label();

				//get span
				ArrayList<Label> nounPhraseTreelabels = nounPhraseTree.yield();
				CoreLabel firstLeafLabel = (CoreLabel) nounPhraseTreelabels.get(0);
				CoreLabel lastLeafLabel = (CoreLabel) nounPhraseTreelabels.get(nounPhraseTreelabels.size()-1);
				//System.out.println("Found noun phrase " + nounPhraseTree);
				//System.out.println("first leaf index: " + firstLeafLabel.index() + ", last leaf index: " + lastLeafLabel.index());
				IntPair nounPhraseSpan = new IntPair(firstLeafLabel.index(), lastLeafLabel.index());

				//get grammatical relation
				for (TypedDependency typedDependency : typedDeps) {
					if(typedDependency.dep().index() == headLabel.index() ){
						//System.out.println("typedDependency match!! : " + typedDependency + ", relation: " + typedDependency.reln());
						String nounPhraseRelation = typedDependency.reln().toString();
						NounPhrase np = new NounPhrase(nounPhraseSpan, nounPhraseRelation, headLabel);
						sentenceToNPs.get(sentences.indexOf(sentence)).add(np);

					}
				}
			}
		}
	}


	private static void prepopulateGrid(Map<Integer, CorefChain> corefChains, List<CoreMap> sentences,
			List<List<String>> entityGrid) {
		for (CoreMap sentence : sentences) {
			//add new row for this sentence
			entityGrid.add(new ArrayList<String>(corefChains.size()));
			for (int i=0; i<corefChains.size(); i++){
				entityGrid.get(sentences.indexOf(sentence)).add(i, "-");
			}
		}
		//System.out.println("After pre-populating: " + entityGrid);
	}


	private static void cleanupEntities(List<List<String>> entityGrid, List<String> headWords) {

		//count how many times each entity appears in the grid
		HashMap<Integer, Integer> indexToCounts = new HashMap<Integer, Integer>();
		Iterator<List<String>> iter = entityGrid.iterator();
		while(iter.hasNext()){
			List<String> relations = iter.next();
			for(int i=0; i<relations.size(); i++){
				if(!relations.get(i).equals("-")){
					Integer oldCount = indexToCounts.getOrDefault(i, 0);
					indexToCounts.put(i, (oldCount+1));
				}
			}
		}
		//System.out.println("counts to index: " + indexToCounts);

		//remove entities with less than 2 mentions
		if(indexToCounts.containsValue(0) || indexToCounts.containsValue(1)){
			List<Integer> indecesToRemove = new ArrayList<Integer>();
			for(Integer index : indexToCounts.keySet()){
				Integer count = indexToCounts.get(index);
				if(count < 2){
					indecesToRemove.add(index.intValue());
				}
			}
			//System.out.println("indeces to remove: " + indecesToRemove);
			Iterator<List<String>> iterCount = entityGrid.iterator();
			while(iterCount.hasNext()){
				List<String> relations = iterCount.next();
				for(int i=relations.size()-1; i>=0; i--){
					if(indecesToRemove.contains(i)){
						relations.remove(i);
						//System.out.println("Removed entity with index: " + i);
					}
				}
			}
			//remove the corresponding head words
			for (int i=headWords.size()-1; i>=0; i--){
				if(indecesToRemove.contains(i)){
					headWords.remove(i);
					//System.out.println("Removed entity from head words with index: " + i);
				}
			}
		}

		//System.out.println("After first removal: " + entityGrid + ", " + headWords);
		//remove entities that never existed (none of them were valid NPs)
		int entityGridSize = entityGrid.get(0).size();
		if (entityGridSize > headWords.size()){
			int numToDelete = entityGridSize - headWords.size();
			for (int i=0; i<numToDelete; i++){
				Iterator<List<String>> iterCount = entityGrid.iterator();
				while(iterCount.hasNext()){
					List<String> relations = iterCount.next();
					relations.remove(relations.size()-1);
				}
			}
		}
	}

	private static NounPhrase getMentionNP(int sentNum, HashMap<Integer, List<NounPhrase>> sentenceToNPs, IntPair mentionSpan) {
		List<NounPhrase> nounPhrases = sentenceToNPs.get(sentNum);
		for (NounPhrase nounPhrase : nounPhrases){
			if (nounPhrase.span.compareTo(mentionSpan)==0){
				return nounPhrase;
			}
		}
		return null;
	}
	private static String getMentionRelation(NounPhrase matchingNounPhrase) {
		String relation = matchingNounPhrase.relation;

		if (relation == null){
			throw new RuntimeException("Could not get grammatical relation for matching noun phrase: " + matchingNounPhrase);
		}
		//System.out.println("np relation: " + relation);
		//translate grammatical relation into s, o, x
		if (relation.equals("nsubj")){
			relation = "s";
		}
		else if (relation.equals("dobj") || relation.equals("iobj") 
				|| relation.equals("nsubjpass")){ //treat passive subject as object
			relation = "o";
		}
		else {
			relation = "x";
		}
		return relation;
	}
}