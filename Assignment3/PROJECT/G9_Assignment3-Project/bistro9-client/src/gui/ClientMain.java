package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for the BistroNine client application.
 * This class initializes the JavaFX application environment and launches 
 * the initial connection screen for the server.
 */
public class ClientMain extends Application {

    /**
     * Initializes and displays the primary stage with the connection GUI.
     * 
     * @param primaryStage The main window stage for this application.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the FXML resource for the server connection interface
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ConnectToServerGui.fxml"));
            Parent root = loader.load();
            
            // Set up the scene and apply the global CSS stylesheet
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());
            
            // Configure window properties
            primaryStage.setTitle("BistroNine Client - Connection to Server");
            primaryStage.setScene(scene);
            
            // The connection setup window is restricted to a fixed size
            primaryStage.setResizable(false);
            
            // Center the window on the screen and show it
            primaryStage.show();
            primaryStage.centerOnScreen();
            
        } catch(Exception e) {
            System.err.println("Critical error launching the Client application:");
            e.printStackTrace();
        }
    }
    
    /**
     * The main method that serves as the entry point for the JVM.
     * 
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }
}