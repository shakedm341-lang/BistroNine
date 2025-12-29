package gui;

import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;

import controller.ClientController;
import data.Command;
import data.Subscriber;
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

public class TabActiveReservationController implements Initializable {

    // UI Components
    @FXML private TableView<TableReservation> tblReservations;
    
    @FXML private TableColumn<TableReservation, Integer> colResId;
    @FXML private TableColumn<TableReservation, Integer> colCustomerId;
    @FXML private TableColumn<TableReservation, Integer> colTableId;
    @FXML private TableColumn<TableReservation, Integer> colDiners;
    @FXML private TableColumn<TableReservation, Timestamp> colDate; // Using Timestamp directly
    @FXML private TableColumn<TableReservation, String> colStatus;
    @FXML private TableColumn<TableReservation, Timestamp> colArrived;

    // Dependencies
    private ClientController client;
    private Subscriber currentUser;
    
    // Data list for the table
    private ObservableList<TableReservation> reservationList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
    }

    /**
     * Initializes the table columns and binds them to the TableReservation properties.
     */
    private void setupTableColumns() {
        // The string inside PropertyValueFactory must match the field name in TableReservation class exactly!
        colResId.setCellValueFactory(new PropertyValueFactory<>("reservationId"));
        colCustomerId.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        colTableId.setCellValueFactory(new PropertyValueFactory<>("tableId"));
        colDiners.setCellValueFactory(new PropertyValueFactory<>("numberOfDiners"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Handling Timestamp columns (ReservationDate)
        colDate.setCellValueFactory(new PropertyValueFactory<>("ReservationDate"));
        formatTimestampColumn(colDate);

        // Handling Timestamp columns (arrivalTime)
        colArrived.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        formatTimestampColumn(colArrived);

        // Bind the list to the table
        tblReservations.setItems(reservationList);
    }
    
    /**
     * Helper to format timestamp cells to look cleaner (e.g., "dd/MM/yyyy HH:mm")
     */
    private void formatTimestampColumn(TableColumn<TableReservation, Timestamp> column) {
        column.setCellFactory(col -> new TableCell<TableReservation, Timestamp>() {
            private SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");

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

    // --- Data Loading Logic ---

    public void initData(ClientController client) {
        this.client = client;
        
    }

    /**
     * Called by parent controller or manually by the Refresh button.
     * Triggers a request to the server.
     */
    public void refreshData() {
        
        System.out.println("TabActiveReservation: Sending request to server for active reservations...");
        
        if (client != null) {
			client.handleMessageFromBoundary(TypeMessage.RESERVATION, // The broad category
												null, 
												Command.GET_ALL_RESERVATIONS_ACTIVE // The specific command
			);
		} else {
			System.out.println("ACTIVE_RESERVATION_CONTROLLER: ClientController is not set. Cannot send data to server.");
		}
    }

    /**
     * Updates the table with data received from the server.
     * Uses Platform.runLater to ensure thread safety, as server responses 
     * usually arrive on a background thread.
     * * @param serverData The list of reservations returned from the server.
     */
    public void updateTableData(ArrayList<TableReservation> serverData) {
        if (serverData == null) {
            return;
        }

        // Execute UI updates on the JavaFX Application Thread
        Platform.runLater(() -> {
            try {
                // Clear the current list to avoid duplicates
                reservationList.clear();
                
                // Add the new data received from the server
                reservationList.addAll(serverData);
                
                // Force a refresh of the table view to ensure visual update
                tblReservations.refresh();
                
                System.out.println("Table successfully updated with " + serverData.size() + " reservations.");
                
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error updating table data: " + e.getMessage());
            }
        });
    }

    @FXML
    void onRefreshClicked(ActionEvent event) {
        refreshData();
    }
}