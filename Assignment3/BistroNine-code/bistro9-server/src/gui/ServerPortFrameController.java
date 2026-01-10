package gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;           
import javafx.scene.control.Alert.AlertType; 
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ServerPortFrameController {

	@FXML
	private Button btnExit = null;
	@FXML
	private Button btnDone = null;
	@FXML
	private TextField portxt;
	
	@FXML
	private PasswordField dbPassTxt;

	/**
	 *  Method to get the port number from the text field.
	 * 
	 * @return The port number as a string.
	 */
	private String getport() {
		return portxt.getText();
	}

	/**
	 *  Method to handle the "Done" button click event. It retrieves the port
	 * number and database password, validates the input, and starts the server.
	 * 
	 * @param event The action event triggered by clicking the "Done" button.
	 * @throws Exception If an error occurs while starting the server.
	 */
	public void Done(ActionEvent event) throws Exception {
		String p = getport();
		String password = dbPassTxt.getText();
		
		//Check if password field is empty 
		if (password.trim().isEmpty()) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("Missing Input");
			alert.setContentText("Please enter the Database Password.");
			alert.showAndWait();
			return; // Stops the function and does not proceed to the server
		}
		
		
		if (p.trim().isEmpty()) {
			System.out.println("You must enter a port number");
		} else {
			((Node) event.getSource()).getScene().getWindow().hide();
			
			// Pass password to runServer
			ServerMain.runServer(p, password);
		}
	}

	/**
	 * Method to handle the "Exit" button click event. It exits the
	 * application.
	 * 
	 * @param event The action event triggered by clicking the "Exit" button.
	 * @throws Exception If an error occurs while exiting the application.
	 */
	public void getExitBtn(ActionEvent event) throws Exception {
		System.out.println("Exit Server Tool");
		System.exit(0);
	}
}