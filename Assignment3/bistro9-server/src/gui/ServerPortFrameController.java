package gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
public class ServerPortFrameController {

	@FXML
	private Button btnExit = null;
	
	@FXML
	private Button btnDone = null;
	
	@FXML
	private TextField portxt;

	private String getport() {
		return portxt.getText();
	}

	public void Done(ActionEvent event) throws Exception {
		String p;
		p = getport();
		
		if (p.trim().isEmpty()) {
			System.out.println("You must enter a port number");
		} else {
			// Hide the window
			((Node) event.getSource()).getScene().getWindow().hide(); 
			
			// Run the server in the background
			ServerMain.runServer(p);
		}
	}

	public void getExitBtn(ActionEvent event) throws Exception {
		System.out.println("Exit Server Tool");
		System.exit(0);
	}
}