package gui;

import controller.ClientController;
import data.Command;
import data.Subscriber;
import data.TypeMessage;
import java.util.ArrayList;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LeaveWaitlistController extends BaseTerminalController {

    @FXML private RadioButton rbGuest;
    @FXML private RadioButton rbSubscriber;
    @FXML private Label lblInstruction;
    @FXML private VBox guestFields;
    @FXML private VBox subscriberFields;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField subscriberIdField;
    @FXML private Button btnBack;
    @FXML private Button btnRemove;

    private boolean isDashboardMode = false;
    private Subscriber dashboardUser;

    @FXML
    public void initialize() {
        // Register this controller instance globally for message routing
        ClientController.leaveWaitlistController = this;

        // Hide radio buttons by default (will be managed in init/setDependencies)
        rbGuest.setVisible(false);
        rbGuest.setManaged(false);
        rbSubscriber.setVisible(false);
        rbSubscriber.setManaged(false);

        // Show/hide fields based on selection
        rbGuest.selectedProperty().addListener((obs, oldVal, newVal) -> {
            guestFields.setVisible(newVal);
            guestFields.setManaged(newVal);
        });
        rbSubscriber.selectedProperty().addListener((obs, oldVal, newVal) -> {
            subscriberFields.setVisible(newVal);
            subscriberFields.setManaged(newVal);
        });

        // Set initial state based on Terminal Mode (default)
        if (!isDashboardMode) {
            setupTerminalMode();
        }
    }

    public void setTerminalMode(boolean isTerminal) {
        if (isTerminal) {
            Platform.runLater(() -> {
                btnBack.setVisible(false);
                btnBack.setManaged(false);
                
                // For casual guests in terminal mode, we should pre-select guest type if not already
                if (BaseTerminalController.currentUserType == BaseTerminalController.UserType.GUEST) {
                    rbGuest.setSelected(true);
                    rbSubscriber.setDisable(true);
                }
            });
        }
    }

    private void setupTerminalMode() {
        if (BaseTerminalController.currentUserType == BaseTerminalController.UserType.SUBSCRIBER) {
            rbSubscriber.setSelected(true);
            rbGuest.setSelected(false);
            rbGuest.setDisable(true);
            
            guestFields.setVisible(false);
            guestFields.setManaged(false);
            subscriberFields.setVisible(true);
            subscriberFields.setManaged(true);
            
            subscriberIdField.setText(BaseTerminalController.currentSubscriberId != null ? BaseTerminalController.currentSubscriberId : "");
            subscriberIdField.setEditable(false);
        } else {
            rbGuest.setSelected(true);
            rbSubscriber.setSelected(false);
            rbSubscriber.setDisable(true);
            
            guestFields.setVisible(true);
            guestFields.setManaged(true);
            subscriberFields.setVisible(false);
            subscriberFields.setManaged(false);
            
            // For guest mode, button is disabled until at least one field is filled
            updateLeaveButtonState();
        }

        // Add listeners to enable/disable remove button as user types
        phoneField.textProperty().addListener((obs, old, newValue) -> updateLeaveButtonState());
        emailField.textProperty().addListener((obs, old, newValue) -> updateLeaveButtonState());
    }

    /**
     * Updates the REMOVE FROM WAITLIST button state based on the presence of contact information.
     * Guests must provide at least a phone number or an email.
     */
    private void updateLeaveButtonState() {
        if (!isDashboardMode && BaseTerminalController.currentUserType == BaseTerminalController.UserType.GUEST) {
            boolean hasPhone = !phoneField.getText().trim().isEmpty();
            boolean hasEmail = !emailField.getText().trim().isEmpty();
            btnRemove.setDisable(!hasPhone && !hasEmail);
        } else {
            // Subscribers or dashboard mode (pre-filled ID) are always enabled
            btnRemove.setDisable(false);
        }
    }

    /**
     * Called when the screen is loaded from the User Dashboard.
     */
    public void setDashboardDependencies(ClientController client, Subscriber user) {
        this.client = client;
        this.dashboardUser = user;
        this.isDashboardMode = true;
        ClientController.leaveWaitlistController = this;

        Platform.runLater(() -> {
            // Hide guest option entirely
            rbGuest.setVisible(false);
            rbGuest.setManaged(false);
            rbSubscriber.setVisible(false);
            rbSubscriber.setManaged(false);
            rbSubscriber.setSelected(true);

            // Hide guest fields
            guestFields.setVisible(false);
            guestFields.setManaged(false);

            // Show subscriber fields but hide the scanner
            subscriberFields.setVisible(true);
            subscriberFields.setManaged(true);

            // Pre-fill and lock subscriber ID
            subscriberIdField.setText(String.valueOf(user.getCustomerId()));
            subscriberIdField.setEditable(false);

            // Hide Back button as requested
            btnBack.setVisible(false);
            btnBack.setManaged(false);
        });
    }

    @FXML
    void handleLeave(ActionEvent event) {
        String type = rbSubscriber.isSelected() ? "subscriber" : "customer";
        
        if (rbSubscriber.isSelected() && !isDashboardMode) {
            if (BaseTerminalController.currentSubscriberId == null || BaseTerminalController.currentSubscriberId.isEmpty()) {
                // Should not happen in terminal mode anymore
                TerminalUtils.showError("Identification Required", "Please login at the beginning.");
                return;
            }
        }
        performLeave(type);
    }

    @FXML
    @Override
    protected void handleBack(ActionEvent event) {
        if (isDashboardMode) {
            // Do nothing, or we could switch back to a default view if the button was visible
            return;
        }
        super.handleBack(event);
    }

    private void performLeave(String type) {
        Object identifier = null;
        String email = null;

        if (type.equals("subscriber")) {
            String subIdText;
            if (isDashboardMode) {
                subIdText = String.valueOf(dashboardUser.getCustomerId());
            } else {
                subIdText = BaseTerminalController.currentSubscriberId;
            }
            
            subscriberIdField.setText(subIdText);
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

        if (client != null) {
            client.handleMessageFromBoundary(TypeMessage.WAITLIST, content, Command.DELETE_FROM_WAIT_LIST);
        } else {
            TerminalUtils.showError("Error", "Client is not connected.");
        }
    }

    public void onDeleteResponse(Object response) {
        Platform.runLater(() -> {
            if (Boolean.TRUE.equals(response)) {
                TerminalUtils.showSuccess("Success", "You have been removed from the waitlist successfully.");
                if (!isDashboardMode) {
                    handleBack(new ActionEvent(rbSubscriber, null));
                }
            } else {
                TerminalUtils.showError("Error", "We couldn't find your entry on the waitlist or an error occurred.");
            }
        });
    }
}

