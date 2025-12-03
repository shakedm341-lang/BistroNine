package gui;

import controller.ClientController;
import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class ConnectToServerController {

	@FXML
	private TextField ipTxt;

	@FXML
	private TextField portTxt;

	private ClientController client;

	@FXML
	// Method to handle the Connect button action
	void connectToServer(ActionEvent event) {
		String ip = ipTxt.getText();
		String portStr = portTxt.getText();

		if (ip.isEmpty() || portStr.isEmpty()) {
			// Use helper to show a warning alert to the user
			showAlert(AlertType.WARNING, "Input Required", "Please enter IP and Port.");
			return;
		}

		try {
			int port = Integer.parseInt(portStr);

			client = new ClientController(ip, port);

			System.out.println("Connected successfully to " + ip);

			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ReservationGui.fxml"));
			Parent root = loader.load();

			// Pass the client to the Reservation controller
			ReservtionBoundry reservationController = loader.getController();
			reservationController.setClient(client);

			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			Scene scene = new Scene(root);
			stage.setScene(scene);
			stage.show();

		} catch (NumberFormatException nfe) {
			showAlert(AlertType.ERROR, "Invalid Port", "Port must be a number.");
		} catch (Exception e) {
			// Show an error alert to the user when connection fails
			String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
			showAlert(AlertType.ERROR, "Connection Failed", "Connection failed: " + msg);

			System.out.println("Connection failed: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// Helper to centralize alert creation and display
	private void showAlert(AlertType type, String title, String content) {
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}
}