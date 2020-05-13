package sample;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.util.Observable;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/sample.fxml"));
        Scene scene = new Scene(root, 1000, 650);
        stage.setTitle("David and Matan Serach Engine");
        stage.setScene(scene);
        stage.show();

        //controller.add(new Data("35","LSB354",42.34,new Button("Show Entities")));

       // tb.add(new Data("51","LS-434",4.23,new Button()));
    }


    public static void main(String[] args) {
        launch(args);
    }
}
