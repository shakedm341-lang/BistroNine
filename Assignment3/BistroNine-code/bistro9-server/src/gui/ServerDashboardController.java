package gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class ServerDashboardController {

    @FXML
    private TableView<ConnectedClient> clientTable;
    @FXML
    private TableColumn<ConnectedClient, String> ipCol;
    @FXML
    private TableColumn<ConnectedClient, String> hostCol;
    @FXML
    private TableColumn<ConnectedClient, String> statusCol;
    @FXML
    private Button btnExit;

    // List to hold the table data
    private ObservableList<ConnectedClient> clientList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Link the columns to the ConnectedClient properties
        ipCol.setCellValueFactory(new PropertyValueFactory<>("clientIp"));
        hostCol.setCellValueFactory(new PropertyValueFactory<>("hostName"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Set the list into the table
        clientTable.setItems(clientList);
    }

    // Called by ServerController when a client connects
    public void addClient(String ip, String host) {
        // Check if client already exists (optional, but good for safety)
    	for (ConnectedClient c : clientList) {
    		if (c.getClientIp().equals(ip)) {
    			c.setStatus("Connected");
    			clientTable.refresh();
    			return;
    		}
    	}
        ConnectedClient client = new ConnectedClient(ip, host, "Connected");
        clientList.add(client);
    }
    
    // Called by ServerController when status changes
    public void updateClientStatus(String ip, String newStatus) {
    	for(ConnectedClient c : clientList) {
    		if(c.getClientIp().equals(ip)) {
    			c.setStatus(newStatus);
    			clientTable.refresh();
    			break;
    		}
    	}
    }

    public void getExitBtn(ActionEvent event) {
        System.out.println("Stopping Server...");
        System.exit(0);
    }
}