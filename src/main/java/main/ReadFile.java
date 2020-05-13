package main;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
/**
 * This class use to find docs of a given path of corpus
 * split each and make the each text inside into ArrayList of string(tokens).
 *
 */
public class ReadFile {
    /**
     * This function get path of corpus
     * and return ArrayList of string of path of each file that contain docs
     * @path -path of the corpus
     * @return @paths  ArrayList of all the paths
     */
    public ArrayList<String> findDocs(String path) {
        Path inputDir = Paths.get(path+"\\corpus");
        ArrayList<String> paths=new ArrayList<>();
        if (Files.isDirectory(inputDir)) {
            List<Path> filePaths = null;
            try {
                filePaths = Files.list(inputDir).collect(Collectors.toList());
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (Path filePath : filePaths)
            {
                File folder = new File(filePath.toString());
                if(folder.getName().equals("stopwords.txt"))
                    continue;
                paths.add(folder.getAbsolutePath()+"\\"+folder.getName());
            }
        }
        return paths;
    }
    /**
     * find the docs in the destination folder
     *
     * @param   path - the path of the folder
     *
     */
    public ArrayList<ArrayList<String>> separateDocs(String path,boolean stemming) {
        ArrayList<ArrayList<String>> result=new ArrayList<>();
        BufferedReader reader=null;
        try {
            reader = new BufferedReader(new FileReader(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String line = null;
        while(true) {
            try {
                if (!((line = reader.readLine()) != null)) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(line.equals("<DOC>")) {

                ArrayList<String> temp = splitText(reader,stemming);
                //temp.add(0,);
                result.add(temp);
//                    parseDoc.Tokenizer(reader);
            }
        }

        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;

    }

    /**
     * This function split the file by doc tag and return the text between the Text tag;
     * @param reader file that contines doces
     * @param stemming-boolean value to know if the use stemmer or not
     * @return list of string that contians the text
     */
    public ArrayList<String> splitText(BufferedReader reader,boolean stemming) {
        String docID;
        String headLine="";
        ArrayList tokens = new ArrayList<String>();
        String line = null;
        boolean startAdd = false;
        while (true) {
            try {
                if (!((line = reader.readLine()) != null)) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (line.startsWith("<DOCNO>")) {
                line = line.replace("<DOCNO>", "");
                line = line.replace("</DOCNO>", "");
                line = line.replace(" ", "");
                docID = line;
                tokens.add(docID);
            }
            if (line.equals("<TEXT>")) {
                startAdd = true;
                continue; // skip the adding of the tag line
            } else if (line.equals("</TEXT>")) {
                return tokens;
            } else if (startAdd) {
                tokens.addAll(cleanLine(stemming,line));
            }

        }
        return tokens;
    }

    public ArrayList<String> cleanLine(boolean stemming, String line) {
        ArrayList tokens = new ArrayList();

        line = line.replace(",", "");
        line = line.replace("!","");
        line = line.replace("?","");
        line = line.replace("--", " ");
        line = line.replace("'", "");
        line = line.replace(";", "");
        line = line.replace("(", "");
        line = line.replace(")", "");
        line = line.replace("[", "");
        line = line.replace("]", "");
        line = line.replace("|", "");
        line = line.replace("...", "");
        line = line.replace("..", "");
        line = line.replace("'", "");
        line = line.replace(":", " ");
        //   line = line.replace("\"", "");
        String[] words = line.split(" ");
        for (String word : words) {
            word = Parse.cleanWord(word);
            if(stemming){
                Stemmer stemm=new Stemmer();
                char[] a;
                for(int i=0; i<word.length(); i++) {
                    a = word.toCharArray();
                    stemm.add(a[i]);
                }
                stemm.stem();
                word=stemm.toString();
            }
            if (word.length() >= 1)
                tokens.add(word);
        }
        return tokens;
    }

}
