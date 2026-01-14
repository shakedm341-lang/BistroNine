package gui;

import data.Command;
import data.TableReservation;
import data.TypeMessage;
import controller.ClientController;
import java.util.ArrayList;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the 'Get Table' screen in Terminal Mode.
 * Handles customer check-in via confirmation code or ReaderTag identification.
 * This controller manages both guest and subscriber check-in workflows, including
 * reservation retrieval for subscribers and code recovery for lost codes.
 */
public class GetTableController extends BaseTerminalController {

    /** The main container for the screen's layout. */
    @FXML
    private StackPane mainStack;

    /** Field for entering the 6-digit confirmation code. */
    @FXML
    private TextField confCodeField;

    /** Field for entering phone number during code recovery. */
    @FXML
    private TextField recoveryPhoneField;
    
    /** Field for entering email address during code recovery. */
    @FXML
    private TextField recoveryEmailField;
    
    /** View container for the main check-in interface. */
    @FXML private VBox checkInView;
    
    /** View container for the lost code recovery interface. */
    @FXML private VBox recoveryView;
    
    /** Container for the subscriber-specific reservation list. */
    @FXML private VBox subscriberCodesBox;
    
    /** List view displaying active reservation codes for identified subscribers. */
    @FXML private ListView<String> codesListView;
    
    /** Title label for the check-in view. */
    @FXML private Label checkInTitle;
    
    /** Guidance text for the check-in view. */
    @FXML private Label checkInGuidance;
    
    /** Button to trigger the lost code recovery view. */
    @FXML private Button lostCodeButton;
    
    /**
     * Initializes the controller. Sets up the UI state based on whether the current user 
     * is a Guest or a Subscriber. For subscribers, it shows a list of their active codes.
     */
    @FXML
    public void initialize() {
        // Register this controller instance globally for message routing
        ClientController.getTableController = this;
        
        // Reset UI to default check-in state
        checkInView.setVisible(true);
        recoveryView.setVisible(false);
        subscriberCodesBox.setVisible(false);
        subscriberCodesBox.setManaged(false);
        
        // Customize UI based on the type of user identified at the terminal
        if (BaseTerminalController.currentUserType == BaseTerminalController.UserType.SUBSCRIBER) {
            // Identified Subscriber mode
            checkInTitle.setText("Reservation Check-In");
            checkInGuidance.setText("Select an active reservation or enter your code manually.");
            
            // Subscribers don't need 'Lost Code' as their codes are retrieved automatically
            lostCodeButton.setVisible(false);
            lostCodeButton.setManaged(false);

            // Show the list of their active reservation codes
            subscriberCodesBox.setVisible(true);
            subscriberCodesBox.setManaged(true);
            
            // Populate the input field automatically when a code is selected from the list
            codesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    confCodeField.setText(newVal);
                }
            });
        } else {
            // Anonymous Guest mode
            checkInTitle.setText("Enter Confirmation Code");
            checkInGuidance.setText("Please enter your confirmation code below.");
            
            // Guests can use the recovery feature if they lost their code
            lostCodeButton.setVisible(true);
            lostCodeButton.setManaged(true);
        }
    }

    /**
     * Sets the client controller and triggers subscriber data retrieval if applicable.
     * 
     * @param client The {@link ClientController} instance for server communication.
     */
    @Override
    public void setClient(ClientController client) {
        super.setClient(client);
        
        // If the user is an identified subscriber, automatically fetch their active codes from the server
        if (BaseTerminalController.currentUserType == BaseTerminalController.UserType.SUBSCRIBER && client != null && currentSubscriberId != null) {
            ArrayList<Object> content = new ArrayList<>();
            try {
                content.add(Integer.parseInt(currentSubscriberId));
                client.handleMessageFromBoundary(TypeMessage.CUSTOMER, content, Command.GET_ALL_CONF_CODE_BY_CUSTOMER_ID);
            } catch (NumberFormatException e) {
                System.out.println("Error parsing subscriber ID: " + currentSubscriberId);
            }
        }
    }

    /**
     * Switches the UI to show the 'Lost Code' recovery view.
     * 
     * @param event The ActionEvent from the 'Lost Code' button.
     */
    @FXML
    public void handleShowRecovery(ActionEvent event) {
        checkInView.setVisible(false);
        recoveryView.setVisible(true);
    }

    /**
     * Switches the UI back to the main Check-In view.
     * 
     * @param event The ActionEvent from the 'Back' button in recovery view.
     */
    @FXML
    public void handleShowCheckIn(ActionEvent event) {
        recoveryView.setVisible(false);
        checkInView.setVisible(true);
    }

    /**
     * Validates and processes the manually entered confirmation code for check-in.
     * 
     * @param event The ActionEvent from the 'Check-In' button.
     */
    @FXML
    public void handleCheckInByCode(ActionEvent event) {
        String codeText = confCodeField.getText().trim();
        
        // Basic validation for empty input
        if (codeText.isEmpty()) {
            TerminalUtils.showError("Input Error", "Please enter your confirmation code.");
            return;
        }

        try {
            // Attempt to parse the code as a number and send the request
            int code = Integer.parseInt(codeText);
            sendCheckInRequest(code);
        } catch (NumberFormatException e) {
            TerminalUtils.showError("Input Error", "Confirmation code must be a number.");
        }
    }

    /**
     * Standard back navigation to the terminal menu.
     * 
     * @param event The ActionEvent from the 'Back' button.
     */
    @Override
    @FXML
    public void handleBack(ActionEvent event) {
        super.handleBack(event);
    }

    /**
     * Processes the request to recover lost confirmation codes via SMS or Email.
     * 
     * @param event The ActionEvent from the 'Send My Codes' button.
     */
    @FXML
    public void handleRecoverLostCodes(ActionEvent event) {
        String phone = recoveryPhoneField.getText().trim();
        String email = recoveryEmailField.getText().trim();

        // Ensure at least one contact method is provided
        if (phone.isEmpty() && email.isEmpty()) {
            TerminalUtils.showError("Input Error", "Please provide either your phone number or email address.");
            return;
        }

        // Validate Phone format: Must be exactly 10 numeric digits
        if (!phone.isEmpty() && !phone.matches("^\\d{10}$")) {
            TerminalUtils.showError("Invalid Phone", "Phone number must be exactly 10 digits.");
            return;
        }

        // Validate Email format using a standard regex pattern
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            TerminalUtils.showError("Invalid Email", "Please enter a valid email (e.g. name@example.com).");
            return;
        }

        // Prepare request parameters for the server
        ArrayList<Object> content = new ArrayList<>();
        content.add("customer");
        content.add(phone.isEmpty() ? null : phone);
        content.add(email.isEmpty() ? null : email);
        
        if (client != null) {
            client.handleMessageFromBoundary(TypeMessage.CUSTOMER, content, Command.LOST_CONF_CODE);
        } else {
            TerminalUtils.showError("Connection Error", "Client connection is not initialized.");
        }
    }

    /**
     * Sends a check-in request to the server with the provided confirmation code.
     * 
     * @param conferenceCode The unique reservation confirmation code.
     */
    private void sendCheckInRequest(int conferenceCode) {
        ArrayList<Object> content = new ArrayList<>();
        content.add(conferenceCode);

        if (client != null) {
            client.handleMessageFromBoundary(TypeMessage.TABLE, content, Command.RECEIVE_TABLE_ID);
        } else {
            TerminalUtils.showError("Connection Error", "Client connection is not initialized.");
        }
    }

    /**
     * Callback handled by ClientController when reservation codes are received.
     * Updates the UI list with the retrieved codes.
     * 
     * @param response The server response, expected to be an ArrayList of Integers.
     */
    public void onCodesResponse(Object response) {
        Platform.runLater(() -> {
            if (response instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<Integer> list = (ArrayList<Integer>) response;
                codesListView.getItems().clear();
                
                if(list.isEmpty()) {
                    TerminalUtils.showError("Retrieval Error", "No active reservations found");
                    return;
                }
                
                // Add each code to the list view for selection
                for (Integer code : list) {
                    codesListView.getItems().add(String.valueOf(code));
                }
            }
        });
    }

    /**
     * Callback handled by ClientController when a check-in response is received.
     * Navigates the user based on their reservation status (ready table or waitlist).
     * 
     * @param response The server response, expected to be a {@link TableReservation} object.
     */
    public void onCheckInResponse(Object response) {
        Platform.runLater(() -> {
            if (response == null) {
                TerminalUtils.showError("Action Failed", "Invalid code or identification failed.");
            } else if (response instanceof TableReservation) {
                TableReservation res = (TableReservation) response;
                
                // Determine user guidance based on reservation status
                if ("arrived".equals(res.getStatus()) || "active".equals(res.getStatus())) {
                    // Table is ready for seating
                    TerminalUtils.showSuccess("Welcome!", "Your table is ready!\nPlease proceed to Table No. " + res.getTableId());
                    handleBack(new ActionEvent(confCodeField, null));
                } else if ("waiting".equals(res.getStatus())) {
                    // User is confirmed but still on waitlist
                    TerminalUtils.showSuccess("Still Waiting", "You are still on the waitlist. We will notify you when a table is ready.");
                    handleBack(new ActionEvent(confCodeField, null));
                } else {
                    // Unexpected status
                    TerminalUtils.showError("Check-In Error", "Reservation status: " + res.getStatus());
                }
            }
        });
    }

    /**
     * Callback handled by ClientController when a code recovery response is received.
     * 
     * @param response The server response, expected to be a Boolean indicating success.
     */
    public void onRecoverCodesResponse(Object response) {
        Platform.runLater(() -> {
            if (response instanceof Boolean) {
                if ((Boolean) response) {
                    // Recovery successful
                    TerminalUtils.showSuccess("Codes Sent", "We have sent your active confirmation codes to your registered email and phone number.");
                    handleShowCheckIn(new ActionEvent(confCodeField, null));
                } else {
                    // No records found for provided contact info
                    TerminalUtils.showError("Identification Failed", "We could not find any active reservations for your account for today.");
                }
            } else {
                TerminalUtils.showError("Error", "An unexpected error occurred while recovering codes.");
            }
        });
    }
}

