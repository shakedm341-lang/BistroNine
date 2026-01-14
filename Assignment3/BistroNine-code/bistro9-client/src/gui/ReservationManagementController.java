package gui;

import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;

import controller.ClientController;
import data.Command;
import data.HistoryReservation;
import data.TypeMessage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

/**
 * Controller for the Reservation Management screen.
 * This class handles viewing, searching, and cancelling restaurant reservations.
 * It allows restaurant staff to search for reservations by different modes:
 * - Show All: Retrieves all reservations.
 * - By Attribute: Search by specific attributes (status, table ID, guests, code).
 * - By Date Range: Search for reservations within a specific time period.
 * 
 * Implements {@link IReservationViewer} to receive reservation lists from the server
 * and {@link IReservationDeleter} to handle cancellation responses.
 */
public class ReservationManagementController implements Initializable, IReservationViewer, IReservationDeleter {

	@FXML
	private ComboBox<String> searchModeCombo; // Dropdown for selecting search mode (All, Attribute, Date Range)

	@FXML
	private HBox allBox; // Container for "Show All" search options

	@FXML
	private HBox attributeBox; // Container for "By Attribute" search options

	@FXML
	private HBox dateRangeBox; // Container for "By Date Range" search options

	@FXML
	private ComboBox<String> attributeCombo; // Dropdown for selecting which attribute to search by

	@FXML
	private TextField attributeValueField; // Input field for numeric or text attribute values

	@FXML
	private ComboBox<String> statusComboBox; // Dropdown for selecting reservation status when searching by "status"

	@FXML
	private DatePicker startDatePicker; // Picker for start date in range search

	@FXML
	private DatePicker endDatePicker; // Picker for end date in range search

	@FXML
	private TableView<HistoryReservation> reservationsTable; // Table for displaying reservation results

	@FXML
	private TableColumn<HistoryReservation, Double> colTotalPaid; // Column for the total paid amount

	@FXML
	private TableColumn<HistoryReservation, Timestamp> colDateTime; // Column for reservation date and time

	@FXML
	private TableColumn<HistoryReservation, Integer> colGuests; // Column for number of diners

	@FXML
	private TableColumn<HistoryReservation, Integer> colConfirmationCode; // Column for reservation confirmation code

	@FXML
	private TableColumn<HistoryReservation, String> colStatus; // Column for current reservation status

	@FXML
	private TableColumn<HistoryReservation, Void> colAction; // Column containing the "Cancel" action button

	/** List of reservations backed by the TableView */
	private final ObservableList<HistoryReservation> reservationList = FXCollections.observableArrayList();

	/** Formatter for displaying date and time in the table */
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	/** Reference to the ClientController for server communication */
	private ClientController client;

	/**
	 * Initializes the controller after its root element has been completely processed.
	 * Sets up table columns, search UI visibility logic, and initial data bindings.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		initColumns();
		addCancelButtonToTable();
		reservationsTable.setItems(reservationList);
		
		setupSearchModes();
		setupAttributeFilters();
	}

	/**
	 * Configures the search mode selection logic.
	 * Toggles the visibility and layout management of different search option boxes
	 * based on the selected mode in the searchModeCombo.
	 */
	private void setupSearchModes() {
		searchModeCombo.getItems().addAll("Show All", "By Attribute", "By Date Range");
		
		searchModeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal == null) return;
			
			// Toggle visibility and managed property to reclaim space when hidden
			allBox.setVisible("Show All".equals(newVal));
			allBox.setManaged("Show All".equals(newVal));
			
			attributeBox.setVisible("By Attribute".equals(newVal));
			attributeBox.setManaged("By Attribute".equals(newVal));
			
			dateRangeBox.setVisible("By Date Range".equals(newVal));
			dateRangeBox.setManaged("By Date Range".equals(newVal));
		});
	}

	/**
	 * Configures the attribute search filters.
	 * Swaps between a TextField and a ComboBox (for status) depending on the 
	 * selected search attribute.
	 */
	private void setupAttributeFilters() {
		attributeCombo.getItems().addAll("status", "tableId", "numberOfDiners", "confirmationCode");
		statusComboBox.getItems().addAll("active", "arrived", "cancelled", "completed", "waiting");

		attributeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
			boolean isStatus = "status".equals(newVal);
			statusComboBox.setVisible(isStatus);
			statusComboBox.setManaged(isStatus);
			attributeValueField.setVisible(!isStatus);
			attributeValueField.setManaged(!isStatus);
		});
	}

	/**
	 * Initializes the TableView columns with their respective CellValueFactories and custom CellFactories.
	 * Formats total paid amount and date/time for user-friendly display.
	 */
	private void initColumns() {
		
		colTotalPaid.setCellValueFactory(new PropertyValueFactory<>("totalAmountAfterDiscount"));
		colTotalPaid.setCellFactory(column -> new TableCell<HistoryReservation, Double>() {
			@Override
			protected void updateItem(Double item, boolean empty) {
				super.updateItem(item, empty);
				// Display "-" if no payment was made, otherwise format to 2 decimal places
				if (empty || item == null || item == 0) {
					setText("-");
				} else {
					setText(String.format("%.2f", item));
				}
			}
		});

		colDateTime.setCellValueFactory(new PropertyValueFactory<>("reservationDate"));

		colDateTime.setCellFactory(column -> new TableCell<HistoryReservation, Timestamp>() {
			@Override
			protected void updateItem(Timestamp item, boolean empty) {
				super.updateItem(item, empty);
				// Format Timestamp to dd/MM/yyyy HH:mm string
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
	}

	/* ================= Cancel Button Column ================= */

	/**
	 * Dynamically adds a "Cancel" button to the action column of the reservations table.
	 * The button is only visible for future reservations that are currently "active".
	 */
	private void addCancelButtonToTable() {

		Callback<TableColumn<HistoryReservation, Void>, TableCell<HistoryReservation, Void>> cellFactory = new Callback<>() {

			@Override
			public TableCell<HistoryReservation, Void> call(final TableColumn<HistoryReservation, Void> param) {

				return new TableCell<>() {

					private final Button btn = new Button("Cancel");

					{
						btn.getStyleClass().addAll("btn-table-action", "btn-table-delete");

						btn.setOnAction(event -> {
							HistoryReservation res = getTableView().getItems().get(getIndex());
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

						HistoryReservation res = getTableView().getItems().get(getIndex());

						// Logic: Only allow cancellation if reservation is in the future AND status is 'active'
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

	/**
	 * Sets the ClientController instance and registers this controller as the reservation manager.
	 * @param client The ClientController to use for server requests.
	 */
	public void setClient(ClientController client) {
		this.client = client;
		ClientController.reservationManagementController = this;
	}

	/* ================= UI Actions ================= */

	/**
	 * Handles the "Get All" search action.
	 * Sends a request to the server to fetch all reservations.
	 */
	@FXML
	private void handleGetAll() {
		client.setReservationViewer(this);
		if(client != null) {
			client.handleMessageFromBoundary(TypeMessage.RESERVATION, null, Command.GET_ALL_RESERVATIONS);
		} else {
			showError("Client is not set");
		}
	}

	/**
	 * Handles the "Search By Attribute" action.
	 * Validates input values and sends a filtered request to the server.
	 */
	@FXML
	private void handleSearchByAttribute() {
		String attribute = attributeCombo.getValue();
		
		if (attribute == null) {
			showError("Please select an attribute");
			return;
		}

		ArrayList<Object> params = new ArrayList<>();
		params.add(attribute);

		// Logic to parse and validate attribute values based on their expected type
		try {
			if ("status".equals(attribute)) {
				String status = statusComboBox.getValue();
				if (status == null) {
					showError("Please select a status");
					return;
				}
				params.add(status);
			} else {
				String valueStr = attributeValueField.getText();
				if (valueStr == null || valueStr.isEmpty()) {
					showError("Please enter a value for " + attribute);
					return;
				}
				
				// Numeric validation for specific attributes
				if ("tableId".equals(attribute) || "numberOfDiners".equals(attribute) || "confirmationCode".equals(attribute)) {
					params.add(Integer.parseInt(valueStr));
				} else {
					params.add(valueStr);
				}
			}
		} catch (NumberFormatException e) {
			showError("Value must be numeric for " + attribute);
			return;
		}

		client.setReservationViewer(this);
		if(client != null) {
			client.handleMessageFromBoundary(TypeMessage.RESERVATION, params, Command.GET_RESERVATION_BY_ATTRIBUTE);
		} else {
			showError("Client is not set");
		}
	}

	/**
	 * Handles the "Search By Date Range" action.
	 * Validates the date selection and sends a range-based request to the server.
	 */
	@FXML
	private void handleSearchByDateRange() {
		LocalDate startDate = startDatePicker.getValue();
		LocalDate endDate = endDatePicker.getValue();

		if (startDate == null || endDate == null) {
			showError("Please select both start and end dates");
			return;
		}

		if (startDate.isAfter(endDate)) {
			showError("Start date cannot be after end date");
			return;
		}

		// Convert LocalDates to Timestamps for server compatibility (covering full day range)
		ArrayList<Object> params = new ArrayList<>();
		params.add(Timestamp.valueOf(startDate.atStartOfDay()));
		params.add(Timestamp.valueOf(endDate.atTime(LocalTime.MAX)));

		client.setReservationViewer(this);
		if(client != null) {
			client.handleMessageFromBoundary(TypeMessage.RESERVATION, params, Command.GET_ALL_RESERVATIONS_BY_DATE_RANGE);
		} else {
			showError("Client is not set");
		}
	}

	/* ================= Server Responses ================= */

	/**
	 * Updates the reservation table with data received from the server.
	 * Part of the {@link IReservationViewer} interface.
	 * @param reservations The list of reservations returned by the server.
	 */
	@Override
	public void setReservationsList(ArrayList<HistoryReservation> reservations) {
		System.out.println("DEBUG: Received " + reservations.size() + " reservations from server");
		javafx.application.Platform.runLater(() -> {
	        
	        if (reservations.size() == 0) {
	            showError("No reservations found");
	            reservationList.clear();
	            return; 
	        }
	        
	        reservationList.setAll(reservations);
	    });
	}

	/**
	 * Displays an alert based on the success of a reservation cancellation request.
	 * Part of the {@link IReservationDeleter} interface.
	 * @param isDeleted True if the reservation was successfully cancelled, false otherwise.
	 */
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
			
			// Note: Table refresh is currently left to the user to trigger by re-searching.
		});
	}

	/* ================= Helpers ================= */

	/**
	 * Initiates a cancellation request for a specific reservation.
	 * @param res The reservation object to cancel.
	 */
	private void handleCancelReservation(HistoryReservation res) {

		ArrayList<Object> params = new ArrayList<>();
		params.add(res.getConfirmationCode());
		client.setReservationDeleter(this);
		if(client != null) {
			client.handleMessageFromBoundary(TypeMessage.RESERVATION, params, Command.DELETE_RESERVATION);
		} else {
			showError("Client is not set");
		}
	}
	
	/**
	 * Utility method to display an error alert to the user.
	 * @param msg The error message to display.
	 */
	private void showError(String msg) {
	    Alert alert = new Alert(Alert.AlertType.ERROR, 
	    							msg, 
	    							javafx.scene.control.ButtonType.OK);
	    
	    alert.setHeaderText(null);
	    alert.showAndWait();
	}

}
