package gui;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

import controller.ClientController;
import data.Command;
import data.OpeningHours;
import data.OpeningHoursPerDay;
import data.Subscriber;
import data.TimeSlot;
import data.TypeMessage;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;

public class SettingsController {

    @FXML private Button btnStandardMode;
    @FXML private Button btnSpecialMode;
    @FXML private VBox standardHoursView;
    @FXML private VBox specialDatesView;

    @FXML private ComboBox<String> dayComboBox;
    @FXML private TableView<TimeSlot> standardHoursTable;
    @FXML private TableColumn<TimeSlot, String> standardOpenCol;
    @FXML private TableColumn<TimeSlot, String> standardCloseCol;
    @FXML private TableColumn<TimeSlot, Void> standardActionCol;
    @FXML private TextField standardOpenTxt;
    @FXML private TextField standardCloseTxt;

    @FXML private DatePicker specialDatePicker;
    @FXML private TableView<TimeSlot> specialHoursTable;
    @FXML private TableColumn<TimeSlot, String> specialOpenCol;
    @FXML private TableColumn<TimeSlot, String> specialCloseCol;
    @FXML private TableColumn<TimeSlot, Void> specialActionCol;
    @FXML private TextField specialOpenTxt;
    @FXML private TextField specialCloseTxt;

    private ClientController client;
    private Subscriber currentUser;
    private ObservableList<TimeSlot> standardSlots = FXCollections.observableArrayList();
    private ObservableList<TimeSlot> specialSlots = FXCollections.observableArrayList();
    private ArrayList<LocalDate> datesWithSpecialHours = new ArrayList<>();
    
    // Server data storage
    private ArrayList<OpeningHours> allWeeklyHours = new ArrayList<>();
    private ArrayList<OpeningHoursPerDay> allSpecialHours = new ArrayList<>();
    
    // Temporary storage for pending delete operations
    private TimeSlot pendingDeleteSlot = null;
    private ObservableList<TimeSlot> pendingDeleteList = null;

    @FXML
    public void initialize() {
        // Register this controller with ClientController
        //ClientController.settingsController = this;

        // Initialize Day ComboBox
        dayComboBox.setItems(FXCollections.observableArrayList(
            "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"
        ));

        // Setup Standard Table Columns
        setupTableColumns(standardHoursTable, standardOpenCol, standardCloseCol, standardActionCol, standardSlots, true);

        // Setup Special Table Columns
        setupTableColumns(specialHoursTable, specialOpenCol, specialCloseCol, specialActionCol, specialSlots, false);

        // Setup placeholders for empty tables
        setupTablePlaceholders();

        // Setup Special Dates Highlighting
        setupDatePickerHighlighting();
    }

    private void setupTablePlaceholders() {
        // Create placeholder for standard hours table
        StackPane standardPlaceholder = new StackPane();
        Text standardPlaceholderText = new Text("the restaurant closes");
        standardPlaceholderText.setStyle("-fx-font-size: 14px; -fx-fill: #666666;");
        standardPlaceholder.getChildren().add(standardPlaceholderText);
        standardHoursTable.setPlaceholder(standardPlaceholder);
        
        // Create placeholder for special hours table
        StackPane specialPlaceholder = new StackPane();
        Text specialPlaceholderText = new Text("the restaurant closes");
        specialPlaceholderText.setStyle("-fx-font-size: 14px; -fx-fill: #666666;");
        specialPlaceholder.getChildren().add(specialPlaceholderText);
        specialHoursTable.setPlaceholder(specialPlaceholder);
    }

    private void setupDatePickerHighlighting() {
        specialDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);

                if (date != null && !empty) {
                    // If the date is in our special list, make it red
                    if (datesWithSpecialHours.contains(date)) {
                        setStyle("-fx-background-color: #ffcccc; " + // Light red background
                               "-fx-text-fill: #d32f2f; " +           // Darker red text
                               "-fx-font-weight: bold; " +
                               "-fx-border-color: #d32f2f; " +       // Red border
                               "-fx-border-radius: 3;");
                        
                        setTooltip(new Tooltip("Has special opening hours configured"));
                    }
                }
            }
        });
    }

    private void setupTableColumns(TableView<TimeSlot> table, 
                                   TableColumn<TimeSlot, String> openCol, 
                                   TableColumn<TimeSlot, String> closeCol, 
                                   TableColumn<TimeSlot, Void> actionCol, 
                                   ObservableList<TimeSlot> dataList,
                                   boolean isStandard) {
        
        openCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getOpen().toString()));
        closeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getClose().toString()));

        actionCol.setCellFactory(new Callback<TableColumn<TimeSlot, Void>, TableCell<TimeSlot, Void>>() {
            @Override
            public TableCell<TimeSlot, Void> call(TableColumn<TimeSlot, Void> param) {
                return new TableCell<TimeSlot, Void>() {
                    private final Button btn = new Button("Remove");
                    {
                        btn.setOnAction((ActionEvent event) -> {
                            TimeSlot slot = getTableView().getItems().get(getIndex());
                            // Send delete command to server first, removal from list happens on success
                            if (isStandard) {
                                deleteStandardSlot(slot, dataList);
                            } else {
                                deleteSpecialSlot(slot, dataList);
                            }
                        });
                    }
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) setGraphic(null);
                        else setGraphic(btn);
                    }
                };
            }
        });

        table.setItems(dataList);
    }

    public void setDependencies(ClientController client, Subscriber currentUser) {
        this.client = client;
        this.currentUser = currentUser;
        ClientController.settingsController = this;
        requestOpeningHoursFromServer();
        // Data will now be requested lazily when the tab is selected
    }

    void requestOpeningHoursFromServer() {
        if (client != null) {
            System.out.println("DEBUG: Requesting all opening hours from server...");
            
            // Request Weekly Hours
            client.handleMessageFromBoundary(TypeMessage.OPENING_TIME, null, Command.GET_WEEKLY_OPENING_TIME);
            
            // Request Special Hours
            client.handleMessageFromBoundary(TypeMessage.OPENING_TIME, null, Command.GET_SPECIAL_OPENING_TIME);
        }
    }

    @FXML
    void onDaySelected(ActionEvent event) {
        String selectedDay = dayComboBox.getValue();
        if (selectedDay != null) {
            loadStandardHours(selectedDay);
        }
    }

    @FXML
    void onDateSelected(ActionEvent event) {
        LocalDate selectedDate = specialDatePicker.getValue();
        if (selectedDate != null) {
            loadSpecialHours(selectedDate);
        }
    }

    @FXML
    void showStandardHours(ActionEvent event) {
        standardHoursView.setVisible(true);
        specialDatesView.setVisible(false);
        
        // Update button styles
        btnStandardMode.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-background-radius: 17;");
        btnSpecialMode.setStyle("-fx-background-color: transparent; -fx-text-fill: #666666; -fx-background-radius: 17;");
    }

    @FXML
    void showSpecialDates(ActionEvent event) {
        standardHoursView.setVisible(false);
        specialDatesView.setVisible(true);
        
        // Update button styles
        btnStandardMode.setStyle("-fx-background-color: transparent; -fx-text-fill: #666666; -fx-background-radius: 17;");
        btnSpecialMode.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-background-radius: 17;");
    }

    private void loadStandardHours(String day) {
        standardSlots.clear();
        for (OpeningHours oh : allWeeklyHours) {
            if (oh.getDay().equals(day)) {
                if (oh.getSlots() != null && !oh.getSlots().isEmpty()) {
                    standardSlots.addAll(oh.getSlots());
                }
                // If slots is null or empty, the table will show the placeholder
                break;
            }
        }
        // If day not found or has no slots, table will show placeholder
    }

    private void loadSpecialHours(LocalDate date) {
        specialSlots.clear();
        for (OpeningHoursPerDay ohpd : allSpecialHours) {
            if (ohpd.getDay().equals(date)) {
                if (ohpd.getSlots() != null && !ohpd.getSlots().isEmpty()) {
                    specialSlots.addAll(ohpd.getSlots());
                }
                // If slots is null or empty, the table will show the placeholder
                break;
            }
        }
        // If date not found or has no slots, table will show placeholder
    }

    // --- Server Callbacks ---

    public void updateWeeklyOpeningHours(ArrayList<OpeningHours> list) {
        Platform.runLater(() -> {
            if (list != null) {
                this.allWeeklyHours = list;
                String currentDay = dayComboBox.getValue();
                if (currentDay != null) {
                    loadStandardHours(currentDay);
                }
            }
        });
    }

    public void updateSpecialOpeningHours(ArrayList<OpeningHoursPerDay> list) {
        Platform.runLater(() -> {
            if (list != null) {
                this.allSpecialHours = list;
                
                // Update dates with special hours for the date picker
                datesWithSpecialHours.clear();
                for (OpeningHoursPerDay ohpd : allSpecialHours) {
                    datesWithSpecialHours.add(ohpd.getDay());
                }
                
                // Refresh date picker cells
                specialDatePicker.setDayCellFactory(specialDatePicker.getDayCellFactory());
                
                LocalDate currentDate = specialDatePicker.getValue();
                if (currentDate != null) {
                    loadSpecialHours(currentDate);
                }
            } else {
                datesWithSpecialHours.clear();
                specialSlots.clear();
                specialDatePicker.setDayCellFactory(specialDatePicker.getDayCellFactory());
            }
        });
    }

    public void onSaveResponse(boolean success) {
        Platform.runLater(() -> {
            if (success) {
                // If there was a pending delete, remove it from the local list now
                if (pendingDeleteSlot != null && pendingDeleteList != null) {
                    pendingDeleteList.remove(pendingDeleteSlot);
                    pendingDeleteSlot = null;
                    pendingDeleteList = null;
                }
                // Refresh data from server to ensure UI is in sync
                requestOpeningHoursFromServer();
            } else {
                // Clear pending delete on failure
                pendingDeleteSlot = null;
                pendingDeleteList = null;
                showAlert(AlertType.ERROR, "Operation Failed", "Could not update opening hours on the server.");
            }
        });
    }

    @FXML
    void addStandardSlot(ActionEvent event) {
        addSlotToList(standardOpenTxt, standardCloseTxt, standardSlots, true);
    }

    @FXML
    void addSpecialSlot(ActionEvent event) {
        addSlotToList(specialOpenTxt, specialCloseTxt, specialSlots, false);
    }

    private void addSlotToList(TextField openTxt, TextField closeTxt, ObservableList<TimeSlot> list, boolean isStandard) {
        try {
            LocalTime open = LocalTime.parse(openTxt.getText());
            LocalTime close = LocalTime.parse(closeTxt.getText());
            
            if (close.isBefore(open)) {
                showAlert(AlertType.ERROR, "Invalid Time", "Closing time must be after opening time.");
                return;
            }

            TimeSlot newSlot = new TimeSlot(open, close);
            list.add(newSlot);
            openTxt.clear();
            closeTxt.clear();

            // Immediately send add command to server
            if (isStandard) {
                addStandardSlotToServer(newSlot);
            } else {
                addSpecialSlotToServer(newSlot);
            }

        } catch (DateTimeParseException e) {
            showAlert(AlertType.ERROR, "Invalid Format", "Please use HH:mm format (e.g., 08:00).");
        }
    }

    private void addStandardSlotToServer(TimeSlot slot) {
        String day = dayComboBox.getValue();
        if (day == null) {
            showAlert(AlertType.WARNING, "Missing Data", "Please select a day first.");
            return;
        }

        // Prepare content for Command.ADD_NEW_OPENING_TIME
        // Format: [String day, LocalTime open, LocalTime close]
        ArrayList<Object> content = new ArrayList<>();
        content.add(day);
        content.add(slot.getOpen());
        content.add(slot.getClose());

        System.out.println("DEBUG: Sending ADD_NEW_OPENING_TIME to server for " + day);
        if (client != null) {
            client.handleMessageFromBoundary(TypeMessage.OPENING_TIME, 
                                                content, 
                                                Command.ADD_NEW_OPENING_TIME);
        }
    }

    private void addSpecialSlotToServer(TimeSlot slot) {
        LocalDate date = specialDatePicker.getValue();
        if (date == null) {
            showAlert(AlertType.WARNING, "Missing Data", "Please select a date first.");
            return;
        }

        // Prepare content for Command.ADD_NEW_SPECIAL_OPENING_TIME
        // Format: [LocalDate date, LocalTime open, LocalTime close]
        ArrayList<Object> content = new ArrayList<>();
        content.add(date);
        content.add(slot.getOpen());
        content.add(slot.getClose());

        System.out.println("DEBUG: Sending ADD_NEW_SPECIAL_OPENING_TIME to server for " + date);
        if (client != null) {
            client.handleMessageFromBoundary(TypeMessage.OPENING_TIME, 
                                                content, 
                                                Command.ADD_NEW_SPECIAL_OPENING_TIME);
        }
    }

    private void deleteStandardSlot(TimeSlot slot, ObservableList<TimeSlot> dataList) {
        String day = dayComboBox.getValue();
        if (day == null) {
            return; // Should not happen, but safe guard
        }

        // Prepare content for Command.DELETE_OPENING_TIME
        // Format: [String day, LocalTime open, LocalTime close]
        ArrayList<Object> content = new ArrayList<>();
        content.add(day);
        content.add(slot.getOpen());
        content.add(slot.getClose());

        System.out.println("DEBUG: Sending DELETE_OPENING_TIME to server for " + day + " slot: " + slot.getOpen() + "-" + slot.getClose());
        if (client != null) {
            // Store the slot and list for removal on success
            pendingDeleteSlot = slot;
            pendingDeleteList = dataList;
            client.handleMessageFromBoundary(TypeMessage.OPENING_TIME, 
                                                content, 
                                                Command.DELETE_OPENING_TIME);
        }
    }

    private void deleteSpecialSlot(TimeSlot slot, ObservableList<TimeSlot> dataList) {
        LocalDate date = specialDatePicker.getValue();
        if (date == null) {
            return; // Should not happen, but safe guard
        }

        // Prepare content for Command.DELETE_SPECIAL_OPENING_TIME
        // Format: [LocalDate date, LocalTime open, LocalTime close]
        ArrayList<Object> content = new ArrayList<>();
        content.add(date);
        content.add(slot.getOpen());
        content.add(slot.getClose());

        System.out.println("DEBUG: Sending DELETE_SPECIAL_OPENING_TIME to server for " + date + " slot: " + slot.getOpen() + "-" + slot.getClose());
        if (client != null) {
            // Store the slot and list for removal on success
            pendingDeleteSlot = slot;
            pendingDeleteList = dataList;
            client.handleMessageFromBoundary(TypeMessage.OPENING_TIME, 
                                                content, 
                                                Command.DELETE_SPECIAL_OPENING_TIME);
        }
    }


    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

