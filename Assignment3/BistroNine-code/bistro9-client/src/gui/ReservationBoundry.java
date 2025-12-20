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

public class ReservationBoundry {

    @FXML private DatePicker datePicker;
    @FXML private TextField txtDiners;
    @FXML private ListView<String> timeList;
    @FXML private Button btnCheckAvailability; // Added reference to the new button

    @FXML private TextField nameTxt;
    @FXML private TextField phoneTxt;
    @FXML private Button btnCreate;
    @FXML private TextField emailTxt;
    @FXML private Button btnReturnToLogin;

    private int diners = 1; // Default diners count
    private Subscriber currentUser;
    private ClientController client;

    public void initData(Subscriber user) {
        this.currentUser = user;
        
        if (user != null) {
            // mode: SUBSCRIBER
            
            // 1. Pre-fill the fields with subscriber details
            // Note: Using First+Last name is usually better for reservations than Username
            nameTxt.setText(user.getFirstName() + " " + user.getLastName()); 
            phoneTxt.setText(user.getPhoneNumber()); 
            emailTxt.setText(user.getEmail());
            
            // 2. Disable fields - Subscribers cannot edit their registered details here
            nameTxt.setDisable(true);
            phoneTxt.setDisable(true);
            emailTxt.setDisable(true);
            
            // 3. Hide the return to login button for subscribers
            btnReturnToLogin.setVisible(false);
            
        } else {
            // mode: GUEST USER
            
            // 1. Clear any previous data to ensure a clean form
            nameTxt.clear();
            phoneTxt.clear();
            emailTxt.clear();
            
            // 2. Enable fields - Guests must manually enter their details
            nameTxt.setDisable(false);
            phoneTxt.setDisable(false);
            emailTxt.setDisable(false);
            
            // 3. Show the return to login button for guests
            btnReturnToLogin.setVisible(true);
        }
    }

    @FXML
    void initialize() {

        datePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        
        // Initialize diners text
        txtDiners.setText(String.valueOf(diners));
    }

    /**
     * Triggered when the date is changed.
     * Now it only clears the list to indicate that the user needs to check availability again.
     */
    @FXML
    void onDateSelected(ActionEvent event) {
        // Clear previous results to avoid confusion
        timeList.getItems().clear();
        btnCreate.setDisable(true); 
    }

    /**
     * New method linked to the "Check Availability" button.
     * This handles the server communication.
     */
    @FXML
    void checkAvailability(ActionEvent event) {
        LocalDate selectedDate = datePicker.getValue();

        // Validation: Ensure a date is selected
        if (selectedDate == null) {
            showAlert(AlertType.WARNING, "Missing Date", "Please select a date first.");
            return;
        }

        // Clear list before new request
        timeList.getItems().clear();
        btnCreate.setDisable(true);

        // Prepare the parameters list for the server
        // Server expects: [0] = numberOfDiners (int), [1] = reservationDate (Timestamp)
        ArrayList<Object> params = new ArrayList<>();
        
        // Add number of diners
        params.add(diners); 
       
        //Send LocalDate directly
       params.add(selectedDate);

        System.out.println("Sending request: Date=" + selectedDate + ", Diners=" + diners);

        // Send the request to the server
        if (client != null) {
            client.handleMessageFromBoundary(
                TypeMessage.RESERVATION,      // The broad category
                params,                       // The data (diners + date)
                Command.CHECK_TABLE_AVAILABILITY // The specific command
            );
        } else {
            System.err.println("Error: Client connection is null.");
        }
    }
    
    public void updateAvailableHours(ArrayList<LocalTime> availableTimes) {
        // Run on JavaFX Application Thread to avoid "Not on FX application thread" exception
        javafx.application.Platform.runLater(() -> {
            if (availableTimes == null || availableTimes.isEmpty()) {
                
                  showAlert(AlertType.INFORMATION, "No Availability", "No available tables found for this date.");
                 return;
            }

            // Convert Timestamps to simple String format (HH:mm) for the ListView
            ObservableList<String> formattedTimes = FXCollections.observableArrayList();
            
            for (LocalTime time : availableTimes) {
                // Formatting: simply convert LocalTime to String (defaults to HH:mm or HH:mm:ss)
                
                String timeStr = time.toString(); 
                formattedTimes.add(timeStr);
            }

            // Update the ListView
            timeList.setItems(formattedTimes);
            System.out.println("Updated time list with " + formattedTimes.size() + " slots.");
        });
    }

    @FXML
    void increaseDiners(ActionEvent event) {
        if (diners < 12) { 
            diners++;
            txtDiners.setText(String.valueOf(diners));
            
            // Clear list when diners change, user must click check availability again
            timeList.getItems().clear();
            btnCreate.setDisable(true);
        }
    }

    @FXML
    void decreaseDiners(ActionEvent event) {
        if (diners > 1) {
            diners--;
            txtDiners.setText(String.valueOf(diners));
            
            // Clear list when diners change, user must click check availability again
            timeList.getItems().clear();
            btnCreate.setDisable(true);
        }
    }

    @FXML
    void onTimeSelected(MouseEvent event) {
        String selectedTime = timeList.getSelectionModel().getSelectedItem();
        if (selectedTime != null) {
            btnCreate.setDisable(false); 
        }
    }

    
    @FXML
    void createReservation(ActionEvent event) {
        LocalDate date = datePicker.getValue();
        String timeStr = timeList.getSelectionModel().getSelectedItem();
        
        // 1. Validation: Ensure both date and time are selected
        if (date == null || timeStr == null) {
            showAlert(AlertType.WARNING, "Missing Details", "Please select a date and time first.");
            return;
        }

        // Get contact info (trim to handle accidental spaces)
        String phone = phoneTxt.getText().trim(); 
        String email = emailTxt.getText().trim();

        // 2. Validation: Ensure at least Phone OR Email is provided 
        if (phone.isEmpty() && email.isEmpty()) {
            showAlert(AlertType.WARNING, "Missing Contact Info", "Please enter at least a Phone Number OR an Email Address.");
            return;
        }

        // 3. Prepare Data for Server
        ArrayList<Object> params = new ArrayList<>();

        // --- Handle User Type & Contact Info ---
        if (currentUser != null) {
            // Case A: Subscriber
            params.add("subscriber");                  // Index 0: Type
            params.add(currentUser.getSubscriberId()); // Index 1: Subscriber ID
            params.add(null);                          // Index 2: Placeholder (Server skips this for subscribers)
        } else {
            // Case B: Guest 
            params.add("customer");                    // Index 0: Type
            params.add(phone.isEmpty() ? null : phone);// Index 1: Phone (or null if empty)
            params.add(email.isEmpty() ? null : email);// Index 2: Email (or null if empty)
        }

        //Add Number of Diners
        params.add(diners); // Index 3

        // --- Convert Date + Time String to SQL Timestamp ---
        try {
            
            // Combine LocalDate and selected time string into LocalDateTime
            LocalTime time = LocalTime.parse(timeStr); 
            LocalDateTime dateTime = LocalDateTime.of(date, time);
            
            // Convert to SQL Timestamp
            Timestamp reservationTimestamp = Timestamp.valueOf(dateTime);
            
            // Add to params
            params.add(reservationTimestamp); // Index 4
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "Failed to process date/time.");
            return;
        }

        System.out.println("Sending Reservation Request: " + params);

        // 4. Send to Server
        if (client != null) {
            client.handleMessageFromBoundary(
                TypeMessage.RESERVATION, 
                params, 
                Command.CREATE_NEW_RESERVATION 
            );
        }
    }
    
    /**
     * Callback method to handle the server's response.
     * The server now returns an Integer (Confirmation Code) on success, or null on failure.
     */
    public void onReservationCreationResponse(Object response) {
        javafx.application.Platform.runLater(() -> {
            if (response instanceof Integer) {
                int confirmationCode = (Integer) response;
                
                showAlert(AlertType.INFORMATION, "Reservation Confirmed", 
                        "The reservation was created successfully!\n\n" +
                        "Your Confirmation Code is: " + confirmationCode + "\n" +
                        "Please save this code for future reference.");
                
                // Optional: Clear form or go back to home
            } else {
                showAlert(AlertType.ERROR, "Failure", 
                    "Failed to create reservation.\nPlease check your details and try again.");
            }
        });
    }

    // --- MOCK SERVER LOGIC (No longer used automatically) ---
    private void loadAvailableHours(LocalDate date) {
        ObservableList<String> hours = FXCollections.observableArrayList();
        
        // Mock logic: even days have evening slots, odd days have midday slots
        if (date.getDayOfMonth() % 2 == 0) {
            hours.addAll("18:00", "18:30", "19:00", "20:30", "21:00");
        } else {
            hours.addAll("12:00", "12:30", "13:00", "14:00", "19:30", "20:00");
        }
        
        timeList.setItems(hours);
    }
    
    public void setClient(ClientController client) {
        this.client = client;
        client.reservationBoundry = this;
    }

    @FXML
    void returnToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/LoginScreen.fxml"));
            Parent root = loader.load();

            LoginController loginController = loader.getController();
            loginController.setClient(client);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("BistroNine Client - Login Screen");
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "Failed to return to login screen.");
        }
    }

    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}