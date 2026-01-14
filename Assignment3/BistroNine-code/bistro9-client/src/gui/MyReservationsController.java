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

/**
 * Controller class for the "My Reservations" view.
 * This class handles displaying a list of reservations for the current subscriber,
 * allowing them to view history and cancel upcoming active reservations.
 * 
 * It implements {@link IReservationDeleter} to handle deletion responses and
 * {@link IReservationViewer} to receive the list of reservations from the server.
 */
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

	/** Action column containing the "Cancel" button */
	@FXML
	private TableColumn<HistoryReservation, Void> colAction;

	/** List of reservations to be displayed in the table */
	private ObservableList<HistoryReservation> reservationList = FXCollections.observableArrayList();

	/** Formatter for displaying date and time in the table */
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	// Dependencies for server communication and user context
	private ClientController clientController;
	private Subscriber currentUser;

	/**
	 * Called to initialize a controller after its root element has been completely processed.
	 * Sets up the table columns.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		initColumns();

	}

	/**
	 * Initializes the table columns with appropriate cell value factories.
	 */
	private void initColumns() {

		// Date and Time Column: Format Timestamp to "dd/MM/yyyy HH:mm"
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

		// Basic Data Columns
		colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfDiners"));
		colConfirmationCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));

		colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

		// Total Amount Column: Format as decimal with 2 decimal places
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

		// Discount Column: Format as percentage
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

		// Payment Method Column: Handle null values with a dash
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
		
		// Add the "Cancel" button to the table for interaction
		addButtonToTable();

		// Link the observable list to the table UI
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
				return new TableCell<>() {

					private final Button btn = new Button("Cancel");

					{
						// Apply CSS styles for the table action buttons
						btn.getStyleClass().addAll("btn-table-action", "btn-table-delete");
						btn.setOnAction((event) -> {
							// Identify the data object associated with this row
							HistoryReservation data = getTableView().getItems().get(getIndex());
							handleCancelReservation(data);
						});
					}

					@Override
					// Show or hide the button based on the reservation's business logic
					public void updateItem(Void item, boolean empty) {
						super.updateItem(item, empty);
						if (empty) {
							setGraphic(null);
						} else {
							HistoryReservation res = getTableView().getItems().get(getIndex());

							// Cancellation logic: Only allowed for active reservations in the future
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

	    // Parameters for the server request
	    ArrayList<Object> params = new ArrayList<>();
	    
	    // Identify the user type and their ID for the query
	    params.add("subscriber");
	    params.add(customerId);
	    
	    if (clientController != null) {
	        // Register this controller as the viewer for the response
	        clientController.setReservationViewer(this);
	        
	        // Request all reservations for the current customer
	        clientController.handleMessageFromBoundary(
	                TypeMessage.RESERVATION, // Broad category
	                params, // [userType, userId]
	                Command.GET_HISTORY_RESERVATION_BY_CUSTOMER_ID // Specific server action
	        );
	    } else {
	        System.err.println("Error: Client connection is null.");
	    }
	}

	/**
	 * Sets the list of reservations to be displayed in the table.
	 * This method is called by the client controller when the server response arrives.
	 */
	@Override
	public void setReservationsList(ArrayList<HistoryReservation> reservations) {
		// UI updates must be performed on the JavaFX Application Thread
		javafx.application.Platform.runLater(() ->{
			if (reservations != null) {
				if (reservations.size() == 0) {
					showError("No reservations found.");
				}
				// Refresh the table with the new data
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
		// UI updates must be performed on the JavaFX Application Thread
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
