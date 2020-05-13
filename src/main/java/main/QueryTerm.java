package main;

import javafx.util.Pair;

import java.util.*;

public class QueryTerm {
    private HashMap<String, Integer> docs;
    private int df;
    private String word;
    /** The class response  to create a query word with the all neccesary deatils for calculate score
     */
    public QueryTerm(String details) {
        docs=new HashMap<>();
        String[] splitDetails = details.split(";");
        this.word=splitDetails[0];
        if (splitDetails.length >= 2) {
            if (splitDetails[1].contains("|")) {
                String[] countDocs = splitDetails[1].split("\\|");
                df = countDocs.length;
                for (String count : countDocs) {
                    String[] stringNumber = count.split("#");
                    int number = Integer.parseInt(stringNumber[0]);
                    String[] names=stringNumber[1].split(":");
                    docs.put(names[0],number); // put the doc id and number the word shown there
                }
            } else {
                df = 1;
                String[] stringNumber=splitDetails[1].split("#");
                int number = Integer.parseInt(stringNumber[0]);
                String[] docName=stringNumber[1].split(":");
                docs.put(docName[0],number);
            }
        }
    }
    /** puts in hash table the all docs
     * get the all docs of word in list
     */
    public ArrayList<String> setDocsInList()
    {
        ArrayList<String> docPerWord=new ArrayList<>();
        Iterator it = docs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            docPerWord.add((String)pair.getKey());
        }
        return docPerWord;
    }
    public HashMap getDocs()
    {
        return docs;
    }
    public String getWord()
    {
        return word;
    }
    public double getDF()
    {
        return df;
    }

}