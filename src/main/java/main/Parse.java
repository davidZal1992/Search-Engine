package main;
import javafx.util.Pair;

import java.io.*;
import java.math.BigDecimal;
import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is use to parse a text and add to dictionary of term tf location
 */

public class Parse {

    AtomicInteger pointer;
    private String[] months;
    private String[] shortMonths;
    private Hashtable<String, String> stopWords;
    public HashMap<String,Integer> entityPerDoc;
    private ArrayList<String> text;
    public HashMap<String, Pair<Integer, String>> termsDictionary;
    public HashSet<String> upperCaseLetter;
    public static HashSet<String> toRemoveChars;
    private String nextWord;
    private String currWord;
    private int docID;
    private int maxtf;
    private int textSize;

    public Parse(String stopWordsPath) {
        pointer = new AtomicInteger(0);
        months = new DateFormatSymbols(Locale.ENGLISH).getMonths();
        shortMonths = new DateFormatSymbols(Locale.ENGLISH).getShortMonths();
        entityPerDoc = new HashMap<String,Integer>();
        upperCaseLetter=new HashSet<>();
        text = new ArrayList<String>();
        stopWords = new Hashtable<String, String>();
        toRemoveChars = new HashSet<String>();
        String[] toRemoveCharsTemp = {"^", "!", "?", "^", "&", "*", "#", "(", ")", ",", ";", ":", "{", "}", "--", "[", "]", "<", ">", "|", "+", "`", "'", "."};
        for (String rmv : toRemoveCharsTemp) {
            toRemoveChars.add(rmv);
        }
        stopWords = loadFromMemory(stopWordsPath+"\\stopwords.txt");
        docID = 1;
        maxtf = Integer.MIN_VALUE;
        textSize=0;


    }

    /**
     * This fucntion tookenizer (parse) each token of docment and return
     * dicrionary of the term and tf.
     * @param text
     * @param docID
     * @param stemming
     * @return
     */
    public HashMap Tokenizer(ArrayList<String> text, String docID, boolean stemming ) {
        this.text = text;
        termsDictionary = new HashMap<String, Pair<Integer, String>>();
        entityPerDoc = new HashMap<>();
        upperCaseLetter=new HashSet<>();
        maxtf = 0;
        textSize=0;
        int currentLocation = 0;
        this.pointer.set(0);
        while (pointer.get() < text.size()) {
            currWord = text.get(pointer.get());
            currentLocation = pointer.get();
            nextWord = getNextWord(text);
            if (isQuote(currWord))
                insertToDictionary(takeCareQuotes(), currentLocation, docID);
            else  if (isGMT(currWord))
                insertToDictionary(takeCareOfGMT(currWord), currentLocation, docID);
            else if (isPhoneNumber(currWord))
                insertToDictionary(takeCareOfPhone(), currentLocation, docID);
            else if (isNumeric(currWord))
                insertToDictionary(takeCareNumbers(), currentLocation, docID);
            else if (isPrecent(currWord))
                insertToDictionary(takeCarePrecent(), currentLocation, docID);
            else if (isPrice(currWord))
                insertToDictionary(takeCarePrice(), currentLocation, docID);
            else if (isRange((currWord)))
                insertToDictionary(takeCareOfRange(), currentLocation, docID);
            else if (isMonths(currWord) && (nextWord != null && nextWord.matches("\\d+")))
                insertToDictionary(takeCareDate(), currentLocation, docID);
            else if (currWord.length()>0&&Character.isUpperCase(currWord.charAt(0)) && nextWord != null && Character.isUpperCase(nextWord.charAt(0)) && !isLegalWord(currWord))
                insertToDictionary(takeCareEntities(), currentLocation, docID);
            else if (checkForStopWords(currWord))
                continue;
            else {
                insertToDictionary(currWord, currentLocation, docID);
                pointer.addAndGet(1);
                textSize++;
            }
        }
        takeCareOfUpperCase();
        return termsDictionary;
    }

    private boolean isLegalWord(String word)
    {
        if ((stopWords.containsKey(word.toLowerCase()))){
            return true;
        }
        return false;
    }
    /**
     * This function stemm word ussing porter stem
     * @see Stemmer
     * @param word
     * @return
     */
    private String stemm(String word) {
        if(word==null)
            return null;
        Stemmer stemm=new Stemmer();
        for (char c:word.toCharArray()) {
            stemm.add(c);
        }
        stemm.stem();
        stemm.stem();
        return stemm.toString();
    }

    /**
     *This function chek if the word in upperCaseLetter is  not in the lowercase dictionry
     * if it is delete from the uppercase dictionary and add to the lowecase dictonary  in the current loaction(the same term)
     */
    private void takeCareOfUpperCase() {
        for(String upperWord:upperCaseLetter)
        {
            String lowerWord=upperWord.toLowerCase();
            Pair tempUpperWord=termsDictionary.get(upperWord);
            if(termsDictionary.containsKey(lowerWord))
            {
                Pair tempLowerWord=termsDictionary.get(lowerWord);
                int lowerCaseShows=((int)tempLowerWord.getKey());
                int upperCaseShows=((int)tempUpperWord.getKey());
                String[] addToLower=((String) tempUpperWord.getValue()).split(":");
                termsDictionary.put(lowerWord,new Pair(lowerCaseShows+upperCaseShows,(String)tempLowerWord.getValue()+","+addToLower[1]));
                termsDictionary.remove(upperWord);
            }
            else {
                termsDictionary.remove(upperWord);
                termsDictionary.put(upperWord.toUpperCase(), new Pair(tempUpperWord.getKey(), tempUpperWord.getValue()));
            }

        }
    }

    /**
     * This fucntion check if the word is contian GMT
     * @param currWord
     * @return
     */
    private boolean isGMT(String currWord) {
        if (isNumeric(currWord) && currWord.length() == 4 && nextWord != null && nextWord.equals("GMT"))
            return true;
        return false;
    }
    /**
     * This fucntion take care of GMT
     * @param currWord
     * @return the time in order
     */
    private String takeCareOfGMT(String currWord) {
        this.pointer.addAndGet(2);
        textSize++;
        return Character.toString(currWord.charAt(0)) + Character.toString(currWord.charAt(1)) + ":" + Character.toString(currWord.charAt(2)) + Character.toString(currWord.charAt(3)) + " GMT";
    }

    public HashMap<String,Integer> getEntity() {
        return entityPerDoc;
    }

    private boolean startWithCapitalLetter(String word) {
        return word != null && word.matches("^[A-Z].*");
    }

    /**
     * This funcrion return true if the word inside in quote
     * @param currWord
     * @return true if is else flase
     */
    private boolean isQuote(String currWord) {
        if (currWord.startsWith("\"")&&currWord.length()>1)
            return true;
        if (currWord.endsWith("\"")) {
            text.set(this.pointer.get(), currWord.replaceAll("^\"|\"$", ""));
            this.currWord = text.get(this.pointer.get());
            return false;
        }
        return false;
    }

    /**
     * This function take care of phone number
     * @return one word of the phone number
     */

    private String takeCareOfPhone() {
        String telephone = "";
        telephone = "(" + currWord + ")" + " " + nextWord;
        this.pointer.addAndGet(2);
        return telephone;
    }

    /**
     * this function check if the word is phone number
     * @param currWord
     * @return
     */
    private boolean isPhoneNumber(String currWord) {
        String telephone = "";
        if (nextWord != null && isRange(nextWord)) {
            telephone = currWord + " " + nextWord;
            Pattern pattern = Pattern.compile("^[0-9]{3} [0-9]{3}-[0-9]{4}$");
            Matcher matcher = pattern.matcher(telephone);
            return (matcher.matches());
        }
        return false;
    }

    /**
     * This function return the nert word from the text if the text end return null
     * @param text
     * @return
     */
    private String getNextWord(ArrayList<String> text) {
        int index = 0;
        if (text.size() > (index = pointer.get() + 1)) {
            return text.get(index);
        }
        return null;
    }

    /**
     * This fucntion retrun true if the word is astop word
     * @param word
     * @return
     */
    private boolean checkForStopWords(String word) {
        if ((stopWords.containsKey(word.toLowerCase())) || word.equals("%")||word.length()==0) {
            this.pointer.addAndGet(1);
            return true;
        }
        return false;
    }

    /**
     * This function add togther words to one word of entities
     * @return
     */
    private String takeCareEntities() {
        final int numOfWords = 3;
        int i = 2;
        int pointerValue = pointer.get();
        String reslut = currWord + " " + nextWord;
        String current;
        while (i < numOfWords) {
            if (pointerValue + i < text.size()) {
                current = text.get(pointerValue + i);
                if (!startWithCapitalLetter(current))
                    break;
                reslut = reslut + " " + current;
                i++;
            } else
                break;
        }//while
        pointer.addAndGet(i);
        addToEntityPerDoc(reslut);
        textSize++;
        return reslut;
    }

    private void addToEntityPerDoc(String entity){
        int value=0;
        if(entityPerDoc.containsKey(entity)){
            value=entityPerDoc.get(entity);
            entityPerDoc.put(entity,value+1);
        }
        else {
            entityPerDoc.put(entity,1);
        }
    }
    /*public ArrayList<String> splitText(BufferedReader reader) {
        String docID;
        ArrayList tokens = new ArrayList<String>();
        String[] toRemoveCharsTemp = {"^", "!", "?", "^", "&", "*", "#", "(", ")", ",", ";", ":", "{", "}", "--", "[", "]", "<", ">", "|", "+", "`", "'", "."};
        for (String rmv : toRemoveCharsTemp) {
            toRemoveChars.add(rmv);
        }
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
             //   line = line.replace("\"", "");
                String[] words = line.split(" ");
                for (String word : words) {
                    word = cleanWord(word);
                    if (word.length() >= 1)
                        tokens.add(word);
                }

            }

        }
        return tokens;
    }*/

    /**
     * This function clean the word from symbals, coma and etc..
     * @param word
     * @return the clean word
     */
    public static String cleanWord(String word) {
        if(word.equals("$"))
            word="";
        if (word.length() < 1)
            return word;
        if (word.length() >= 2 && word.charAt(word.length() - 1) == '`' && word.charAt(word.length() - 2) == '`')
            word = word.replace("'", "");
        else {
            if (word.length() >= 2 && word.charAt(word.length() - 1) == '\'' && word.charAt(word.length() - 2) == '\'')
                word = word.replace("'", "");
            if (word.length() >= 2 && word.charAt(0) == '\'' && word.charAt(1) == '\'')
                word = word.replace("'", "");
        }
        if (word.length() >= 2 && word.charAt(0) == '<' && word.charAt(1) == '/') {
            word = word.replace("<", "");
            word = word.replace(">", "");
            word = word.replace("/", "");
        }
        if ((word.length()>=1)&&(toRemoveChars.contains(Character.toString(word.charAt(0))) || Character.toString(word.charAt(0)).equals("%") || Character.toString(word.charAt(0)).equals("/") ||  Character.toString(word.charAt(0)).equals(".")||Character.toString(word.charAt(0)).equals("\"")))
            word = word.substring(1, word.length());
        if ((word.length()>=1)&&(toRemoveChars.contains( Character.toString(word.charAt(word.length()-1)))))
            word = word.substring(0, word.length() - 1);
        return word;
    }

    /**
     * Load the Stopwords from the memory
     * @param path
     * @return hashtable of stopword
     */
    private Hashtable<String, String> loadFromMemory(String path) {
        String line = "";
        Hashtable<String, String> tempArray = new Hashtable<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            while (true) {
                try {
                    if (!((line = reader.readLine()) != null)) break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                tempArray.put(line, "");
            }

        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return tempArray;
    }

    //======================================================================================================================

    /**
     * This fucntion check the format of the date and change it to the defalut format
     * @return string in the defalut format
     */
    private String takeCareDate() {
        String dateString = "";
        String nextWord = text.get(pointer.get() + 1);
        String word = text.get(pointer.get());
        String dateFormat = "";
        String newFormat = "";
        dateString = word + " " + nextWord;
        if (nextWord.matches("\\d+") && Float.parseFloat(nextWord) > 1000) {
            dateFormat = "MMM yyyy";
            newFormat = "yyyy-MM";
        } else {
            dateFormat = "MMM dd";
            newFormat = "MM-dd";
        }
        this.pointer.addAndGet(2);
        textSize++;
        return dateParser(dateString, dateFormat, newFormat);
    }
    ///do nothing not a date

    /**
     * This function take care of quoets and return a clean word
     * @return
     */
    private String takeCareQuotes() {
        //search the end of the quotes
        text.set(this.pointer.get(), currWord.replaceAll("^\"|\"$", ""));
        int index = pointer.get()+1;
        int i = 0;
        final int numberOfWords = 4;//number of words inside a quotes
        String tempString;
        String reslut = currWord;
        reslut = reslut.replaceAll("^\"|\"$", "");
        while (index < text.size() && i < numberOfWords) {
            tempString = text.get(index);
            if (tempString.endsWith("\"")) {
                String endQuote = tempString.replaceAll("^\"|\"$", "");
                text.set(index, endQuote.replaceAll("^\"|\"$", ""));
                reslut = reslut + " " + endQuote;
                i++;
                break;
            } else {
                reslut = reslut + " " + tempString;
                index++;
                i++;
            }
        }
        this.pointer.addAndGet(i + 1);
        reslut = cleanWord(reslut);
        textSize++;
        return reslut;
    }

    /**
     * This fucntion take care of all the  scenarios that
     * we ask to take care in the task for numbers
     * @return the word after the parse
     */
    private String takeCareNumbers() {
        double parseOriginalNumber;
        String original2Word = "";
        String original3Word = "";
        String original4Word = "";
        String originalNumber = text.get(this.pointer.get());
        String tempOriginalNumber = originalNumber;

        tempOriginalNumber = tempOriginalNumber.replace(",", "");
        parseOriginalNumber = Double.parseDouble(tempOriginalNumber);
        if (checkIfDecimalNum(parseOriginalNumber)) { //cut all number that come after 3 numbers after point
            parseOriginalNumber = cut3NumbersDecimel(parseOriginalNumber);
            originalNumber = "" + parseOriginalNumber;
        } else originalNumber = "" + (int) parseOriginalNumber;

        if (checkIfSizeLegal(text.size(), this.pointer.get() + 1))
            original2Word = text.get(this.pointer.get() + 1);
        if (checkIfSizeLegal(text.size(), this.pointer.get() + 2))
            original3Word = text.get(this.pointer.get() + 2);
        if (checkIfSizeLegal(text.size(), this.pointer.get() + 3))
            original4Word = text.get(this.pointer.get() + 3);

        if (parseOriginalNumber >= 1000 && parseOriginalNumber < 1000000 || original2Word.equals("Thousand") || original2Word.equals("thousand"))   // Pattern : Number Thousand or number above 1000
            originalNumber = ThousandPattern(parseOriginalNumber);
        else if (parseOriginalNumber >= 1000000 && parseOriginalNumber < 1000000000 || original2Word.equals("Million") || original2Word.equals("million") && !(original3Word.equalsIgnoreCase("U.S")))   // Pattern : Number Million or number above 1,000,000
            originalNumber = millionPattern(parseOriginalNumber);
        else if (parseOriginalNumber >= 1000000000 || original2Word.equalsIgnoreCase("billion") && !(original3Word.equalsIgnoreCase("u.s")))  // Pattern : Number Billion or number above 1,000,000,000
            originalNumber = billionPattern(parseOriginalNumber);
        else if (isFraction(original2Word)) { //check pattern : number fraction
            originalNumber = fractionPattern(original2Word, original3Word, originalNumber, parseOriginalNumber);
        } else if (parseOriginalNumber % 1 == 0 && parseOriginalNumber >= 1 && parseOriginalNumber <= 31 && isMonths(original2Word)) {
            pointer.addAndGet(2);
            return dateParser(originalNumber + " " + original2Word, "dd MMM", "MM-dd");
        } else if (!original2Word.toLowerCase().equals("dollars") && !(original2Word.toLowerCase().equals("percent"))&&!(original2Word.toLowerCase().equals("percentage")) && (!(original3Word.equalsIgnoreCase("u.s")) || !(original4Word.equalsIgnoreCase("dollars"))))
            this.pointer.addAndGet(1); // number under 1000
        if (original2Word.equalsIgnoreCase("percent") || original2Word.equalsIgnoreCase("percentage")) { //number precent pattern
            originalNumber = originalNumber + "%";
            this.pointer.addAndGet(2);
        }
        if (original2Word.equalsIgnoreCase("Dollars")) // number dollars pattern or number U.S. dollars pattern
            originalNumber = dollarPattern(originalNumber);
        else if (original3Word.equalsIgnoreCase("u.s") && original4Word.equalsIgnoreCase("dollars"))
            originalNumber = dollarPatternComplex(original2Word, originalNumber, parseOriginalNumber);
        textSize++;
        return originalNumber;
    }


    /**
     * This fucntion take care of all the  scenarios that
     * we ask to take care in the task for price
     * @return the word after the parse
     */
    private String takeCarePrice() { //take care all patterns including $Price that above1000000
        double parseOriginalNumber = 0;
        boolean isDecimal = false;
        String originalString = text.get(this.pointer.get());
        String original2Word = "";
        String clearNumber = "";

        clearNumber = clearNumberFromSigns(originalString);
        clearNumber = clearNumber.replace(",", "");
        parseOriginalNumber = Double.parseDouble(clearNumber);
        isDecimal = checkIfDecimalNum(parseOriginalNumber);
        if (isDecimal) {
            parseOriginalNumber = cut3NumbersDecimel(parseOriginalNumber);
            clearNumber = "" + parseOriginalNumber;
        } else
            clearNumber = "" + (int) parseOriginalNumber;
        if (checkIfSizeLegal(text.size(), this.pointer.get() + 1))
            original2Word = text.get(this.pointer.get() + 1);

        if (original2Word.equals("million"))
            originalString = priceMillion(clearNumber);
        else if (originalString.charAt(originalString.length() - 1) == 'm' && (original2Word.equals("dollars") || original2Word.equals("Dollars")))
            originalString = priceMDollarsPattern(clearNumber, originalString);
        else if ((originalString.length() >= 3 && originalString.substring(originalString.length() - 3, originalString.length() - 1).equals("bn") && original2Word.equals("dollars") || original2Word.equals("Dollars")) || original2Word.equals("billion"))
            originalString = priceBnDollarsPattern(parseOriginalNumber, clearNumber);
        else if (parseOriginalNumber >= 1000000 && parseOriginalNumber < 1000000000)  // $price Pattern
            originalString = $PricePattern(parseOriginalNumber, clearNumber);
        else {
            originalString = clearNumberFromSigns(originalString) + " Dollars";
            this.pointer.addAndGet(1);
        }
        // push to dictionary//
        textSize++;
        return originalString;
    }

    /**
     * This fucntion take care of all the  scenarios that
     * we ask to take care in the task for number in precent
     * @return the word after the parse
     */
    private String takeCarePrecent() { // take care all patterns including %Number
        String originalPercent = text.get(this.pointer.get());
        this.pointer.addAndGet(1);
        //retur push to dictionary//
        textSize++;
        return originalPercent;
    }
    /**
     * This fucntion take care of all the scenarios that
     * we ask to take care in the task for range
     * @return the word after the parse
     */
    private String takeCareOfRange() {

        String originalTerm = text.get(this.pointer.get());
        ArrayList<String> tempList = new ArrayList<>();
        String newTokenizerString = "";
        if (originalTerm.contains("-")) {
            String[] terms = originalTerm.split("-");
            if (terms.length == 1) {
                this.pointer.addAndGet(1);
                return terms[0];
            }
            for (int i = 0; i < terms.length; i++) {
                newTokenizerString = newTokenizerString + "" + terms[i] + "-";
            }
            this.pointer.addAndGet(1);
            newTokenizerString = newTokenizerString.substring(0, newTokenizerString.length() - 1);
        } else {
            String termRange1 = "";
            String termRange2 = "";
            if (checkIfSizeLegal(text.size(), this.pointer.get() + 1))
                termRange1 = text.get(this.pointer.get() + 1);
            if (checkIfSizeLegal(text.size(), this.pointer.get() + 1))
                termRange2 = text.get(this.pointer.get() + 3);
            this.pointer.addAndGet(1);
            if (isNumeric(termRange1)) {
                termRange1 = takeCareNumbers();
            }
            this.pointer.addAndGet(1);
            if (isNumeric(termRange2)) {
                termRange2 = takeCareNumbers();
            }
            newTokenizerString = termRange1+ "-" + termRange2;
            text.add(termRange1);
            text.add(termRange2);
            textSize=textSize-2;
        }
        textSize++;
        return newTokenizerString;
    }
    /*Checks the type of term functions:
===================================================================================================================*/

    /**
     * This funcation return true if the word is number
     * @param str
     * @return
     */
    private boolean isNumeric(String str) {
        try {
            if (str.contains("d") || str.contains("f"))
                return false;
            double i = Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Thhis function return true if the word is two number and '/' between
     * @param str
     * @return
     */
    private boolean isFraction(String str) {
        if (!str.contains("/"))
            return false;
        String[] numbers = str.split("/");
        if (numbers.length > 1 && isNumeric(numbers[0]) && isNumeric(numbers[1]))
            return true;
        return false;
    }

    /**
     * This function return true if the word contains % and number
     * @param str
     * @return
     */
    private boolean isPrecent(String str) {
        if (str.length() <= 1)
            return false;
        if (str.charAt(str.length() - 1) == '%' && isNumeric(str.substring(0, str.length() - 1)))
            return true;
        return false;
    }

    /**
     * This function return true if the word start with '$' or end with "bn"  or "m""
     * @param str word
     * @return
     */
    private boolean isPrice(String str) {
        if (str.length() > 1 && (str.charAt(0) == '$' && isNumeric(str.substring(1, str.length())) || str.charAt(str.length() - 1) == 'm' && isNumeric(str.substring(0, str.length() - 1)) || str.substring(str.length() - 2, str.length()).equals("bn") && isNumeric(str.substring(0, str.length() - 2))))
            return true;
        return false;
    }

    /**
     * This function return true if the token is rnage
     * @param str
     * @return boolean
     */
    private boolean isRange(String str) {
        if (str.contains("-")) {
            String[] terms = str.split("-");
            if (terms.length >= 1)
                return true;
            else
                return false;
        } else if (str.equalsIgnoreCase("between")) {
            if (this.pointer.get() + 3 >= text.size())
                return false;
            String range1 = text.get(this.pointer.get() + 1);
            String range2 = text.get(this.pointer.get() + 2);
            String range3 = text.get(this.pointer.get() + 3);
            if (range1 != null && range2 != null && range2.equals("and") && isNumeric(range1) && isNumeric(range3))
                return true;
        }
        return false;
    }

    /**
     * This function return true if the word is a month or short month(example: oct)
     * else flase
     * @param word
     * @return boolean
     */
    private boolean isMonths(String word) {
        boolean flag1, flag2;
        if (word.equalsIgnoreCase("June")) {
            int i = 0;
        }
        flag1 = false;
        flag2 = false;
        for (int i = 0; i < 12 && !(flag1 || flag2); i++) {
            flag1 = word.equalsIgnoreCase(months[i]);
            flag2 = word.equalsIgnoreCase(shortMonths[i]);
        }
        return flag1 || flag2;

//        boolean flag=false;
//        String nextWord="";
//        if(text.size()>pointer.get()+1)
//            nextWord=text.get(pointer.get()+1);
//        else
//            return false;
//        for(int i=0;i<12||flag;i++){
//            flag=(word.equalsIgnoreCase(months[i])|| word.equalsIgnoreCase(shortMonths[i]));
//        }
//        if(flag&&isNumeric(nextWord)&&!(isRange(nextWord))&&!(nextWord.contains(".")))
//            return true;
//        return false;
    }


    /*Useful functions
================================================================================================================================*/

    /**
     * This function return  only there number after the decimal
     * @param parseOriginalNumber
     * @return
     */
    private double cut3NumbersDecimel(double parseOriginalNumber) {
        String originalNumber = "";
        if (parseOriginalNumber % 1 != 0) // if decimal
            parseOriginalNumber = new BigDecimal(String.valueOf(parseOriginalNumber)).setScale(3, BigDecimal.ROUND_FLOOR).doubleValue();
        return parseOriginalNumber;
    }

    /**
     * This function check if the nuber is a decinal number
     * @param number
     * @return  true if is else false
     */
    private boolean checkIfDecimalNum(double number) {
        if (number % 1 != 0)
            return true;
        return false;

    }

    /**
     * This function return clean number from sign
     * @param numberWithSign
     * @return
     */
    private String clearNumberFromSigns(String numberWithSign) {
        String clearNumber = numberWithSign;
        if (numberWithSign.contains("$"))
            clearNumber = numberWithSign.substring(1, numberWithSign.length());
        else if (numberWithSign.contains("m"))
            clearNumber = numberWithSign.substring(0, numberWithSign.length() - 1);
        else if (numberWithSign.contains("bn"))
            clearNumber = numberWithSign.substring(0, numberWithSign.length() - 2);
        numberWithSign = clearNumber;
        return numberWithSign;
    }

    /**
     * This function check if the index is inside the length
     * @param length
     * @param currIndex
     * @return true if is and else false
     */
    private Boolean checkIfSizeLegal(int length, int currIndex) {
        if (currIndex >= length)
            return false;
        return true;
    }

    /**
     * This function use SimpleDataFormat and parse date from old format to new format
     * @param dateString
     * @param dateFormat
     * @param newFormat
     * @return the date in the new format
     */
    private String dateParser(String dateString, String dateFormat, String newFormat) {
        final Locale locale = Locale.ENGLISH;
        String newDateString = "";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, locale);
        Date d = null;
        try {
            d = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        sdf.applyPattern(newFormat);
        newDateString = sdf.format(d);
        return newDateString;
    }

       /*Patern Function parsing
   ===================================================================================================================*/


    //takecare of $Price

    /**
     * This function parse price number
     * @param parseOriginalNumber
     * @param clearNumber
     * @return the clean number
     */
    private String $PricePattern(double parseOriginalNumber, String clearNumber) {
        parseOriginalNumber = parseOriginalNumber / 1000000;
        clearNumber = "" + parseOriginalNumber;
        if (checkIfDecimalNum(parseOriginalNumber))
            clearNumber = "" + parseOriginalNumber + " M Dollars";
        else
            clearNumber = "" + (int) parseOriginalNumber + " M Dollars";
        this.pointer.addAndGet(1);
        return clearNumber;
    }

    // take care of  price bn dollars pattern

    /**
     * take care of  price bn dollars pattern
     * @param parseOriginalNumber
     * @param clearNumber
     * @return
     */
    private String priceBnDollarsPattern(double parseOriginalNumber, String clearNumber) {
        parseOriginalNumber = parseOriginalNumber * 1000;
        clearNumber = "" + parseOriginalNumber;
        if (checkIfDecimalNum(parseOriginalNumber))
            clearNumber = clearNumber + " M Dollars";
        else
            clearNumber = "" + (int) parseOriginalNumber + " M Dollars";
        this.pointer.addAndGet(2);
        return clearNumber;
    }

    // Take care pf price m Dollars

    /**
     * Take care of price m Dollars
     * @param clearNumber
     * @param originalString
     * @return
     */
    private String priceMDollarsPattern(String clearNumber, String originalString) {
        clearNumber = originalString.substring(0, originalString.length() - 1) + " M Dollars";
        this.pointer.addAndGet(2);
        return clearNumber;
    }

    /**
     * Take care of $Price million
     * @param clearNumber
     * @return
     */
    private String priceMillion(String clearNumber) { // Take care of $Price million
        clearNumber = clearNumber + " M Dollars";
        this.pointer.addAndGet(2);
        return clearNumber;
    }

    /**
     * take care of trillion/billion/million U.S dollars Pattern
     * @param original2Word
     * @param originalNumber
     * @param parseOriginalNumber
     * @return
     */
    private String dollarPatternComplex(String original2Word, String originalNumber, double parseOriginalNumber) {
        if (original2Word.equals("billion")) {
            parseOriginalNumber = parseOriginalNumber * 1000;
            originalNumber = "" + parseOriginalNumber;
            if (checkIfDecimalNum(parseOriginalNumber))
                originalNumber = "" + parseOriginalNumber + " M Dollars";
            else
                originalNumber = "" + (int) parseOriginalNumber + " M Dollars";
        }
        if (original2Word.equals("million")) {
            if (checkIfDecimalNum(parseOriginalNumber))
                originalNumber = "" + parseOriginalNumber + " M Dollars";
            else
                originalNumber = "" + (int) parseOriginalNumber + " M Dollars";
        }
        if (original2Word.equals("trillion")) {
            parseOriginalNumber = parseOriginalNumber * 1000000;
            originalNumber = "" + parseOriginalNumber;
            if (checkIfDecimalNum(parseOriginalNumber))
                originalNumber = "" + parseOriginalNumber + " M Dollars";
            else
                originalNumber = "" + (int) parseOriginalNumber + " M Dollars";
        }
        this.pointer.addAndGet(4);
        return originalNumber;
    }

    /**
     * take care of number dollars pattern
     * @param originalNumber
     * @return
     */
    private String dollarPattern(String originalNumber) {
        if (originalNumber.contains("M"))
            originalNumber = originalNumber.replace("M", " M");
        originalNumber = originalNumber + " Dollars";
        this.pointer.addAndGet(2);
        return originalNumber;
    }

    /**
     * takecare of fractions pattern : x/y dollars
     * @param original2Word
     * @param original3Word
     * @param originalNumber
     * @param parseOriginalNumber
     * @return
     */
    //takecare of fractions pattern : x/y dollars
    private String fractionPattern(String original2Word, String original3Word, String originalNumber, double parseOriginalNumber) {
        originalNumber = originalNumber + " " + original2Word;
        if (original3Word.equals("Dollars") || original3Word.equals("dollars") && parseOriginalNumber < 1000000) {
            this.pointer.addAndGet(3);
            if (originalNumber.contains(("K")))
                originalNumber = originalNumber.replace("K", "");
            originalNumber = originalNumber + " Dollars";
        } else
            this.pointer.addAndGet(2);
        return originalNumber;
    }

    /**
     * check if number is aboke 1000000000 so add him B
     * @param parseOriginalNumber
     * @return
     */
    private String billionPattern(double parseOriginalNumber) {            //check if number is aboke 1000000000 so add him B
        String originalNumber = "";
        if (parseOriginalNumber >= 1000000000) {
            parseOriginalNumber = parseOriginalNumber / 1000000000;
            this.pointer.addAndGet(1);
        } else
            this.pointer.addAndGet(2);
        if (checkIfDecimalNum(parseOriginalNumber)) {
            parseOriginalNumber = cut3NumbersDecimel(parseOriginalNumber);
            originalNumber = "" + parseOriginalNumber + "B";
        } else
            originalNumber = "" + (int) parseOriginalNumber + "B";
        return originalNumber;
    }

    /**
     * check if number is aboke 1000000 so add him M
     * @param parseOriginalNumber
     * @return
     */
    private String millionPattern(double parseOriginalNumber) {      //
        String originalNumber = "";
        if (parseOriginalNumber >= 1000000 && parseOriginalNumber < 1000000000) {
            parseOriginalNumber = parseOriginalNumber / 1000000;
            if(!( nextWord!=null&& nextWord.toLowerCase().equals("dollars")))
               this.pointer.addAndGet(1);
        } else
            this.pointer.addAndGet(2);
        if (checkIfDecimalNum(parseOriginalNumber)) {
            parseOriginalNumber = cut3NumbersDecimel(parseOriginalNumber);
            originalNumber = "" + parseOriginalNumber + "M";
        } else
            originalNumber = "" + (int) parseOriginalNumber + "M";

        return originalNumber;
    }

    /**
     * This function check if number is aboke 1000 so add him K
     * @param parseOriginalNumber
     * @return
     */
    private String ThousandPattern(double parseOriginalNumber) {         //check if number is aboke 1000 so add him K
        String originalNumber = "";
        if (parseOriginalNumber >= 1000 && parseOriginalNumber < 1000000) {
            parseOriginalNumber = parseOriginalNumber / 1000;
            this.pointer.addAndGet(1);
        } else
            this.pointer.addAndGet(2);

        if (checkIfDecimalNum(parseOriginalNumber)) {
            parseOriginalNumber = cut3NumbersDecimel(parseOriginalNumber);
            originalNumber = "" + parseOriginalNumber;
        } else
            originalNumber = "" + (int) parseOriginalNumber;

        return originalNumber = originalNumber + "K";
    }

    /**
     * This function insert to the dictionary
     * @param word
     * @param location
     * @param docID
     */
    private void insertToDictionary(String word, int location, String docID) {
        if(word.length()==0)
            return;
        if(word.length()>=2&&word.charAt(word.length()-1)=='.')
            word=word.substring(0,word.length()-1);
        if(Character.isUpperCase(word.charAt(0)))
            upperCaseLetter.add(word);
        if (termsDictionary.containsKey(word)) {
            if (termsDictionary.get(word).getKey() + 1 > maxtf)
                maxtf = termsDictionary.get(word).getKey() + 1;
            termsDictionary.put(word, new Pair(termsDictionary.get(word).getKey() + 1, termsDictionary.get(word).getValue() + "," + location));
        } else {
            if (1 > maxtf)
                maxtf = 1;
            termsDictionary.put(word, new Pair<>(1, "#" + docID + ":" + location));
        }
    }

    public int getMaxtf() {
        return maxtf;
    }

    public int getSumTerms() {
        return termsDictionary.size();
    }
    public int getDocSize()
    {
        return textSize;
    }
}
