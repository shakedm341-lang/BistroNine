package gui;

import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;

import controller.ClientController;
import data.Command;
import data.TableReservation; 
import data.TypeMessage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller class for the Active Reservations tab in the GUI.
 * This class handles the display and management of active table reservations,
 * allowing users to view current bookings and refresh the data from the server.
 */
public class TabActiveReservationController implements Initializable {

    // --- UI Components ---
    
    /** The table view displaying the list of active reservations. */
    @FXML private TableView<TableReservation> tblReservations;
    
    /** Column for displaying the Reservation ID. */
    @FXML private TableColumn<TableReservation, Integer> colResId;
    
    /** Column for displaying the Customer ID associated with the reservation. */
    @FXML private TableColumn<TableReservation, Integer> colCustomerId;
    
    /** Column for displaying the assigned Table ID. */
    @FXML private TableColumn<TableReservation, Integer> colTableId;
    
    /** Column for displaying the number of diners in the reservation. */
    @FXML private TableColumn<TableReservation, Integer> colDiners;
    
    /** Column for displaying the date and time of the reservation. */
    @FXML private TableColumn<TableReservation, Timestamp> colDate;
    
    /** Column for displaying the current status of the reservation. */
    @FXML private TableColumn<TableReservation, String> colStatus;
    
    /** Column for displaying the actual arrival time of the customers. */
    @FXML private TableColumn<TableReservation, Timestamp> colArrived;

    // --- Dependencies & State ---
    
    /** The client controller used for communication with the server. */
    private ClientController client;
    
    /** Observable list that acts as the data source for the TableView. */
    private ObservableList<TableReservation> reservationList = FXCollections.observableArrayList();

    /**
     * Initializes the controller class. This method is automatically called
     * after the FXML file has been loaded.
     * 
     * @param location The location used to resolve relative paths for the root object.
     * @param resources The resources used to localize the root object.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
    }

    /**
     * Configures the table columns by binding them to the properties of the TableReservation class.
     * Also applies custom formatting for timestamp-based columns.
     */
    private void setupTableColumns() {
        // Bind column cells to TableReservation field names using PropertyValueFactory
        colResId.setCellValueFactory(new PropertyValueFactory<>("reservationId"));
        colCustomerId.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        colTableId.setCellValueFactory(new PropertyValueFactory<>("tableId"));
        colDiners.setCellValueFactory(new PropertyValueFactory<>("numberOfDiners"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Setup and format the Reservation Date column
        colDate.setCellValueFactory(new PropertyValueFactory<>("ReservationDate"));
        formatTimestampColumn(colDate);

        // Setup and format the Arrival Time column
        colArrived.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        formatTimestampColumn(colArrived);

        // Attach the observable list to the table view
        tblReservations.setItems(reservationList);
    }
    
    /**
     * A helper method to provide a custom cell factory for Timestamp columns.
     * Formats the raw Timestamp into a user-friendly "dd/MM/yyyy HH:mm" string.
     * 
     * @param column The TableColumn to apply formatting to.
     */
    private void formatTimestampColumn(TableColumn<TableReservation, Timestamp> column) {
        column.setCellFactory(col -> new TableCell<TableReservation, Timestamp>() {
            private final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            @Override
            protected void updateItem(Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(format.format(item));
                }
            }
        });
    }

    // --- Data Loading & Synchronization Logic ---

    /**
     * Injects the ClientController dependency and registers this controller
     * in the global client state for receiving server callbacks.
     * 
     * @param client The active ClientController instance.
     */
    public void initData(ClientController client) {
        this.client = client;
        // Register this instance so the client can route incoming reservation updates here
        ClientController.tabActiveReservationController = this;      
    }

    /**
     * Requests the latest active reservation data from the server.
     * This method is called during initial load and whenever the user clicks 'Refresh'.
     */
    public void refreshData() {
        System.out.println("TabActiveReservation: Sending request to server for active reservations...");
        
        if (client != null) {
            // Send a message to the server via the client controller
            client.handleMessageFromBoundary(
                TypeMessage.RESERVATION, 
                null, 
                Command.GET_ALL_RESERVATIONS_ACTIVE
            );
        } else {
            System.err.println("ACTIVE_RESERVATION_CONTROLLER: ClientController is null. Cannot refresh.");
        }
    }

    /**
     * Updates the TableView with a new list of reservations received from the server.
     * This operation is thread-safe and executes on the JavaFX Application Thread.
     * 
     * @param serverData The list of active TableReservation objects from the database.
     */
    public void updateTableData(ArrayList<TableReservation> serverData) {
        if (serverData == null) {
            return;
        }

        // Ensure UI updates happen on the JavaFX thread to prevent concurrency issues
        Platform.runLater(() -> {
            try {
                // Update the observable list which automatically updates the UI
                reservationList.clear();
                reservationList.addAll(serverData);
                
                // Explicitly refresh the table to ensure visual consistency
                tblReservations.refresh();
                
                System.out.println("Table successfully updated with " + serverData.size() + " reservations.");
                
            } catch (Exception e) {
                System.err.println("Error updating table data: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Event handler for the 'Refresh' button.
     * 
     * @param event The ActionEvent triggered by the button click.
     */
    @FXML
    void onRefreshClicked(ActionEvent event) {
        refreshData();
    }
}
