package gui;

import data.Command;
import data.Subscriber;
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
import javafx.scene.layout.VBox;

/**
 * Controller for the 'Get Table' screen in Terminal Mode.
 * Handles customer check-in via confirmation code or ReaderTag identification.
 */
public class GetTableController extends BaseTerminalController {

    @FXML
    private TextField confCodeField;

    @FXML
    private TextField recoveryPhoneField;
    @FXML
    private TextField recoveryEmailField;
    
    @FXML private VBox checkInView;
    @FXML private VBox recoveryView;
    
    @FXML private VBox tagIdentificationBox;
    @FXML private VBox scanTagView;
    @FXML private VBox codesListViewContainer;
    @FXML private ListView<Integer> codesListView;
    
    @FXML private Label checkInTitle;
    @FXML private Label codeSubtitle;
    @FXML private Button lostCodeButton;
    
    // Stores the ID from the simulated barcode/tag scan
    public static String lastScannedCode = "";

    @FXML
    public void initialize() {
        // Register this controller instance globally for message routing
        ClientController.getTableController = this;
        
        // Default UI state
        checkInView.setVisible(true);
        recoveryView.setVisible(false);
        
        // Add selection listener to the codes list
        codesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                confCodeField.setText(String.valueOf(newVal));
            }
        });
        
        // Initial state logic based on Terminal Mode
        if (BaseTerminalController.currentUserType == BaseTerminalController.UserType.SUBSCRIBER) {
            lastScannedCode = BaseTerminalController.currentSubscriberId != null ? BaseTerminalController.currentSubscriberId : "";
            
            // Subscriber check-in options
            checkInTitle.setText("Choose Identification Method");
            codeSubtitle.setText("Option 1: Enter Confirmation Code Manually");
            codeSubtitle.setVisible(true);
            codeSubtitle.setManaged(true);
            tagIdentificationBox.setVisible(true);
            tagIdentificationBox.setManaged(true);
            
            // Reset to scan view initially
            scanTagView.setVisible(true);
            scanTagView.setManaged(true);
            codesListViewContainer.setVisible(false);
            codesListViewContainer.setManaged(false);
            
            lostCodeButton.setVisible(false);
            lostCodeButton.setManaged(false);
        } else {
            // Guest check-in options (default)
            checkInTitle.setText("Enter Confirmation Code");
            codeSubtitle.setVisible(false);
            codeSubtitle.setManaged(false);
            tagIdentificationBox.setVisible(false);
            tagIdentificationBox.setManaged(false);
            lostCodeButton.setVisible(true);
            lostCodeButton.setManaged(true);
        }
    }

    /**
     * Navigates to the 'Lost Code' recovery view.
     */
    @FXML
    void handleShowRecovery(ActionEvent event) {
        checkInView.setVisible(false);
        recoveryView.setVisible(true);
    }

    /**
     * Navigates back to the main Check-In view.
     */
    @FXML
    void handleShowCheckIn(ActionEvent event) {
        recoveryView.setVisible(false);
        checkInView.setVisible(true);
    }

    @FXML
    void handleCheckInByTag(ActionEvent event) {
        TerminalUtils.simulateBarcodeScan(id -> {
            if (id != null && !id.trim().isEmpty()) {
                try {
                    int subId = Integer.parseInt(id.trim());
                    ArrayList<Object> content = new ArrayList<>();
                    content.add(subId);
                    
                    if (client != null) {
                        // Use the command string as specified on server
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

    /**
     * Handles server response for subscriber identification by tag.
     */
    public void onIdentificationResponse(Object response) {
        Platform.runLater(() -> {
            if (response instanceof Subscriber) {
                Subscriber sub = (Subscriber) response;
                int customerId = sub.getCustomerId();
                
                // Now request all active confirmation codes for this customer for today
                ArrayList<Object> content = new ArrayList<>();
                content.add(customerId);
                
                if (client != null) {
                    try {
                        client.handleMessageFromBoundary(TypeMessage.CUSTOMER, content, 
                            Command.GET_ALL_CONF_CODE_BY_CUSTOMER_ID);
                    } catch (IllegalArgumentException e) {
                        TerminalUtils.showError("System Error", "The code retrieval command is not recognized.");
                    }
                }
            } else {
                TerminalUtils.showError("Identification Failed", "Could not identify subscriber. Please check the ID and try again.");
            }
        });
    }

    /**
     * Handles server response for retrieving confirmation codes.
     */
    @SuppressWarnings("unchecked")
    public void onCodesResponse(Object response) {
        Platform.runLater(() -> {
            if (response instanceof ArrayList) {
                ArrayList<Integer> codes = (ArrayList<Integer>) response;
                if (codes.isEmpty()) {
                    TerminalUtils.showError("No Reservations", "No active reservations found for your account for today.");
                    return;
                }
                
                // Populate ListView
                codesListView.getItems().clear();
                codesListView.getItems().addAll(codes);
                
                // Show codes list area, hide scan button
                scanTagView.setVisible(false);
                scanTagView.setManaged(false);
                codesListViewContainer.setVisible(true);
                codesListViewContainer.setManaged(true);
            } else {
                TerminalUtils.showError("Error", "Failed to retrieve confirmation codes.");
            }
        });
    }

    @FXML
    void handleResetIdentification(ActionEvent event) {
        // Reset to scan view
        codesListViewContainer.setVisible(false);
        codesListViewContainer.setManaged(false);
        scanTagView.setVisible(true);
        scanTagView.setManaged(true);
        
        // Clear selection
        codesListView.getSelectionModel().clearSelection();
        confCodeField.clear();
    }

    /**
     * Attempts check-in using the manually entered 6-digit confirmation code.
     */
    @FXML
    void handleCheckInByCode(ActionEvent event) {
        String codeText = confCodeField.getText().trim();
        if (codeText.isEmpty()) {
            TerminalUtils.showError("Input Error", "Please enter your 6-digit confirmation code.");
            return;
        }

        try {
            int code = Integer.parseInt(codeText);
            sendCheckInRequest(code);
        } catch (NumberFormatException e) {
            TerminalUtils.showError("Input Error", "Confirmation code must be a number.");
        }
    }

    /**
     * Handles the 'Recover Lost Codes' action.
     * Requests the server to send codes via SMS/Email using the provided contact info.
     */
    @FXML
    void handleRecoverLostCodes(ActionEvent event) {
        String phone = recoveryPhoneField.getText().trim();
        String email = recoveryEmailField.getText().trim();

        if (phone.isEmpty() && email.isEmpty()) {
            TerminalUtils.showError("Input Error", "Please provide either your phone number or email address.");
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

