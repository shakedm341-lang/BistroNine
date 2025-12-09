package gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField; // Import this
import javafx.scene.control.TextField;

public class ServerPortFrameController {

	@FXML
	private Button btnExit = null;
	@FXML
	private Button btnDone = null;
	@FXML
	private TextField portxt;
	
	@FXML
	private PasswordField dbPassTxt; // Added Variable

	private String getport() {
		return portxt.getText();
	}

	public void Done(ActionEvent event) throws Exception {
		String p = getport();
		String password = dbPassTxt.getText(); // Get password
		
		if (p.trim().isEmpty()) {
			System.out.println("You must enter a port number");
		} else {
			((Node) event.getSource()).getScene().getWindow().hide();
			
			// Pass password to runServer
			ServerMain.runServer(p, password);
		}
	}
	
//	public void Done(ActionEvent event) throws Exception {
//		String p = getport();
//		String password = dbPassTxt.getText(); // Get password
//		
//		// DEBUG PRINT: Remove this after it works!
//		System.out.println("DEBUG: Password sent to server is: [" + password + "]");
//		
//		if (p.trim().isEmpty()) {
//			System.out.println("You must enter a port number");
//		} else {
//			((Node) event.getSource()).getScene().getWindow().hide();
//			ServerMain.runServer(p, password);
//		}
//	}

	public void getExitBtn(ActionEvent event) throws Exception {
		System.out.println("Exit Server Tool");
		System.exit(0);
	}
}