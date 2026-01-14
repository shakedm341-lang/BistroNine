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

/**
 * Controller class for the Client Registration screen.
 * This class handles the registration of new subscribers into the system,
 * including input validation, communication with the server, and feedback display.
 */
public class RegisterClientController {

    // --- FXML UI Components ---
    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private TextArea txtPersonalInfo;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cmbType;
    @FXML private Label lblMessage;
    
    /** The client controller used to send messages to the server */
    private ClientController client;

    /**
     * Event handler for the registration button.
     * Validates all mandatory fields and formats (phone, email) before sending
     * the registration request to the server.
     * 
     * @param event The action event triggered by clicking the registration button.
     */
    @FXML
    void getRegistrationBtn(ActionEvent event) {
        
        // --- Validation Section ---
        
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
        
        // Validate phone: check if empty first, then validate format (10 digits)
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

        // Validate email: check if empty first, then validate format via regex
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

        // --- Data Packaging Section ---
        
        ArrayList<Object> subscriberData = new ArrayList<>();
        
        subscriberData.add(txtFirstName.getText().trim());       // Index 0: First Name
        subscriberData.add(txtLastName.getText().trim());        // Index 1: Last Name
        subscriberData.add(cmbType.getValue());           // Index 2: Type
        subscriberData.add(txtPersonalInfo.getText().trim());    // Index 3: Personal Info 
        subscriberData.add(txtUsername.getText().trim());        // Index 4: Username
        subscriberData.add(txtPassword.getText());        // Index 5: Password
        subscriberData.add(phone);           // Index 6: Phone
        subscriberData.add(email);           // Index 7: Email

       
        // --- Server Communication Section ---
        
        if (client != null) {
            client.handleMessageFromBoundary(
                TypeMessage.CUSTOMER,            // Broad category: CUSTOMER
                subscriberData,                 // Subscriber details payload
                Command.ADD_NEW_SUBSCRIBER      // Action: Add new subscriber
            );
        } else {
        	lblMessage.setText("Error: Client is not connected.");
        	lblMessage.setStyle("-fx-text-fill: red;");
        }
        
        System.out.println("Data prepared for server: " + subscriberData);
        lblMessage.setText("Registration request sent!");
        lblMessage.setStyle("-fx-text-fill: green;");
    }
    
    /**
     * Sets the ClientController dependency and registers this controller as the active one.
     * 
     * @param client The ClientController instance.
     */
    public void setClientController(ClientController client) {
		this.client = client;
		ClientController.registerClientController = this;
	}
    
    /**
     * Initialization hook called by JavaFX after FXML loading.
     * Configures default values for the registration form.
     * Pre-selects 'subscriber' as the default client type and disables the dropdown.
     */
    @FXML
    public void initialize() {
        if (cmbType != null) {
            cmbType.setValue("subscriber");
            cmbType.setDisable(true);
        }
    }
    
    /**
     * Processes the registration response received from the server.
     * Updates the UI message label on the JavaFX application thread.
     * 
     * @param msg The message object received from the server (expected to be Subscriber or Integer error code).
     */
    public void handleServerResponse(Object msg) {
       
        javafx.application.Platform.runLater(() -> {
            
            // If server explicitly returns -1, treat as failure
            if (msg instanceof Integer && ((Integer) msg) == -1) {
                lblMessage.setText("Error: Registration failed on server side. Please try again.");
                lblMessage.setStyle("-fx-text-fill: red;");
                return;
            }

            // If a Subscriber object is returned, it means registration was successful
            if (msg instanceof Subscriber) {
                Subscriber newSub = (Subscriber) msg;

                // Check for failure indicated by an ID of -1
                if (newSub.getSubscriberId() == -1) {
                    lblMessage.setText("Error: Registration failed on server side. Please try again.");
                    lblMessage.setStyle("-fx-text-fill: red;");
                    return;
                }
                
                // Success: Display the new subscriber's ID
                lblMessage.setText("Success! Subscriber ID: " + newSub.getSubscriberId() + " added.");
                lblMessage.setStyle("-fx-text-fill: green;");
                
                // Clear the form for the next entry
                clearFormFields();

                // Keep success message for 10 seconds, then clear it automatically
                PauseTransition pause = new PauseTransition(Duration.seconds(10));
                pause.setOnFinished(e -> lblMessage.setText(""));
                pause.play();
                
            } else {
                // Unexpected response type
                lblMessage.setText("Error: Registration failed. Please check inputs or try again.");
                lblMessage.setStyle("-fx-text-fill: red;");
            }
        });
    }

    /**
     * Clears all input fields in the registration form to prepare for a new entry.
     * Resets the client type ComboBox to 'subscriber'.
     */
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