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
    private TableColumn<ConnectedClient, String> portCol; // New Column
    @FXML
    private TableColumn<ConnectedClient, String> statusCol;
    @FXML
    private Button btnExit;

    private ObservableList<ConnectedClient> clientList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        ipCol.setCellValueFactory(new PropertyValueFactory<>("clientIp"));
        hostCol.setCellValueFactory(new PropertyValueFactory<>("hostName"));
        portCol.setCellValueFactory(new PropertyValueFactory<>("clientPort")); // Bind Port
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        clientTable.setItems(clientList);
    }

    // Updated to accept Port
    public void addClient(String ip, String host, String port) {
        // Check if client (IP + Port combination) already exists
        for (ConnectedClient c : clientList) {
            if (c.getClientIp().equals(ip) && c.getClientPort().equals(port)) {
                c.setStatus("Connected");
                clientTable.refresh();
                return;
            }
        }
        ConnectedClient client = new ConnectedClient(ip, host, port, "Connected");
        clientList.add(client);
    }
    
    // Updated to identify by IP AND Port
    public void updateClientStatus(String ip, String port, String newStatus) {
        for(ConnectedClient c : clientList) {
            if(c.getClientIp().equals(ip) && c.getClientPort().equals(port)) {
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