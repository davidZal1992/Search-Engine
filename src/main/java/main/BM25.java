package main;

import sun.security.util.BitArray;

import java.io.*;
import java.util.*;

public class BM25  {

    private Indexer indexer;
    private double avgl;
    private double k;
    private double b;
    private int corpusSize;
    public BM25(Indexer indexer,int corpusSize,double k, double b) {
        this.indexer=indexer;
        avgl=this.indexer.getAvgdl();
        this.corpusSize=corpusSize;
        this.k=k;
        this.b=b;
    }

    public double getScoreOfBM25( QueryTerm qt,String docName,double goodLocations,String originalQuery) throws IOException {
        double idf;
        double tf;
        int documentLength;
        double entitConnection;
        int semantics;
        idf=calculateIDF(qt,corpusSize);
        tf=calculateTF(qt,docName);
        documentLength=indexer.getDocLenth(docName);
        if(indexer.getEntites().containsKey(qt.getWord()))
            entitConnection=1;
        else
            entitConnection=0;
        int inQuerytitle=0;
        if(originalQuery.toLowerCase().contains(qt.getWord().toLowerCase()))
            inQuerytitle=1;
        if(goodLocations>0.3)
            goodLocations=0;
        else
            goodLocations=1;
        if(Searcher.semantic!=null&&Searcher.semantic.size()>0&&Searcher.semantic.contains(qt.getWord().toLowerCase()))
            semantics=1;
        else
            semantics=0;

        return 0.85*(idf*(tf*(k+1)))/(tf+k*(1-b+(b*(documentLength/avgl))))+0.05*entitConnection+0.05*inQuerytitle+0.05*goodLocations+0.01*semantics;
    }

    private double calculateTF(QueryTerm qt,String docName) {
        HashMap<String,Integer> docs=qt.getDocs();
        return docs.get(docName);
    }

    private double calculateIDF(QueryTerm qt,int corpusSize) {
        return Math.log(((corpusSize-qt.getDF()+0.5))/(qt.getDF()+0.5));
    }


}
