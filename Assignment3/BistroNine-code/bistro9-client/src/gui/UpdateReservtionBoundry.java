package gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

import controller.ClientController;
import data.*;

public class UpdateReservtionBoundry {

	@FXML
	private TableView<TableReservation> orderTable;

	@FXML
	private TableColumn<TableReservation, Integer> orderNumCol;

	@FXML
	private TableColumn<TableReservation, String> orderDateCol;

	@FXML
	private TableColumn<TableReservation, Integer> guestsCol;

	@FXML
	private TableColumn<TableReservation, Integer> confirmCodeCol;

	@FXML
	private TableColumn<TableReservation, Integer> subscriberCol;

	@FXML
	private TableColumn<TableReservation, String> placedDateCol;

	@FXML
	private TextField dateField;

	@FXML
	private TextField orderIdField;

	@FXML
	private TextField guestsField;

	@FXML
	private Button updateBtn;

	// ObservableList to hold the data for the table
	private ObservableList<TableReservation> orderList = FXCollections.observableArrayList();

	// Reference to the client logic (for future server communication)
	private ClientController client;

	@FXML
	public void initialize() {
		// Bind columns to fields in the TableReservation class
		// PropertyValueFactory string must match the bean getter names (without 'get')
		orderNumCol.setCellValueFactory(new PropertyValueFactory<>("reservationId"));
		orderDateCol.setCellValueFactory(new PropertyValueFactory<>("reservationDate"));
		guestsCol.setCellValueFactory(new PropertyValueFactory<>("numberOfDiners"));
		confirmCodeCol.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
		subscriberCol.setCellValueFactory(new PropertyValueFactory<>("subscriberId"));
		placedDateCol.setCellValueFactory(new PropertyValueFactory<>("dateOfMakeReservation"));

		// Set the data into the table
		orderTable.setItems(orderList);


	}
	// Method to set the client controller reference
	public void setClient(ClientController client) {
		this.client = client;
		ClientController.updatereservationBoundary = this;
	}
	// Method to update the table data from server response
	 public void updateReservationTable(ArrayList<TableReservation> reservationsFromServer) {
		 Platform.runLater(new Runnable() {// Ensure UI updates are run on the JavaFX Application Thread
	            @Override
	            public void run() {
	                orderList.clear(); // Clear existing data
	                orderList.addAll(reservationsFromServer);// Add new data
	                orderTable.refresh();// Refresh the table view
	                System.out.println("GUI updated with " + reservationsFromServer.size() + " orders.");
	            }
	        });
	    }



	@FXML
	// Method to refresh table data from server
	void refreshTable(ActionEvent event) {

		System.out.println("Refreshing table data...");
		client.handleMessageFromBoundary(TypeMessage.RESERVATION, null, Command.GET_ALL_RESERVATIONS);
		
	}

	@FXML
	// Method to handle update button click
	void updateOrder(ActionEvent event) {
		// 1. Validate required fields
	    if (orderIdField.getText().isEmpty() || dateField.getText().isEmpty() || guestsField.getText().isEmpty()) {
	        showAlert("Input Error", "Please fill all fields.");
	        return;
	    }

	    // 2. Validate Guests (Not negative)
	    try {
	        int guests = Integer.parseInt(guestsField.getText());
	        if (guests < 0) {
	            showAlert("Input Error", "Number of guests cannot be negative.");
	            return;
	        }
	    } catch (NumberFormatException e) {
	        showAlert("Input Error", "Guests field must contain a valid number.");
	        return;
	    }

	    // 3. Validate Date AND Time (Not in the past)
	    try {
	        
	        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	        
	        
	        LocalDateTime inputDateTime = LocalDateTime.parse(dateField.getText(), formatter);
	        
	       
	        if (inputDateTime.isBefore(LocalDateTime.now())) {
	            showAlert("Input Error", "The date and time cannot be in the past.");
	            return;
	        }
	    } catch (DateTimeParseException e) {
	        
	        showAlert("Input Error", "Invalid format. Please use 'yyyy-MM-dd HH:mm' (e.g., 2025-05-22 14:30).");
	        return;
	    }

		ArrayList<String> params = new ArrayList<>();

		// Collect parameters for update
		params.add(orderIdField.getText());

		params.add(dateField.getText());

		params.add(guestsField.getText());
		
		// Send update request to server via client controller
		client.handleMessageFromBoundary(
                TypeMessage.RESERVATION, 
                params, 
                Command.UPDATE_RESERVATION_DETAILS
            );
	}


	// Method to show update result message
	public void showUpdateMessage(Boolean isSuccess) {
		
		// Ensure UI updates are run on the JavaFX Application Thread
		Platform.runLater(() -> {// Use Platform.runLater to ensure this runs on the JavaFX Application Thread
            if (isSuccess) {
                showAlert("Update Status", "Reservation updated successfully!");
                refreshTable(null); // Auto-refresh table after success
                orderIdField.clear();
                dateField.clear();
                guestsField.clear();
            } else {
                showAlert("Update Status", "Failed to update reservation.");
            }
        });
    }
	// Helper method to show alert dialogs
	public void showAlert(String title, String content) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}
}