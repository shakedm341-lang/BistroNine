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
 */
public class GetTableController extends BaseTerminalController {

    @FXML
    private StackPane mainStack;

    @FXML
    private TextField confCodeField;

    @FXML
    private TextField recoveryPhoneField;
    @FXML
    private TextField recoveryEmailField;
    
    @FXML private VBox checkInView;
    @FXML private VBox recoveryView;
    @FXML private VBox subscriberCodesBox;
    @FXML private ListView<String> codesListView;
    
    @FXML private Label checkInTitle;
    @FXML private Label checkInGuidance;
    @FXML private Button lostCodeButton;
    
    @FXML
    public void initialize() {
        // Register this controller instance globally for message routing
        ClientController.getTableController = this;
        
        // Default UI state
        checkInView.setVisible(true);
        recoveryView.setVisible(false);
        subscriberCodesBox.setVisible(false);
        subscriberCodesBox.setManaged(false);
        
        // Initial state logic based on Terminal Mode
        if (BaseTerminalController.currentUserType == BaseTerminalController.UserType.SUBSCRIBER) {
            // Subscriber check-in options
            checkInTitle.setText("Reservation Check-In");
            checkInGuidance.setText("Select an active reservation or enter your code manually.");
            
            lostCodeButton.setVisible(false);
            lostCodeButton.setManaged(false);

            // Show and request codes list
            subscriberCodesBox.setVisible(true);
            subscriberCodesBox.setManaged(true);
            
            // Add selection listener to ListView
            codesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    confCodeField.setText(newVal);
                }
            });
        } else {
            // Guest check-in options (default)
            checkInTitle.setText("Enter Confirmation Code");
            checkInGuidance.setText("Please enter your confirmation code below.");
            lostCodeButton.setVisible(true);
            lostCodeButton.setManaged(true);
        }
    }

    @Override
    public void setClient(ClientController client) {
        super.setClient(client);
        // If we are a subscriber, request the codes now that we have the client
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
     * Navigates to the 'Lost Code' recovery view.
     */
    @FXML
    public void handleShowRecovery(ActionEvent event) {
        checkInView.setVisible(false);
        recoveryView.setVisible(true);
    }

    /**
     * Navigates back to the main Check-In view.
     */
    @FXML
    public void handleShowCheckIn(ActionEvent event) {
        recoveryView.setVisible(false);
        checkInView.setVisible(true);
    }

    /**
     * Attempts check-in using the manually entered 6-digit confirmation code.
     */
    @FXML
    public void handleCheckInByCode(ActionEvent event) {
        String codeText = confCodeField.getText().trim();
        if (codeText.isEmpty()) {
            TerminalUtils.showError("Input Error", "Please enter your confirmation code.");
            return;
        }

        try {
            int code = Integer.parseInt(codeText);
            sendCheckInRequest(code);
        } catch (NumberFormatException e) {
            TerminalUtils.showError("Input Error", "Confirmation code must be a number.");
        }
    }

    @Override
    @FXML
    public void handleBack(ActionEvent event) {
        super.handleBack(event);
    }

    /**
     * Handles the 'Recover Lost Codes' action.
     * Requests the server to send codes via SMS/Email using the provided contact info.
     */
    @FXML
    public void handleRecoverLostCodes(ActionEvent event) {
        String phone = recoveryPhoneField.getText().trim();
        String email = recoveryEmailField.getText().trim();

        // Health Checks for Lost Codes Recovery
        if (phone.isEmpty() && email.isEmpty()) {
            TerminalUtils.showError("Input Error", "Please provide either your phone number or email address.");
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
     * Handles the server's response with all active confirmation codes for a subscriber.
     */
    public void onCodesResponse(Object response) {
        Platform.runLater(() -> {
            if (response instanceof ArrayList) {
                ArrayList<Integer> list = (ArrayList<Integer>) response;
                codesListView.getItems().clear();
                if(list.isEmpty()) {
                    TerminalUtils.showError("Retrieval Error", "No active reservations found");
                    return;
                }
                for (Integer code : list) {
                    codesListView.getItems().add(String.valueOf(code));
                }
            }
        });
    }
    /**
     * Handles the server's response to a check-in attempt.
     * Redirects the user to their table or notifies waitlist status.
     */
    public void onCheckInResponse(Object response) {
        Platform.runLater(() -> {
            if (response == null) {
                TerminalUtils.showError("Action Failed", "Invalid code or identification failed.");
            } else if (response instanceof TableReservation) {
                TableReservation res = (TableReservation) response;
                //check if the reservation is active or arrived
                if ("arrived".equals(res.getStatus()) || "active".equals(res.getStatus())) {
                    TerminalUtils.showSuccess("Welcome!", "Your table is ready!\nPlease proceed to Table No. " + res.getTableId());
                    handleBack(new ActionEvent(confCodeField, null));
                } else if ("waiting".equals(res.getStatus())) {
                    TerminalUtils.showSuccess("Still Waiting", "You are still on the waitlist. We will notify you when a table is ready.");
                    handleBack(new ActionEvent(confCodeField, null));
                } else {
                    TerminalUtils.showError("Check-In Error", "Reservation status: " + res.getStatus());
                }
            }
        });
    }

    /**
     * Handles the server's response to a code recovery request.
     */
    public void onRecoverCodesResponse(Object response) {
        Platform.runLater(() -> {
            if (response instanceof Boolean) {
                if ((Boolean) response) {
                    TerminalUtils.showSuccess("Codes Sent", "We have sent your active confirmation codes to your registered email and phone number.");
                    handleShowCheckIn(new ActionEvent(confCodeField, null));
                } else {
                    TerminalUtils.showError("Identification Failed", "We could not find any active reservations for your account for today.");
                }
            } else {
                TerminalUtils.showError("Error", "An unexpected error occurred while recovering codes.");
            }
        });
    }
}

