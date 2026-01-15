package gui;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import controller.ClientController;
import data.Command;
import data.Customer;
import data.Subscriber;
import data.TypeMessage;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller for the "Current Diners" tab in the restaurant management system.
 * This class is responsible for displaying a real-time list of all customers 
 * (both Guests and Subscribers) who are currently seated in the restaurant.
 * 
 * It manages a TableView that dynamically adjusts displayed information based on 
 * whether a customer is a registered subscriber or a walk-in guest.
 */
public class TabCurrentDinersController implements Initializable {

    // --- UI Components ---
    
    /** Table displaying the list of current diners */
    @FXML private TableView<Customer> tblDiners;
    
    /** Column for Customer/Subscriber ID */
    @FXML private TableColumn<Customer, Integer> colId;
    
    /** Column for First Name - displays 'Guest' for non-subscribers */
    @FXML private TableColumn<Customer, String> colFirstName;
    
    /** Column for Last Name - displays '-' for non-subscribers */
    @FXML private TableColumn<Customer, String> colLastName;
    
    /** Column for Phone Number */
    @FXML private TableColumn<Customer, String> colPhone;
    
    /** Column for Email Address */
    @FXML private TableColumn<Customer, String> colEmail;
    
    /** Column indicating if the customer is a 'Subscriber' or 'Regular Customer' */
    @FXML private TableColumn<Customer, String> colType;

    // --- Dependencies & Data ---
    
    /** Reference to the main client controller for server communication */
    private ClientController client;
    
    /** Local list of diners being displayed in the table */
    private ObservableList<Customer> dinersList = FXCollections.observableArrayList();

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
    }

    /**
     * Configures the TableView columns.
     * Uses both standard PropertyValueFactory for simple fields and custom 
     * cell value factories to handle polymorphic behavior between Customer and Subscriber.
     */
    private void setupTableColumns() {
        // 1. Basic columns: Map directly to fields in the 'Customer' class
        colId.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // 2. Smart columns: These columns check if the Customer object is an instance 
        // of Subscriber to extract additional details like names.
        
        // Logic for First Name Column
        colFirstName.setCellValueFactory(cellData -> {
            Customer c = cellData.getValue();
            if (c instanceof Subscriber) {
                // If the customer is a subscriber, cast and return their first name
                return new SimpleStringProperty(((Subscriber) c).getFirstName());
            } else {
                // Otherwise, treat them as an anonymous walk-in guest
                return new SimpleStringProperty("Guest");
            }
        });

        // Logic for Last Name Column
        colLastName.setCellValueFactory(cellData -> {
            Customer c = cellData.getValue();
            if (c instanceof Subscriber) {
                return new SimpleStringProperty(((Subscriber) c).getLastName());
            } else {
                // No last name recorded for guests
                return new SimpleStringProperty("-");
            }
        });

        // Logic for Type Column: Helps staff distinguish between regular users and subscribers
        colType.setCellValueFactory(cellData -> {
            Customer c = cellData.getValue();
            if (c instanceof Subscriber) {
                return new SimpleStringProperty("Subscriber");
            } else {
                return new SimpleStringProperty("Regular Customer");
            }
        });

        // Connect the observable list to the table UI
        tblDiners.setItems(dinersList);
    }

    // --- Data Loading Logic ---

    /**
     * Injects the ClientController dependency and registers this controller 
     * as the active handler for current diner updates.
     * 
     * @param client The main ClientController instance.
     */
    public void initData(ClientController client) {
        this.client = client;
        // Register this instance globally so the ClientController can push updates here
        ClientController.tabCurrentDinersController = this;
    }

    /**
     * Sends a request to the server to fetch the latest list of customers currently in the restaurant.
     */
    public void refreshData() {
        if (client == null) {
            System.out.println("TAB_CURRENT_DINERS: ClientController is not set. Cannot fetch data.");
            return;
        }
        
        System.out.println("TabCurrentDiners: Sending request for active diners...");
        client.handleMessageFromBoundary(
            TypeMessage.RESERVATION, 
            null, 
            Command.GET_ALL_DINERS_AT_RESTAURANT
        );
    }

    /**
     * Updates the TableView with new data received from the server.
     * This method ensures the UI update happens on the JavaFX Application Thread.
     * 
     * @param serverData The list of customers currently seated, provided by the server.
     */
    public void updateTableData(ArrayList<Customer> serverData) {
        if (serverData == null) return;

        // Ensure UI updates occur on the main JavaFX thread
        Platform.runLater(() -> {
            dinersList.clear();
            dinersList.addAll(serverData);
            tblDiners.refresh();
            System.out.println("Diners table updated with " + serverData.size() + " customers.");
        });
    }

    /**
     * Event handler for the Refresh button.
     * Triggers a manual data fetch from the server.
     */
    @FXML
    void onRefreshClicked(ActionEvent event) {
        refreshData();
    }
}
