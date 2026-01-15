package gui;

import java.net.URL;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import controller.ClientController;
import data.Command;
import data.Subscriber;
import data.TableReservation;
import data.TypeMessage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller for the Visit History view.
 * This class handles displaying the past reservations and visits of a subscriber.
 * It uses a TableView to present the reservation details, including date, time, 
 * number of guests, and reservation status.
 */
public class VisitHistoryController implements Initializable, IVisitHistory {

    /** Table displaying the subscriber's past reservations. */
    @FXML
    private TableView<TableReservation> historyTable;

    /** Column for the date of the reservation. */
    @FXML
    private TableColumn<TableReservation, Timestamp> colDate;

    /** Column for the customer's arrival time. */
    @FXML
    private TableColumn<TableReservation, Timestamp> colArrival;

    /** Column for the customer's leaving time. */
    @FXML
    private TableColumn<TableReservation, Timestamp> colLeaving;

    /** Column for the number of guests in the reservation. */
    @FXML
    private TableColumn<TableReservation, Integer> colGuests;

    /** Data source for the historyTable. */
    private ObservableList<TableReservation> historyList = FXCollections.observableArrayList();
    
    /** Formatter for displaying dates in DD/MM/YYYY format. */
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    /** Formatter for displaying times in HH:mm format. */
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    /** Reference to the ClientController for network requests. */
    private ClientController clientController;
    
    /** The subscriber whose history is being displayed. */
    private Subscriber currentUser;

    /**
     * Initializes the controller. This method is called automatically by JavaFX.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initColumns();
    }

    /**
     * Configures the TableView columns, including custom cell factories for formatting timestamps.
     */
    private void initColumns() {
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

        colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfDiners"));

        historyTable.setItems(historyList);
    }

    /**
     * Sets the dependencies for this controller and initiates the data loading process.
     * 
     * @param user The subscriber whose visit history is to be displayed.
     * @param controller The ClientController used for communication with the server.
     */
    public void setDependencies(Subscriber user, ClientController controller) {
        this.currentUser = user;
        this.clientController = controller;
        // Register this controller in the ClientController to handle incoming reservation data
        ClientController.visitHistoryController = this;
        loadHistoryData();
    }

    /**
     * Requests the visit history data from the server for the current subscriber.
     * It sends a message with the customer ID and the command to retrieve all reservations.
     */
    private void loadHistoryData() {
        if (currentUser == null || clientController == null) return;

        ArrayList<Object> params = new ArrayList<>();
        params.add("subscriber");
        params.add(currentUser.getCustomerId());

        // Send a request to the server to get all reservations for this customer
        clientController.handleMessageFromBoundary(
                TypeMessage.RESERVATION,
                params,
                Command.GET_ALL_RESERVATIONS_BY_CUSTOMER
        );
    }

    /**
     * Updates the UI with the list of reservations received from the server.
     * This method filters for completed visits and updates the table on the JavaFX Application Thread.
     * 
     * @param reservations The list of all reservations associated with the customer.
     */
    @Override
    public void setReservationsList(ArrayList<TableReservation> reservations) {
        javafx.application.Platform.runLater(() -> {
            if (reservations != null) {
                // Filter for completed visits only: 
                // 1. Explicitly marked as 'completed' status
                // 2. Or has both arrival and leaving times recorded
                ArrayList<TableReservation> completedVisits = reservations.stream()
                        .filter(res -> "completed".equalsIgnoreCase(res.getStatus()) || 
                                      (res.getArrivalTime() != null && res.getLeavingTime() != null))
                        .collect(Collectors.toCollection(ArrayList::new));
                
                // Update the observable list which triggers the TableView update
                historyList.setAll(completedVisits);
            }
        });
    }
}
