package main;

import com.medallia.word2vec.Word2VecModel;
import javafx.util.Pair;

import java.io.*;
import java.util.*;
/** Represents the search class
 * response to split the query and bring her to state the we can send her to rank include split and parse
 */
public class Searcher {
    private String query="";
   // private Indexer indexer;
    private boolean stemming;
    private Parse parser;
    private String path;
    private String originalString;
    private Ranker ranker;
    private boolean semantics;
    private Indexer indexer;
    public static HashMap<String,ArrayList<Pair<String,Double>>> rankEntitiesInDocs;
    public static ArrayList<String> semantic;
    public  Searcher(Indexer indexer,boolean stemming,String stopWords,String output,boolean semantics,String queriesPath,String path)
    {
        rankEntitiesInDocs=new HashMap<>();
        originalString=query;
        //this.indexer=indexer;
        this.stemming=stemming;
        parser=new Parse(stopWords);
        this.indexer=indexer;
        indexer.setOutput(output);
        this.semantics=semantics;
        this.path=path;
    }
    public void setQuery(String query)
    {
        this.query=query;

    }
    /** parse the query and send her to rank
     * @param stringNumber - the QueryID
     */
    public void parseAndRankQuery(String stringNumber) throws Exception {

            if (this.semantics) {  //if the user choose semantic
                semantic=new ArrayList<>();
                Word2VecModel model = Word2VecModel.fromTextFile(new File("word2vec.c.output.model.txt"));
                com.medallia.word2vec.Searcher semanticSearchrt = model.forSearch();
                int num = 10;
                String[] semanQuery = query.split(" ");
                    for (String s : semanQuery)
                    {
                        try {
                        List<com.medallia.word2vec.Searcher.Match> matches = semanticSearchrt.getMatches(s.toLowerCase(), num);
                        for (com.medallia.word2vec.Searcher.Match match : matches) {
                            match.match();
                        }
                        if(matches.get(0).equals(s)) {
                            query = query + " " + matches.get(1);
                            String dd=""+matches.get(1);
                            semantic.add(dd);
                        }
                        else {
                            query = query + " " + matches.get(0); //add to query the high  semantics 3 top words score
                            String dd=""+matches.get(0);
                            semantic.add(dd);
                        }
                    } catch(com.medallia.word2vec.Searcher.UnknownWordException e){
                    }
                }
            }

        ranker = new Ranker(this.indexer,stringNumber,path,originalString); // new ranker
        ArrayList<String> cleanQuery;
        ArrayList<String> parsedQueryList;
        ReadFile rd=new ReadFile();
        cleanQuery=rd.cleanLine(stemming,query);  //clean the query from dots and other signs
        HashMap<String, Pair<Integer,String>> parsedQuery=new HashMap<>();
        parsedQuery=parser.Tokenizer(cleanQuery,"",stemming); //parse the query
        Iterator it = parsedQuery.entrySet().iterator();
        parsedQueryList=new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            parsedQueryList.add((String)pair.getKey());
            it.remove(); // avoids a ConcurrentModificationException
        }
        ranker.setParseredQuery(parsedQueryList);
        try {
            ranker.orgByPostFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Ranker getRanker()
    {
        return ranker;
    }

    public void setQueryOriginal(String query) {
        originalString=query;
    }

}

