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

public class TabCurrentDinersController implements Initializable {

    // UI Components
    @FXML private TableView<Customer> tblDiners;
    
    @FXML private TableColumn<Customer, Integer> colId;
    @FXML private TableColumn<Customer, String> colFirstName; // Will handle both Guest and Subscriber
    @FXML private TableColumn<Customer, String> colLastName;
    @FXML private TableColumn<Customer, String> colPhone;
    @FXML private TableColumn<Customer, String> colEmail;
    @FXML private TableColumn<Customer, String> colType; // New column to show Customer Type

    // Dependencies
    private ClientController client;
    private Subscriber currentUser;
    
    // Data list
    private ObservableList<Customer> dinersList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
    }

    private void setupTableColumns() {
        // 1. Basic columns that exist in 'Customer' class
        colId.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // 2. Smart columns: Check if the object is a Subscriber to get the name
        
        // First Name Column Logic
        colFirstName.setCellValueFactory(cellData -> {
            Customer c = cellData.getValue();
            if (c instanceof Subscriber) {
                // If it's a subscriber, we can cast it and get the name
                return new SimpleStringProperty(((Subscriber) c).getFirstName());
            } else {
                // If it's a regular customer, we don't know the name
                return new SimpleStringProperty("Guest");
            }
        });

        // Last Name Column Logic
        colLastName.setCellValueFactory(cellData -> {
            Customer c = cellData.getValue();
            if (c instanceof Subscriber) {
                return new SimpleStringProperty(((Subscriber) c).getLastName());
            } else {
                return new SimpleStringProperty("-");
            }
        });

        // Type Column Logic (Optional, helps visualize who is who)
        colType.setCellValueFactory(cellData -> {
            Customer c = cellData.getValue();
            if (c instanceof Subscriber) {
                return new SimpleStringProperty("Subscriber");
            } else {
                return new SimpleStringProperty("Regular Customer");
            }
        });

        // Bind data
        tblDiners.setItems(dinersList);
    }

    // --- Data Loading Logic ---

    public void initData(ClientController client) {
        this.client = client;
       
    }

    public void refreshData() {
        if (client == null) return;
        System.out.println("TabCurrentDiners: Sending request for active diners...");
        if (client != null) {
			client.handleMessageFromBoundary(TypeMessage.RESERVATION, // The broad category
												null, 
												Command.GET_ALL_DINERS_AT_RESTAURANT // The specific command
			);
		} else {
			System.out.println("ACTIVE_RESERVATION_CONTROLLER: ClientController is not set. Cannot send data to server.");
		}
    }

    public void updateTableData(ArrayList<Customer> serverData) {
        if (serverData == null) return;

        Platform.runLater(() -> {
            dinersList.clear();
            dinersList.addAll(serverData);
            tblDiners.refresh();
            System.out.println("Diners table updated with " + serverData.size() + " customers.");
        });
    }

    @FXML
    void onRefreshClicked(ActionEvent event) {
        refreshData();
    }
}