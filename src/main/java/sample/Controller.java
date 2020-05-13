package sample;


import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.ImageCursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javafx.util.Callback;
import javafx.util.Pair;
import main.Indexer;
import main.Ranker;
import main.ReadFile;
import main.Searcher;


import javax.swing.plaf.metal.MetalIconFactory;
import java.io.*;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Controller implements  Initializable {
    @FXML public javafx.scene.control.CheckBox checkBox_Stemming;
    @FXML public javafx.scene.control.CheckBox checkBox_Semantic;
    @FXML public javafx.scene.control.Button btn_start;
    @FXML public javafx.scene.control.Button btn_set_deafult;
    @FXML public javafx.scene.control.Button btn_browse_Corpus;
    @FXML public javafx.scene.control.Button btn_browse_postfile;
    @FXML public javafx.scene.control.Button btn_reset;
    @FXML public javafx.scene.control.TextField tf_postfilePath;
    @FXML public javafx.scene.control.TextField tf_results;
    @FXML public javafx.scene.control.TextField tf_corpusPath;
    @FXML public javafx.scene.control.Button btn_dictionary;
    @FXML public javafx.scene.control.Button btn_run;
    @FXML public javafx.scene.control.Button btn_browse_queries;
    @FXML public javafx.scene.control.TextField tf_queries_location;
    @FXML public javafx.scene.control.TextField tf_query;
    @FXML public javafx.scene.control.Button btn_browse_results;
    @FXML public javafx.scene.control.Button btn_showEntities;
    @FXML public javafx.scene.control.TextArea output;
    @FXML public ListView entities;

    ArrayList<String> cities = new ArrayList<>();
    ObservableList<String> list = FXCollections.observableArrayList(cities);
    private Stage mainStage;
    private Indexer indexer=new Indexer();
    private ReadFile rf;
    private Searcher search;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    /**
     * Show openfile windows
     * @param event
     * @return
     */
    public String openFolder(ActionEvent event) throws IOException {
            while(true) {
                DirectoryChooser chooser = new DirectoryChooser();
                File selectedDirectory = chooser.showDialog(new Stage());
                if (selectedDirectory == null)
                    return null;
                return selectedDirectory.getPath();
            }
    }
    public String openFile(ActionEvent event) throws IOException {
        while(true) {
            FileChooser chooser = new FileChooser();
            File selectedDirectory = chooser.showOpenDialog(new Stage());
            if (selectedDirectory == null)
                return null;
            return selectedDirectory.getPath();
        }
    }
    public void setQueries(ActionEvent event){
        try {
            String postfilePath = openFile(event);
            if(postfilePath!=null)
                tf_queries_location.textProperty().setValue(postfilePath);
            if(!tf_queries_location.getText().isEmpty())
                tf_query.setDisable(true);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void setPostfilePath(ActionEvent event) {
            try {
                String postfilePath = openFolder(event);
                if(postfilePath!=null)
                    tf_postfilePath.textProperty().setValue(postfilePath);

        }catch (IOException e){
            e.printStackTrace();
        }

    }

    public void setCorpusPath(ActionEvent event) {
        String corpusPath= null;
        try {
            corpusPath = openFolder(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(corpusPath!=null)
         tf_corpusPath.textProperty().setValue(corpusPath);
    }
    public void setResults(ActionEvent event) {
        try {
                String postfilePath = openFolder(event);
            if(postfilePath!=null)
                tf_results.textProperty().setValue(postfilePath);
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    public void startIndex(ActionEvent event) {
        String corpusPath = tf_corpusPath.textProperty().getValue();
            File f = new File(corpusPath + "\\stopwords.txt");
            if(!f.exists())
            {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Problem");
                alert.setHeaderText("Look, an Information Dialog");
                alert.setContentText("stop words doesn't exist");
                alert.showAndWait();
                tf_corpusPath.clear();
                return;
            }
        String postfilePath = tf_postfilePath.textProperty().getValue();
        if (corpusPath.length() > 0 && postfilePath.length() > 0) {
            long startTime = System.currentTimeMillis();
            indexer = new Indexer();
            indexer.n=1;
            rf = new ReadFile();
            try {
                indexer.Start(rf, corpusPath, checkBox_Stemming.isSelected(), postfilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            File toSaveStopWord;
            if(checkBox_Stemming.isSelected())
            toSaveStopWord= new File(postfilePath+"\\postFilesStemm\\stopwords.txt");
            else
                toSaveStopWord= new File(postfilePath+"\\postFilesNoStemm\\stopwords.txt");
            if(!toSaveStopWord.exists()) {
                File stopWords = new File(corpusPath + "\\stopwords.txt");
                if (stopWords.exists()) {
                    try {
                        Files.copy(stopWords.toPath(), toSaveStopWord.toPath());
                    } catch (IOException ex) {
                    }
                }
            }
            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;
            long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText("Look, an Information Dialog");
            alert.setContentText("Number of documents:" + indexer.n + "\n"
                    + "Number of uniq terms:" + Indexer.numberOfuniqWords+ "\n" + "Running time: " + seconds+ " seconds\n");
            btn_dictionary.setDisable(false);
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Problem");
            alert.setHeaderText("Look, an Information Dialog");
            alert.setContentText("The path are empty or both not. please insert only one path postingfile or corpus");

            alert.showAndWait();
        }
    }

    /**
     * reset the index and delete the post file
     * @param event
     */
    public void resetIndexer(ActionEvent event) {
        Boolean rsultDelelte=false;
        Boolean isDelete=indexer.deletePostFile(tf_postfilePath.textProperty().getValue(),checkBox_Stemming.isSelected());
        Boolean isDeleteAfterQuery=false;
        File afterQ;
        String path=tf_postfilePath.textProperty().getValue();
        if(path.contains("postFilesStemm")||path.contains("postFilesNoStemm")) {
          if(path.contains("postFilesStemm")) {
              path = path.replace("postFilesStemm", "");
              isDeleteAfterQuery = indexer.deletePostFile(path,true);
          }
          else {
              path = path.replace("postFilesNoStemm", "");
              isDeleteAfterQuery = indexer.deletePostFile(path,false);
          }
        }
            isDelete=isDelete||isDeleteAfterQuery;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        if(!tf_results.getText().isEmpty())
           {
               File file = new File(tf_results.getText()+"\\results.txt");
               if(file.exists()) {
                   file.delete();
                   rsultDelelte = true;
               }
               else
                   rsultDelelte=true;
           }
        if(isDelete&&rsultDelelte){
            alert.setContentText("The dictionary,postingfiles and result deleted");
            indexer = new Indexer();
            rf = null;
        }
        else if(isDelete){
            alert.setContentText("The dictionary and postingfiles  deleted");
            indexer = new Indexer();
            rf = null;
        }
        else if(rsultDelelte)
        {
            alert.setContentText("The results deleted");
            indexer = new Indexer();
        }
        else {
            alert.setContentText("The files are not found");
        }
        alert.setTitle("Information");
        alert.setHeaderText("Look, an Information Dialog");
        alert.showAndWait();
        tf_postfilePath.textProperty().setValue("");
        tf_corpusPath.textProperty().setValue("");
    }

    /**
     * Show the dictionary in table view term and Tf
     * @param event
     */

    public void setDefault(ActionEvent event) {
        tf_queries_location.setDisable(false);
        tf_query.setDisable(false);
        tf_query.clear();
        tf_queries_location.clear();
        tf_results.clear();
        tf_postfilePath.clear();
        tf_corpusPath.clear();
        output.clear();
        entities.getItems().clear();
    }
    public void showDictionary(ActionEvent event) {
        if (indexer != null) {
            Map<String, String> map=indexer.getDic();
            TableColumn<Map.Entry<String, String>, String> columnTerm = new TableColumn<>("Term");
            columnTerm.setMinWidth(300);
            columnTerm.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map.Entry<String, String>, String>, ObservableValue<String>>() {

                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<Map.Entry<String, String>, String> p) {
                    // this callback returns property for just one cell, you can't use a loop here
                    // for first column we use key
                    return new SimpleStringProperty(p.getValue().getKey());
                }
            });
            columnTerm.setSortType(TableColumn.SortType.ASCENDING);
            TableColumn<Map.Entry<String, String>, String> columnTf = new TableColumn<>("Tf");
            columnTf.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map.Entry<String, String>, String>, ObservableValue<String>>() {

                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<Map.Entry<String, String>, String> p) {
                    // for second column we use value
                    String tfAndPostfile=p.getValue().getValue();
                    String[] temp=tfAndPostfile.split(",");
                    return new SimpleStringProperty(temp[0]);
                }
            });
            columnTf.setMinWidth(100);
            ObservableList<Map.Entry<String, String>> items = FXCollections.observableArrayList(map.entrySet());
            TableView<Map.Entry<String, String>> table = new TableView<>(items);
            table.getColumns().setAll(columnTerm, columnTf);
            Stage s = new Stage();
            Scene scene = new Scene(table, 400, 400);
            s.setScene(scene);
            s.showAndWait();
        }
        else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Problem");
            alert.setHeaderText("Look, an Information Dialog");
            alert.setContentText("The dictionary is empty");
            alert.showAndWait();

        }
    }

    /**
     * load the dictionary from postfile path
     * @param actionEvent
     */

    public void loadDictionary(ActionEvent actionEvent) {
        String postfilePath=tf_postfilePath.textProperty().getValue();
        if(postfilePath.length()>0){
            indexer = new Indexer();
            try {
                indexer.createFinalDictionary(postfilePath);
                btn_dictionary.setDisable(false);
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Problem");
                alert.setHeaderText("Look, an Information Dialog");
                alert.setContentText("There is not posting files path");
                alert.showAndWait();
            }
        }else{
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Problem");
            alert.setHeaderText("Look, an Information Dialog");
            alert.setContentText("There is not posting files path");
            alert.showAndWait();
        }
    }

    public void runSearch(ActionEvent actionEvent) throws Exception {
       /* tf_postfilePath.setText("D:\\tes\\postFilesNoStemm");
        tf_results.setText("C:\\Users\\David\\Desktop\\Web developer course");
        tf_queries_location.setText("C:\\Users\\David\\Desktop\\Web developer course\\queries.txt");*/
        if((!tf_postfilePath.textProperty().getValue().contains("postFilesStemm"))&&(!tf_postfilePath.textProperty().getValue().contains("postFilesNoStemm")))
        {
            if(checkBox_Stemming.isSelected())
                tf_postfilePath.textProperty().set(tf_postfilePath.textProperty().getValue()+"\\postFilesStemm");
            else
                tf_postfilePath.textProperty().set(tf_postfilePath.textProperty().getValue()+"\\postFilesNoStemm");
        }
        String postFilesoutput=tf_postfilePath.getText();
        File stopWordsExits=new File(postFilesoutput+"\\stopwords.txt");
        if(!stopWordsExits.exists())
        {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Problem");
                alert.setHeaderText("Look, an Information Dialog");
                alert.setContentText("stop words doesn't exist");
                alert.showAndWait();
                tf_corpusPath.clear();
                return;
        }
        String stopWords= tf_postfilePath.textProperty().getValue();
        Searcher searcher;
        if(tf_postfilePath.textProperty().getValue().equals("")||tf_results.textProperty().getValue().equals("")||(tf_queries_location.textProperty().getValue().equals("")&&tf_query.textProperty().getValue().equals("")))
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Look, an Information Dialog");
            alert.setContentText("Please fill the all fields to run");
            alert.showAndWait();
            return;
        }
        if(tf_query.getText().length()>0) {
            String query = tf_query.textProperty().get();
          searcher = new Searcher(indexer, checkBox_Stemming.isSelected(),stopWords, tf_postfilePath.textProperty().getValue(),checkBox_Semantic.isSelected(),  tf_queries_location.textProperty().getValue(),tf_results.textProperty().getValue());
          this.search=searcher;
            searcher.setQuery(query);
            if(entities.getItems().size()!=0)
                entities.getItems().clear();
            parseAndRankQueryAll("",searcher,true,query);
        }
        else
        {
            searcher = new Searcher(indexer, checkBox_Stemming.isSelected(),stopWords, tf_postfilePath.textProperty().getValue(),checkBox_Semantic.isSelected(),  tf_queries_location.textProperty().getValue(),tf_results.textProperty().getValue());
            this.search=searcher;
            try {
                parseAndRankQueryAll(tf_queries_location.textProperty().getValue(),searcher,false,"");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        tf_queries_location.setDisable(false);

    }
    public void parseAndRankQueryAll(String querisPath,Searcher sh,boolean oneQuery,String queryInput) throws IOException {
        List<Pair<String,String>> queries = new ArrayList<>();
        ObservableList names =FXCollections.observableArrayList();
        ArrayList<String> onlyQuery=new ArrayList<>();
        HashSet<String> docs=new HashSet<>();
        if(!oneQuery) {
        String query="";
       // origialString=new ArrayList<>();
        FileReader reader = null;
        reader=new FileReader(querisPath);
        BufferedReader bufferRead = new BufferedReader(reader);
        String line = "";
        String stringNum="";
        int num=0;
            line = bufferRead.readLine();
            while (line != null) {
                if (line.startsWith("<num>")) {
                    String[] number = line.split(":");
                    stringNum = number[1].replace("  ", "");

                }
                if (line.startsWith("<title>")) {
                    line = line.replace("<title>", "");
                    line = line.replace("</title>", "");
                    line = line.substring(1, line.length() - 1);
                    query = line;
                    onlyQuery.add(query);
                }
                if (line.startsWith("<desc>")) {
                    line = bufferRead.readLine();
                    while (!line.startsWith("<narr>")) {
                        query = query + " " + line;
                        line = bufferRead.readLine();
                    }
                    queries.add(new Pair(stringNum, query));
                }
                line = bufferRead.readLine();

            }
            bufferRead.close();

        }
        else
        {
            queries.add(new Pair("123",queryInput));
        }
        int i=0;
        for(Pair query2:queries)
        {
            StringBuilder st=new StringBuilder();
            try {
                if(oneQuery==false)
                sh.setQueryOriginal(onlyQuery.get(i));
                else {
                    sh.setQueryOriginal(queryInput);

                }

                sh.setQuery((String)query2.getValue());
                sh.parseAndRankQuery((String)query2.getKey());
                Searcher.semantic=null;
                HashSet<String> ranks=sh.getRanker().getTop50();
                Iterator it = ranks.iterator();
                for(String p:ranks){
                    st.append(query2.getKey()+" - "+p+System.lineSeparator());
                   if(!docs.contains(p)) {
                      names.add(p);
                       docs.add((String) p);
                   }
                    }
                entities.getItems().addAll(names);
                names=FXCollections.observableArrayList();
                //}
                   st.append("----------------------------------------"+System.lineSeparator());
                output.appendText(st.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        i++;
    }
    public void showEntitesButton(ActionEvent event)
    {
        String chooee=(String)entities.getSelectionModel().getSelectedItem();
        if(chooee.isEmpty()||chooee==null)
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("No document has chosen");
            alert.showAndWait();
            return;
        }
        ArrayList<Pair<String,Double>> entitiesSorted=Searcher.rankEntitiesInDocs.get(chooee);
        if(entitiesSorted.size()==0){
            Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText("No entities in document");
        alert.showAndWait();
        return;
    }
        else
        {
            HashMap<String,String> j=new HashMap<>();
            for(Pair p:entitiesSorted) {
                j.put((String)p.getKey(),""+p.getValue());
            }
            showTableEntities(j);
        }


}

    private void showTableEntities(HashMap<String,String> j) {
            Map<String,String> map=j;
            TableColumn<Map.Entry<String, String>, String> columnTerm = new TableColumn<>("Entity");
        columnTerm.setMinWidth(300);
        columnTerm.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map.Entry<String, String>, String>, ObservableValue<String>>() {

            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Map.Entry<String, String>, String> p) {
                // this callback returns property for just one cell, you can't use a loop here
                // for first column we use key
                return new SimpleStringProperty(p.getValue().getKey());
            }
        });
        columnTerm.setSortType(TableColumn.SortType.ASCENDING);
        TableColumn<Map.Entry<String, String>, String> columnTf = new TableColumn<>("Rank");
        columnTf.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map.Entry<String, String>, String>, ObservableValue<String>>() {

            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Map.Entry<String, String>, String> p) {
                // for second column we use value
                String tfAndPostfile=p.getValue().getValue();
                return new SimpleStringProperty(tfAndPostfile);
            }
        });
        columnTf.setMinWidth(100);
        ObservableList<Map.Entry<String, String>> items = FXCollections.observableArrayList(map.entrySet());
        TableView<Map.Entry<String, String>> table = new TableView<>(items);
        table.getColumns().setAll(columnTerm, columnTf);
            Stage s = new Stage();
            Scene scene = new Scene(table, 400, 400);
            s.setScene(scene);
            s.showAndWait();
        }
    }









