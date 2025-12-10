package gui;

import java.time.LocalDate;
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

public class ReservationBoundry {

    @FXML private DatePicker datePicker;
    @FXML private TextField txtDiners;
    @FXML private ListView<String> timeList;
    
    @FXML private TextField nameTxt;
    @FXML private TextField phoneTxt;
    @FXML private Button btnCreate;

    private int diners = 2; // Default diners count
    private StubUser currentUser;

    public void initData(StubUser user) {
        this.currentUser = user;
        if (user != null) {
            nameTxt.setText(user.getUsername()); 
            phoneTxt.setText("050-0000000"); 
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
    }

    @FXML
    void onDateSelected(ActionEvent event) {
        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate != null) {
        	// Fetch available hours from server (mocked here)
            loadAvailableHours(selectedDate);	
            //To Do : call server with date and diners to get available hours
            btnCreate.setDisable(true); 
        }
    }

    @FXML
    void increaseDiners(ActionEvent event) {
        if (diners < 12) { 
            diners++;
            txtDiners.setText(String.valueOf(diners));
            
             if (datePicker.getValue() != null) loadAvailableHours(datePicker.getValue());
        }
    }

    @FXML
    void decreaseDiners(ActionEvent event) {
        if (diners > 1) {
            diners--;
            txtDiners.setText(String.valueOf(diners));
            if (datePicker.getValue() != null) loadAvailableHours(datePicker.getValue());
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
        String time = timeList.getSelectionModel().getSelectedItem();
        
        // Simple confirmation message
        String msg = String.format("Reservation Request:\nDate: %s\nTime: %s\nDiners: %d\nUser: %s", 
                date, time, diners, currentUser.getUsername());
        
        showAlert(AlertType.INFORMATION, "Reservation Sent", "We are processing your request!\n\n" + msg);
        
        // TODO: Send to Server...
    }

    // --- MOCK SERVER LOGIC ---
    
    private void loadAvailableHours(LocalDate date) {
        ObservableList<String> hours = FXCollections.observableArrayList();
        
        //reality: fetch from server based on date and diners
        
        // Mock logic: even days have evening slots, odd days have midday slots
        if (date.getDayOfMonth() % 2 == 0) {
            hours.addAll("18:00", "18:30", "19:00", "20:30", "21:00");
        } else {
            hours.addAll("12:00", "12:30", "13:00", "14:00", "19:30", "20:00");
        }
        
        timeList.setItems(hours);
    }

    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}