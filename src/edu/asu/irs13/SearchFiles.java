/*
Author: Udai Arora
Date: 2/14/2014
*/

package edu.asu.irs13;
import java.math.*;
import java.util.*;
import java.util.Map.Entry;

import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.document.*;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;



public class SearchFiles {
	//@SuppressWarnings({ "unchecked", "rawtypes", "rawtypes", "resource" })
	//Defining Public Variables
	static Map<String, Double> idf_map = new HashMap<String, Double>();
	static IndexReader r;
	static double mod_d_idf[];
	static double mod_d_tf[];
	static Term temp_term;
	static TermDocs temp_doc;
	static double mod_q=0; //Modulus of query
	static Map<Integer,Double> result;
	static double tfidf_similarity;
	static double tf_similarity;
	
	public static void main(String[] args) throws Exception
	{
		// the IndexReader object is the main handle that will give you all the documents, terms and inverted index
		r = IndexReader.open(FSDirectory.open(new File("index")));
		
		// You can figure out the number of documents using the maxDoc() function
		System.out.println("The number of documents in this index is: " + r.maxDoc());
		
		
		//Start Timer
		long start_timer=startTimer();
		//Pre-computing Mod D and IDF
		System.out.println("Please wait while |D| is calculated...");
		mod_d_idf=new double[r.maxDoc()];
		mod_d_tf=new double[r.maxDoc()];
		TermEnum term_index= r.terms();
		double no_in_which_term_appears=0;
		while(term_index.next())
		{	
			
			temp_term = new Term("contents", term_index.term().text());
			temp_doc = r.termDocs(temp_term);

			//Update the IDF map			
			no_in_which_term_appears=r.docFreq(temp_term);
			if (no_in_which_term_appears==0) {
				no_in_which_term_appears=r.maxDoc();
			}
			idf_map.put(term_index.term().text(), (double) Math.log(r.maxDoc()/no_in_which_term_appears));
			
			//Now for each doc in the term, increase the mod_d value by the square of the frequency of the term in that document
			
			while(temp_doc.next())
			{
				mod_d_tf[temp_doc.doc()]= (double) (mod_d_tf[temp_doc.doc()] + Math.pow((temp_doc.freq()),2));
				//System.out.println("yo");
				mod_d_idf[temp_doc.doc()]= (double) (mod_d_idf[temp_doc.doc()] + Math.pow((temp_doc.freq()*idf_map.get(term_index.term().text())),2));
				
			}
			
		}
		
		//The the square root to get the final mod_d value.
		int j=0;
		for(j=0;j<r.maxDoc();j++){
			mod_d_idf[j]=(double) Math.sqrt(mod_d_idf[j]);
			mod_d_tf[j]=(double) Math.sqrt(mod_d_tf[j]);
		}
		System.out.println("Done.");

		/* END PRECOMPUTING |D| */
		//End Timer
		endTimer("Time taken for pre-computations: ", start_timer);
	

	
		// Input Query		
		Scanner sc = new Scanner(System.in);
		String str = "";
		System.out.print("query> ");
		while(!(str = sc.nextLine()).equals("quit"))
		{
			result = new HashMap<Integer, Double>();
			Map<String, Integer> query=new HashMap<String, Integer>();
			mod_q=0;
			tf_similarity=0;
			String[] terms = str.split("\\s+");
			
			
			//Finding |Q|
			for(String w : terms)
			{
				if(query.containsKey(w)){
					query.put(w, (int)query.get(w)+1);
				}
				else {
					query.put(w, 1);
				}
			}
			
			
			Iterator it = query.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pairs = (Map.Entry)it.next();
		        mod_q=(int) (mod_q+Math.pow((int)pairs.getValue(),2));
		    }
		    mod_q=(double) Math.sqrt(mod_q);
			    //|Q| done
			
			//Call function to calculate IDF
		    System.out.println("Enter 1 for TF or 2 for TF-IDF");
		    Scanner reader = new Scanner(System.in);
		    int choice=1;
		    try {
		    	choice=reader.nextInt();
		    } catch(Exception e){
		    	System.out.println("Error. Autoselecting TF.");
		    	choice=1;
		    }

			start_timer=startTimer();
			//call relevant function according to choice
		    if(choice==1){
				tf_calculator(terms);
		    }
		    else{
		    	tfidf_calculator(terms);
		    }
		    endTimer("Total Time Taken: ", start_timer);

			System.out.print("query> ");
		}
		
	}
	
	
	
	
	
		//Function to calculate tf semilarity
	public static void tf_calculator(String[] terms) throws IOException{
				
		for(String word : terms)
		{
			Term term = new Term("contents", word);
			TermDocs tdocs = r.termDocs(term);
			
			while(tdocs.next())
			{
				tf_similarity=(double)tdocs.freq()/((double)mod_q * (double)mod_d_tf[tdocs.doc()]); //where tdocs.freq=tf
				
				if(result.containsKey(tdocs.doc()))
				{
					//result.get(tdocs.doc());
					result.put(tdocs.doc(), ((double)result.get(tdocs.doc())) + tf_similarity);
				}
				else {
					result.put(tdocs.doc(), tf_similarity);
				}
			}
		}

		long start_timer=startTimer();
        Map<Integer, Double> sorted_result = sortByValue(result);
        endTimer("Time taken to sort the results: ", start_timer);
        printResult(sorted_result);
	}
	
	//Function to calculate tf-idf semilarity
	public static void tfidf_calculator(String[] terms) throws IOException{
		
		double idf=0;
		
		for(String word : terms)
		{
			Term term = new Term("contents", word);
			TermDocs tdocs = r.termDocs(term);
			while(tdocs.next())
			{
				idf= (double) idf_map.get(word);
				
				tfidf_similarity=(double)tdocs.freq()* idf /((double)mod_q * (double)mod_d_idf[tdocs.doc()]); //where tdocs.freq=tf
				
				if(result.containsKey(tdocs.doc()))
				{
					//result.get(tdocs.doc());
					result.put(tdocs.doc(), ((double)result.get(tdocs.doc())) + tfidf_similarity);
				}
				else {
					result.put(tdocs.doc(), tfidf_similarity);
				}
			}
		}

		long start_timer=startTimer();
        Map<Integer, Double> sorted_result = sortByValue(result);
        endTimer("Time taken to sort the results: ", start_timer);
        printResult(sorted_result);
	}

		
	//Function to print the resulting hash map similarity
	public static void printResult(Map result) throws CorruptIndexException, IOException{
		//System.out.println(result);
		Iterator i = result.entrySet().iterator(); 
		int count=1;
		
			
		while(i.hasNext() && count<11){
			Integer key = (Integer) ((Entry) i.next()).getKey();
			System.out.println(key);
            count++;
            
        }
	}
	
	
	//Defining Functions for measuring time
	public static long startTimer(){
		return(System.nanoTime());
	}
	
	public static void endTimer(String S, long start_time){
		long duration=  System.nanoTime()-start_time;
		System.out.println(S + duration/1000000.0 + " milli-seconds"); 
	}
		
	//Function which inputs a hash map, sorts it and returns it.
	@SuppressWarnings("rawtypes")
	public static <Key extends Comparable, Value extends Comparable> Map<Key,Value> sortByValue(Map<Key,Value> map){
		
		List<Map.Entry<Key,Value>> obj = new LinkedList<Map.Entry<Key,Value>>(map.entrySet());
		
	    Collections.sort(obj, new Comparator<Map.Entry<Key,Value>>() {
	        @SuppressWarnings("unchecked")
			@Override
	        public int compare(Entry<Key, Value> first_entry, Entry<Key, Value> second_entry) {
	            return second_entry.getValue().compareTo(first_entry.getValue());
	        }
	    });
	  
	    Map<Key,Value> sortMap = new LinkedHashMap<Key,Value>();
	  
	    for(Map.Entry<Key,Value> entry: obj){
	        sortMap.put(entry.getKey(), entry.getValue());
	    }
	  
	    return sortMap;
	}

	
}
