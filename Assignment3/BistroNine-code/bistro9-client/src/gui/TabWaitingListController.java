package gui;

import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.ResourceBundle;

import controller.ClientController;
import data.Command;
import data.TypeMessage;
import data.ManWaiting; // Uncomment this when the server developer moves the class to the data package
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.text.SimpleDateFormat;
import javafx.scene.control.TableCell;

/**
 * Controller for the Waiting List tab in the Restaurant Management UI.
 * This class manages the display of customers currently waiting for a table
 * and provides functionality for restaurant staff to view, refresh, and remove 
 * customers from the queue.
 * 
 * It communicates with the {@link ClientController} to fetch and update waitlist 
 * data from the server.
 */
public class TabWaitingListController implements Initializable {

    /** Table displaying the queue of customers waiting for a table. */
    @FXML
    private TableView<ManWaiting> waitingTable;

    /** Column for the customer's first name. */
    @FXML
    private TableColumn<ManWaiting, String> colFirstName;

    /** Column for the customer's last name. */
    @FXML
    private TableColumn<ManWaiting, String> colLastName;

    /** Column for the customer's phone number. */
    @FXML
    private TableColumn<ManWaiting, String> colPhone;

    /** Column for the customer's email address. */
    @FXML
    private TableColumn<ManWaiting, String> colEmail;

    /** Column for the time the customer entered the waitlist. */
    @FXML
    private TableColumn<ManWaiting, Timestamp> colEntryTime;

    /** Button to manually refresh the waiting list data from the server. */
    @FXML
    private Button btnRefresh;

    /** Button to remove the selected customer from the waiting list. */
    @FXML
    private Button btnRemove;

    /** Reference to the ClientController for server communication. */
    private ClientController client;
    
    /** 
     * The customer currently selected for deletion. 
     * Stored to allow removal from the local UI list only after server confirmation. 
     */
    private ManWaiting pendingDeletion;
    
    /** Observable list acting as the data source for the waitingTable. */
    private ObservableList<ManWaiting> waitingList = FXCollections.observableArrayList();

    /**
     * Initializes the controller, setting up column bindings and custom cell factories.
     * This is called automatically after the FXML file has been loaded.
     * 
     * @param location  The location used to resolve relative paths for the root object, or null.
     * @param resources The resources used to localize the root object, or null.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Map table columns to ManWaiting object properties
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        
        // Map entry time and format the timestamp to display as HH:mm
        colEntryTime.setCellValueFactory(new PropertyValueFactory<>("entryTimeToList"));
        colEntryTime.setCellFactory(column -> new TableCell<ManWaiting, Timestamp>() {
            private SimpleDateFormat format = new SimpleDateFormat("HH:mm");
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

        // Bind the observable list to the table
        waitingTable.setItems(waitingList);
    }

    /**
     * Injects the ClientController and registers this controller instance.
     * This allows the ClientController to push updates back to this UI component.
     * 
     * @param client The ClientController instance to use for communication.
     */
    public void initData(ClientController client) {
        this.client = client;
        ClientController.tabWaitingListController = this;
    }

    /**
     * Event handler for the Refresh button.
     * Triggers a request to fetch the latest waiting list data.
     */
    @FXML
    public void handleRefresh() {
        refreshData();
    }

    /**
     * Sends a request to the server to retrieve the current waiting list.
     */
    public void refreshData() {
        if (client != null) {
            client.handleMessageFromBoundary(TypeMessage.WAITLIST, null, Command.GET_WAIT_LIST);
            System.out.println("TabWaitingList: Sending request to server for wait list...");
        }
    }

    /**
     * Event handler for the Remove button.
     * Gets the selected customer and requests their removal from the server's waitlist.
     */
    @FXML
    public void handleRemove() {
        // Get the selected item from the table
        ManWaiting selected = waitingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            displayAlert(AlertType.WARNING, "Selection Required", null, "Please select a customer from the waitlist to remove.");
            return;
        }
        
        if (client != null) {
            // Track the item being deleted to update UI later
            pendingDeletion = selected; 
            
            // Prepare the removal request data
            // The server expects: ["customer", phoneNumber, email]
            ArrayList<Object> deleteContent = new ArrayList<>();
            deleteContent.add("customer"); 
            deleteContent.add(selected.getPhoneNumber());
            deleteContent.add(selected.getEmail());
            
            // Send deletion message to server
            client.handleMessageFromBoundary(TypeMessage.WAITLIST, deleteContent, Command.DELETE_FROM_WAIT_LIST);
        }
    }

    /**
     * Processes data received from the server to update the UI.
     * This method handles both full list refreshes and confirmation of deletions.
     * 
     * @param data The data returned from the server (either an ArrayList of ManWaiting or a Boolean).
     */
    @SuppressWarnings("unchecked")
    public void updateTableData(Object data) {
        // Ensure UI updates happen on the JavaFX Application Thread
        Platform.runLater(() -> {
            if (data instanceof ArrayList) {
                // Handle full list refresh
                ArrayList<ManWaiting> list = (ArrayList<ManWaiting>) data;
                System.out.println("Updating table data with " + list.size() + " items");
                waitingList.setAll(list);
                waitingTable.refresh();
            } else if (data instanceof Boolean) {
                // Handle deletion confirmation
                boolean success = (Boolean) data;
                if (success && pendingDeletion != null) {
                    // Remove from local list if server confirmed success
                    waitingList.remove(pendingDeletion);
                    waitingTable.refresh();
                } else if (!success) {
                    displayAlert(AlertType.ERROR, "Error", "Deletion Failed", "The server failed to delete the customer from the waitlist.");
                }
                // Clear the tracking variable
                pendingDeletion = null;
            }
        });
    }

    /**
     * Displays a generic JavaFX Alert to the user.
     * Ensures the alert is shown on the JavaFX Application Thread.
     *
     * @param type    The type of alert (ERROR, WARNING, INFORMATION, etc.)
     * @param title   The title of the alert window
     * @param header  The header text (can be null)
     * @param content The main message content
     */
    private void displayAlert(AlertType type, String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
