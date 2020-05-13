package main;

import javafx.util.Pair;
import sun.reflect.generics.tree.Tree;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
/** Represents the Ranker class
 * Response to rank the queiries and give the best 50 queris
 */
public class Ranker {
    private ArrayList<String> parseredQuery;
    private ArrayList<QueryTerm> queryTerms;
    private HashMap<String,Double> rankDocs;
    private HashSet<String> top50rankDocs;
    private Indexer indexer;
    private BM25 bm25;
    private String path;
    private String queryNumber;
    private String originalQuery;
    private ArrayList<String> semantic=new ArrayList<>();

    public  Ranker(Indexer indexer,String queryNumber,String path,String originalQuery)
    {
        new HashMap<>();
        queryTerms =new ArrayList<>();
        this.indexer=indexer;
        this.queryNumber=queryNumber;
        rankDocs=new HashMap<>();
        this.originalQuery=originalQuery;
        this.path=path;
        try {
            bm25=new BM25(indexer,this.indexer.getCorpusSize(),1.9,0.5);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * locate the line details of word in posting file and create QueryTerm after calculate the tf idf .
     * @throws IOException
     */
    public void orgByPostFile() throws IOException {
        //TODO
        checkIfAllWordExist();
        Collections.sort(parseredQuery);
        String output = indexer.output;
        String line = "";
        BufferedReader bufferedReader = null;
        int in=0;

        String A_Z=output+"\\post_A-Z.txt";
        String a_d=output+"\\post_a-d.txt";
        String e_h=output+"\\post_e-h.txt";
        String i_n=output+"\\post_i-n.txt";
        String o_z=output+"\\post_o-z.txt";
        String numbers=output+"\\post_Num.txt";

        try {
            AtomicInteger i=new AtomicInteger();
        while(i.get()<parseredQuery.size()) {
            char firstChar = parseredQuery.get(i.get()).charAt(0);
                    if (firstChar >= 'A' && firstChar <= 'Z')
                        line=findInPosting( i,'A','Z',A_Z);
                    if (firstChar >= 'a' && firstChar <= 'd')
                        line=findInPosting( i,'a','d',a_d);
                    if (firstChar >= 'e' && firstChar <= 'h')
                        line=findInPosting( i,'e','h',e_h);
                    if (firstChar >= 'i' && firstChar <= 'n')
                       line=findInPosting( i,'i','n',i_n);
                    if (firstChar >= 'o' && firstChar <= 'z')
                        line=findInPosting(i,'o','z',o_z);
                    if(!Character.isLetter(firstChar)) {
                        bufferedReader = new BufferedReader(new FileReader(numbers));
                        while(true) {
                            line=findWord(bufferedReader, parseredQuery.get(i.get()));
                            if (i.get()+1 < parseredQuery.size() && !Character.isLetter(parseredQuery.get(i.get() + 1).charAt(0))) {
                                if(line!="") {
                                    QueryTerm newQueryTerm = createQueryTerm(line);
                                    setDoc(newQueryTerm);
                                    queryTerms.add(newQueryTerm);
                                }
                                i.addAndGet(1);
                            }
                            else
                                break;
                        }
                        bufferedReader.close();
                    }
                if(line!="") {
                    QueryTerm newQueryTerm=createQueryTerm(line); //// after find the details of the word create QueryTerm
                    setDoc(newQueryTerm); ////add to the docs relevant hash map
                    queryTerms.add(newQueryTerm);// add to queryTerms list
                }
                i.addAndGet(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        rankDocs();
    }

    private void checkIfAllWordExist() {
        TreeMap<String,String> allword=indexer.getDic();
        ArrayList<String> dd=new ArrayList<>();
        for(String s:parseredQuery)
        {
            if(!allword.containsKey(s))
            {
                if(Character.isUpperCase(s.charAt(0)))
                    s=s.toLowerCase();
                else
                    s=s.toUpperCase();
                dd.add(0,s);
            }
        }
        parseredQuery.addAll(dd);
    }

    /**
     * initalize the all docs ranks that queryTerm is shown to 0
     * @param qt - QueryTerm
     */
    private void setDoc(QueryTerm qt) {
           ArrayList<String> docName=qt.setDocsInList();
           for(String s:docName)
           {
               if(!rankDocs.containsKey(s))
                 rankDocs.put(s,0.0);
           }
       }
                //send the qi to  BM25 formula and rank
    /**
     * send to BM25 function and get score , put the score in  hashMap of docsRank
     * @throws IOException
     */
    private void rankDocs() throws IOException {
        double score;
        for(QueryTerm qt: queryTerms) {
            HashMap<String, Double> lcoationScores=indexer.getLocation(qt.getWord());  //get the first location of the word , can help for good score
            Iterator it = qt.getDocs().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                double goodLocation=lcoationScores.get((String)pair.getKey());
                score=bm25.getScoreOfBM25(qt,(String)pair.getKey(),goodLocation,originalQuery); //calculate
                if(rankDocs.containsKey((String)pair.getKey()))
                {
                    double value=rankDocs.get(pair.getKey());
                    rankDocs.put((String)pair.getKey(),value+score);  //update the score for current document
                }
                else {
                    rankDocs.put((String) pair.getKey(), score);
                }
            }
        }
        try {
            sortAndPrintResults(queryNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public HashMap getRankDocs()
    {
        return rankDocs;
    }
    public HashSet getTop50()
    {
        return top50rankDocs;
    }
    /**
     * by giving the query id the function sort the scores of the all ranks and returns the best 50
     * @param titleNum
     */
    private void sortAndPrintResults(String titleNum) throws IOException {
        Object[] a = rankDocs.entrySet().toArray();
        top50rankDocs=new HashSet<>();
        int counter=0;
        Arrays.sort(a, new Comparator() {  //sorting
            public int compare(Object o1, Object o2) {
                return ((Map.Entry<String,Double>) o2).getValue()
                        .compareTo(((Map.Entry<String, Double>) o1).getValue());
            }
        });
        try {
            File tempFile = new File(path + "\\results.txt");
            boolean exists = tempFile.exists();
            StringBuilder sb = new StringBuilder();
            if (exists) {
                FileWriter fw = new FileWriter(path + "\\results.txt", true); //the true will append the new data
                for (Object e : a) {
                    if (counter == 50)
                        break;
                    top50rankDocs.add((String)((Map.Entry<String, Double>) e).getKey());   //for TRECEVAL
                    sb.append(titleNum + " 1 " + ((Map.Entry<String, Double>) e).getKey() + " "
                            + "1" + " 42.38 mt");
                    sb.append(System.lineSeparator());
                    counter++;
                }
                fw.append(sb);
                fw.close();
            } else {
                FileWriter fw = new FileWriter(path + "\\results.txt");
                BufferedWriter bf = new BufferedWriter(fw);
                for (Object e : a) {

                    if (counter == 50)
                        break;
                    top50rankDocs.add((String)((Map.Entry<String, Double>) e).getKey());    //add the best 50
                    bf.write(titleNum + " 1 " + ((Map.Entry<String, Double>) e).getKey() + " "
                            + "1" + " 42.38 mt");
                    bf.newLine();
                    counter++;
                }
                bf.close();
            }
            showEntities();
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
        }
    /**
     * create new QueryTerm
     * @param line
     */
    private QueryTerm createQueryTerm(String line) {
            QueryTerm qt=new QueryTerm(line);
            return qt;
    }
    /**
     * find the deatils of word in posting file
     * @param adress - the address of posting file
     * @param end  - the end range of posting file
     * @param i    - index of the word in query
     * @param start - the start of range posting file
     */
    private String findInPosting( AtomicInteger i,char start,char end,String adress) throws IOException {
        BufferedReader bufferedReader;
        String line;
        bufferedReader = new BufferedReader(new FileReader(adress));
        while(true) {
            line=findWord(bufferedReader, parseredQuery.get(i.get()));
            if (i.get() + 1 < parseredQuery.size() && parseredQuery.get(i.get() + 1).charAt(0) >= start && parseredQuery.get(i.get()+ 1).charAt(0) <= end) {
                if(line!="") {
                    QueryTerm newQueryTerm = createQueryTerm(line);
                    setDoc( newQueryTerm);
                    queryTerms.add(newQueryTerm);
                }
                i.addAndGet(1);
            }
            else
                break;
        }
        bufferedReader.close();
        return line;
    }
    /**
     * locate the line - sub function of findInPosting
     * @param br - last postion of bufferReader
     * @param word - the Word in query
     */
    private String findWord(BufferedReader br,String word) throws IOException {
        String line="";
        line=br.readLine();
        while(line!=null)
        {
            String[] split=line.split(";");
            if(word.equals(split[0]))
                return line;
            line=br.readLine();
        }
        return "";
    }

    public void setParseredQuery(ArrayList<String> query) {
        parseredQuery = query;
    }
    /// calculate TF for each and update Score than sort hash map and return the 5 highest entites
    /**
     * calculate IDF for each entity and update Score.
     * Sort hash map and return the 5 highest entites
     * @preturn showEntities - return the updated entities with scores
     */
    public HashMap<String,ArrayList<Pair<String,Double>>>  showEntities() {
        HashMap<String,String> docPost;
        HashMap<String,Integer> entitiesWithCount;
        entitiesWithCount=indexer.getEntites();
        docPost=indexer.getFinalTextIndex();
        for( String s :top50rankDocs)
        {
            //split to find if the entity is legal
        String line=docPost.get(s);
            String[] findEntity = line.split("\\|");
            ArrayList<Pair<String,Double>> top5=new ArrayList();
        if(line.contains("|")) {
            for(int i=1; i<findEntity.length; i++)
            {
                String[] splitted=findEntity[i].split(":");
                double tfidf;
                String entity=splitted[0];
                int df;
                int count=Integer.parseInt(splitted[1]);
                if(top5.size()>=5)
                    break;
                if(entitiesWithCount.containsKey(entity.toUpperCase())&&entitiesWithCount.get(entity.toUpperCase())>1) {
                    df=entitiesWithCount.get(entity.toUpperCase());
                    tfidf = tfidfForEntities(entity.toUpperCase(), splitted[1],s, df);
                    top5.add(new Pair(entity.toUpperCase(),tfidf));
                }
            }
            //sort
            top5.sort(new Comparator<Pair<String, Double>>() {
                @Override
                public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                    if (o1.getValue() > o2.getValue()) {
                        return -1;
                    } else if (o1.getValue().equals(o2.getValue())) {
                        return 0; // You can change this to make it then look at the
                        //words alphabetical order
                    } else {
                        return 1;
                    }
                }
            });        }
        Searcher.rankEntitiesInDocs.put(s,top5);
        }

        return Searcher.rankEntitiesInDocs;
    }

    private double tfidfForEntities(String word,String count,String docName,int df) {
        try {
            int size;
            double idf=Math.log(indexer.getCorpusSize()/(df));
            return idf;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }

    }

}
