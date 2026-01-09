package gui;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

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
        
        // Check for empty mandatory fields first
        if(txtFirstName.getText().trim().isEmpty()) {
            lblMessage.setText("Please enter First Name.");
            lblMessage.setStyle("-fx-text-fill: red;");
            return;
        }
        if(txtLastName.getText().trim().isEmpty()) {
            lblMessage.setText("Please enter Last Name.");
            lblMessage.setStyle("-fx-text-fill: red;");
            return;
        }
        if(txtUsername.getText().trim().isEmpty()) {
            lblMessage.setText("Please enter Username.");
            lblMessage.setStyle("-fx-text-fill: red;");
            return;
        }
        if(txtPassword.getText().isEmpty()) {
            lblMessage.setText("Please enter Password.");
            lblMessage.setStyle("-fx-text-fill: red;");
            return;
        }
        if(cmbType.getValue() == null) {
            lblMessage.setText("Please select Client Type.");
            lblMessage.setStyle("-fx-text-fill: red;");
            return;
        }
        
        // Validate phone: check if empty first, then validate format
        String phone = txtPhone.getText().trim();
        if (phone.isEmpty()) {
            lblMessage.setText("Please enter Phone Number.");
            lblMessage.setStyle("-fx-text-fill: red;");
            return;
        }
        if (!phone.matches("\\d{10}")) {
            lblMessage.setText("Phone must contain only digits and be exactly 10 digits long.");
            lblMessage.setStyle("-fx-text-fill: red;");
            return;
        }

        // Validate email: check if empty first, then validate format
        String email = txtEmail.getText().trim();
        if (email.isEmpty()) {
            lblMessage.setText("Please enter Email Address.");
            lblMessage.setStyle("-fx-text-fill: red;");
            return;
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            lblMessage.setText("Please enter a valid email address (must contain '@' and domain like .com or .co.il).");
            lblMessage.setStyle("-fx-text-fill: red;");
            return;
        }

        
        ArrayList<Object> subscriberData = new ArrayList<>();
        
        subscriberData.add(txtFirstName.getText().trim());       // Index 0: First Name
        subscriberData.add(txtLastName.getText().trim());        // Index 1: Last Name
        subscriberData.add(cmbType.getValue());           // Index 2: Type
        subscriberData.add(txtPersonalInfo.getText().trim());    // Index 3: Personal Info 
        subscriberData.add(txtUsername.getText().trim());        // Index 4: Username
        subscriberData.add(txtPassword.getText());        // Index 5: Password
        subscriberData.add(phone);           // Index 6: Phone (use trimmed and validated value)
        subscriberData.add(email);           // Index 7: Email (use trimmed and validated value)

       
        if (client != null) {
            client.handleMessageFromBoundary(
                TypeMessage.CUSTOMER,            // The broad category
                subscriberData,                 // The data of the subscriber
                Command.ADD_NEW_SUBSCRIBER // The specific command
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
		ClientController.registerClientController = this;
	}
    
    /**
     * Initialization hook called by JavaFX after FXML loading.
     * Currently, representatives can only register subscribers,
     * so we pre-select 'subscriber' and disable changing the type.
     * The other values remain in the ComboBox items for future use.
     */
    @FXML
    public void initialize() {
        if (cmbType != null) {
            cmbType.setValue("subscriber");
            cmbType.setDisable(true);
        }
    }
    
   
    public void handleServerResponse(Object msg) {
       
        javafx.application.Platform.runLater(() -> {
            
            // If server explicitly returns -1, treat as failure
            if (msg instanceof Integer && ((Integer) msg) == -1) {
                lblMessage.setText("Error: Registration failed on server side. Please try again.");
                lblMessage.setStyle("-fx-text-fill: red;");
                return;
            }

            if (msg instanceof Subscriber) {
                Subscriber newSub = (Subscriber) msg;

                // Some server/database flows may set subscriberId to -1 on failure
                if (newSub.getSubscriberId() == -1) {
                    lblMessage.setText("Error: Registration failed on server side. Please try again.");
                    lblMessage.setStyle("-fx-text-fill: red;");
                    return;
                }
                
                lblMessage.setText("Success! Subscriber ID: " + newSub.getSubscriberId() + " added.");
                lblMessage.setStyle("-fx-text-fill: green;");
                
                clearFormFields();

                // Keep success message for 10 seconds, then clear it
                PauseTransition pause = new PauseTransition(Duration.seconds(10));
                pause.setOnFinished(e -> lblMessage.setText(""));
                pause.play();
                
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
        cmbType.setValue("subscriber"); // Reset to subscriber (locked value)
    }
    
    
}