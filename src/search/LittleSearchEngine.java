package search;

import java.io.*;
import java.util.*;

/**
 * This class encapsulates an occurrence of a keyword in a document. It stores the
 * document name, and the frequency of occurrence in that document. Occurrences are
 * associated with keywords in an index hash table.
 * 
 * @author Sesh Venugopal
 * 
 */
class Occurrence {
	/**
	 * Document in which a keyword occurs.
	 */
	String document;
	
	/**
	 * The frequency (number of times) the keyword occurs in the above document.
	 */
	int frequency;
	
	/**
	 * Initializes this occurrence with the given document,frequency pair.
	 * 
	 * @param doc Document name
	 * @param freq Frequency
	 */
	public Occurrence(String doc, int freq) {
		document = doc;
		frequency = freq;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "(" + document + "," + frequency + ")";
	}
}

/**
 * This class builds an index of keywords. Each keyword maps to a set of documents in
 * which it occurs, with frequency of occurrence in each document. Once the index is built,
 * the documents can searched on for keywords.
 *
 */
public class LittleSearchEngine {
	
	/**
	 * This is a hash table of all keywords. The key is the actual keyword, and the associated value is
	 * an array list of all occurrences of the keyword in documents. The array list is maintained in descending
	 * order of occurrence frequencies.
	 */
	HashMap<String,ArrayList<Occurrence>> keywordsIndex;
	
	/**
	 * The hash table of all noise words - mapping is from word to itself.
	 */
	HashMap<String,String> noiseWords;
	
	/**
	 * Creates the keyWordsIndex and noiseWords hash tables.
	 */
	public LittleSearchEngine() {
		keywordsIndex = new HashMap<String,ArrayList<Occurrence>>(1000,2.0f);
		noiseWords = new HashMap<String,String>(100,2.0f);
	}
	
	/**
	 * This method indexes all keywords found in all the input documents. When this
	 * method is done, the keywordsIndex hash table will be filled with all keywords,
	 * each of which is associated with an array list of Occurrence objects, arranged
	 * in decreasing frequencies of occurrence.
	 * 
	 * @param docsFile Name of file that has a list of all the document file names, one name per line
	 * @param noiseWordsFile Name of file that has a list of noise words, one noise word per line
	 * @throws FileNotFoundException If there is a problem locating any of the input files on disk
	 */
	public void makeIndex(String docsFile, String noiseWordsFile) 
	throws FileNotFoundException {
		// load noise words to hash table
		Scanner sc = new Scanner(new File(noiseWordsFile));
		while (sc.hasNext()) {
			String word = sc.next();
			noiseWords.put(word,word);
		}
		
		// index all keywords
		sc = new Scanner(new File(docsFile));
		while (sc.hasNext()) {
			String docFile = sc.next();
			HashMap<String,Occurrence> kws = loadKeyWords(docFile);
			mergeKeyWords(kws);
		}
		
	}

	/**
	 * Scans a document, and loads all keywords found into a hash table of keyword occurrences
	 * in the document. Uses the getKeyWord method to separate keywords from other words.
	 * 
	 * @param docFile Name of the document file to be scanned and loaded
	 * @return Hash table of keywords in the given document, each associated with an Occurrence object
	 * @throws FileNotFoundException If the document file is not found on disk
	 */
	public HashMap<String,Occurrence> loadKeyWords(String docFile) 
	throws FileNotFoundException {
		Scanner lineReader = new Scanner(new File(docFile));
		HashMap<String,Occurrence> keywords = new HashMap<String,Occurrence>(1000, 2.0f);
		while (lineReader.hasNextLine()){
			String[] line = lineReader.nextLine().split("\\s+"); //splits over all white space
			for (int i = 0; i < line.length; i++){
				String word = line[i];
				word = getKeyWord(word);
				if (word == null || word.length() == 0){
					continue;
				}
				if (keywords.containsKey(word)){
					keywords.get(word).frequency++;
				}
				else {
					keywords.put(word, new Occurrence(docFile, 1));
				}
			}
		}
		lineReader.close();
		return keywords;
	}
	
	/**
	 * Merges the keywords for a single document into the master keywordsIndex
	 * hash table. For each keyword, its Occurrence in the current document
	 * must be inserted in the correct place (according to descending order of
	 * frequency) in the same keyword's Occurrence list in the master hash table. 
	 * This is done by calling the insertLastOccurrence method.
	 * 
	 * @param kws Keywords hash table for a document
	 */
	public void mergeKeyWords(HashMap<String,Occurrence> kws) {
		for (Map.Entry<String, Occurrence> entry : kws.entrySet()){
			String key = entry.getKey();
			if (!keywordsIndex.containsKey(key)){
				keywordsIndex.put(key, new ArrayList<Occurrence>());
			}
			keywordsIndex.get(key).add(entry.getValue());
			insertLastOccurrence(keywordsIndex.get(key));
		}
	}
	
	/**
	 * Given a word, returns it as a keyword if it passes the keyword test,
	 * otherwise returns null. A keyword is any word that, after being stripped of any
	 * TRAILING punctuation, consists only of alphabetic letters, and is not
	 * a noise word. All words are treated in a case-INsensitive manner.
	 * 
	 * Punctuation characters are the following: '.', ',', '?', ':', ';' and '!'
	 * 
	 * @param word Candidate word
	 * @return Keyword (word without trailing punctuation, LOWER CASE)
	 */
	public String getKeyWord(String word) {
		if (word == null || word.length() == 0){										
			return word;
		}
		int i = word.length() - 1;
		while (i >= 0 &&  !word.substring(i).matches("[a-zA-Z]")){									//is not an alphabet character
			word = word.substring(0, i);
			i--;
		}
		for (int j = 0; j < word.length(); j++){										//checks if punctuation is inside word
			if (!word.substring(j, j + 1).matches("[a-zA-Z]")){
				return null;
			}
		}
		word = word.toLowerCase();		//checks if word is noiseword
		if (noiseWords.containsKey(word)){
			return null;
		}
		return word;
	}
	
	/**
	 * Inserts the last occurrence in the parameter list in the correct position in the
	 * same list, based on ordering occurrences on descending frequencies. The elements
	 * 0..n-2 in the list are already in the correct order. Insertion of the last element
	 * (the one at index n-1) is done by first finding the correct spot using binary search, 
	 * then inserting at that spot.
	 * 
	 * @param occs List of Occurrences
	 * @return Sequence of mid point indexes in the input list checked by the binary search process,
	 *         null if the size of the input list is 1. This returned array list is only used to test
	 *         your code - it is not used elsewhere in the program.
	 */
	public ArrayList<Integer> insertLastOccurrence(ArrayList<Occurrence> occs) {
		int lo, hi, mid;
		lo = 0;
		mid = 0;
		hi = occs.size() - 2;
		ArrayList<Integer> midPoints = new ArrayList<Integer>();
		while (lo <= hi){	//binary search to find index of insertion + insertion
			mid = (lo + hi) / 2;
			midPoints.add(mid);
			if (occs.get(occs.size() - 1).frequency == occs.get(mid).frequency){
				occs.add(mid, occs.get(occs.size() - 1)); 
				occs.remove(occs.size() - 1); //removes duplicate Occurrence 
				break;
			}
			if (occs.get(occs.size() - 1).frequency < occs.get(mid).frequency){
				lo = mid + 1;
			}
			else{
				hi = mid - 1;
			}
		}
		if (hi < lo){
			if (occs.get(occs.size() - 1).frequency < occs.get(mid).frequency){
				occs.add(mid + 1, occs.get(occs.size() - 1));
				occs.remove(occs.size() - 1);
			}
			else{
				occs.add(mid, occs.get(occs.size() - 1)); 
				occs.remove(occs.size() - 1);
			}
		}
		if (midPoints.size() == 1){
			return null;
		}
		return midPoints;
	}
	
	/**
	 * Search result for "kw1 or kw2". A document is in the result set if kw1 or kw2 occurs in that
	 * document. Result set is arranged in descending order of occurrence frequencies. (Note that a
	 * matching document will only appear once in the result.) Ties in frequency values are broken
	 * in favor of the first keyword. (That is, if kw1 is in doc1 with frequency f1, and kw2 is in doc2
	 * also with the same frequency f1, then doc1 will appear before doc2 in the result. 
	 * The result set is limited to 5 entries. If there are no matching documents, the result is null.
	 * 
	 * @param kw1 First keyword
	 * @param kw1 Second keyword
	 * @return List of NAMES of documents in which either kw1 or kw2 occurs, arranged in descending order of
	 *         frequencies. The result size is limited to 5 documents. If there are no matching documents,
	 *         the result is null.
	 */
	public ArrayList<String> top5search(String kw1, String kw2) {
		ArrayList<String> result = new ArrayList<String>();
		if (!keywordsIndex.containsKey(kw1) && !keywordsIndex.containsKey(kw2)){
			return null;
		}
		//first list exists, but not second
		if (keywordsIndex.containsKey(kw1) && !keywordsIndex.containsKey(kw2)){
			for (Occurrence occ : keywordsIndex.get(kw1)){
				if (result.size() < 5){
					result.add(occ.document);
				}
			}
			return result;
		}
		//second list exists, but not first
		if (!keywordsIndex.containsKey(kw1) && keywordsIndex.containsKey(kw2)){
			for (Occurrence occ : keywordsIndex.get(kw2)){
				if (result.size() < 5){
					result.add(occ.document);
				}
				
			}
			return result;
		}
		//both lists exist
		ArrayList<Occurrence> list1 = keywordsIndex.get(kw1);
		ArrayList<Occurrence> list2 = keywordsIndex.get(kw2);

		int i = 0; int j = 0;
		while (i < list1.size() && j < list2.size() && result.size() < 5){
			if (list1.get(i).frequency == list2.get(j).frequency){
				if (result.contains(list1.get(i).document)){
					i++;
				}
				else {
				result.add(list1.get(i).document);
				i++;
				}
			}
			else if (list1.get(i).frequency < list2.get(j).frequency) {
				if (result.contains(list2.get(j).document)){
					j++;
				}
				else{
					result.add(list2.get(j).document);
					j++;
				}
				
			}
			else{
				if (result.contains(list1.get(i).document)){
					i++;
				} 
				else{
					result.add(list1.get(i).document);
					i++;
				}
				
			}
		}
		
		while (i < list1.size() && result.size() < 5){
			if (result.contains(list1.get(i).document)){
				i++;
			}else{
				result.add(list1.get(i).document);
				i++;
			}
			
		}
		while (j < list2.size() && result.size() < 5){
			if (result.contains(list1.get(j).document)){
				j++;
			}
			else{
				result.add(list2.get(j).document);
				j++;
			}
			
		}
		
		return result;
	}
}
