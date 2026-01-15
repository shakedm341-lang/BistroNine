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
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Controller class for the "Cancel Registration" screen.
 * <p>
 * This class handles the logic for removing a guest or subscriber from the restaurant's waitlist
 * or cancelling an existing reservation by confirmation code.
 * It supports two primary operating modes:
 * <ul>
 *   <li><b>Terminal Mode:</b> Used at the physical restaurant terminal by walk-in guests or logged-in subscribers.</li>
 *   <li><b>Dashboard Mode:</b> Used by subscribers logged into their personal dashboard.</li>
 * </ul>
 */
public class CancelRegistrationController extends BaseTerminalController implements IReservationDeleter {

    // FXML UI Components
    @FXML private ToggleButton rbWaitlist;
    @FXML private ToggleButton rbReservation;
    @FXML private VBox waitlistModeContainer;
    @FXML private VBox reservationModeContainer;
    
    @FXML private RadioButton rbGuest;
    @FXML private RadioButton rbSubscriber;
    @FXML private Label lblInstruction;
    @FXML private VBox guestFields;
    @FXML private VBox subscriberFields;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField subscriberIdField;
    
    @FXML private TextField confCodeField;
    
    @FXML private Button btnBack;
    @FXML private Button btnAction;

    /** Flag indicating if the controller is being used within the Subscriber Dashboard */
    private boolean isDashboardMode = false;
    
    /** The subscriber currently using the dashboard, if applicable */
    private Subscriber dashboardUser;

    /**
     * Initializes the controller. This method is automatically called after the FXML file has been loaded.
     * Sets up visibility listeners for user type selection and configures the initial state.
     */
    @FXML
    public void initialize() {
        // Register this controller instance globally for message routing from the ClientController
        ClientController.cancelRegistrationController = this;

        // Mode Toggling: Exit Waitlist vs Cancel Reservation
        rbWaitlist.selectedProperty().addListener((obs, oldVal, newVal) -> {
            waitlistModeContainer.setVisible(newVal);
            waitlistModeContainer.setManaged(newVal);
            updateButtonState();
        });
        rbReservation.selectedProperty().addListener((obs, oldVal, newVal) -> {
            reservationModeContainer.setVisible(newVal);
            reservationModeContainer.setManaged(newVal);
            updateButtonState();
        });

        // Radio buttons for User Type (within Waitlist mode)
        rbGuest.selectedProperty().addListener((obs, oldVal, newVal) -> {
            guestFields.setVisible(newVal);
            guestFields.setManaged(newVal);
            updateButtonState();
        });
        rbSubscriber.selectedProperty().addListener((obs, oldVal, newVal) -> {
            subscriberFields.setVisible(newVal);
            subscriberFields.setManaged(newVal);
            updateButtonState();
        });

        // Hide user type radios by default in some modes
        rbGuest.setVisible(false);
        rbGuest.setManaged(false);
        rbSubscriber.setVisible(false);
        rbSubscriber.setManaged(false);

        // Real-time validation listeners
        phoneField.textProperty().addListener((obs, old, newValue) -> updateButtonState());
        emailField.textProperty().addListener((obs, old, newValue) -> updateButtonState());
        confCodeField.textProperty().addListener((obs, old, newValue) -> updateButtonState());

        // Set initial state based on Terminal Mode (default if not explicitly set to Dashboard)
        if (!isDashboardMode) {
            setupTerminalMode();
        }
    }

    /**
     * Configures the UI for Terminal Mode.
     * 
     * @param isTerminal True if the app is running in terminal mode.
     */
    public void setTerminalMode(boolean isTerminal) {
        if (isTerminal) {
            Platform.runLater(() -> {
                // Hide back button in terminal mode to maintain flow
                btnBack.setVisible(false);
                btnBack.setManaged(false);
                
                // For walk-in guests at the terminal, pre-select guest type
                if (BaseTerminalController.currentUserType == BaseTerminalController.UserType.GUEST) {
                    rbGuest.setSelected(true);
                    rbSubscriber.setDisable(true);
                }
            });
        }
    }

    /**
     * Internal helper to set up the UI based on the current user's session type (Subscriber vs Guest).
     */
    private void setupTerminalMode() {
        if (BaseTerminalController.currentUserType == BaseTerminalController.UserType.SUBSCRIBER) {
            // User is a logged-in subscriber
            rbSubscriber.setSelected(true);
            rbGuest.setSelected(false);
            rbGuest.setDisable(true);
            
            guestFields.setVisible(false);
            guestFields.setManaged(false);
            subscriberFields.setVisible(true);
            subscriberFields.setManaged(true);
            
            // Pre-fill and lock the ID field for the logged-in user
            subscriberIdField.setText(BaseTerminalController.currentSubscriberId != null ? BaseTerminalController.currentSubscriberId : "");
            subscriberIdField.setEditable(false);
        } else {
            // User is a walk-in guest
            rbGuest.setSelected(true);
            rbSubscriber.setSelected(false);
            rbSubscriber.setDisable(true);
            
            guestFields.setVisible(true);
            guestFields.setManaged(true);
            subscriberFields.setVisible(false);
            subscriberFields.setManaged(false);
            
            // Validate input immediately to set button state
            updateButtonState();
        }
    }

    /**
     * Updates the state of the main action button based on the selected mode and input validity.
     */
    private void updateButtonState() {
        if (rbWaitlist.isSelected()) {
            btnAction.setText("REMOVE FROM WAITLIST");
            if (!isDashboardMode && BaseTerminalController.currentUserType == BaseTerminalController.UserType.GUEST) {
                boolean hasPhone = !phoneField.getText().trim().isEmpty();
                boolean hasEmail = !emailField.getText().trim().isEmpty();
                btnAction.setDisable(!hasPhone && !hasEmail);
            } else {
                btnAction.setDisable(false);
            }
        } else {
            btnAction.setText("CANCEL RESERVATION");
            String codeText = confCodeField.getText().trim();
            boolean hasCode = !codeText.isEmpty() && codeText.matches("\\d+");
            btnAction.setDisable(!hasCode);
        }
    }

    /**
     * Configures the controller for use within the Subscriber Dashboard.
     * 
     * @param client The ClientController for server communication.
     * @param user The Subscriber object for the current user.
     */
    public void setDashboardDependencies(ClientController client, Subscriber user) {
        this.client = client;
        this.dashboardUser = user;
        this.isDashboardMode = true;
        ClientController.cancelRegistrationController = this;

        Platform.runLater(() -> {
            // Hide selection UI as dashboard users are always subscribers
            rbGuest.setVisible(false);
            rbGuest.setManaged(false);
            rbSubscriber.setVisible(false);
            rbSubscriber.setManaged(false);
            rbSubscriber.setSelected(true);

            // Hide guest input fields
            guestFields.setVisible(false);
            guestFields.setManaged(false);

            // Show subscriber input fields
            subscriberFields.setVisible(true);
            subscriberFields.setManaged(true);

            // Pre-fill and lock subscriber ID from the dashboard user object
            subscriberIdField.setText(String.valueOf(user.getCustomerId()));
            subscriberIdField.setEditable(false);

            // Hide Back button in dashboard view
            btnBack.setVisible(false);
            btnBack.setManaged(false);
        });
    }

    /**
     * FXML Action handler for the main action button.
     * 
     * @param event The action event.
     */
    @FXML
    void handleAction(ActionEvent event) {
        if (rbWaitlist.isSelected()) {
            String type = rbSubscriber.isSelected() ? "subscriber" : "customer";
            
            // Security check for terminal mode subscribers
            if (rbSubscriber.isSelected() && !isDashboardMode) {
                if (BaseTerminalController.currentSubscriberId == null || BaseTerminalController.currentSubscriberId.isEmpty()) {
                    TerminalUtils.showError("Identification Required", "Please login at the beginning.");
                    return;
                }
            }
            performLeave(type);
        } else {
            performCancelReservation();
        }
    }

    /**
     * Overrides the standard back button behavior to handle dashboard constraints.
     */
    @FXML
    @Override
    protected void handleBack(ActionEvent event) {
        if (isDashboardMode) {
            return;
        }
        super.handleBack(event);
    }

    /**
     * Gathers user input, performs validation, and sends the deletion request for waitlist to the server.
     * 
     * @param type The type of user attempting to leave ("subscriber" or "customer").
     */
    private void performLeave(String type) {
        Object identifier = null;
        String email = null;

        if (type.equals("subscriber")) {
            String subIdText = isDashboardMode ? String.valueOf(dashboardUser.getCustomerId()) : BaseTerminalController.currentSubscriberId;
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

            if (!phone.isEmpty() && !phone.matches("^\\d{10}$")) {
                TerminalUtils.showError("Invalid Phone", "Phone number must be exactly 10 digits.");
                return;
            }

            if (!email.isEmpty() && !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                TerminalUtils.showError("Invalid Email", "Please enter a valid email (e.g. name@example.com).");
                return;
            }

            identifier = phone.isEmpty() ? null : phone;
            email = email.isEmpty() ? null : email;
        }

        ArrayList<Object> content = new ArrayList<>();
        content.add(type);       // 0: user type
        content.add(identifier); // 1: phone string or subscriber ID integer
        content.add(email);      // 2: email string (if provided)

        if (client != null) {
            client.handleMessageFromBoundary(TypeMessage.WAITLIST, content, Command.DELETE_FROM_WAIT_LIST);
        } else {
            TerminalUtils.showError("Error", "Client is not connected.");
        }
    }

    /**
     * Gathers the confirmation code and sends a reservation deletion request to the server.
     */
    private void performCancelReservation() {
        String codeText = confCodeField.getText().trim();
        if (codeText.isEmpty() || !codeText.matches("\\d+")) {
            TerminalUtils.showError("Input Error", "Please enter a valid confirmation code (digits only).");
            return;
        }

        int code;
        try {
            code = Integer.parseInt(codeText);
        } catch (NumberFormatException e) {
            TerminalUtils.showError("Input Error", "Invalid Confirmation Code.");
            return;
        }

        ArrayList<Object> content = new ArrayList<>();
        content.add(code); // 0: confirmation code

        if (client != null) {
            // Register this controller as the deleter to receive the response
            client.setReservationDeleter(this);
            // Note: DELETE_RESERVATION handles both waitlist and fixed reservations on the server side
            client.handleMessageFromBoundary(TypeMessage.RESERVATION, content, Command.DELETE_RESERVATION);
        } else {
            TerminalUtils.showError("Error", "Client is not connected.");
        }
    }

    /**
     * Implementation of IReservationDeleter callback.
     * 
     * @param isDeleted true if successfully deleted.
     */
    @Override
    public void handleDeleteReservationResponse(boolean isDeleted) {
        onDeleteResponse(isDeleted);
    }

    /**
     * Callback method called when the server responds to the deletion request.
     * 
     * @param response Boolean response indicating success or failure.
     */
    public void onDeleteResponse(Object response) {
        Platform.runLater(() -> {
            if (Boolean.TRUE.equals(response)) {
                String msg = rbWaitlist.isSelected() ? 
                    "You have been removed from the waitlist successfully." : 
                    "Your reservation has been cancelled successfully.";
                TerminalUtils.showSuccess("Success", msg);
                if (!isDashboardMode) {
                    handleBack(new ActionEvent(btnAction, null));
                }
            } else {
                String msg = rbWaitlist.isSelected() ? 
                    "We couldn't find your entry on the waitlist or an error occurred." : 
                    "Invalid confirmation code or reservation not found.";
                TerminalUtils.showError("Error", msg);
            }
        });
    }
}
