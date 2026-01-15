package gui;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import controller.ClientController;
import data.Command;
import data.Subscriber;
import data.Table;
import data.TypeMessage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.Region;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

/**
 * Controller for the Table Management view.
 * This class allows restaurant staff to view all tables in the restaurant,
 * monitor their status (Available, Occupied), and modify table properties
 * like seat capacity or location. It also handles adding and deleting tables.
 */
public class TableManagementController implements Initializable {

    @FXML
    private TableView<Table> tablesTable;

    @FXML
    private TableColumn<Table, Integer> idCol;

    @FXML
    private TableColumn<Table, Integer> seatsCol;

    @FXML
    private TableColumn<Table, String> locationCol;

    @FXML
    private TableColumn<Table, String> statusCol;

    @FXML
    private TableColumn<Table, Void> actionsCol;

    @FXML
    private Spinner<Integer> seatsSpinner;

    @FXML
    private ComboBox<String> locationComboBox;

    private ClientController client;
    private ObservableList<Table> tableList = FXCollections.observableArrayList();

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded. It sets up the table columns,
     * action buttons, and input controls for new tables.
     * 
     * @param location The location used to resolve relative paths for the root object.
     * @param resources The resources used to localize the root object.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Map table columns to Table object properties
        idCol.setCellValueFactory(new PropertyValueFactory<>("tableId"));
        seatsCol.setCellValueFactory(new PropertyValueFactory<>("seatsNumber"));
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Initialize the custom actions column (Update/Delete buttons)
        setupActionsColumn();

        // Bind the observable list to the TableView
        tablesTable.setItems(tableList);

        // Configure the spinner for seat capacity selection
        seatsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 4));

        // Configure the location selection combo box
        locationComboBox.setItems(FXCollections.observableArrayList("inside", "bar", "outside"));
        locationComboBox.getSelectionModel().selectFirst();
    }

    /**
     * Configures the actions column by creating a custom cell factory.
     * Each row will contain "Update Seats" and "Delete" buttons with their
     * respective event handlers.
     */
    private void setupActionsColumn() {
        Callback<TableColumn<Table, Void>, TableCell<Table, Void>> cellFactory = new Callback<TableColumn<Table, Void>, TableCell<Table, Void>>() {
            @Override
            public TableCell<Table, Void> call(final TableColumn<Table, Void> param) {
                final TableCell<Table, Void> cell = new TableCell<Table, Void>() {
                    private final Button deleteBtn = new Button("Delete");
                    private final Button updateBtn = new Button("Update Seats");

                    {
                        // Apply CSS styles for a consistent UI look
                        deleteBtn.getStyleClass().addAll("btn-table-action", "btn-table-delete");
                        updateBtn.getStyleClass().addAll("btn-table-action", "btn-table-update");

                        // Action for the Delete button
                        deleteBtn.setOnAction((ActionEvent event) -> {
                            Table table = getTableView().getItems().get(getIndex());
                            handleDeleteTable(table);
                        });

                        // Action for the Update Seats button
                        updateBtn.setOnAction((ActionEvent event) -> {
                            Table table = getTableView().getItems().get(getIndex());
                            handleUpdateSeats(table);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            // Place both buttons in a horizontal container with spacing
                            HBox container = new HBox(10, updateBtn, deleteBtn);
                            setGraphic(container);
                        }
                    }
                };
                return cell;
            }
        };

        actionsCol.setCellFactory(cellFactory);
    }

    /**
     * Refreshes the table data displayed in the view by fetching the latest
     * information from the server.
     * 
     * @param event The action event triggered by the refresh button.
     */
    @FXML
    void refreshTableData(ActionEvent event) {
        fetchTables();
    }

    /**
     * Sends a request to the server to retrieve all current tables.
     */
    void fetchTables() {
        if (client != null) {
            client.handleMessageFromBoundary(TypeMessage.TABLE, null, Command.GET_ALL_AVAILABLE_TABLES);
        }
    }

    /**
     * Handles the "Add Table" request. Validates UI input and sends
     * the new table details to the server.
     * 
     * @param event The action event triggered by the add button.
     */
    @FXML
    void handleAddTable(ActionEvent event) {
        int seats = seatsSpinner.getValue();
        String loc = locationComboBox.getValue();

        if (client != null) {
            ArrayList<Object> content = new ArrayList<>();
            content.add(seats);
            content.add(loc);
            client.handleMessageFromBoundary(TypeMessage.TABLE, content, Command.ADD_TABLE);
        }
    }

    /**
     * Initiates a delete operation for a specific table.
     * 
     * @param table The Table object to be deleted.
     */
    private void handleDeleteTable(Table table) {
        if (client != null) {
            ArrayList<Object> content = new ArrayList<>();
            content.add(table.getTableId());
            client.handleMessageFromBoundary(TypeMessage.TABLE, content, Command.DELETE_TABLE);
        }
    }

    /**
     * Opens a dialog to prompt for a new seat count and sends the update
     * request to the server if the input is valid.
     * 
     * @param table The Table object to be updated.
     */
    private void handleUpdateSeats(Table table) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(table.getSeatsNumber()));
        dialog.setTitle("Update Seats");
        dialog.setHeaderText("Updating Table #" + table.getTableId());
        dialog.setContentText("Enter new number of seats:");

        dialog.showAndWait().ifPresent(result -> {
            try {
                int newSeats = Integer.parseInt(result);
                if (newSeats <= 0) {
                    throw new NumberFormatException();
                }

                if (client != null) {
                    ArrayList<Object> content = new ArrayList<>();
                    content.add(table.getTableId());
                    content.add(newSeats);
                    client.handleMessageFromBoundary(TypeMessage.TABLE, content, Command.UPDATE_TABLE_SEATS);
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid positive number for seats.");
            }
        });
    }

    /**
     * Updates the TableView with a new list of tables received from the server.
     * Ensures the update occurs on the JavaFX Application Thread.
     * 
     * @param tables The updated list of Table objects.
     */
    public void updateTableList(ArrayList<Table> tables) {
        Platform.runLater(() -> {
            tableList.clear();
            if (tables != null) {
                tableList.addAll(tables);
            }
            tablesTable.refresh();
        });
    }

    /**
     * Processes responses from the server regarding table operations.
     * Handles success notifications, occupancy warnings, and reservation conflicts.
     * 
     * @param response The response object received from the server.
     */
    public void handleOperationResponse(Object response) {
        Platform.runLater(() -> {
            if (response instanceof String) {
                // Handle simple success or notification strings
                String res = (String) response;
                if (res.equals("true")) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Operation completed successfully.");
                    fetchTables();
                } else {
                    // String response typically contains the next reservation date preventing the action
                    showAlert(Alert.AlertType.WARNING, "Notice", "Table is needed for future reservations until " + res + ". You can perform this action after that date.");
                }
            } else if (response instanceof Table) {
                // Successful addition of a table
                showAlert(Alert.AlertType.INFORMATION, "Success", "Table added successfully.");
                fetchTables();
            } else if (response instanceof ArrayList) {
                // Handle conflict scenarios where existing reservations prevent the operation
                ArrayList<?> list = (ArrayList<?>) response;
                System.out.println("DEBUG: Received " + list.size() + " items in response list (Conflicting reservations: " + list.size() + ")");
                
                if (list.isEmpty()) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Operation completed successfully.");
                    fetchTables();
                } else {
                    // Construct a detailed list of customers who need to be contacted to resolve conflicts
                    StringBuilder sb = new StringBuilder("Operation failed due to conflicting reservations.\n\nPlease contact the following customers:\n\n");
                    for (Object obj : list) {
                        if (obj instanceof Subscriber) {
                            Subscriber sub = (Subscriber) obj;
                            // Format the customer's name
                            String name = (sub.getFirstName() != null && !sub.getFirstName().isEmpty()) 
                                        ? sub.getFirstName() + (sub.getLastName() != null ? " " + sub.getLastName() : "")
                                        : "Guest #" + sub.getCustomerId();
                            
                            // Gather available contact information
                            ArrayList<String> contactMethods = new ArrayList<>();
                            if (sub.getPhoneNumber() != null && !sub.getPhoneNumber().isEmpty()) {
                                contactMethods.add("Phone: " + sub.getPhoneNumber());
                            }
                            if (sub.getEmail() != null && !sub.getEmail().isEmpty()) {
                                contactMethods.add("Email: " + sub.getEmail());
                            }
                            
                            sb.append("• ").append(name);
                            if (!contactMethods.isEmpty()) {
                                sb.append(" (").append(String.join(", ", contactMethods)).append(")");
                            }
                            sb.append("\n");
                        }
                    }
                    sb.append("\nReassign their tables before attempting this operation again.");
                    showAlert(Alert.AlertType.WARNING, "Conflict Found", sb.toString());
                }
            } else {
                // Fallback for unexpected response formats
                showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred.");
            }
        });
    }

    /**
     * Displays a customized JavaFX Alert dialog.
     * 
     * @param type The AlertType (e.g., INFORMATION, WARNING, ERROR).
     * @param title The title text for the dialog window.
     * @param content The message body of the dialog.
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        // Ensure long content strings are fully visible and the dialog is appropriately sized
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.getDialogPane().setPrefWidth(500); 
        alert.setResizable(true);
        
        alert.showAndWait();
    }

    /**
     * Sets the ClientController and registers this instance for callbacks.
     * Automatically triggers an initial data fetch.
     * 
     * @param client The main ClientController instance for communication.
     */
    public void setClient(ClientController client) {
        this.client = client;
        // Global reference for the ClientController to use when messages arrive
        ClientController.tableManagementController = this;
        fetchTables();
    }
}
