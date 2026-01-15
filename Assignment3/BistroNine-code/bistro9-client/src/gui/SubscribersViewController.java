package gui;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import controller.ClientController;
import data.Command;
import data.Message;
import data.Subscriber;
import data.TypeMessage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;


/**
 * Controller for the Subscribers View.
 * Handles displaying a list of subscribers in a TableView and provides
 * functionality to refresh the data from the server.
 * 
 * This class implements Initializable to set up the TableView columns
 * and link them to the Subscriber data model.
 */
public class SubscribersViewController implements Initializable {

    // --- FXML Components ---
    
    /** The table displaying subscriber information */
    @FXML
    private TableView<Subscriber> subscribersTable;

    /** Column for the Subscriber ID */
    @FXML
    private TableColumn<Subscriber, Integer> idCol;

    /** Column for the First Name */
    @FXML
    private TableColumn<Subscriber, String> firstNameCol;

    /** Column for the Last Name */
    @FXML
    private TableColumn<Subscriber, String> lastNameCol;

    /** Column for the Phone Number */
    @FXML
    private TableColumn<Subscriber, String> phoneCol;

    /** Column for the Email Address */
    @FXML
    private TableColumn<Subscriber, String> emailCol;

    /** Column for the Subscriber Type (e.g., Member, Gold, etc.) */
    @FXML
    private TableColumn<Subscriber, String> typeCol;
    
    /** Reference to the ClientController for server communication */
    private ClientController client;

    // --- Class Members ---
    
    /** ObservableList that holds the subscriber data displayed in the table */
    private ObservableList<Subscriber> subscriberList = FXCollections.observableArrayList();

    /**
     * Initializes the controller class. 
     * This method is automatically called after the FXML file has been loaded.
     * It sets up the cell value factories for each column to map Subscriber properties 
     * to the table columns.
     * 
     * @param location The location used to resolve relative paths for the root object, or null if the location is not known.
     * @param resources The resources used to localize the root object, or null if the root object was not localized.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Set up the columns. 
        // The strings inside PropertyValueFactory must match the variable names in Subscriber.java
        idCol.setCellValueFactory(new PropertyValueFactory<>("subscriberId"));
        firstNameCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        lastNameCol.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        // 2. Link the list to the table
        subscribersTable.setItems(subscriberList);
        
        // 3. Load data will now be handled lazily by the parent controller or refresh button
    }

    /**
     * Event handler for the "Refresh Table" button.
     * Triggers a request to the server to fetch the latest subscriber data.
     * 
     * @param event The ActionEvent triggered by clicking the button.
     */
    @FXML
    void refreshTableData(ActionEvent event) {
        System.out.println("Refresh button clicked. Reloading data...");
        sendRequestToServer();
    }

    /**
     * Sends a request to the server via ClientController to retrieve all subscribers.
     * Checks if the client controller is properly initialized before sending.
     */
    public void sendRequestToServer() {
        
    	if (client != null) {
            // Send request to server using the CUSTOMER message type and GET_ALL_SUBSCRIBERS command
			client.handleMessageFromBoundary(TypeMessage.CUSTOMER, // The broad category
												null, 
												Command.GET_ALL_SUBSCRIBERS // The specific command
			);
		} else {
			System.out.println("ClientController is not set. Cannot send data to server.");
		}
        
    }
    
    /**
     * Updates the subscriber table with new data received from the server.
     * This method is designed to be called from the ClientController's message handling logic.
     * The update is performed on the JavaFX Application Thread to ensure UI thread safety.
     * 
     * @param subscribers The list of subscribers to display in the table.
     */
    public void updateSubscriberTable(ArrayList<Subscriber> subscribers) {
        System.out.println("DEBUG: Received " + (subscribers != null ? subscribers.size() : 0) + " subscribers from server.");
        
        // Ensure UI updates happen on the JavaFX Application Thread
        Platform.runLater(() -> {
            // 1. Clear the existing data in the observable list
            subscriberList.clear();

            // 2. Add the new data if it's not null
            if (subscribers != null) {
                subscriberList.addAll(subscribers);
            }
            
            // 3. Optional: Refresh the table view to ensure visual update
            subscribersTable.refresh();
        });
    }
    
    /**
     * Sets the ClientController and registers this view controller with it.
     * This allows the ClientController to push updates (like subscriber lists) back to this view.
     * 
     * @param client The ClientController instance to use.
     */
    public void setClientController(ClientController client) {
		this.client = client;
		ClientController.subscribersViewController = this;
	}
    
}