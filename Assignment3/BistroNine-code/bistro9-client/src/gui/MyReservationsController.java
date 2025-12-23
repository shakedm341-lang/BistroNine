package gui;

import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;

import controller.ClientController;
import data.Command;
import data.Message;
import data.Subscriber;
import data.TableReservation;
import data.TypeMessage;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.paint.Color;
import javafx.util.Callback;

public class MyReservationsController implements Initializable, IReservationViewer,IReservationDeleter {

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

	private ObservableList<TableReservation> reservationList = FXCollections.observableArrayList();

	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private ClientController clientController;
	private Subscriber currentUser;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		initColumns();

	}

	private void initColumns() {

		colDateTime.setCellValueFactory(cellData -> {
			Timestamp ts = cellData.getValue().getReservationDate();
			if (ts != null) {
				return new SimpleStringProperty(ts.toLocalDateTime().format(formatter));
			}
			return new SimpleStringProperty("");
		});

		colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfDiners"));
		colConfirmationCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));

		colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

		addButtonToTable();

		reservationsTable.setItems(reservationList);
	}

	private void addButtonToTable() {
		Callback<TableColumn<TableReservation, Void>, TableCell<TableReservation, Void>> cellFactory = new Callback<>() {
			@Override
			public TableCell<TableReservation, Void> call(final TableColumn<TableReservation, Void> param) {
				return new TableCell<>() {

					private final Button btn = new Button("Cancel");

					{
						btn.setOnAction((event) -> {
							TableReservation data = getTableView().getItems().get(getIndex());
							handleCancelReservation(data);
						});
						btn.setStyle("-fx-background-color: #ffcdd2; -fx-text-fill: #b71c1c; -fx-cursor: hand;");
					}

					@Override
					public void updateItem(Void item, boolean empty) {
						super.updateItem(item, empty);
						if (empty) {
							setGraphic(null);
						} else {
							TableReservation res = getTableView().getItems().get(getIndex());

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

	public void setDependencies(Subscriber user, ClientController controller) {
		this.currentUser = user;
		this.clientController = controller;
		controller.MyReservation = this;

		loadReservationsData();
	}

	private void loadReservationsData() {

		int customerId = currentUser.getCustomerId();

		ArrayList<Object> params = new ArrayList<>();
		params.add(customerId);
		
		if (clientController != null) {
			clientController.setReservationViewer(this);
			clientController.handleMessageFromBoundary(
					TypeMessage.RESERVATION, // The broad category
					params, // The data (customer ID)
					Command.GET_ALL_RESERVATIONS // The specific command
			);
		} else {
			System.err.println("Error: Client connection is null.");
		}
	}

	/*
	 * Sets the list of reservations to be displayed in the table.
	 */
	public void setReservationsList(ArrayList<TableReservation> reservations) {
		if (reservations != null) {
			reservationList.setAll(reservations);
		}
	}

	private void handleCancelReservation(TableReservation res) {

		ArrayList<Object> params = new ArrayList<>();
		params.add(res.getConfirmationCode());
		if (clientController != null) {
			clientController.setReservationDeleter(this);
			clientController.handleMessageFromBoundary(TypeMessage.RESERVATION, params, Command.DELETE_RESERVATION);
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
				showErrorMessage("Could not delete reservation. Please try again.");
			}
		});
	}

	private void showErrorMessage(String message) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText("Operation Failed");
		alert.setContentText(message);
		alert.showAndWait();

	}

	public void showSuccessMessage(String text) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Success");
		alert.setHeaderText(null);
		alert.setContentText(text);
		alert.showAndWait();
		loadReservationsData();
	}
}