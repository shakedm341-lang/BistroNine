package gui;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

import controller.ClientController;
import data.Command;
import data.Subscriber;
import data.TypeMessage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Boundary class for the Reservation screen.
 * This class handles user interaction for creating new table reservations.
 * It supports different user modes: logged-in Subscribers, Guests, and 
 * Representatives booking on behalf of customers.
 */
public class ReservationBoundry {

    // --- FXML UI Components ---
    @FXML private DatePicker datePicker;
    @FXML private TextField txtDiners;
    @FXML private ListView<String> timeList;
    @FXML private Button btnCheckAvailability;
    @FXML private TextField phoneTxt;
    @FXML private Button btnCreate;
    @FXML private TextField emailTxt;
    @FXML private Button btnReturnToLogin;

    // --- Controller State ---
    /** Current number of diners selected (default is 1) */
    private int diners = 1; 
    /** The currently logged-in user (null for guests) */
    private Subscriber currentUser;
    /** The client controller for server communication */
    private ClientController client;

    /** Flag indicating if the screen is being used by a Representative to book for a customer */
    private boolean isRepMod = false;
    /** Flag indicating if the screen is embedded within another UI component */
    private boolean isEmbedded = false;
    
    /**
     * Overloaded initData for simple customer mode initialization.
     * 
     * @param user The Subscriber object.
     * @param custMod true if in Representative/Customer mode.
     */
    public void initData(Subscriber user, boolean custMod) {
        initData(user, custMod, false);
    }

    /**
     * Initializes the controller with user data and operation mode.
     * Configures field visibility and editability based on whether the user is a 
     * Subscriber or a Guest/Representative.
     * 
     * @param user The Subscriber object (can be null for Guests).
     * @param custMod true if the operator is a Representative booking for a customer.
     * @param isEmbedded true if the view is embedded in another screen (e.g., Dashboard).
     */
    public void initData(Subscriber user, boolean custMod, boolean isEmbedded) {
        this.currentUser = user;
        this.isRepMod = custMod;
        this.isEmbedded = isEmbedded;

        if (user != null && !custMod) {
            // --- Subscriber Mode ---
            // 1. Pre-fill the fields with subscriber details
            phoneTxt.setText(user.getPhoneNumber());
            emailTxt.setText(user.getEmail());

            // 2. Disable fields - Subscribers cannot edit their registered details here
            phoneTxt.setDisable(true);
            emailTxt.setDisable(true);

            // 3. Hide the return to login button for subscribers
            btnReturnToLogin.setVisible(false);

        } else {
            // --- Guest or Representative Mode ---
            // 1. Clear any previous data to ensure a clean form
            phoneTxt.clear();
            emailTxt.clear();

            // 2. Enable fields - Guests must manually enter their details
            phoneTxt.setDisable(false);
            emailTxt.setDisable(false);

            if (isEmbedded) {
                btnReturnToLogin.setVisible(false);
                btnReturnToLogin.setManaged(false);
            } else if (user == null) {
                btnReturnToLogin.setVisible(true);
            } else {
                btnReturnToLogin.setVisible(false);
            }
        }
    }

    /**
     * Initialization hook called by JavaFX.
     * Sets up date restrictions and initial values.
     */
    @FXML
    void initialize() {
    	// Restrict date selection: Today up to 1 month from now
        datePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                
                LocalDate today = LocalDate.now();
                LocalDate oneMonthFromNow = today.plusMonths(1);

                // Disable if empty, in the past, or >1 month away
                setDisable(empty || date.isBefore(today) || !date.isBefore(oneMonthFromNow));
            }
        });
        
        // Initialize diners display
        txtDiners.setText(String.valueOf(diners));
    }

    /**
     * Event handler for date selection changes.
     * Clears available time slots to force a fresh availability check.
     * 
     * @param event The action event.
     */
    @FXML
    void onDateSelected(ActionEvent event) {
        timeList.getItems().clear();
        btnCreate.setDisable(true); 
    }

    /**
     * Communicates with the server to check for available table slots.
     * Validates that a date is selected before sending the request.
     * 
     * @param event The action event from the "Check Availability" button.
     */
    @FXML
    void checkAvailability(ActionEvent event) {
        checkAvailabilityInternal();
    }

    /**
     * Internal logic for checking table availability.
     * Can be called from UI events or triggered by external updates.
     */
    private void checkAvailabilityInternal() {
        LocalDate selectedDate = datePicker.getValue();

        // Validation: Ensure a date is selected
        if (selectedDate == null) {
            showAlert(AlertType.WARNING, "Missing Date", "Please select a date first.");
            return;
        }

        // Reset UI state for new request
        timeList.getItems().clear();
        btnCreate.setDisable(true);

        // Prepare parameters: [Diners (int), Date (LocalDate)]
        ArrayList<Object> params = new ArrayList<>();
        params.add(diners); 
        params.add(selectedDate);

        // Send request to server
        if (client != null) {
            client.handleMessageFromBoundary(
                TypeMessage.RESERVATION,
                params,
                Command.CHECK_TABLE_AVAILABILITY
            );
        } else {
            System.err.println("Error: Client connection is null.");
        }
        System.out.println("Sending request: Date=" + selectedDate + ", Diners=" + diners);
    }
    
    /**
     * Callback method to update the UI with available time slots from the server.
     * Filters out slots that are too close to the current time for same-day bookings.
     * 
     * @param availableTimes List of LocalTime slots returned by the server.
     */
    public void updateAvailableHours(ArrayList<LocalTime> availableTimes) {
        // Ensure UI updates happen on the JavaFX Application Thread
        javafx.application.Platform.runLater(() -> {
            if (availableTimes == null || availableTimes.isEmpty()) {
                  showAlert(AlertType.INFORMATION, "No Availability", "No available tables found for this date.");
                 return;
            }

            ObservableList<String> formattedTimes = FXCollections.observableArrayList();
            
            LocalDate selectedDate = datePicker.getValue();
            LocalDate today = LocalDate.now();
            // Minimum 1 hour lead time for same-day bookings
            LocalTime cutoffTime = LocalTime.now().plusHours(1);

            for (LocalTime time : availableTimes) {
                if (selectedDate != null && selectedDate.equals(today)) {
                    // Filter same-day slots by cutoff time
                    if (time.isAfter(cutoffTime)) {
                        formattedTimes.add(time.toString());
                    }
                } else {
                    // Future dates show all available slots
                    formattedTimes.add(time.toString());
                }
            }

            // Handle case where all today's slots were filtered out
            if (formattedTimes.isEmpty()) {
                 showAlert(AlertType.INFORMATION, "No Availability", "No available times left for today (too late to book).");
            }

            // Update the ListView display
            timeList.setItems(formattedTimes);
            System.out.println("Updated time list with " + formattedTimes.size() + " slots.");
        });
    }

    /**
     * Increases the number of diners (max 12).
     * Clears previous availability results.
     */
    @FXML
    void increaseDiners(ActionEvent event) {
        if (diners < 12) { 
            diners++;
            txtDiners.setText(String.valueOf(diners));
            
            timeList.getItems().clear();
            btnCreate.setDisable(true);
        }
    }

    /**
     * Decreases the number of diners (min 1).
     * Clears previous availability results.
     */
    @FXML
    void decreaseDiners(ActionEvent event) {
        if (diners > 1) {
            diners--;
            txtDiners.setText(String.valueOf(diners));
            
            timeList.getItems().clear();
            btnCreate.setDisable(true);
        }
    }

    /**
     * Enables the "Create Reservation" button once a time slot is selected.
     */
    @FXML
    void onTimeSelected(MouseEvent event) {
        String selectedTime = timeList.getSelectionModel().getSelectedItem();
        if (selectedTime != null) {
            btnCreate.setDisable(false); 
        }
    }

    /**
     * Validates all inputs and sends a reservation creation request to the server.
     * Handles different data packaging for Subscribers vs Guests.
     * 
     * @param event The action event from the "Create" button.
     */
    @FXML
    void createReservation(ActionEvent event) {
        LocalDate date = datePicker.getValue();
        String timeStr = timeList.getSelectionModel().getSelectedItem();
        
        // 1. Basic Validation
        if (date == null || timeStr == null) {
            showAlert(AlertType.WARNING, "Missing Details", "Please select a date and time first.");
            return;
        }

        String phone = phoneTxt.getText().trim(); 
        String email = emailTxt.getText().trim();

        // 2. Contact Info Validation (for Guests or Representative Mode)
        if (currentUser == null || isRepMod) {
            if (phone.isEmpty() && email.isEmpty()) {
                showAlert(AlertType.WARNING, "Missing Contact Info", "Please enter at least a phone number or an email address.");
                return;
            }

            // Validate Phone: 10 digits
            if (!phone.isEmpty() && !phone.matches("^\\d{10}$")) {
                showAlert(AlertType.WARNING, "Invalid Phone", "Phone number must be exactly 10 digits.");
                return;
            }

            // Validate Email format
            if (!email.isEmpty() && !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                showAlert(AlertType.WARNING, "Invalid Email", "Please enter a valid email (e.g. name@example.com).");
                return;
            }
        }

        // 3. Prepare Payload for Server
        ArrayList<Object> params = new ArrayList<>();

        // Handle Identity & Contact details
        if (currentUser != null && !isRepMod) {
            // Logged-in Subscriber
            params.add("subscriber");                  // Index 0: Type
            params.add(currentUser.getCustomerId()); // Index 1: Customer ID
            params.add(null);                          // Index 2: Placeholder
        } else {
            // Guest or external customer
            params.add("customer");                    // Index 0: Type
            params.add(phone);                         // Index 1: Phone
            params.add(email);                         // Index 2: Email
        }

        params.add(diners); // Index 3: Diners count

        // Convert Date + Time String to SQL Timestamp
        try {
            LocalTime time = LocalTime.parse(timeStr); 
            LocalDateTime dateTime = LocalDateTime.of(date, time);
            Timestamp reservationTimestamp = Timestamp.valueOf(dateTime);
            params.add(reservationTimestamp); // Index 4: Timestamp
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "Failed to process date/time.");
            return;
        }

        System.out.println("Sending Reservation Request: " + params);

        // 4. Send request
        if (client != null) {
            client.handleMessageFromBoundary(
                TypeMessage.RESERVATION, 
                params, 
                Command.CREATE_NEW_RESERVATION 
            );
        }
    }
    
    /**
     * Resets the reservation form to its initial state after a successful booking
     * or to clear all fields.
     */
    private void resetForm() {
        datePicker.setValue(null);
        diners = 1;
        txtDiners.setText(String.valueOf(diners));
        timeList.getItems().clear();
        btnCreate.setDisable(true);
        
        // Refresh based on mode
        initData(currentUser, isRepMod, isEmbedded);
    }

    /**
     * Callback method called by ClientController when the server notifies that
     * opening hours have changed. Displays an informative message and
     * automatically refreshes availability.
     */
    public void onOpeningHoursChanged() {
        javafx.application.Platform.runLater(() -> {
            showAlert(AlertType.INFORMATION, "Opening Hours Changed", 
                "The restaurant's opening hours have changed. We are re-checking availability for you.");
            checkAvailabilityInternal();
        });
    }

    /**
     * Callback method called by ClientController upon receiving a reservation response.
     * Displays success with confirmation code or failure message.
     * 
     * @param response The response from server (Integer code on success).
     */
    public void onReservationCreationResponse(Object response) {
        javafx.application.Platform.runLater(() -> {
            if (response instanceof Integer) {
                int confirmationCode = (Integer) response;
                
                showAlert(AlertType.INFORMATION, "Reservation Confirmed", 
                        "The reservation was created successfully!\n\n" +
                        "Your Confirmation Code is: " + confirmationCode + "\n" +
                        "Please save this code for future reference.");
                
                resetForm();
            } else {
                showAlert(AlertType.ERROR, "Failure", 
                    "Failed to create reservation.\nPlease check your details and try again.");
            }
        });
    }

    /**
     * Sets the ClientController and registers this boundary for callbacks.
     * 
     * @param client The ClientController instance.
     */
    public void setClient(ClientController client) {
        this.client = client;
        ClientController.subscribeReservationBoundry(this);
    }

    /**
     * Explicitly unregisters this boundary from the ClientController.
     * Should be called when navigating away from this screen.
     */
    public void unregister() {
        ClientController.unsubscribeReservationBoundry(this);
    }

    /**
     * Event handler to return to the Login screen.
     * 
     * @param event The action event.
     */
    @FXML
    void returnToLogin(ActionEvent event) {
        try {
            // Unsubscribe before navigating away to avoid logical memory leaks
            unregister();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/LoginScreen.fxml"));
            Parent root = loader.load();

            LoginController loginController = loader.getController();
            loginController.setClient(client);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("BistroNine Client - Login Screen");
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "Failed to return to login screen.");
        }
    }

    /**
     * Utility to show alert dialogs.
     * 
     * @param type The AlertType.
     * @param title The dialog title.
     * @param content The message content.
     */
    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}