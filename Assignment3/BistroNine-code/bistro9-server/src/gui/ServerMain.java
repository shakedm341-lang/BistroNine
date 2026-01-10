package gui;

import controller.ServerController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ServerMain extends Application {

	// Static variable to allow access if needed
	public static ServerController sv;

	/**
	 * * The main entry point for all JavaFX applications. The start method is
	 * called after the init method has returned, and after the system is ready for
	 * the application to begin running.
	 *
	 * @param primaryStage the primary stage for this application, onto which the
	 *                     application scene can be set. The primary stage will be
	 *                     embedded in the browser if the application is launched as
	 *                     an applet.
	 */
	public static void main(String[] args) {
		launch(args);
	}

	/**
	 * * The main entry point for all JavaFX applications. The start method is
	 * called after the init method has returned, and after the system is ready for
	 * the application to begin running.
	 *
	 * @param primaryStage the primary stage for this application, onto which the
	 *                     application scene can be set. The primary stage will be
	 *                     embedded in the browser if the application is launched as
	 *                     an applet.
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		// Load the Initial Login Screen
		Parent root = FXMLLoader.load(getClass().getResource("/gui/serverPort.fxml"));
		Scene scene = new Scene(root);

		primaryStage.setTitle("Server Connection");
		primaryStage.setScene(scene);

		// If 'X' is clicked on the first screen, exit the entire system
		primaryStage.setOnCloseRequest(event -> {
			System.out.println("Closing Stage 1 -> Exit System");
			System.exit(0);
		});

		primaryStage.show();
	}

	/**
	 * * Method to run the server and show the dashboard.
	 * 
	 * @param p          The port number as a string.
	 * @param dbPassword The database password.
	 */
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
			// Load the Dashboard
			FXMLLoader loader = new FXMLLoader(ServerMain.class.getResource("/gui/ServerDashboard.fxml"));
			Parent root = loader.load();

			// Get the Controller to pass it to the Server
			ServerDashboardController dashboardController = loader.getController();

			Stage dashboardStage = new Stage();
			Scene scene = new Scene(root);

			dashboardStage.setTitle("Server Dashboard");
			dashboardStage.setScene(scene);

			// If 'X' is clicked on the dashboard, kill the process completely
			dashboardStage.setOnCloseRequest(event -> {
				System.out.println("Closing Dashboard -> Stopping Server and Exiting");
				// Try to close server nicely before exiting
				if(sv != null) {
					try { sv.close(); } catch(Exception e) {} 
				}
				System.exit(0); 
			});

			// Show the new window
			dashboardStage.show();


			if (sv == null) {
				// CASE 1: Server not running yet -> Create new and listen
				sv = new ServerController(finalPort, dbPassword, dashboardController);
				sv.listen();
			} else {
				// CASE 2: Server already running -> Update UI and Password ONLY
				System.out.println("Server already running. Updating DB Password & UI.");
				sv.updateServerDetails(dashboardController, dbPassword);
			}

		} catch (Exception ex) {
			System.out.println("ERROR - Could not start server or load dashboard!");
			ex.printStackTrace();
		}
	}
}