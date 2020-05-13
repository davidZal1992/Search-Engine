package main;
import javafx.util.Pair;
import java.io.*;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
/**
 * This class use to invert index the corpus of a given path
 * create tmep postFile, merga postfile and create dictionary
 *
 */
public class Indexer {
    private TreeMap<String, String> tempPost = new TreeMap<>(); //temp dictonary to export the post to temp file.
    private HashSet<String> wordForSearchInPost = new HashSet<>();
    private HashMap<String, Integer> entirtiesDictionery = new HashMap<>();
    private HashMap<String, String> finalTextIndex = new HashMap<>();
    private TreeMap<String,String> finalDic = new TreeMap<>();
    boolean existDirNoStem=false;
    public int n = 1;//number of doc in the corpus
    public static  int numberOfuniqWords=0;
    public String output;
    private boolean stemming;
    private boolean existDirStem=false;

    public Indexer() {

    }

    /**
     * This function start the indexing of the corpus
     * @param readFiles the readFile class to get the tokens
     * @param corpusPath  the corpus path
     * @param steimming to use stemmer or not
     * @param output the path where to create the postfiles
     * @throws IOException
     */
    public void Start(ReadFile readFiles, String corpusPath, Boolean steimming,String output) throws IOException {
       this.output =output;
        ArrayList<ArrayList<String>> result;
       this.stemming=steimming;
        ArrayList<String> paths = readFiles.findDocs(corpusPath);
        Parse parser = new Parse(corpusPath);
        for (String docPath : paths) {
            result=readFiles.separateDocs(docPath,steimming);
            for (ArrayList<String> text : result){
                String docName = text.get(0);
                text.remove(0);
                HashMap map = parser.Tokenizer(text, docName, steimming);
                addToEntity(parser.getEntity());
                addToText(docName, parser.getMaxtf(), parser.getSumTerms(), parser.getDocSize(),parser.getEntity());
                buildtempPost(map);
                if(n%10000==0){
                    writePostToDisk();
                }
                n++;
            }
        }
        paths=null;
        result=null;
        parser=null;
        if(tempPost.size()>=1)
            writePostToDisk();
        getAllPostForMerge();
        fixPostFileUpperLowerCase();
        cleanEntities();
       splitPostFiles();
        createFinalDictionary(this.output);
      createFinalIndex();


    }
    public void setOutput(String newOutput)
    {
        this.output=newOutput;
    }

    /** split the posting file to chunks of letters
     */
    private void splitPostFiles() throws IOException {
        BufferedReader bufferedReader=null;
        String nextLine;
        Reader reader = null;
        File folder = new File(output);
        try {
            reader = new FileReader(output +"\\finalPost.txt");
             bufferedReader=new BufferedReader(reader);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        nextLine=writeSplittedPosting(bufferedReader,"post_Num.txt",'A',"");
        nextLine=writeSplittedPosting(bufferedReader,"post_A-Z.txt",'a',nextLine);
        nextLine= writeSplittedPosting(bufferedReader,"post_a-d.txt",'e',nextLine);
        nextLine= writeSplittedPosting(bufferedReader,"post_e-h.txt",'i',nextLine);
        nextLine= writeSplittedPosting(bufferedReader,"post_i-n.txt",'o',nextLine);
        writeSplittedPosting(bufferedReader,"post_o-z.txt",' ',nextLine);
        bufferedReader.close();
        File file=new File(output+"\\finalPost.txt");
        file.delete();
    }
    /** Represents an employee.
     * @param br - bufferReader
     * @param postingName - the name of the chunk
     * @param range - range of words
     * @param line  - the line of previus chunk. we dont want to lose it
     */
        private String writeSplittedPosting(BufferedReader br, String postingName, char range,String line) throws IOException {
                BufferedWriter bufferedWriter=null;
                bufferedWriter=new BufferedWriter(new FileWriter(output+"\\"+postingName));
                if(line.equals(""))
                line=br.readLine();
                while(line!=null&&line.charAt(0)!=range) {
                    bufferedWriter.write(line);
                    bufferedWriter.newLine();
                    line = br.readLine();
                }
                bufferedWriter.close();
                return line;
        }

    /**
     * This functaion create the final index dictionary in the disk.
     * @throws IOException
     */
    private void createFinalIndex() throws IOException {
        FileWriter write=new FileWriter(output+"\\post_Documents.txt");
        Iterator it = finalTextIndex.entrySet().iterator();
        BufferedWriter buferwriter=new BufferedWriter(write);
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            buferwriter.write(pair.getKey()+";"+pair.getValue());
            buferwriter.newLine();
        }
        buferwriter.close();
    }

    /**
     * This create the final dictionary in the disk name Dictionary.txt
     * @param postingFilePath
     * @throws IOException
     */


    public void createFinalDictionary(String postingFilePath) throws IOException {
        System.out.println(postingFilePath);
        this.output=postingFilePath;
        this.finalDic=new TreeMap<>();
        FileWriter write = new FileWriter(output + "\\Dictionary.txt");
        BufferedWriter writer = new BufferedWriter(write);
        String[] postFiles={"post_Num.txt","post_A-Z.txt","post_a-d.txt","post_e-h.txt","post_i-n.txt","post_o-z.txt"};
        for(String name:postFiles) {
            FileReader reader = new FileReader(postingFilePath + "\\"+name);
            BufferedReader bufferedReader = new BufferedReader(reader);
            entirtiesDictionery = new HashMap<>();
            String line = bufferedReader.readLine();
            int sum = 0;
            while (line != null) {
                String[] s = line.split(";");
                String t = s[1];
                String[] s2 = t.split("\\|");
                for (String count : s2) {
                    String[] stringNumber = count.split("#");
                    int number = Integer.parseInt(stringNumber[0]);
                    sum = sum + number;
                }
                this.finalDic.put(s[0], sum + "," + numberOfuniqWords);
                writer.write(s[0] + ";" + sum);
                writer.newLine();
                numberOfuniqWords++;
                line = bufferedReader.readLine();
                sum = 0;
            }
            bufferedReader.close();
        }
        writer.close();
    }

    /*private void writeE() throws IOException {
        Iterator it = entirtiesDictionery.entrySet().iterator();
        FileWriter writer=new FileWriter("corpus\\entities");
        BufferedWriter buferwriter=new BufferedWriter(writer);
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            buferwriter.write(pair.getKey()+";"+pair.getValue());
            buferwriter.newLine();
        }
        buferwriter.close();
    }

    private void  readEntity() throws IOException {
        FileReader reader=new FileReader("corpus\\entities.txt");
        BufferedReader bufereader=new BufferedReader(reader);
        entirtiesDictionery=new HashMap<>();
        String line=bufereader.readLine();
        while(line!=null) {
            String[] s = line.split(";");
            entirtiesDictionery.put(s[0],Integer.parseInt(s[1]));
            line=bufereader.readLine();
        }
    }*/
    /**
     * Build a temp dictionary and write in disk with writePost to disk
     * after a 10000 doc
     * @param map
     */
    private void buildtempPost(HashMap map) {
        String value;
        if (map != null) {
            Iterator it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                if (wordForSearchInPost.contains(pair.getKey())) {

                    value = tempPost.get(pair.getKey());
                    String oldString = "" + tempPost.get(pair.getKey());
                    tempPost.put((String) pair.getKey(), oldString + "|" + ((Pair) pair.getValue()).getKey() + ((Pair) pair.getValue()).getValue());
                } else {
                    wordForSearchInPost.add((String) pair.getKey());
                    tempPost.put((String) pair.getKey(), "" + ((Pair) pair.getValue()).getKey() + ((Pair) pair.getValue()).getValue());
                }
                it.remove(); // avoids a ConcurrentModificationException
            }
        }
    }

    /**
     * This function return the location score of word
     * @param word - the word
     * @return getLocation - hashmap of string and score
     */
    public HashMap<String,Double> getLocation(String word) throws IOException {
        String line = "";
        char firstChar = word.charAt(0);
        if (firstChar >= 'A' && firstChar <= 'Z')
            line = findWord(word, output + "\\post_A-Z.txt");
        if (firstChar >= 'a' && firstChar <= 'd')
            line = findWord(word, output + "\\post_a-d.txt");
        if (firstChar >= 'e' && firstChar <= 'h')
            line = findWord(word, output + "\\post_e-h.txt");
        if (firstChar >= 'i' && firstChar <= 'n')
            line = findWord(word, output + "\\post_i-n.txt");
        if (firstChar >= 'o' && firstChar <= 'z')
            line = findWord(word, output + "\\post_o-z.txt");
        if (!Character.isLetter(firstChar))
            line = findWord(word, output + "\\post_Num.txt");
        HashMap<String,Double> locationScore=new HashMap<>();

        int number;
        String[] split1 = line.split(";");
        if (line.contains("|")) {
            String[] split2 = split1[1].split("\\|");
            for (int i = 0; i < split2.length; i++) {
                String[] split3 = split2[i].split(":");
                String[] spllit4 = split3[0].split("#");
                String[] split5 = split3[1].split(",");
                    number = Integer.parseInt(split5[0]);
                    locationScore.put(spllit4[1],(double)number / getDocLenth(spllit4[1]));
                }
            }
        else
        {
            String[] split3 = split1[1].split(":");
            String[] spllit4 = split3[0].split("#");
            String[] split5 = split3[1].split(",");
                number = Integer.parseInt(split5[0]);
            number = Integer.parseInt(split5[0]);
            locationScore.put(spllit4[1],(double)number / getDocLenth(spllit4[1]));
        }
                return locationScore;
    }
    /** Represents an employee.
     * @param word - the word that we looking for her
     * @param path - path of posting file
     */
    private String findWord(String word,String path ) throws IOException {
        FileReader rd=new FileReader(path);
        BufferedReader reader=new BufferedReader(rd);
        String line="";
        line=reader.readLine();
        while(line!=null)
        {
            String[] split=line.split(";");
            if(word.equals(split[0])) {
                reader.close();
                return line;
            }
            line=reader.readLine();
        }
        reader.close();
        return "";
    }
    /** write to post disk
     */
    private void writePostToDisk() {
        FileWriter writer = null;
        String key, value;
        try {
            if(stemming){
                if(!existDirStem) {
                    existDirStem=true;
                    File stemFile = new File(output + "\\postFilesStemm");
                    if (!stemFile.exists()) {
                        stemFile.mkdir();
                    }
                    output = output + "\\postFilesStemm";
                }
            }
            else
            {
                if(!existDirNoStem) {
                    existDirStem=true;
                    File stemFile = new File(output + "\\postFilesNoStemm");
                    if (!stemFile.exists()) {
                        stemFile.mkdir();
                        existDirNoStem = true;
                    }
                    output = output + "\\postFilesNoStemm";
                }
            }
            writer = new FileWriter(output +"\\postingFile" + n / 10000 +".txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter buffer = new BufferedWriter(writer);
        Iterator it = tempPost.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry<String, String>) it.next();
            try {
                if (Character.isUpperCase(((String) pair.getKey()).charAt(0)))
                    buffer.write(((String) pair.getKey()) + ";" + pair.getValue());
                else
                    buffer.write(pair.getKey() + ";" + pair.getValue());
                buffer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            buffer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        tempPost = new TreeMap<>();
        wordForSearchInPost = new HashSet<>();
    }

    /**
     * This function add not exsits entirties to the entities dicionary
     * @param entityPerDoc
     */
    private void addToEntity(HashMap<String,Integer> entityPerDoc) {
        String key;
        for (Map.Entry<String,Integer> entry : entityPerDoc.entrySet()) {
            key=entry.getKey();
            if (entirtiesDictionery.containsKey(key)) {
                int counter = entirtiesDictionery.get(key);
                counter=counter+1;
                entirtiesDictionery.put(key, counter);
            } else
                entirtiesDictionery.put(key, 1);
        }
    }

    /**
     * This fucntion add to value and key to the document indexer (finalTextIndex)
     * @param docName
     * @param maxTf
     * @param sumTermsUniqe
     * @param textSize
     */
    public void addToText(String docName, int maxTf, int sumTermsUniqe, int textSize,HashMap<String,Integer> entityDictonary ) {
        String key;
        int value;
        String line="";
        for (Map.Entry<String,Integer> entry : entityDictonary.entrySet()) {
            key=entry.getKey();
            value=entry.getValue();
            line=line+"|"+key+":"+value;
        }

        this.finalTextIndex.put(docName, "" + maxTf + "," + sumTermsUniqe + "," + textSize+line);

    }

    /**
     * This function finde all the temp postfile for marge
     */
    public void getAllPostForMerge() {
        int totalPostFiles = 47;
        int index = 0;
        File folder = new File(output);
        File[] listOfFiles = folder.listFiles();
        while (listOfFiles.length != 1) {
            listOfFiles = folder.listFiles();
            for (int i = 0; i <= listOfFiles.length - 2; i = i + 2) {
                try {
                    mergePostFiles(listOfFiles[i].getPath(), listOfFiles[i + 1].getPath(), index);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                listOfFiles[i].delete();
                listOfFiles[i + 1].delete();
                index++;

            }

        }
    }

    /**
     * This function delete the PostFile from path givin
     * @param output
     * @param stemOn -stem checkbox on or off
     * @return true if the file delete false else
     */

    public boolean deletePostFile(String output,boolean stemOn) {
        String folderLocation = (stemOn) ? "\\postFilesStemm" : "\\postFilesNoStemm";
        try {
            File f = new File(output + folderLocation);
            File[] files = f.listFiles();
            if (!f.exists())
                return false;
            else if (files.length == 0) {
                f.delete();
                return true;
            }
            for (File file : files) {
                file.delete();
            }
            return f.delete();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * This function mergae two postfile togther
     * @param Path1
     * @param Path2
     * @param index
     * @throws IOException
     */
    public void mergePostFiles(String Path1, String Path2, int index) throws IOException {
        FileWriter writer = null;
        FileReader reader1 = null;
        FileReader reader2 = null;
        String line1 = null;
        String line2 = null;


        try {
            writer = new FileWriter(output +"\\MergePost" + index + ".txt");
            reader1 = new FileReader(Path1);
            reader2 = new FileReader(Path2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter bufferWrite = new BufferedWriter(writer);
        BufferedReader bufferRead1 = new BufferedReader(reader1);
        BufferedReader bufferRead2 = new BufferedReader(reader2);
        line1 = bufferRead1.readLine();
        line2 = bufferRead2.readLine();
        while (line1 != null && line2 != null) {
            String[] word1 = line1.split(";");
            String[] word2 = line2.split(";");
            if (word1[0].compareTo(word2[0]) == 0) {
                bufferWrite.write(word1[0] + ";" + word1[1] + "|" + word2[1]);
                //bufferWrite.write(word1[0] + ";" + word1[1] + "|" + word2[1]);
                bufferWrite.newLine();
                line1 = bufferRead1.readLine();
                line2 = bufferRead2.readLine();
            } else if (word1[0].compareTo(word2[0]) < 0) {
                bufferWrite.write(line1);
                line1 = bufferRead1.readLine();
                bufferWrite.newLine();
            } else {
                bufferWrite.write(line2);
                bufferWrite.newLine();
                line2 = bufferRead2.readLine();
            }
        }
        if (line1 != null & line2 == null) {
            while (line1 != null) {
                bufferWrite.write(line1);
                line1 = bufferRead1.readLine();
                bufferWrite.newLine();
            }
        }
        if (line2 != null & line1 == null) {
            while (line2 != null) {
                bufferWrite.write(line2);
                bufferWrite.newLine();
                line2 = bufferRead1.readLine();
            }
        }
        bufferRead1.close();
        bufferRead2.close();
        bufferWrite.close();
    }

    /**
     * This fix the postfile after the merge
     * check if the term only appers in appers case the term key is Upper case
     * else the term will appers in dictionary in lowercase
     */
    public void fixPostFileUpperLowerCase() {
        try {
            File folder = new File(output);
            File[] listOfFiles = folder.listFiles();
            FileReader reader = new FileReader(listOfFiles[0]);
            FileWriter write = new FileWriter(output+"\\Upper.txt");
            BufferedWriter bufferWriter1 = new BufferedWriter(write);
            BufferedReader bufferRead = new BufferedReader(reader);
            String line = bufferRead.readLine();
            while (line!=null) {
                if((Character.isLowerCase(line.charAt(0))))
                    break;
                bufferWriter1.write(line);
                bufferWriter1.newLine();
                line = bufferRead.readLine();
            }
            write = new FileWriter(output+"\\Lower.txt");
            BufferedWriter bufferWriter2 = new BufferedWriter(write);
            while (line != null) {
                bufferWriter2.write(line);
                bufferWriter2.newLine();
                line = bufferRead.readLine();
            }
            bufferRead.close();
            bufferWriter1.close();
            bufferWriter2.close();
            listOfFiles[0].delete();
            mergeFixLowerAndUpper();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    /** the function return the entities
     * return getEntites
     */
    public HashMap<String,Integer> getEntites()
    {
        if(entirtiesDictionery.size()==0||entirtiesDictionery==null)
        {
            uploadEntitiesToRam();
        }
            return entirtiesDictionery;
    }
    /**
     *This function merge the lower postfile and the upper postfile togger
     *
     */
    public  void mergeFixLowerAndUpper() throws IOException {
        FileReader readerLower = null;
        int counter=0;
        ArrayList<String> oldContent = new ArrayList<>();
       // BufferedWriter bufferWriteFinal = new BufferedWriter(new FileWriter(output+"\\finalPostFile.txt", true));
        FileReader readerUpper = null;
        BufferedReader bufferReadLower = null;
        BufferedWriter bufferWriteMergeUpper=null;
        BufferedReader bufferReadUpper = null;
        BufferedWriter bufferWrite = null;
        File folder = new File(output);
        File[] listOfFiles = folder.listFiles();
        try {
            readerLower = new FileReader(output+"\\Lower.txt");
            readerUpper = new FileReader(output+"\\Upper.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            FileWriter writer = new FileWriter(output+"\\finalPostFile.txt");
            FileWriter writerMerge = new FileWriter(output+"\\mergeWithUpper.txt");
            bufferWriteMergeUpper = new BufferedWriter(writerMerge);
            bufferWrite = new BufferedWriter(writer);
            bufferReadLower = new BufferedReader(readerLower);
            bufferReadUpper = new BufferedReader(readerUpper);
        } catch (IOException e) {

        }
        String line1 = null;
        try {
            String lineLower = bufferReadLower.readLine();
            String lineUpper = bufferReadUpper.readLine();
            while (lineLower != null && lineUpper != null) {
                String[] wordLower = lineLower.split(";");
                String[] wordUpper = lineUpper.split(";");
                if(wordUpper[0].length()==0) {
                    lineUpper = bufferReadUpper.readLine();
                    wordUpper = lineUpper.split(";");
                }
                if(wordLower[0].length()==0) {
                    lineLower = bufferReadLower.readLine();
                    wordLower = lineLower.split(";");
                }
                if ((Character.isUpperCase(wordUpper[0].charAt(0)))) {
                    String upperToLower = wordUpper[0].toLowerCase();
                    String lower = wordLower[0];
                    if (upperToLower.equals(lower)) {
                        String newContent = lower + ";" + wordLower[1] + "|" + wordUpper[1];
                        bufferWriteMergeUpper.write(newContent);
                        bufferWriteMergeUpper.newLine();
                        lineLower = bufferReadLower.readLine();
                        lineUpper = bufferReadUpper.readLine();
                    } else if (lower.compareTo(upperToLower) < 0) {
                        bufferWriteMergeUpper.write(lineLower);
                        bufferWriteMergeUpper.newLine();
                        lineLower = bufferReadLower.readLine();
                    } else {
                        bufferWrite.write(wordUpper[0].toUpperCase()+";"+wordUpper[1]);
                        bufferWrite.newLine();
                        lineUpper = bufferReadUpper.readLine();
                    }
                } else {
                    bufferWrite.write(lineUpper);
                    bufferWrite.newLine();
                    lineUpper = bufferReadUpper.readLine();
                }
                counter++;
            }
            if (lineUpper != null) {
                while (lineUpper != null) {
                    String[] wordUpper = lineUpper.split(";");
                    bufferWrite.write(wordUpper[0].toUpperCase()+";"+wordUpper[1]);
                    bufferWrite.newLine();
                    lineUpper = bufferReadUpper.readLine();
                }
            }
            if (lineLower != null) {
                while (lineLower != null) {
                    bufferWriteMergeUpper.write(lineLower);
                    bufferWriteMergeUpper.newLine();
                    lineLower = bufferReadLower.readLine();
                }
            }
            bufferReadLower.close();
            bufferReadUpper.close();
            bufferWriteMergeUpper.close();
            bufferWrite.close();
            bufferWrite = new BufferedWriter(new FileWriter(output+"\\finalPostFile.txt", true));
            String line="";
            bufferReadLower=new BufferedReader(new FileReader(output+"\\mergeWithUpper.txt"));
            line=bufferReadLower.readLine();
            while(line!=null) {
                bufferWrite.append(line);
                bufferWrite.newLine();
                line=bufferReadLower.readLine();
            }
            bufferWrite.close();
            bufferReadLower.close();
            listOfFiles=folder.listFiles();
            for(int i=0; i<listOfFiles.length; i++) {
                if (listOfFiles[i].getName().equals("Upper.txt"))
                    listOfFiles[i].delete();
                if (listOfFiles[i].getName().equals("Lower.txt"))
                    listOfFiles[i].delete();
                if (listOfFiles[i].getName().equals("mergeWithUpper.txt"))
                    listOfFiles[i].delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * This function clean all entities with only one appears in the dictionary.
     *
     */
    public void cleanEntities() {
        File folder = new File(output );
        File[] listOfFiles = folder.listFiles();
        FileWriter writer = null;
        FileReader reader = null;
        FileWriter writeEntites=null;
        try {
            reader = new FileReader(listOfFiles[0]);
            writer = new FileWriter(output + "\\finalPost.txt");
            writeEntites = new FileWriter(output+"\\post_Entities.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter bufferWrite = new BufferedWriter(writer);
        BufferedWriter    bufferWriteEntites = new BufferedWriter(writeEntites);
        BufferedReader bufferRead1 = new BufferedReader(reader);
        String line;
        try {
            line = bufferRead1.readLine();
            while (line != null) {
                String[] temp = line.split(";");
                if (entirtiesDictionery.containsKey(temp[0]) && entirtiesDictionery.get(temp[0]) > 1) {
                    bufferWriteEntites.write(temp[0]+";"+entirtiesDictionery.get(temp[0]));
                    bufferWriteEntites.newLine();
                    bufferWrite.write(line);
                    bufferWrite.newLine();
                  //  bufferWriteEntites.write(""+(temp[0])+";"+entirtiesDictionery.get(temp[0]));
                 //   bufferWriteEntites.newLine();
                    line = bufferRead1.readLine();
                } else if (entirtiesDictionery.containsKey(temp[0]) && entirtiesDictionery.get(temp[0]) <= 1)
                    line = bufferRead1.readLine();
                else {
                    bufferWrite.write(line);
                    bufferWrite.newLine();
                    line = bufferRead1.readLine();
                }
            }
            bufferWriteEntites.close();
            bufferRead1.close();
            bufferWrite.close();
            listOfFiles = folder.listFiles();
            for(int i=0; i <listOfFiles.length; i++)
            {
                if(listOfFiles[i].getName().equals("finalPostFile.txt")) {
                    listOfFiles[i].delete();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This funcation load the dictionary from the disk.
     * @return the final dictionary
     */
    public TreeMap<String, String> getDic() {
        if (finalDic.size() == 0 || finalDic == null)
            uploadFindlDicToRam();
        return finalDic;
    }

//        BufferedReader reader=null;
////        try {
////            reader = new BufferedReader(new FileReader(output+"\\Dictionary.txt"));
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
////        BufferedReader bufferedReader=new BufferedReader(reader);
////        String line = null;
////        while(true) {
////            try {
////                if (!((line = reader.readLine()) != null)) break;
////            } catch (IOException e) {
////                e.printStackTrace();
////            }
////            String[] temp = line.split(";");
////            dic.put(temp[0],temp[1]);
////        }
////         this.finalDic=dic;

    private void uploadFindlDicToRam() {
        finalDic=new TreeMap<>();
        try {
            BufferedReader reader=new BufferedReader(new FileReader(output+"\\Dictionary.txt"));
            String line=reader.readLine();
            while(line!=null)
            {
                String[] wordDetails=line.split(";");
                finalDic.put(wordDetails[0],(wordDetails[1]));
                line=reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /** counts and return the number of documnts
     * @return int - return the size of the corpus
     */
    public int getCorpusSize() throws IOException {
        if(finalTextIndex.size()==0)
           uploadTextPostToRam();
            return finalTextIndex.size();
    }
    /** upload the text Posting from disk to ram
     */
    private  void uploadTextPostToRam()
    {
        finalTextIndex=new HashMap<>();
            try {
                BufferedReader reader=new BufferedReader(new FileReader(output+"\\post_Documents.txt"));
                String line=reader.readLine();
                while(line!=null)
                {
                    String[] wordDetails=line.split(";");
                    finalTextIndex.put(wordDetails[0],wordDetails[1]);
                    line=reader.readLine();
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

    }
    /** upload the entities from disk to ram
     */
    private  void uploadEntitiesToRam()
    {
        finalTextIndex=new HashMap<>();
        try {
            BufferedReader reader=new BufferedReader(new FileReader(output+"\\post_Entities.txt"));
            String line=reader.readLine();
            while(line!=null)
            {
                String[] wordDetails=line.split(";");
               entirtiesDictionery.put(wordDetails[0],Integer.parseInt(wordDetails[1]));
                line=reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    /** return the text posting
     */
    public HashMap<String,String> getFinalTextIndex(){
        if(finalTextIndex==null||finalTextIndex.size()==0)
            uploadTextPostToRam();
        return finalTextIndex;
    }
    public int getDocLenth(String docName)
    {
        if(finalTextIndex.size()==0||finalTextIndex==null)
            uploadTextPostToRam();
        int sum=0;
        String split=finalTextIndex.get(docName);
        if(split.contains("|"))
        {
            String[] split2=split.split("\\|");
            String[] split3=split2[0].split(",");
            return Integer.parseInt(split3[2]);
        }
        else {
            String[] split2=split.split(",");
            return Integer.parseInt(split2[2]);
        }
    }
    /** calculate avgdf for bm25
     */
    public double getAvgdl() {
        if (finalTextIndex.size()==0)
            uploadTextPostToRam();
        float sum = 0;
        Iterator it = finalTextIndex.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            String split = (String) pair.getValue();
            if (split.contains("|")) {
                String[] split2 = split.split("\\|");
                String[] split3 = split2[0].split(",");
                sum = sum + Integer.parseInt(split3[2]);
            } else {
                String[] split3 = split.split(",");
                sum = sum + Integer.parseInt(split3[2]);
            }
        }
        return sum / (finalTextIndex.size());
    }
    public double getMaxOfDoc(String docName) {
        String maxTF=finalTextIndex.get(docName);
        if(maxTF.contains("|")) {
            String[] max = maxTF.split("|");
            String []max2= max[0].split(",");
            return Double.parseDouble(max2[0]);
        }
        else {
            String[] max2 = maxTF.split(",");
            return Double.parseDouble(max2[0]);
        }
    }
}


