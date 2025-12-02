package gui;

import controller.ServerController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ServerUI extends Application {
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
	    // 1. Load the FXML
	    Parent root = FXMLLoader.load(getClass().getResource("ServerPort.fxml"));
	    
	    // 2. Create the Scene
	    Scene scene = new Scene(root);
	    
	    // 3. ADD THIS LINE TO LOAD CSS:
	    scene.getStylesheets().add(getClass().getResource("ServerPort.css").toExternalForm());
	    
	    // 4. Show the stage
	    primaryStage.setTitle("Server Connection");
	    primaryStage.setScene(scene);
	    primaryStage.show();
	}
	
	/**
	 * This method is called by the Controller when the user clicks "Done".
	 * It hides the GUI and starts the OCSF server logic.
	 */
	public static void runServer(String p) {
		int port = 0; // Port to listen on

		try {
			port = Integer.parseInt(p);
		} catch (Throwable t) {
			System.out.println("Invalid port, using default 5555");
			port = 5555; 
		}

		// Create the server instance
		ServerController sv = new ServerController(port);

		try {
			sv.listen(); // Start listening for connections
		} catch (Exception ex) {
			System.out.println("ERROR - Could not listen for clients!");
			ex.printStackTrace();
		}
	}
}