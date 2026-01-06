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

	private String getport() {
		return portxt.getText();
	}

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

	public void getExitBtn(ActionEvent event) throws Exception {
		System.out.println("Exit Server Tool");
		System.exit(0);
	}
}