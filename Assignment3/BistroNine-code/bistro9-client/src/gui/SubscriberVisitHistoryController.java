package gui;

import java.net.URL;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import controller.ClientController;
import data.Command;
import data.TableReservation;
import data.TypeMessage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller for the Subscriber Visit History view.
 * This class handles the UI logic for searching and displaying the past visits 
 * of a specific subscriber based on their unique ID.
 * 
 * It implements {@link Initializable} for JavaFX setup and {@link IVisitHistory}
 * to receive data from the server via the {@link ClientController}.
 */
public class SubscriberVisitHistoryController implements Initializable, IVisitHistory {

    // --- FXML UI Components ---

    @FXML
    private TextField txtCustomerId;

    @FXML
    private Button btnSearch;

    @FXML
    private TableView<TableReservation> historyTable;

    @FXML
    private TableColumn<TableReservation, Timestamp> colDate;

    @FXML
    private TableColumn<TableReservation, Timestamp> colArrival;

    @FXML
    private TableColumn<TableReservation, Timestamp> colLeaving;

    @FXML
    private TableColumn<TableReservation, Integer> colGuests;

    // --- State and Configuration ---

    /** Observable list that holds the filtered reservation data for the UI table. */
    private ObservableList<TableReservation> historyList = FXCollections.observableArrayList();

    /** Formatter for displaying dates in DD/MM/YYYY format. */
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Formatter for displaying times in HH:mm format. */
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    /** Reference to the main client controller for server communication. */
    private ClientController clientController;

    /**
     * Initializes the controller class. Automatically called after the fxml file has been loaded.
     * 
     * @param location  The location used to resolve relative paths for the root object.
     * @param resources The resources used to localize the root object.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initColumns();
    }

    /**
     * Sets up the TableView columns, including custom cell factories for formatting
     * SQL Timestamps into readable date and time strings.
     */
    private void initColumns() {
        // Set up Date column with custom formatting
        colDate.setCellValueFactory(new PropertyValueFactory<>("reservationDate"));
        colDate.setCellFactory(column -> new TableCell<TableReservation, Timestamp>() {
            @Override
            protected void updateItem(Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toLocalDateTime().format(dateFormatter));
                }
            }
        });

        // Set up Arrival Time column with custom formatting
        colArrival.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        colArrival.setCellFactory(column -> new TableCell<TableReservation, Timestamp>() {
            @Override
            protected void updateItem(Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("-");
                } else {
                    setText(item.toLocalDateTime().format(timeFormatter));
                }
            }
        });

        // Set up Leaving Time column with custom formatting
        colLeaving.setCellValueFactory(new PropertyValueFactory<>("leavingTime"));
        colLeaving.setCellFactory(column -> new TableCell<TableReservation, Timestamp>() {
            @Override
            protected void updateItem(Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("-");
                } else {
                    setText(item.toLocalDateTime().format(timeFormatter));
                }
            }
        });

        // Set up Guests column
        colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfDiners"));
        
        // Bind the list to the table
        historyTable.setItems(historyList);
    }

    /**
     * Injects the ClientController dependency.
     * 
     * @param controller The ClientController instance.
     */
    public void setClientController(ClientController controller) {
        this.clientController = controller;
    }

    /**
     * Handles the search button click event.
     * Validates the input ID and sends a request to the server to fetch reservation history.
     * 
     * @param event The action event triggered by clicking the search button.
     */
    @FXML
    void onSearchClicked(ActionEvent event) {
        String idStr = txtCustomerId.getText().trim();
        
        // Basic validation for empty input
        if (idStr.isEmpty()) {
            showAlert("Input Error", "Please enter a Subscriber ID.");
            return;
        }

        try {
            int subscriberId = Integer.parseInt(idStr);
            if (clientController != null) {
                // Register this instance as the current receiver for visit history data
                clientController.setVisitHistoryViewer(this);

                // Prepare parameters for the server request
                ArrayList<Object> params = new ArrayList<>();
                // Now the server expects only the subscriber ID at index 0 for this command
                params.add(subscriberId);

                // Dispatch the request to the server
                clientController.handleMessageFromBoundary(
                        TypeMessage.RESERVATION,
                        params,
                        Command.GET_ALL_RESERVATIONS_BY_CUSTOMER
                );
            }
        } catch (NumberFormatException e) {
            showAlert("Input Error", "Subscriber ID must be a numeric value.");
        }
    }

    /**
     * Callback method implemented from {@link IVisitHistory}.
     * Receives the list of reservations from the server and filters them to display 
     * only completed visits.
     * 
     * @param reservations The list of all reservations found for the given subscriber.
     */
    @Override
    public void setReservationsList(ArrayList<TableReservation> reservations) {
        // Ensure UI updates happen on the JavaFX Application Thread
        javafx.application.Platform.runLater(() -> {
            if (reservations == null || reservations.isEmpty()) {
                historyList.clear();
                showAlert("Search Results", "No visit history found for this subscriber ID.");
                return;
            }

            // Filter logic: A visit is considered "completed" if it has the status 'completed'
            // OR if it has both an arrival time and a leaving time recorded.
            ArrayList<TableReservation> completedVisits = reservations.stream()
                    .filter(res -> "completed".equalsIgnoreCase(res.getStatus()) || 
                                  (res.getArrivalTime() != null && res.getLeavingTime() != null))
                    .collect(Collectors.toCollection(ArrayList::new));
            
            if (completedVisits.isEmpty()) {
                historyList.clear();
                showAlert("Search Results", "Subscriber has reservations, but no completed visits were found.");
            } else {
                // Update the table data
                historyList.setAll(completedVisits);
            }
        });
    }

    /**
     * Displays an information alert to the user.
     * 
     * @param title   The title of the alert window.
     * @param content The message content to display.
     */
    private void showAlert(String title, String content) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
