package gui;

import controller.ClientController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

	@FXML
	private TextField usernameTxt;

	@FXML
	private PasswordField passwordTxt;

	private ClientController client;

	// Method to set the client reference from other controllers
	public void setClient(ClientController client) {
		this.client = client;
	}

	/*
	 * Method to handle the Login button action
	 * 
	 * @param event The ActionEvent triggered by the button click
	 */
	@FXML
	void getLoginBtn(ActionEvent event) {
		String username = usernameTxt.getText();
		String password = passwordTxt.getText();

		if (username.isEmpty() || password.isEmpty()) {
			showAlert(AlertType.WARNING, "Missing Input", "Please enter both username and password.");
			return;
		}

		// -----------------------------------------------------------
		// TEMPORARY: Mocking server response for Frontend testing
		// -----------------------------------------------------------

		// TODO: Change this value to "Customer", "Worker", or "Manager" to test the
		// specific dashboard view
		String userTypeToTest = "Manager";

		System.out.println("Simulating login for: " + username + " as " + userTypeToTest);

		// Create a temporary user object (Stub) to simulate data from server
		// Note: Ideally, 'id' should also be set if your User class has it
		StubUser mockUser = new StubUser(username, password, userTypeToTest);

		// Proceed to dashboard immediately (Bypassing server communication for now)
		openDashboard(event, mockUser);
	}

	private void openDashboard(ActionEvent event, StubUser mockUser) {
		try {
			// Load the User Dashboard FXML
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/UserDashboard.fxml"));
			Parent root = loader.load();

			// Get the controller and pass the client and user data
			UserDashboardController controller = loader.getController();
			controller.setClient(client);
			controller.loadUserDetails(mockUser); // Load the mock user details

			// Set up the stage
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			stage.setTitle("BistroNine Client - User Dashboard");
			Scene scene = new Scene(root);
			stage.setScene(scene);
			stage.show();

		} catch (Exception e) {
			e.printStackTrace();
			showAlert(AlertType.ERROR, "Error", "Failed to load the dashboard.");
		}

	}

	/*
	 * Method to handle the Back button action
	 * 
	 * @param event The ActionEvent triggered by the button click
	 */
	@FXML
	void getBackBtn(ActionEvent event) {
		try {

			// back to main selection screen
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MainSelection.fxml"));
			Parent root = loader.load();

			MainSelectionController controller = loader.getController();
			controller.setClient(client);

			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			stage.setTitle("BistroNine Client - Main Menu");
			Scene scene = new Scene(root);
			stage.setScene(scene);
			stage.show();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void showAlert(AlertType type, String title, String content) {
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}
}