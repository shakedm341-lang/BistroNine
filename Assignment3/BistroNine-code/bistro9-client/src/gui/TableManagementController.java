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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        idCol.setCellValueFactory(new PropertyValueFactory<>("tableId"));
        seatsCol.setCellValueFactory(new PropertyValueFactory<>("seatsNumber"));
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        setupActionsColumn();

        tablesTable.setItems(tableList);

        // Setup Spinner
        seatsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 4));

        // Setup ComboBox
        locationComboBox.setItems(FXCollections.observableArrayList("inside", "bar", "outside"));
        locationComboBox.getSelectionModel().selectFirst();
    }

    private void setupActionsColumn() {
        Callback<TableColumn<Table, Void>, TableCell<Table, Void>> cellFactory = new Callback<TableColumn<Table, Void>, TableCell<Table, Void>>() {
            @Override
            public TableCell<Table, Void> call(final TableColumn<Table, Void> param) {
                final TableCell<Table, Void> cell = new TableCell<Table, Void>() {
                    private final Button deleteBtn = new Button("Delete");
                    private final Button updateBtn = new Button("Update Seats");

                    {
                        deleteBtn.getStyleClass().addAll("btn-table-action", "btn-table-delete");
                        updateBtn.getStyleClass().addAll("btn-table-action", "btn-table-update");

                        deleteBtn.setOnAction((ActionEvent event) -> {
                            Table table = getTableView().getItems().get(getIndex());
                            handleDeleteTable(table);
                        });

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

    @FXML
    void refreshTableData(ActionEvent event) {
        fetchTables();
    }

    void fetchTables() {
        if (client != null) {
            client.handleMessageFromBoundary(TypeMessage.TABLE, null, Command.GET_ALL_AVAILABLE_TABLES);
        }
    }

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

    private void handleDeleteTable(Table table) {
        if (client != null) {
            ArrayList<Object> content = new ArrayList<>();
            content.add(table.getTableId());
            client.handleMessageFromBoundary(TypeMessage.TABLE, content, Command.DELETE_TABLE);
        }
    }

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

    public void updateTableList(ArrayList<Table> tables) {
        Platform.runLater(() -> {
            tableList.clear();
            if (tables != null) {
                tableList.addAll(tables);
            }
            tablesTable.refresh();
        });
    }

    public void handleOperationResponse(Object response) {
        Platform.runLater(() -> {
            if (response instanceof String) {
                String res = (String) response;
                if (res.equals("true")) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Operation completed successfully.");
                    fetchTables();
                } else {
                    showAlert(Alert.AlertType.WARNING, "Notice", "Table is needed for future reservations until " + res + ". You can perform this action after that date.");
                }
            } else if (response instanceof Table) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Table added successfully.");
                fetchTables();
            } else if (response instanceof ArrayList) {
                ArrayList<?> list = (ArrayList<?>) response;
                System.out.println("DEBUG: Received " + list.size() + " items in response list (Conflicting reservations: " + list.size() + ")");
                if (list.isEmpty()) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Operation completed successfully.");
                    fetchTables();
                } else {
                    StringBuilder sb = new StringBuilder("Operation failed due to conflicting reservations.\n\nPlease contact the following customers:\n\n");
                    for (Object obj : list) {
                        if (obj instanceof Subscriber) {
                            Subscriber sub = (Subscriber) obj;
                            // Build full name if available
                            String name = (sub.getFirstName() != null && !sub.getFirstName().isEmpty()) 
                                        ? sub.getFirstName() + (sub.getLastName() != null ? " " + sub.getLastName() : "")
                                        : "Guest #" + sub.getCustomerId();
                            
                            // Collect all available contact methods
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
                showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred.");
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        // Fix for long text being truncated in JavaFX alerts
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.getDialogPane().setPrefWidth(500); // Set a reasonable width for the list
        alert.setResizable(true);
        
        alert.showAndWait();
    }

    public void setClient(ClientController client) {
        this.client = client;
        ClientController.tableManagementController = this;
        fetchTables();
        // Assume other developer will add this field to ClientController
        // ClientController.tableManagementController = this;
    }
}

