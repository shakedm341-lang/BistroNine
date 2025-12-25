package gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;


import java.util.ArrayList;

import controller.ClientController;
import data.Command;
import data.Subscriber;
import data.TypeMessage;

public class RegisterClientController {

    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private TextArea txtPersonalInfo;
    
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cmbType;
    
    @FXML private Label lblMessage;
    
    private ClientController client;

    @FXML
    void getRegistrationBtn(ActionEvent event) {
        
        if(txtFirstName.getText().isEmpty() || txtLastName.getText().isEmpty() || 
           txtUsername.getText().isEmpty() || txtPassword.getText().isEmpty() || 
           cmbType.getValue() == null) {
            lblMessage.setText("Please fill all mandatory fields.");
            return;
        }

        
        ArrayList<Object> subscriberData = new ArrayList<>();
        
        subscriberData.add(txtFirstName.getText());       // Index 0: First Name
        subscriberData.add(txtLastName.getText());        // Index 1: Last Name
        subscriberData.add(cmbType.getValue());           // Index 2: Type
        subscriberData.add(txtPersonalInfo.getText());    // Index 3: Personal Info 
        subscriberData.add(txtUsername.getText());        // Index 4: Username
        subscriberData.add(txtPassword.getText());        // Index 5: Password
        subscriberData.add(txtPhone.getText());           // Index 6: Phone
        subscriberData.add(txtEmail.getText());           // Index 7: Email

       
        if (client != null) {
            client.handleMessageFromBoundary(
                TypeMessage.CUSTOMER,            // The broad category
                subscriberData,                 // The data of the subscriber
                Command.CREATE_NEW_RESERVATION // The specific command
            );
        } else {
        	lblMessage.setText("Error: Client is not connected.");
        	lblMessage.setStyle("-fx-text-fill: red;");
        }
        
        System.out.println("Data prepared for server: " + subscriberData);
        lblMessage.setText("Registration request sent!");
        lblMessage.setStyle("-fx-text-fill: green;");
    }
    
    public void setClientController(ClientController client) {
		this.client = client;
	}
    
   
    public void handleServerResponse(Object msg) {
       
        javafx.application.Platform.runLater(() -> {
            
            if (msg instanceof Subscriber) {
                Subscriber newSub = (Subscriber) msg;
                
                lblMessage.setText("Success! Client ID: " + newSub.getSubscriberId() + " added.");
                lblMessage.setStyle("-fx-text-fill: green;");
                
                clearFormFields();
                
            } else {
                lblMessage.setText("Error: Registration failed. Please check inputs or try again.");
                lblMessage.setStyle("-fx-text-fill: red;");
            }
        });
    }

    
    private void clearFormFields() {
        txtFirstName.clear();
        txtLastName.clear();
        txtPhone.clear();
        txtEmail.clear();
        txtPersonalInfo.clear();
        txtUsername.clear();
        txtPassword.clear();
        cmbType.getSelectionModel().clearSelection();
    }
    
    
}