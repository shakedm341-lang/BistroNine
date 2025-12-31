package gui;

import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;

import controller.ClientController;
import data.Command;
import data.TableReservation;
import data.TypeMessage;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

public class ReservationManagementController implements Initializable, IReservationViewer, IReservationDeleter {

	@FXML
	private TextField customerIdField;

	@FXML
	private TableView<TableReservation> reservationsTable;

	@FXML
	private TableColumn<TableReservation, String> colDateTime;

	@FXML
	private TableColumn<TableReservation, Integer> colGuests;

	@FXML
	private TableColumn<TableReservation, Integer> colConfirmationCode;

	@FXML
	private TableColumn<TableReservation, String> colStatus;

	@FXML
	private TableColumn<TableReservation, Void> colAction;

	private final ObservableList<TableReservation> reservationList = FXCollections.observableArrayList();

	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private ClientController client;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		initColumns();
		addCancelButtonToTable();
		reservationsTable.setItems(reservationList);
	}

	private void initColumns() {

		colDateTime.setCellValueFactory(cellData -> {
			Timestamp ts = cellData.getValue().getReservationDate();
			return new SimpleStringProperty(ts != null ? ts.toLocalDateTime().format(formatter) : "");
		});

		colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfDiners"));
		colConfirmationCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
		colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
	}

	/* ================= Cancel Button Column ================= */

	private void addCancelButtonToTable() {

		Callback<TableColumn<TableReservation, Void>, TableCell<TableReservation, Void>> cellFactory = new Callback<>() {

			@Override
			public TableCell<TableReservation, Void> call(final TableColumn<TableReservation, Void> param) {

				return new TableCell<>() {

					private final Button btn = new Button("Cancel");

					{
						btn.setStyle(
								"-fx-background-color: #ffcdd2;" + "-fx-text-fill: #b71c1c;" + "-fx-cursor: hand;");

						btn.setOnAction(event -> {
							TableReservation res = getTableView().getItems().get(getIndex());
							handleCancelReservation(res);
						});
					}

					@Override
					public void updateItem(Void item, boolean empty) {
						super.updateItem(item, empty);

						if (empty) {
							setGraphic(null);
							return;
						}

						TableReservation res = getTableView().getItems().get(getIndex());

						boolean isFuture = res.getReservationDate().toLocalDateTime().isAfter(LocalDateTime.now());

						boolean isActive = "active".equalsIgnoreCase(res.getStatus());

						setGraphic(isFuture && isActive ? btn : null);
					}
				};
			}
		};

		colAction.setCellFactory(cellFactory);
	}

	/* ================= Dependency Injection ================= */

	public void setClient(ClientController client) {
		this.client = client;
		client.reservationManagementController = this;
	}

	/* ================= UI Actions ================= */

	@FXML
	private void handleSearchByCustomer() {

	    if (customerIdField.getText().isEmpty()) {
	        showError("Please enter a customer ID");
	        return;
	    }

	    int customerId;
	    try {
	        customerId = Integer.parseInt(customerIdField.getText());
	    } catch (NumberFormatException e) {
	        showError("Customer ID must be numeric");
	        return;
	    }

	    ArrayList<Object> params = new ArrayList<>();
	    
	    params.add("subscriber"); 
	    
	    params.add(customerId);

	    client.setReservationViewer(this);
	    client.handleMessageFromBoundary(TypeMessage.RESERVATION, params, Command.GET_ALL_RESERVATIONS_BY_CUSTOMER);
	}

	@FXML
	private void handleLoadActiveReservations() {

		System.out.println("DEBUG:loading activate reservations");

//        client.handleMessageFromBoundary(
//                TypeMessage.RESERVATION,
//                null,
//                Command.GET_ACTIVE_RESERVATIONS
//        );
	}

	/* ================= Server Responses ================= */
	@Override
	public void setReservationsList(ArrayList<TableReservation> reservations) {
		System.out.println("DEBUG: Received " + reservations.size() + " reservations from server");
		javafx.application.Platform.runLater(() -> {
	        
	        if (reservations.size() == 0) {
	            showError("No reservations found for the given customer ID");
	            return; 
	        }
	        
	        reservationList.setAll(reservations);
	    });
	}

	public void handleDeleteReservationResponse(boolean isDeleted) {

		javafx.application.Platform.runLater(() -> {

			Alert alert;
			if (isDeleted) {
				alert = new Alert(Alert.AlertType.INFORMATION);
				alert.setContentText("Reservation cancelled successfully");
			} else {
				alert = new Alert(Alert.AlertType.ERROR);
				alert.setContentText("Failed to cancel reservation");
			}

			alert.setHeaderText(null);
			alert.showAndWait();
		});
	}

	/* ================= Helpers ================= */

	private void handleCancelReservation(TableReservation res) {

		ArrayList<Object> params = new ArrayList<>();
		params.add(res.getConfirmationCode());
		client.setReservationDeleter(this);
		client.handleMessageFromBoundary(TypeMessage.RESERVATION, params, Command.DELETE_RESERVATION);
	}
	
	private void showError(String msg) {
	    // One-liner to create the alert
	    Alert alert = new Alert(Alert.AlertType.ERROR, 
	    							msg, 
	    							javafx.scene.control.ButtonType.OK);
	    
	    // Remove the header to make it look cleaner
	    alert.setHeaderText(null);
	    
	    // Optional: Only needed if you want the window centered on your app
	    // alert.initOwner(opsTabPane.getScene().getWindow()); 
	    
	    alert.showAndWait();
	}

}
