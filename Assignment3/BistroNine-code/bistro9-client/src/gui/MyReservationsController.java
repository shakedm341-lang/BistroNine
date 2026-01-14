package gui;

import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;

import controller.ClientController;
import data.HistoryReservation;
import data.Command;
import data.Subscriber;
import data.TypeMessage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

public class MyReservationsController implements Initializable, IReservationDeleter, IReservationViewer {

	@FXML
	private TableView<HistoryReservation> reservationsTable;

	@FXML
	private TableColumn<HistoryReservation, Timestamp> colDateTime;

	@FXML
	private TableColumn<HistoryReservation, Integer> colGuests;

	@FXML
	private TableColumn<HistoryReservation, Integer> colConfirmationCode;

	@FXML
	private TableColumn<HistoryReservation, String> colStatus;

	@FXML
	private TableColumn<HistoryReservation, Double> colTotal;

	@FXML
	private TableColumn<HistoryReservation, Double> colDiscount;

	@FXML
	private TableColumn<HistoryReservation, String> colPayment;

	@FXML
	private TableColumn<HistoryReservation, Void> colAction;

	private ObservableList<HistoryReservation> reservationList = FXCollections.observableArrayList();

	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	// Dependencies
	private ClientController clientController;
	private Subscriber currentUser;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		initColumns();

	}

	/**
	 * Initializes the table columns with appropriate cell value factories.
	 */
	private void initColumns() {

		colDateTime.setCellValueFactory(new PropertyValueFactory<>("reservationDate"));

		colDateTime.setCellFactory(column -> new TableCell<HistoryReservation, Timestamp>() {
			@Override
			protected void updateItem(Timestamp item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
				} else {
					setText(item.toLocalDateTime().format(formatter));
				}
			}
		});

		colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfDiners"));
		colConfirmationCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));

		colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

		colTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmountAfterDiscount"));
		colTotal.setCellFactory(column -> new TableCell<HistoryReservation, Double>() {
			@Override
			protected void updateItem(Double item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null || item == 0) {
					setText("-");
				} else {
					setText(String.format("%.2f", item));
				}
			}
		});

		colDiscount.setCellValueFactory(new PropertyValueFactory<>("discountSize"));
		colDiscount.setCellFactory(column -> new TableCell<HistoryReservation, Double>() {
			@Override
			protected void updateItem(Double item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null || item == 0) {
					setText("-");
				} else {
					setText(String.format("%.0f%%", item));
				}
			}
		});

		colPayment.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
		colPayment.setCellFactory(column -> new TableCell<HistoryReservation, String>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText("-");
				} else {
					setText(item);
				}
			}
		});
		
		// Add the "Cancel" button to the table
		addButtonToTable();

		reservationsTable.setItems(reservationList);
	}
	
	/**
	 * Adds a "Cancel" button to each row in the table for active future reservations.
	 */
	private void addButtonToTable() {
		
		// Define a cell factory to create the "Cancel" button in each row
		Callback<TableColumn<HistoryReservation, Void>, TableCell<HistoryReservation, Void>> cellFactory = new Callback<>() {
			
			@Override
			// Create a new TableCell with a "Cancel" button
			public TableCell<HistoryReservation, Void> call(final TableColumn<HistoryReservation, Void> param) {
				// Return a new TableCell instance
				return new TableCell<>() {

					// Create the "Cancel" button
					private final Button btn = new Button("Cancel");

					{
						btn.getStyleClass().addAll("btn-table-action", "btn-table-delete");
						btn.setOnAction((event) -> {
							HistoryReservation data = getTableView().getItems().get(getIndex());
							handleCancelReservation(data);
						});
					}

					@Override
					// Update the cell item to show or hide the button based on reservation status
					public void updateItem(Void item, boolean empty) {
						super.updateItem(item, empty);
						if (empty) {
							setGraphic(null);
						} else {
							HistoryReservation res = getTableView().getItems().get(getIndex());

							LocalDateTime resDate = res.getReservationDate().toLocalDateTime();
							boolean isFuture = resDate.isAfter(LocalDateTime.now());
							boolean isActive = "active".equalsIgnoreCase(res.getStatus());

							if (isFuture && isActive) {
								setGraphic(btn);
							} else {
								setGraphic(null);
							}
						}
					}
				};
			}
		};

		colAction.setCellFactory(cellFactory);
	}
	
	/**
	 * Sets the dependencies for this controller.
	 * 
	 * @param user       The current subscriber user.
	 * @param controller The client controller for server communication.
	 */
	public void setDependencies(Subscriber user, ClientController controller) {
		this.currentUser = user;
		this.clientController = controller;
		ClientController.MyReservation = this;

		loadReservationsData();
	}
	
	/**
	 * Loads the reservations data for the current user from the server.
	 */
	private void loadReservationsData() {

	    int customerId = currentUser.getCustomerId();

	    ArrayList<Object> params = new ArrayList<>();
	    
	    params.add("subscriber");
	    
	    params.add(customerId);
	    
	    if (clientController != null) {
	        clientController.setReservationViewer(this);
	        // Request all reservations for the current customer
	        clientController.handleMessageFromBoundary(
	                TypeMessage.RESERVATION, // The broad category
	                params, // The data (now includes type + ID)
	                Command.GET_HISTORY_RESERVATION_BY_CUSTOMER_ID // The specific command
	        );
	    } else {
	        System.err.println("Error: Client connection is null.");
	    }
	}

	/**
	 * Sets the list of reservations to be displayed in the table.
	 */
	@Override
	public void setReservationsList(ArrayList<HistoryReservation> reservations) {
		javafx.application.Platform.runLater(() ->{
			if (reservations != null) {
				if (reservations.size() == 0) {
					showError("No reservations found.");
				}
				reservationList.setAll(reservations);
			}
			
		});
		
	}
	
	/**
	 * Handles the cancellation of a reservation.
	 * 
	 * @param res The reservation to be cancelled.
	 */
	private void handleCancelReservation(HistoryReservation res) {

		// Prepare parameters for the delete reservation request
		ArrayList<Object> params = new ArrayList<>();
		params.add(res.getConfirmationCode());
		
		// Send the delete reservation request to the server
		if (clientController != null) {
			clientController.setReservationDeleter(this);
			clientController.handleMessageFromBoundary(TypeMessage.RESERVATION, 
														params, 
														Command.DELETE_RESERVATION);
		} else {
			System.err.println("Error: Client connection is null.");
		}

	}
	
	@Override
	/**
	 * Handles the server response for a reservation deletion request.
	 * 
	 * @param isDeleted true if the deletion was successful, false otherwise.
	 */
	public void handleDeleteReservationResponse(boolean isDeleted) {
		javafx.application.Platform.runLater(() -> {
			if (isDeleted) {
				showSuccessMessage("Reservation cancelled successfully!");
			} else {
				showError("Could not delete reservation. Please try again.");
			}
		});
	}

	/**
	 * Displays an error message in an alert dialog.
	 * 
	 * @param message The error message to display.
	 */
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

	
	/**
	 * Displays a success message in an alert dialog and reloads the reservations data.
	 * 
	 * @param text The success message to display.
	 */
	public void showSuccessMessage(String text) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Success");
		alert.setHeaderText(null);
		alert.setContentText(text);
		alert.showAndWait();
		loadReservationsData();
	}
}
