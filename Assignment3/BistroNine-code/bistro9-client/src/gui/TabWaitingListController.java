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

public class TabWaitingListController implements Initializable {

    @FXML
    private TableView<ManWaiting> waitingTable;

    @FXML
    private TableColumn<ManWaiting, String> colFirstName;

    @FXML
    private TableColumn<ManWaiting, String> colLastName;

    @FXML
    private TableColumn<ManWaiting, String> colPhone;

    @FXML
    private TableColumn<ManWaiting, String> colEmail;

    @FXML
    private TableColumn<ManWaiting, Timestamp> colEntryTime;

    @FXML
    private Button btnRefresh;

    @FXML
    private Button btnRemove;

    private ClientController client;
    private ManWaiting pendingDeletion;
    private ObservableList<ManWaiting> waitingList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up column bindings
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        
        // Format the timestamp for the entry time column
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

        waitingTable.setItems(waitingList);
    }

    public void initData(ClientController client) {
        this.client = client;
        ClientController.tabWaitingListController = this;
    }

    @FXML
    public void handleRefresh() {
        refreshData();
    }

    public void refreshData() {
        if (client != null) {
            client.handleMessageFromBoundary(TypeMessage.WAITLIST, null, Command.GET_WAIT_LIST);
            System.out.println("TabWaitingList: Sending request to server for wait list...");
        }
    }

    @FXML
    public void handleRemove() {
        ManWaiting selected = waitingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            displayAlert(AlertType.WARNING, "Selection Required", null, "Please select a customer from the waitlist to remove.");
            return;
        }
        
        if (client != null) {
            pendingDeletion = selected; // Store the selected item to remove it only if server confirms success
            ArrayList<Object> deleteContent = new ArrayList<>();
            // The server expects: [type, phone/id, email]
            deleteContent.add("customer"); 
            deleteContent.add(selected.getPhoneNumber());
            deleteContent.add(selected.getEmail());
            client.handleMessageFromBoundary(TypeMessage.WAITLIST, deleteContent, Command.DELETE_FROM_WAIT_LIST);
        }
    }

    @SuppressWarnings("unchecked")
    public void updateTableData(Object data) {
        Platform.runLater(() -> {
            if (data instanceof ArrayList) {
                ArrayList<ManWaiting> list = (ArrayList<ManWaiting>) data;
                System.out.println("Updating table data with " + list.size() + " items");
                waitingList.setAll(list);
                waitingTable.refresh();
            } else if (data instanceof Boolean) {
                boolean success = (Boolean) data;
                if (success && pendingDeletion != null) {
                    waitingList.remove(pendingDeletion);
                    waitingTable.refresh();
                } else if (!success) {
                    displayAlert(AlertType.ERROR, "Error", "Deletion Failed", "The server failed to delete the customer from the waitlist.");
                }
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
