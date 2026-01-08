package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientMain extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ConnectToServerGui.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());
            
           // Set the stage title and scene
            primaryStage.setTitle("BistroNine Client - Connection to Server");
            primaryStage.setScene(scene);
            
            // Connection-to-server window should have a fixed size
            primaryStage.setResizable(false);
            
            primaryStage.show();
            primaryStage.centerOnScreen();
            
        } catch(Exception e) {
            System.out.println("Error loading FXML:");
            e.printStackTrace();
        }
    }
    

    public static void main(String[] args) {
        launch(args);
    }
}