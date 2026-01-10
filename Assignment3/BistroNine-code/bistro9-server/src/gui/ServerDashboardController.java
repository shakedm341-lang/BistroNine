package gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class ServerDashboardController {

	@FXML
	private TableView<ConnectedClient> clientTable;
	@FXML
	private TableColumn<ConnectedClient, String> ipCol;
	@FXML
	private TableColumn<ConnectedClient, String> hostCol;
	@FXML
	private TableColumn<ConnectedClient, String> portCol;
	@FXML
	private TableColumn<ConnectedClient, String> statusCol;

	@FXML
	private Button btnExit;

	@FXML
	private Button btnBack; 

	private ObservableList<ConnectedClient> clientList = FXCollections.observableArrayList();

	/**
	 * * Initializes the controller class. This method is automatically called after
	 * the FXML file has been loaded.
	 */
	@FXML
	public void initialize() {
		ipCol.setCellValueFactory(new PropertyValueFactory<>("clientIp"));
		hostCol.setCellValueFactory(new PropertyValueFactory<>("hostName"));
		portCol.setCellValueFactory(new PropertyValueFactory<>("clientPort"));
		statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

		clientTable.setItems(clientList);
	}

	/**
	 * * Adds a new client to the client list or updates the status if the client
	 * already exists.
	 * 
	 * @param ip   The IP address of the client.
	 * @param host The hostname of the client.
	 * @param port The port number of the client.
	 */
	public void addClient(String ip, String host, String port) {
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

	/**
	 * Updates the status of an existing client.
	 * 
	 * @param ip        The IP address of the client.
	 * @param port      The port number of the client.
	 * @param newStatus The new status to set for the client.
	 */
	public void updateClientStatus(String ip, String port, String newStatus) {
		for(ConnectedClient c : clientList) {
			if(c.getClientIp().equals(ip) && c.getClientPort().equals(port)) {
				c.setStatus(newStatus);
				clientTable.refresh();
				break;
			}
		}
	}

	/**
	 * Handles the action of the Back button to return to the server port
	 * configuration screen.
	 * 
	 * @param event The action event triggered by clicking the Back button.
	 */
	@FXML
	public void getBackBtn(ActionEvent event) {
		try {


			((Node) event.getSource()).getScene().getWindow().hide();


			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/serverPort.fxml"));
			Parent root = loader.load();

			Stage primaryStage = new Stage();
			Scene scene = new Scene(root);

			primaryStage.setTitle("Server Connection");
			primaryStage.setScene(scene);


			primaryStage.setOnCloseRequest(e -> System.exit(0));

			primaryStage.show();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/** Handles the action of the Exit button to stop the server and exit the
	 * application.
	 * @param event The action event triggered by clicking the Exit button.
	 */
	public void getExitBtn(ActionEvent event) {
		System.out.println("Stopping Server...");
		System.exit(0);
	}
}