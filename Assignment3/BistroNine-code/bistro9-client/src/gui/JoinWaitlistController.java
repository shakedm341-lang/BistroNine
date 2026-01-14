package gui;

import data.Command;
import data.TableReservation;
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
            
            String currentId = BaseTerminalController.currentSubscriberId != null ? BaseTerminalController.currentSubscriberId : "";
            scannedSubIdField.setText(currentId);
            scannedSubIdField.setEditable(false);
            
            // In Terminal Mode, subscriber is already identified at login
            btnJoin.setDisable(currentId.isEmpty());
        } else {
            rbGuest.setSelected(true);
            rbSubscriber.setSelected(false);
            rbSubscriber.setDisable(true);
            
            guestFields.setVisible(true);
            guestFields.setManaged(true);
            subscriberFields.setVisible(false);
            subscriberFields.setManaged(false);
            
            // For guest mode, button is disabled until at least one field is filled
            updateJoinButtonState();
        }

        // Add listeners to enable/disable join button as user types
        phoneField.textProperty().addListener((obs, old, newValue) -> updateJoinButtonState());
        emailField.textProperty().addListener((obs, old, newValue) -> updateJoinButtonState());
    }

    /**
     * Updates the JOIN WAITLIST button state based on the presence of contact information.
     * Guests must provide at least a phone number or an email.
     */
    private void updateJoinButtonState() {
        if (BaseTerminalController.currentUserType == BaseTerminalController.UserType.GUEST) {
            boolean hasPhone = !phoneField.getText().trim().isEmpty();
            boolean hasEmail = !emailField.getText().trim().isEmpty();
            btnJoin.setDisable(!hasPhone && !hasEmail);
        }
    }

    @FXML
    void handleJoin(ActionEvent event) {
        String type = rbSubscriber.isSelected() ? "subscriber" : "customer";
        performJoin(type);
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
            
            // Health Checks for Guest Mode
            if (phone.isEmpty() && email.isEmpty()) {
                TerminalUtils.showError("Input Error", "Please provide at least a Phone Number or an Email.");
                return;
            }

            // Validate Phone if provided: Must be exactly 10 digits
            if (!phone.isEmpty() && !phone.matches("^\\d{10}$")) {
                TerminalUtils.showError("Invalid Phone", "Phone number must be exactly 10 digits.");
                return;
            }

            // Validate Email if provided
            if (!email.isEmpty() && !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                TerminalUtils.showError("Invalid Email", "Please enter a valid email (e.g. name@example.com).");
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
            } else if (response instanceof TableReservation) {
                TableReservation res = (TableReservation) response;
                int tableId = res.getTableId();
                int confCode = res.getConfirmationCode();

                if (tableId > 0) {
                    // Immediate seating: positive table ID
                    TerminalUtils.showSuccess("Table Ready!", 
                        "A table is available immediately!\n" +
                        "Please proceed to Table No. " + tableId + "\n" +
                        "Your confirmation code is: " + confCode);
                    handleBack(new ActionEvent(btnJoin, null)); // Return to menu
                } else {
                    // Waitlist: confirmation code
                    TerminalUtils.showSuccess("Waitlist Joined", 
                        "You have been added to the waitlist.\n" +
                        "Your confirmation code is: " + confCode + "\n" +
                        "You will receive a notification when your table is ready.");
                    handleBack(new ActionEvent(btnJoin, null)); // Return to menu
                }
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
}

