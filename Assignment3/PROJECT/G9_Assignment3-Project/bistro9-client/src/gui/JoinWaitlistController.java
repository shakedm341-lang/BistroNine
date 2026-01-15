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

/**
 * Controller for the Join Waitlist screen in Terminal Mode.
 * This class handles the logic for both Guests and Subscribers to join the restaurant's waitlist
 * or get an immediate table if one is available.
 * 
 * In Terminal Mode, the user type is usually pre-determined by the login/identification step
 * in {@link BaseTerminalController}.
 */
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

    /**
     * Initializes the controller. This method is automatically called by JavaFX after the FXML file is loaded.
     * It sets up the UI components, visibility based on the current user type, and validation listeners.
     */
    @FXML
    public void initialize() {
        // Registers this controller instance with the ClientController to receive server responses
    	ClientController.joinWaitlistController = this;    
    	
        // Hide radio buttons as the user type (Guest/Subscriber) is already determined by the terminal's state
        rbGuest.setVisible(false);
        rbGuest.setManaged(false);
        rbSubscriber.setVisible(false);
        rbSubscriber.setManaged(false);

        // Initialize the number of diners spinner (range 1-20, default 2)
        dinersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 2));

        // Add listeners to toggle field visibility if the user type were to change (though mostly static in Terminal Mode)
        rbGuest.selectedProperty().addListener((obs, oldVal, newVal) -> {
            guestFields.setVisible(newVal);
            guestFields.setManaged(newVal);
        });
        rbSubscriber.selectedProperty().addListener((obs, oldVal, newVal) -> {
            subscriberFields.setVisible(newVal);
            subscriberFields.setManaged(newVal);
        });

        // Configure the initial UI state based on the currentUserType determined during identification
        if (BaseTerminalController.currentUserType == BaseTerminalController.UserType.SUBSCRIBER) {
            setupSubscriberView();
        } else {
            setupGuestView();
        }

        // Add validation listeners to enable/disable the join button as the user provides contact details
        phoneField.textProperty().addListener((obs, old, newValue) -> updateJoinButtonState());
        emailField.textProperty().addListener((obs, old, newValue) -> updateJoinButtonState());
    }

    /**
     * Configures the UI for a Subscriber user.
     * Pre-fills the Subscriber ID and disables guest fields.
     */
    private void setupSubscriberView() {
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
        
        // In Terminal Mode, subscribers are identified at login; button is enabled if ID is present
        btnJoin.setDisable(currentId.isEmpty());
    }

    /**
     * Configures the UI for a Guest user.
     * Shows contact fields and disables subscriber fields.
     */
    private void setupGuestView() {
        rbGuest.setSelected(true);
        rbSubscriber.setSelected(false);
        rbSubscriber.setDisable(true);
        
        guestFields.setVisible(true);
        guestFields.setManaged(true);
        subscriberFields.setVisible(false);
        subscriberFields.setManaged(false);
        
        // Disable join button until mandatory contact info is provided
        updateJoinButtonState();
    }

    /**
     * Updates the JOIN WAITLIST button state based on the presence of contact information.
     * Guests must provide at least a phone number or an email to be reachable.
     */
    private void updateJoinButtonState() {
        if (BaseTerminalController.currentUserType == BaseTerminalController.UserType.GUEST) {
            boolean hasPhone = !phoneField.getText().trim().isEmpty();
            boolean hasEmail = !emailField.getText().trim().isEmpty();
            btnJoin.setDisable(!hasPhone && !hasEmail);
        }
    }

    /**
     * Event handler for the "Join" button.
     * Determines the user type and initiates the join process.
     * 
     * @param event The action event triggered by the button click.
     */
    @FXML
    void handleJoin(ActionEvent event) {
        String type = rbSubscriber.isSelected() ? "subscriber" : "customer";
        performJoin(type);
    }

    /**
     * Orchestrates the join waitlist logic: validates input, packages the data,
     * and sends it to the server via the client controller.
     * 
     * @param type The type of user attempting to join ("subscriber" or "customer").
     */
    private void performJoin(String type) {
        Object identifier = null;
        String email = null;
        int numDiners = dinersSpinner.getValue();

        if (type.equals("subscriber")) {
            String subIdText = BaseTerminalController.currentSubscriberId;
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
            
            // Validation for Guest Mode: phone or email is required
            if (phone.isEmpty() && email.isEmpty()) {
                TerminalUtils.showError("Input Error", "Please provide at least a Phone Number or an Email.");
                return;
            }

            // Regex validation: Phone must be 10 digits
            if (!phone.isEmpty() && !phone.matches("^\\d{10}$")) {
                TerminalUtils.showError("Invalid Phone", "Phone number must be exactly 10 digits.");
                return;
            }

            // Regex validation: Basic email format check
            if (!email.isEmpty() && !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                TerminalUtils.showError("Invalid Email", "Please enter a valid email (e.g. name@example.com).");
                return;
            }

            identifier = phone.isEmpty() ? null : phone;
            email = email.isEmpty() ? null : email;
        }

        // Package data for the server: [type, identifier (phone/ID), email, numDiners]
        ArrayList<Object> content = new ArrayList<>();
        content.add(type);       
        content.add(identifier); 
        content.add(email);      
        content.add(numDiners);  

        // Send message to server through OCSF client
        client.handleMessageFromBoundary(TypeMessage.WAITLIST, content, Command.GET_IN_TO_WAIT_LIST);
    }

    /**
     * Callback method invoked by {@link ClientController} when the server responds
     * to the waitlist request.
     * 
     * @param response The response from the server, expected to be a {@link TableReservation} 
     *                 object or an {@link Integer} representing a confirmation code or table ID.
     */
    public void onJoinResponse(Object response) {
        Platform.runLater(() -> {
            if (response == null) {
                TerminalUtils.showError("Error", "Could not join the waitlist. Please try again.");
            } else if (response instanceof TableReservation) {
                // Handling complex TableReservation response
                TableReservation res = (TableReservation) response;
                int tableId = res.getTableId();
                int confCode = res.getConfirmationCode();

                if (tableId > 0) {
                    // Scenario: A table was immediately available
                    TerminalUtils.showSuccess("Table Ready!", 
                        "A table is available immediately!\n" +
                        "Please proceed to Table No. " + tableId + "\n" +
                        "Your confirmation code is: " + confCode);
                } else {
                    // Scenario: Added to the waitlist
                    TerminalUtils.showSuccess("Waitlist Joined", 
                        "You have been added to the waitlist.\n" +
                        "Your confirmation code is: " + confCode + "\n" +
                        "You will receive a notification when your table is ready.");
                }
                handleBack(new ActionEvent(btnJoin, null)); // Navigate back to the main menu
            } else if (response instanceof Integer) {
                // Handling simplified Integer response (legacy or alternate flow)
                int val = (Integer) response;
                if (val < 0) {
                    // Negative value implies immediate table assignment (ABS for table ID)
                    int tableId = Math.abs(val);
                    TerminalUtils.showSuccess("Table Ready!", "A table is available immediately!\nPlease proceed to Table No. " + tableId);
                } else {
                    // Positive value is the waitlist confirmation code
                    TerminalUtils.showSuccess("Waitlist Joined", "You have been added to the waitlist.\nYour confirmation code is: " + val + "\nYou will receive a notification when your table is ready.");
                }
                handleBack(new ActionEvent(btnJoin, null));
            }
        });
    }
}
