package gui;

import controller.ServerController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ServerMain extends Application {
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		Parent root = FXMLLoader.load(getClass().getResource("serverPort.fxml"));
		Scene scene = new Scene(root);
		scene.getStylesheets().add(getClass().getResource("serverPort.css").toExternalForm());
		primaryStage.setTitle("Server Connection");
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	// Added dbPassword parameter
	public static void runServer(String p, String dbPassword) {
		int port = 0; 

		try {
			port = Integer.parseInt(p);
		} catch (Throwable t) {
			System.out.println("Invalid port, using default 5555");
			port = 5555; 
		}

		// Pass password to ServerController
		ServerController sv = new ServerController(port, dbPassword);

		try {
			sv.listen(); 
		} catch (Exception ex) {
			System.out.println("ERROR - Could not listen for clients!");
			ex.printStackTrace();
		}
	}
}