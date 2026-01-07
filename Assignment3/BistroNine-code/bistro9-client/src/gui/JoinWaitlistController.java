package gui;

import data.Command;
import data.TypeMessage;
import java.util.ArrayList;

import controller.ClientController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class JoinWaitlistController extends BaseTerminalController {

    @FXML private RadioButton rbGuest;
    @FXML private RadioButton rbSubscriber;
    @FXML private VBox guestFields;
    @FXML private VBox subscriberFields;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField scannedSubIdField;
    @FXML private Spinner<Integer> dinersSpinner;
    @FXML private Button btnJoin;
    @FXML private Label lblDetected;

    public static String lastScannedCode = "";

    @FXML
    public void initialize() {
        //TODO: Uncomment this when  developer moves the class to the data package
    	ClientController.joinWaitlistController = this;    
    	
        // Hide radio buttons as type is already determined in Terminal Mode
        rbGuest.setVisible(false);
        rbGuest.setManaged(false);
        rbSubscriber.setVisible(false);
        rbSubscriber.setManaged(false);

        // Initialize spinner with range 1-20, default 2
        dinersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 2));

        // Show/hide fields based on selection
        rbGuest.selectedProperty().addListener((obs, oldVal, newVal) -> {
            guestFields.setVisible(newVal);
            guestFields.setManaged(newVal);
        });
        rbSubscriber.selectedProperty().addListener((obs, oldVal, newVal) -> {
            subscriberFields.setVisible(newVal);
            subscriberFields.setManaged(newVal);
        });

        // Set initial state based on Terminal Mode
        if (BaseTerminalController.currentUserType == BaseTerminalController.UserType.SUBSCRIBER) {
            rbSubscriber.setSelected(true);
            rbGuest.setSelected(false);
            rbGuest.setDisable(true);
            
            guestFields.setVisible(false);
            guestFields.setManaged(false);
            subscriberFields.setVisible(true);
            subscriberFields.setManaged(true);
            
            lastScannedCode = BaseTerminalController.currentSubscriberId != null ? BaseTerminalController.currentSubscriberId : "";
            scannedSubIdField.setText(lastScannedCode);
            scannedSubIdField.setEditable(false);
            
            // If subscriber is not yet identified, disable join button until they scan
            if (lastScannedCode.isEmpty()) {
                btnJoin.setDisable(true);
                lblDetected.setVisible(false);
            } else {
                btnJoin.setDisable(false);
                lblDetected.setVisible(true);
            }
        } else {
            rbGuest.setSelected(true);
            rbSubscriber.setSelected(false);
            rbSubscriber.setDisable(true);
            
            guestFields.setVisible(true);
            guestFields.setManaged(true);
            subscriberFields.setVisible(false);
            subscriberFields.setManaged(false);
            
            btnJoin.setDisable(false);
            lblDetected.setVisible(false);
        }
    }

    @FXML
    void handleScanBarcode(ActionEvent event) {
        TerminalUtils.simulateBarcodeScan(id -> {
            if (id != null && !id.trim().isEmpty()) {
                try {
                    int subId = Integer.parseInt(id.trim());
                    ArrayList<Object> content = new ArrayList<>();
                    content.add(subId);
                    
                    if (client != null) {
                        client.handleMessageFromBoundary(TypeMessage.CUSTOMER, content, 
                            Command.CHECK_LOGIN_DETAILSֹֹ_BY_TAG_READER);
                    } else {
                        TerminalUtils.showError("Connection Error", "Client connection is not initialized.");
                    }
                } catch (NumberFormatException e) {
                    TerminalUtils.showError("Input Error", "Subscriber ID must be a number.");
                } catch (IllegalArgumentException e) {
                    TerminalUtils.showError("System Error", "The identification command is not recognized by the system.");
                }
            }
        });
    }

    @FXML
    void handleJoin(ActionEvent event) {
        String type = rbSubscriber.isSelected() ? "subscriber" : "customer";
        
        if (rbSubscriber.isSelected()) {
            ensureSubscriberIdentified(() -> {
                performJoin(type);
            });
        } else {
            performJoin(type);
        }
    }

    private void performJoin(String type) {
        Object identifier = null;
        String email = null;
        int numDiners = dinersSpinner.getValue();

        if (type.equals("subscriber")) {
            String subIdText = BaseTerminalController.currentSubscriberId;
            // Update UI if identification just happened
            scannedSubIdField.setText(subIdText);
            
            try {
                identifier = Integer.parseInt(subIdText);
            } catch (NumberFormatException e) {
                TerminalUtils.showError("Input Error", "Invalid Subscriber ID.");
                return;
            }
        } else {
            String phone = phoneField.getText().trim();
            email = emailField.getText().trim();
            if (phone.isEmpty() && email.isEmpty()) {
                TerminalUtils.showError("Input Error", "Please provide at least a Phone Number or an Email.");
                return;
            }
            identifier = phone.isEmpty() ? null : phone;
            email = email.isEmpty() ? null : email;
        }

        ArrayList<Object> content = new ArrayList<>();
        content.add(type);       // 0: type
        content.add(identifier); // 1: phone/subscriberId
        content.add(email);      // 2: email
        content.add(numDiners);  // 3: numberOfDiners

        client.handleMessageFromBoundary(TypeMessage.WAITLIST, content, Command.GET_IN_TO_WAIT_LIST);
    }

    public void onJoinResponse(Object response) {
        Platform.runLater(() -> {
            if (response == null) {
                TerminalUtils.showError("Error", "Could not join the waitlist. Please try again.");
            } else if (response instanceof Integer) {
                int val = (Integer) response;
                if (val < 0) {
                    // Immediate seating: negative table ID
                    int tableId = Math.abs(val);
                    TerminalUtils.showSuccess("Table Ready!", "A table is available immediately!\nPlease proceed to Table No. " + tableId);
                    handleBack(new ActionEvent(btnJoin, null)); // Return to menu
                } else {
                    // Waitlist: confirmation code
                    TerminalUtils.showSuccess("Waitlist Joined", "You have been added to the waitlist.\nYour confirmation code is: " + val + "\nYou will receive a notification when your table is ready.");
                    handleBack(new ActionEvent(btnJoin, null)); // Return to menu
                }
            }
        });
    }

    /**
     * Handles server response for subscriber identification.
     */
    public void onIdentificationResponse(Object response) {
        Platform.runLater(() -> {
            if (response instanceof data.Subscriber) {
                data.Subscriber sub = (data.Subscriber) response;
                lastScannedCode = String.valueOf(sub.getCustomerId());
                BaseTerminalController.currentSubscriberId = lastScannedCode;
                scannedSubIdField.setText(lastScannedCode);
                
                lblDetected.setVisible(true);
                btnJoin.setDisable(false);
                
                // If it was casual guest mode, switch to subscriber (though in Terminal Mode it's usually fixed)
                rbSubscriber.setSelected(true);
            } else {
                lblDetected.setVisible(false);
                btnJoin.setDisable(true);
                TerminalUtils.showError("Identification Failed", "Could not identify subscriber. Please check the ID and try again.");
            }
        });
    }
}

