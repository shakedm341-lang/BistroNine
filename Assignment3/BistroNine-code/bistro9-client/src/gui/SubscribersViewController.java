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


public class SubscribersViewController implements Initializable {

    // --- FXML Components ---
    @FXML
    private TableView<Subscriber> subscribersTable;

    @FXML
    private TableColumn<Subscriber, Integer> idCol;

    @FXML
    private TableColumn<Subscriber, String> firstNameCol;

    @FXML
    private TableColumn<Subscriber, String> lastNameCol;

    @FXML
    private TableColumn<Subscriber, String> phoneCol;

    @FXML
    private TableColumn<Subscriber, String> emailCol;

    @FXML
    private TableColumn<Subscriber, String> typeCol;
    
    private ClientController client;

    // --- Class Members ---
    // ObservableList to hold data for the table
    private ObservableList<Subscriber> subscriberList = FXCollections.observableArrayList();

    /**
     * Initializes the controller class. This method is automatically called
     * after the FXML file has been loaded.
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
        
        // 3. Load initial data when the screen opens
        sendRequestToServer(); 
    }

    /**
     * Triggered when the "Refresh Table" button is clicked.
     * @param event The click event
     */
    @FXML
    void refreshTableData(ActionEvent event) {
        System.out.println("Refresh button clicked. Reloading data...");
        sendRequestToServer();
    }

    /**
     * Sends a request to the server to get the list of subscribers
     * and updates the table.
     */
    public void sendRequestToServer() {
        
    	if (client != null) {
			client.handleMessageFromBoundary(TypeMessage.CUSTOMER, // The broad category
												null, 
												Command.GET_ALL_SUBSCRIBERS // The specific command
			);
		} else {
			System.out.println("ClientController is not set. Cannot send data to server.");
		}
        
        
    }
    
    public void updateSubscriberTable(ArrayList<Subscriber> subscribers) {
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
    
    public void setClientController(ClientController client) {
		this.client = client;
		ClientController.subscribersViewController = this;
	}
    
}