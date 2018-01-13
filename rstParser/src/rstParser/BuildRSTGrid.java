package rstParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
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
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;
import entityGrid.NounPhrase;

public class BuildRSTGrid {
	
	private static List<List<List<String>>> entityGrid = null;
	
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

					//pipeline.prettyPrint(annotation, out);

					//get coref chains for building entity grid
					Map<Integer, CorefChain> corefChains = annotation.get(CorefCoreAnnotations.CorefChainAnnotation.class);
					List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
					entityGrid = new ArrayList<List<List<String>>>(sentences.size());

					//prepopulate
					prepopulateGrid(corefChains, sentences);
					
					//parse NPs to get dependency and head word
					List<String> headWords = new ArrayList<String>();
					HashMap<Integer, List<NounPhrase>> sentenceToNPs = new HashMap<Integer, List<NounPhrase>>(sentences.size());
					parseNPs(sentences, sentenceToNPs);

					//loop through coref chains to build grid	
					HashMap<Integer, Integer> chainIdToIndex = new HashMap<Integer, Integer>();
					parseCorefChains(corefChains, headWords, sentenceToNPs, chainIdToIndex);
					
					//System.out.println("Before cleanup: " + entityGrid + ", " + headWords);
					cleanupEntities(headWords);
					//System.out.println("After cleanup: " + entityGrid + ", " + headWords);
					
					//save grid to tsv file
					String fileName = saveDirectoryPath+child.getName()+"_grid.tsv";
					writeToFile(headWords, fileName);
					
				}
			}
		}

	}


	private static void writeToFile(List<String> headWords, String fileName) throws FileNotFoundException{
		PrintWriter pw = new PrintWriter(new File(fileName));
		StringBuilder sb = new StringBuilder();

		//header
		sb.append(String.join("\t", headWords));
		sb.append('\n');

		//rows
		for(List<List<String>> mentions : entityGrid){
			for (List<String> spans: mentions){
				sb.append(String.join(",", spans.toString()));
				sb.append('\t');
			}
			sb.append('\n');
		}

		pw.write(sb.toString());
		pw.close();
	}

	private static void parseCorefChains(Map<Integer, CorefChain> corefChains,
			List<String> headWords, HashMap<Integer, List<NounPhrase>> sentenceToNPs,
			HashMap<Integer, Integer> chainIdToIndex) {
		//loop through chains
		for (Map.Entry<Integer,CorefChain> entry: corefChains.entrySet()) {
			int chainId = entry.getValue().getChainID();

			//get representative mention to add to grid later
			CorefMention representativeMention = entry.getValue().getRepresentativeMention();
			IntPair representativeMentionSpan = new IntPair(representativeMention.startIndex, representativeMention.endIndex-1);
			NounPhrase matchingRepMention = getMentionNP(representativeMention.sentNum-1, sentenceToNPs, representativeMentionSpan);
			
			//loop through mentions
			for (CorefChain.CorefMention m : entry.getValue().getMentionsInTextualOrder()) {
				//System.out.println("Mention : " + m + ", chain id: " + chainId);
				//is this mention actually an NP?
				IntPair mentionSpan = new IntPair(m.startIndex, m.endIndex-1); //endIndex ends after last word
				int sentNum = m.sentNum - 1; //sentence index starts at 1 while list starts at 0
				NounPhrase matchingNounPhrase = getMentionNP(sentNum, sentenceToNPs, mentionSpan);
				if(matchingNounPhrase != null){
					Integer chainIndex = chainIdToIndex.get(chainId);
					if (chainIndex != null) {
						entityGrid.get(sentNum).get(chainIndex).add(matchingNounPhrase.absoluteSpan.toString());		
					}
					//new coref chain
					else {
						chainIndex = chainIdToIndex.size();
						chainIdToIndex.put(chainId, chainIndex);
						entityGrid.get(sentNum).get(chainIndex).add(matchingNounPhrase.absoluteSpan.toString());
						//sometimes representative mention is not a valid NP!
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

	private static void prepopulateGrid(Map<Integer, CorefChain> corefChains, List<CoreMap> sentences) {
		for (CoreMap sentence : sentences) {
			//add new row for this sentence
			entityGrid.add(new ArrayList<List<String>>(corefChains.size()));
			for (int i=0; i<corefChains.size(); i++){
				entityGrid.get(sentences.indexOf(sentence)).add(i, new ArrayList<String>());
			}
		}
	}

	private static void parseNPs(List<CoreMap> sentences, HashMap<Integer, List<NounPhrase>> sentenceToNPs) {
		int absoluteIndex = 0;
		for (CoreMap sentence : sentences) {
			//add new row for this sentence
			sentenceToNPs.put(sentences.indexOf(sentence), new ArrayList<NounPhrase>());

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
				IntPair nounPhraseSpan = new IntPair(firstLeafLabel.index(), lastLeafLabel.index());
				IntPair nounPhraseAbsoluteSpan = new IntPair(firstLeafLabel.index()+absoluteIndex, lastLeafLabel.index()+absoluteIndex);
				NounPhrase np = new NounPhrase(nounPhraseSpan, nounPhraseAbsoluteSpan, headLabel);
				sentenceToNPs.get(sentences.indexOf(sentence)).add(np);
			}
			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);			
			absoluteIndex += tokens.size();
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
	
	private static void cleanupEntities(List<String> headWords) {

		//count how many times each entity appears in the grid
		HashMap<Integer, Integer> indexToCounts = new HashMap<Integer, Integer>();
		Iterator<List<List<String>>> sentenceIter = entityGrid.iterator();
		while(sentenceIter.hasNext()){
			List<List<String>> mentions = sentenceIter.next();
			for(int i=0; i < mentions.size(); i++){
				Integer oldCount = indexToCounts.getOrDefault(i, 0);
				Integer newCount = oldCount;
				if(!mentions.get(i).isEmpty())
				{	
					newCount++;
				}
				indexToCounts.put(i, newCount);
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
			Iterator<List<List<String>>> iterCount = entityGrid.iterator();
			while(iterCount.hasNext()){
				List<List<String>> spans = iterCount.next();
				for(int i=spans.size()-1; i>=0; i--){
					if(indecesToRemove.contains(i)){
						spans.remove(i);
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
				Iterator<List<List<String>>> iterCount = entityGrid.iterator();
				while(iterCount.hasNext()){
					List<List<String>> spans = iterCount.next();
					spans.remove(spans.size()-1);
				}
			}
		}
	}
}