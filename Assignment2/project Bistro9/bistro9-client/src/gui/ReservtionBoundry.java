package gui;

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


import java.util.ArrayList;

import controller.ClientController;
import data.*;

public class ReservtionBoundry {

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

		// Load mock data for testing purposes
		// loadMockData();

		// When a row is selected, populate the edit fields so the user can update
		// easily
//        orderTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
//            if (newSel != null) {
//                orderIdField.setText(String.valueOf(newSel.getReservationId()));
//                dateField.setText(newSel.getReservationDate());
//                guestsField.setText(String.valueOf(newSel.getNumberOfDiners()));
//            }
//        });
	}

	public void setClient(ClientController client) {
		this.client = client;
		ClientController.reservationBoundary = this;
	}
	
	 public void updateReservationTable(ArrayList<TableReservation> reservationsFromServer) {
	        orderList.clear(); 
	        orderList.addAll(reservationsFromServer); 
	        orderTable.refresh(); 
	        System.out.println("GUI updated with " + reservationsFromServer.size() + " orders.");
	    }

//    /**
//     * Generates mock data so we can verify the UI without a server.
//     */
//    private void loadMockData() {
//        orderList.clear();
//
//        TableReservation r1 = new TableReservation();
//        r1.setReservationId("101");
//        r1.setReservationDate("2025-01-01 19:00");
//        r1.setNumberOfDiners("4");
//        r1.setConfirmationCode("5551");
//        r1.setSubscriberId("1");
//        r1.setDateOfMakeReservation("2024-12-20");
//
//        TableReservation r2 = new TableReservation();
//        r2.setReservationId("102");
//        r2.setReservationDate("2025-01-02 20:00");
//        r2.setNumberOfDiners("2");
//        r2.setConfirmationCode("5552");
//        r2.setSubscriberId("2");
//        r2.setDateOfMakeReservation("2024-12-21");
//
//        TableReservation r3 = new TableReservation();
//        r3.setReservationId("103");
//        r3.setReservationDate("2025-01-03 18:30");
//        r3.setNumberOfDiners("6");
//        r3.setConfirmationCode("5553");
//        r3.setSubscriberId("3");
//        r3.setDateOfMakeReservation("2024-12-22");
//
//        orderList.addAll(r1, r2, r3);
//    }

	@FXML
	void refreshTable(ActionEvent event) {

		System.out.println("Refreshing table data...");
		client.handleMessageFromBoundary(TypeMessage.RESERVATION, null, Command.GET_ALL_RESERVATIONS);
		
	}

	@FXML
	void updateOrder(ActionEvent event) {
		// Validate required fields: order ID, date and guests
		if (orderIdField.getText().isEmpty() || dateField.getText().isEmpty() || guestsField.getText().isEmpty()) {
			showAlert("Input Error", "Please fill all fields (Order ID, Date, Guests).");
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


	
	public void showUpdateMessage(Boolean isSuccess) {
        if (isSuccess) {
            showAlert("Update Status", "Reservation updated successfully!");
            
            refreshTable(null); 
            
            orderIdField.clear();
            dateField.clear();
            guestsField.clear();
        } else {
            showAlert("Update Status", "Failed to update reservation. ");
        }
    }

	private void showAlert(String title, String content) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}
}