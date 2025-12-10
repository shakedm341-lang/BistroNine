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
		// Load the Initial Login Screen
		Parent root = FXMLLoader.load(getClass().getResource("serverPort.fxml"));
		Scene scene = new Scene(root);
		scene.getStylesheets().add(getClass().getResource("serverPort.css").toExternalForm());
		primaryStage.setTitle("Server Connection");
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	// This method is called by ServerPortFrameController when 'Done' is clicked
	public static void runServer(String p, String dbPassword) {
		int port = 0; 

		try {
			port = Integer.parseInt(p);
		} catch (Throwable t) {
			System.out.println("Invalid port, using default 5555");
			port = 5555; 
		}
		
		final int finalPort = port;

		try {
			// 1. Create the New Window (Dashboard)
			Stage dashboardStage = new Stage();
			FXMLLoader loader = new FXMLLoader(ServerMain.class.getResource("/gui/ServerDashboard.fxml"));
			Parent root = loader.load();
			
			// 2. Get the Controller so we can pass it to the Server
			ServerDashboardController dashboardController = loader.getController();
			
			Scene scene = new Scene(root);
			dashboardStage.setTitle("Server Dashboard");
			dashboardStage.setScene(scene);
			
			// 3. Show the new window
			dashboardStage.show();
			
			// 4. Start the Server logic
			ServerController sv = new ServerController(finalPort, dbPassword, dashboardController);
			sv.listen();
			
		} catch (Exception ex) {
			System.out.println("ERROR - Could not start server or load dashboard!");
			ex.printStackTrace();
		}
	}
}