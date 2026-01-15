package gui;

import controller.ServerController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ServerMain extends Application {

	// Static variable to allow access if needed
	public static ServerController sv;
	private static Stage primaryStage; // Keep reference to stage

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		primaryStage = stage;
		// Change: Load ServerInit (Screen Zero) first
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ServerInit.fxml"));
		Parent root = loader.load();
		Scene scene = new Scene(root);

		primaryStage.setTitle("DB Initialization");
		primaryStage.setScene(scene);

		// If 'X' is clicked on the first screen, exit the entire system
		primaryStage.setOnCloseRequest(event -> {
			System.out.println("Closing Stage 0 -> Exit System");
			System.exit(0);
		});

		primaryStage.show();
	}

	/**
	 * New method to transition from ServerInit to ServerPort (original first screen).
	 */
	public static void showServerPortScreen() {
		try {
			FXMLLoader loader = new FXMLLoader(ServerMain.class.getResource("/gui/serverPort.fxml"));
			Parent root = loader.load();
			Scene scene = new Scene(root);

			Platform.runLater(() -> {
				primaryStage.setTitle("Server Connection");
				primaryStage.setScene(scene);
				primaryStage.setOnCloseRequest(event -> {
					System.out.println("Closing Stage 1 -> Exit System");
					System.exit(0);
				});
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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
			FXMLLoader loader = new FXMLLoader(ServerMain.class.getResource("/gui/ServerDashboard.fxml"));
			Parent root = loader.load();

			ServerDashboardController dashboardController = loader.getController();

			Stage dashboardStage = new Stage();
			Scene scene = new Scene(root);

			dashboardStage.setTitle("Server Dashboard");
			dashboardStage.setScene(scene);

			dashboardStage.setOnCloseRequest(event -> {
				System.out.println("Closing Dashboard -> Stopping Server and Exiting");
				if(sv != null) {
					try { sv.close(); } catch(Exception e) {} 
				}
				System.exit(0); 
			});

			dashboardStage.show();
            // Close the connection/init stage
			if(primaryStage != null) primaryStage.close();

			if (sv == null) {
				sv = new ServerController(finalPort, dbPassword, dashboardController);
				sv.listen();
			} else {
				System.out.println("Server already running. Updating DB Password & UI.");
				sv.updateServerDetails(dashboardController, dbPassword);
			}

		} catch (Exception ex) {
			System.out.println("ERROR - Could not start server or load dashboard!");
			ex.printStackTrace();
		}
	}
}