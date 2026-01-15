package gui;

import java.time.LocalDate;
import java.time.LocalTime;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;

/**
 * Controller class for the restaurant operation settings screen.
 * Manage standard weekly opening hours and special date-specific opening hours.
 * Allows restaurant managers to add, edit, remove shifts, and close the restaurant for specific days.
 */
public class SettingsController {

    // --- FXML Components ---
    
    @FXML private Button btnStandardMode;
    @FXML private Button btnSpecialMode;
    @FXML private VBox standardHoursView;
    @FXML private VBox specialDatesView;

    @FXML private ComboBox<String> dayComboBox;
    @FXML private TableView<TimeSlot> standardHoursTable;
    @FXML private TableColumn<TimeSlot, String> standardOpenCol;
    @FXML private TableColumn<TimeSlot, String> standardCloseCol;
    @FXML private TableColumn<TimeSlot, Void> standardActionCol;
    @FXML private ComboBox<LocalTime> standardOpenCombo;
    @FXML private ComboBox<LocalTime> standardCloseCombo;

    @FXML private DatePicker specialDatePicker;
    @FXML private TableView<TimeSlot> specialHoursTable;
    @FXML private TableColumn<TimeSlot, String> specialOpenCol;
    @FXML private TableColumn<TimeSlot, String> specialCloseCol;
    @FXML private TableColumn<TimeSlot, Void> specialActionCol;
    @FXML private ComboBox<LocalTime> specialOpenCombo;
    @FXML private ComboBox<LocalTime> specialCloseCombo;

    // --- State Variables ---
    
    /** Reference to the client controller for server communication */
    private ClientController client;
    
    /** The currently logged-in user (Restaurant Manager) */
    private Subscriber currentUser;
    
    /** Observable list for the standard hours table */
    private ObservableList<TimeSlot> standardSlots = FXCollections.observableArrayList();
    
    /** Observable list for the special hours table */
    private ObservableList<TimeSlot> specialSlots = FXCollections.observableArrayList();
    
    /** List of dates that have special hours defined, used for date picker highlighting */
    private ArrayList<LocalDate> datesWithSpecialHours = new ArrayList<>();
    
    /** Local cache of all weekly opening hours from the server */
    private ArrayList<OpeningHours> allWeeklyHours = new ArrayList<>();
    
    /** Local cache of all special opening hours from the server */
    private ArrayList<OpeningHoursPerDay> allSpecialHours = new ArrayList<>();
    
    /** Observable list for time selection options (every 30 minutes) */
    private ObservableList<LocalTime> timeOptions = FXCollections.observableArrayList();
    
    /** Text node for the special hours table placeholder */
    private Text specialPlaceholderText;
    
    /** Temporary storage for a slot being deleted, to be removed from the list on success */
    private TimeSlot pendingDeleteSlot = null;
    
    /** The list from which the pending delete slot should be removed */
    private ObservableList<TimeSlot> pendingDeleteList = null;

    /**
     * Initializes the controller. This method is called automatically after the FXML file has been loaded.
     * Sets up UI components, tables, and date picker.
     */
    @FXML
    public void initialize() {
        // Setup the days of the week in the standard hours dropdown
        dayComboBox.setItems(FXCollections.observableArrayList(
            "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"
        ));

        // Setup time options for the ComboBoxes (every 30 minutes)
        timeOptions.clear();
        for (int h = 0; h < 24; h++) {
            timeOptions.add(LocalTime.of(h, 0));
            timeOptions.add(LocalTime.of(h, 30));
        }
        
        standardOpenCombo.setItems(timeOptions);
        standardCloseCombo.setItems(timeOptions);
        specialOpenCombo.setItems(timeOptions);
        specialCloseCombo.setItems(timeOptions);

        // Configure standard hours table columns
        setupTableColumns(standardHoursTable, standardOpenCol, standardCloseCol, standardActionCol, standardSlots, true);

        // Configure special hours table columns
        setupTableColumns(specialHoursTable, specialOpenCol, specialCloseCol, specialActionCol, specialSlots, false);

        // Set custom placeholder nodes for empty tables
        setupTablePlaceholders();

        // Configure date cell factory to highlight dates with overrides
        setupDatePickerHighlighting();
    }

    /**
     * Creates and sets custom placeholder nodes for both table views when they are empty.
     */
    private void setupTablePlaceholders() {
        // Placeholder for Standard Hours table
        StackPane standardPlaceholder = new StackPane();
        Text standardPlaceholderText = new Text("the restaurant is closed on this day");
        standardPlaceholderText.setStyle("-fx-font-size: 14px; -fx-fill: #666666;");
        standardPlaceholder.getChildren().add(standardPlaceholderText);
        standardHoursTable.setPlaceholder(standardPlaceholder);
        
        // Placeholder for Special Hours table
        StackPane specialPlaceholder = new StackPane();
        specialPlaceholderText = new Text("no special hours defined for this day");
        specialPlaceholderText.setStyle("-fx-font-size: 14px; -fx-fill: #666666;");
        specialPlaceholder.getChildren().add(specialPlaceholderText);
        specialHoursTable.setPlaceholder(specialPlaceholder);
    }

    /**
     * Sets a custom cell factory for the special dates DatePicker to highlight dates
     * that already have special opening hours configured.
     */
    private void setupDatePickerHighlighting() {
        specialDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);

                if (date != null && !empty) {
                    // Highlight red if the date has special hours
                    if (datesWithSpecialHours.contains(date)) {
                        setStyle("-fx-background-color: #ffcccc; " +
                               "-fx-text-fill: #d32f2f; " +
                               "-fx-font-weight: bold; " +
                               "-fx-border-color: #d32f2f; " +
                               "-fx-border-radius: 3;");
                        
                        setTooltip(new Tooltip("Has special opening hours configured"));
                    }
                }
            }
        });
    }

    /**
     * Configures the columns of a TimeSlot TableView.
     * 
     * @param table The TableView to configure
     * @param openCol The column for opening time
     * @param closeCol The column for closing time
     * @param actionCol The column for action buttons (Edit/Remove)
     * @param dataList The observable list backing the table
     * @param isStandard True if this is for weekly standard hours, false for special dates
     */
    private void setupTableColumns(TableView<TimeSlot> table, 
                                   TableColumn<TimeSlot, String> openCol, 
                                   TableColumn<TimeSlot, String> closeCol, 
                                   TableColumn<TimeSlot, Void> actionCol, 
                                   ObservableList<TimeSlot> dataList,
                                   boolean isStandard) {
        
        // Setup display factory for Open Time (displays "CLOSED" if MIDNIGHT to MIDNIGHT)
        openCol.setCellValueFactory(cellData -> {
            LocalTime time = cellData.getValue().getOpen();
            if (time.equals(LocalTime.MIDNIGHT) && cellData.getValue().getClose().equals(LocalTime.MIDNIGHT)) {
                return new SimpleStringProperty("CLOSED");
            }
            return new SimpleStringProperty(time.toString());
        });
        
        // Setup display factory for Close Time
        closeCol.setCellValueFactory(cellData -> {
            LocalTime time = cellData.getValue().getClose();
            if (time.equals(LocalTime.MIDNIGHT) && cellData.getValue().getOpen().equals(LocalTime.MIDNIGHT)) {
                return new SimpleStringProperty("CLOSED");
            }
            return new SimpleStringProperty(time.toString());
        });

        // Setup custom cell factory for the Action column (buttons)
        actionCol.setCellFactory(new Callback<TableColumn<TimeSlot, Void>, TableCell<TimeSlot, Void>>() {
            @Override
            public TableCell<TimeSlot, Void> call(TableColumn<TimeSlot, Void> param) {
                return new TableCell<TimeSlot, Void>() {
                    private final Button btnRemove = new Button("Remove");
                    private final Button btnEdit = new Button("Edit");
                    private final HBox container = new HBox(5);
                    {
                        // Remove Button styling and handler
                        btnRemove.getStyleClass().addAll("btn-table-action", "btn-table-delete");
                        btnRemove.setOnAction((ActionEvent event) -> {
                            TimeSlot slot = getTableView().getItems().get(getIndex());
                            if (isStandard) {
                                deleteStandardSlot(slot, dataList);
                            } else {
                                deleteSpecialSlot(slot, dataList);
                            }
                        });

                        // Edit Button styling and handler
                        btnEdit.getStyleClass().addAll("btn-table-action", "btn-table-edit");
                        btnEdit.setStyle("-fx-background-color: -primary-blue; -fx-text-fill: white;");
                        btnEdit.setOnAction((ActionEvent event) -> {
                            TimeSlot slot = getTableView().getItems().get(getIndex());
                            handleEditSlot(slot, isStandard);
                        });

                        container.getChildren().addAll(btnEdit, btnRemove);
                        container.setAlignment(Pos.CENTER);
                    }
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) setGraphic(null);
                        else setGraphic(container);
                    }
                };
            }
        });

        table.setItems(dataList);
    }

    /**
     * Sets the required external dependencies for this controller.
     * 
     * @param client The ClientController instance
     * @param currentUser The currently logged-in user
     */
    public void setDependencies(ClientController client, Subscriber currentUser) {
        this.client = client;
        this.currentUser = currentUser;
        ClientController.settingsController = this; // Register this instance globally
        requestOpeningHoursFromServer();
    }

    /**
     * Sends commands to the server to request all current opening hours data.
     */
    void requestOpeningHoursFromServer() {
        if (client != null) {
            System.out.println("DEBUG: Requesting all opening hours from server...");
            
            // Fetch weekly schedule
            client.handleMessageFromBoundary(TypeMessage.OPENING_TIME, null, Command.GET_WEEKLY_OPENING_TIME);
            
            // Fetch special overridden dates
            client.handleMessageFromBoundary(TypeMessage.OPENING_TIME, null, Command.GET_SPECIAL_OPENING_TIME);
        }
    }

    /**
     * Triggered when a new day is selected in the Standard Hours dropdown.
     */
    @FXML
    void onDaySelected(ActionEvent event) {
        String selectedDay = dayComboBox.getValue();
        if (selectedDay != null) {
            loadStandardHours(selectedDay);
        }
    }

    /**
     * Triggered when a date is selected in the Special Dates picker.
     */
    @FXML
    void onDateSelected(ActionEvent event) {
        LocalDate selectedDate = specialDatePicker.getValue();
        if (selectedDate != null) {
            loadSpecialHours(selectedDate);
        }
    }

    /**
     * Switches the UI to display standard weekly hours management.
     */
    @FXML
    void showStandardHours(ActionEvent event) {
        standardHoursView.setVisible(true);
        specialDatesView.setVisible(false);
        
        // Update tab button appearance - matched to application theme
        btnStandardMode.setStyle("-fx-background-color: -primary-blue; -fx-text-fill: white; -fx-background-radius: 20; -fx-font-weight: bold; -fx-focus-color: transparent;");
        btnSpecialMode.setStyle("-fx-background-color: transparent; -fx-text-fill: -text-secondary; -fx-background-radius: 20; -fx-font-weight: bold; -fx-focus-color: transparent;");
    }

    /**
     * Switches the UI to display special date-specific hours management.
     */
    @FXML
    void showSpecialDates(ActionEvent event) {
        standardHoursView.setVisible(false);
        specialDatesView.setVisible(true);
        
        // Update tab button appearance - matched to application theme
        btnStandardMode.setStyle("-fx-background-color: transparent; -fx-text-fill: -text-secondary; -fx-background-radius: 20; -fx-font-weight: bold; -fx-focus-color: transparent;");
        btnSpecialMode.setStyle("-fx-background-color: -primary-blue; -fx-text-fill: white; -fx-background-radius: 20; -fx-font-weight: bold; -fx-focus-color: transparent;");
    }

    /**
     * Filters the cached standard weekly hours for the selected day and updates the table.
     * 
     * @param day The name of the day (e.g., "MONDAY")
     */
    private void loadStandardHours(String day) {
        standardSlots.clear();
        for (OpeningHours oh : allWeeklyHours) {
            if (oh.getDay().equals(day)) {
                if (oh.getSlots() != null && !oh.getSlots().isEmpty()) {
                    standardSlots.addAll(oh.getSlots());
                }
                break;
            }
        }
    }

    /**
     * Filters the cached special hours for the selected date and updates the table.
     * 
     * @param date The selected date
     */
    private void loadSpecialHours(LocalDate date) {
        specialSlots.clear();
        boolean dateFound = false;
        for (OpeningHoursPerDay ohpd : allSpecialHours) {
            if (ohpd.getDay().equals(date)) {
                dateFound = true;
                if (ohpd.getSlots() != null && !ohpd.getSlots().isEmpty()) {
                    specialSlots.addAll(ohpd.getSlots());
                }
                break;
            }
        }
        
        // Update placeholder text dynamically
        if (specialSlots.isEmpty()) {
            if (dateFound) {
                // If there's a record but no slots, the restaurant is explicitly closed
                specialPlaceholderText.setText("the restaurant is closed on this date");
            } else {
                // If there's no record at all, no special hours are defined (standard hours apply)
                specialPlaceholderText.setText("no special hours defined for this day");
            }
        }
    }

    // --- Server Callbacks ---

    /**
     * Callback method called by ClientController when weekly opening hours data arrives.
     * 
     * @param list The list of weekly opening hours
     */
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

    /**
     * Callback method called by ClientController when special date opening hours data arrives.
     * 
     * @param list The list of special opening hours
     */
    public void updateSpecialOpeningHours(ArrayList<OpeningHoursPerDay> list) {
        Platform.runLater(() -> {
            if (list != null) {
                this.allSpecialHours = list;
                
                // Track dates with special hours for picker highlighting
                datesWithSpecialHours.clear();
                for (OpeningHoursPerDay ohpd : allSpecialHours) {
                    datesWithSpecialHours.add(ohpd.getDay());
                }
                
                // Trigger refresh of date picker cells
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

    /**
     * Handles the server's response to an add/update/delete operation.
     * Detects if the operation succeeded or failed due to conflicts.
     * 
     * @param response Can be a Boolean (simple success/failure) or an ArrayList of Subscribers (conflicts)
     */
    public void onSaveResponse(Object response) {
        Platform.runLater(() -> {
            if (response instanceof Boolean && (Boolean) response) {
                handleSuccess();
            } else if (response instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<Subscriber> conflicts = (ArrayList<Subscriber>) response;
                System.out.println("DEBUG: Received " + conflicts.size() + " conflicting reservations from server.");
                if (conflicts.isEmpty()) {
                    handleSuccess();
                } else {
                    // Fail: Conflicts found with existing reservations
                    showConflictDialog(conflicts);
                    // Refresh data to revert local list to match server state
                    requestOpeningHoursFromServer();
                }
            } else {
                // Unexpected error
                pendingDeleteSlot = null;
                pendingDeleteList = null;
                showAlert(AlertType.ERROR, "Operation Failed", "Could not update opening hours on the server.");
            }
        });
    }

    /**
     * Cleans up local state after a successful server operation.
     */
    private void handleSuccess() {
        if (pendingDeleteSlot != null && pendingDeleteList != null) {
            pendingDeleteList.remove(pendingDeleteSlot);
            pendingDeleteSlot = null;
            pendingDeleteList = null;
        }
        // Refresh data to ensure synchronization
        requestOpeningHoursFromServer();
        showAlert(AlertType.INFORMATION, "Success", "Opening hours updated successfully.");
    }

    /**
     * Displays a detailed warning dialog showing customers whose reservations conflict
     * with the requested change in opening hours.
     * 
     * @param conflicts List of conflicting subscribers/customers
     */
    private void showConflictDialog(ArrayList<Subscriber> conflicts) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Conflict Detected");
        alert.setHeaderText("The operation cannot be completed due to " + conflicts.size() + " conflicting reservation(s).");

        VBox content = new VBox(10);
        Label label = new Label("Affected Customers:");
        label.setStyle("-fx-font-weight: bold;");
        
        StringBuilder sb = new StringBuilder();
        for (Subscriber sub : conflicts) {
            if (sub.getSubscriberId() > 0) {
                sb.append("• ").append(sub.getFirstName()).append(" ").append(sub.getLastName()).append("\n");
            } else {
                sb.append("• Guest\n");
            }
            sb.append("  Email: ").append(sub.getEmail()).append("\n");
            sb.append("  Phone: ").append(sub.getPhoneNumber()).append("\n\n");
        }

        javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(200);
        
        VBox.setVgrow(textArea, Priority.ALWAYS);
        content.getChildren().addAll(label, textArea);
        
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setMinWidth(450);
        
        alert.showAndWait();
    }

    /**
     * Handles clicking the "Add Shift" button for standard weekly hours.
     */
    @FXML
    void addStandardSlot(ActionEvent event) {
        addSlotToList(standardOpenCombo, standardCloseCombo, standardSlots, true);
    }

    /**
     * Handles clicking the "Add Shift" button for special date-specific hours.
     */
    @FXML
    void addSpecialSlot(ActionEvent event) {
        addSlotToList(specialOpenCombo, specialCloseCombo, specialSlots, false);
    }

    /**
     * Sends a command to the server to close the restaurant for the entire selected special date.
     */
    @FXML
    void closeRestaurantForDay(ActionEvent event) {
        LocalDate date = specialDatePicker.getValue();
        if (date == null) {
            showAlert(AlertType.WARNING, "Missing Data", "Please select a date first.");
            return;
        }

        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Closure");
        confirm.setHeaderText("Close restaurant for the entire day?");
        confirm.setContentText("Are you sure you want to close the restaurant on " + date + "? This will remove all existing special shifts for this day.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            ArrayList<Object> content = new ArrayList<>();
            content.add(date);

            System.out.println("DEBUG: Sending CLOSE_RESTAURANT_ON_SPECIAL_DAY to server for " + date);
            if (client != null) {
                client.handleMessageFromBoundary(TypeMessage.OPENING_TIME, 
                                                    content, 
                                                    Command.CLOSE_RESTAURANT_ON_SPECIAL_DAY);
            }
        }
    }

    /**
     * Displays a pop-up dialog to edit an existing opening/closing time slot.
     * 
     * @param slot The current TimeSlot being edited
     * @param isStandard True if editing standard hours, false for special hours
     */
    private void handleEditSlot(TimeSlot slot, boolean isStandard) {
        Dialog<TimeSlot> dialog = new Dialog<>();
        dialog.setTitle("Edit Opening Hours");
        dialog.setHeaderText("Adjust opening and closing times");

        ButtonType saveButtonType = new ButtonType("Save", ButtonType.OK.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<LocalTime> openField = new ComboBox<>(timeOptions);
        openField.setValue(slot.getOpen());
        ComboBox<LocalTime> closeField = new ComboBox<>(timeOptions);
        closeField.setValue(slot.getClose());

        grid.add(new Label("Open Time:"), 0, 0);
        grid.add(openField, 1, 0);
        grid.add(new Label("Close Time:"), 0, 1);
        grid.add(closeField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Autofocus on the first field
        Platform.runLater(openField::requestFocus);

        // Convert the result to a TimeSlot object when save is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                LocalTime newOpen = openField.getValue();
                LocalTime newClose = closeField.getValue();
                if (newOpen != null && newClose != null) {
                    return new TimeSlot(newOpen, newClose);
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newSlot -> {
            sendUpdateToServer(slot, newSlot, isStandard);
        });
    }

    /**
     * Prepares and sends an update command to the server for an existing slot.
     * 
     * @param oldSlot Original values
     * @param newSlot New values
     * @param isStandard Context (standard vs special)
     */
    private void sendUpdateToServer(TimeSlot oldSlot, TimeSlot newSlot, boolean isStandard) {
        ArrayList<Object> content = new ArrayList<>();
        Command command;

        if (isStandard) {
            String day = dayComboBox.getValue();
            if (day == null) return;
            content.add(day);
            command = Command.UPDATE_OPENING_TIME;
        } else {
            LocalDate date = specialDatePicker.getValue();
            if (date == null) return;
            content.add(date);
            command = Command.UPDATE_SPECIAL_OPENING_TIME;
        }

        // Format: [Day/Date, oldOpen, oldClose, newOpen, newClose]
        content.add(oldSlot.getOpen());
        content.add(oldSlot.getClose());
        content.add(newSlot.getOpen());
        content.add(newSlot.getClose());

        System.out.println("DEBUG: Sending " + command + " to server...");
        if (client != null) {
            client.handleMessageFromBoundary(TypeMessage.OPENING_TIME, content, command);
        }
    }

    /**
     * Internal logic to parse input, validate times, and send an "add" request to the server.
     * 
     * @param openTxt TextField containing open time
     * @param closeTxt TextField containing close time
     * @param list The local list to update speculatively
     * @param isStandard Context (standard vs special)
     */
    private void addSlotToList(ComboBox<LocalTime> openCombo, ComboBox<LocalTime> closeCombo, ObservableList<TimeSlot> list, boolean isStandard) {
        LocalTime open = openCombo.getValue();
        LocalTime close = closeCombo.getValue();
        
        if (open == null || close == null) {
            showAlert(AlertType.WARNING, "Missing Data", "Please select both opening and closing times.");
            return;
        }

        TimeSlot newSlot = new TimeSlot(open, close);
        list.add(newSlot); // Speculative update
        
        // Reset selections
        openCombo.setValue(null);
        closeCombo.setValue(null);

        if (isStandard) {
            addStandardSlotToServer(newSlot);
        } else {
            addSpecialSlotToServer(newSlot);
        }
    }

    /**
     * Sends an ADD_NEW_OPENING_TIME command to the server for standard weekly hours.
     */
    private void addStandardSlotToServer(TimeSlot slot) {
        String day = dayComboBox.getValue();
        if (day == null) {
            showAlert(AlertType.WARNING, "Missing Data", "Please select a day first.");
            return;
        }

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

    /**
     * Sends an ADD_NEW_SPECIAL_OPENING_TIME command to the server for a specific date.
     */
    private void addSpecialSlotToServer(TimeSlot slot) {
        LocalDate date = specialDatePicker.getValue();
        if (date == null) {
            showAlert(AlertType.WARNING, "Missing Data", "Please select a date first.");
            return;
        }

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

    /**
     * Sends a DELETE_OPENING_TIME command to the server for standard weekly hours.
     */
    private void deleteStandardSlot(TimeSlot slot, ObservableList<TimeSlot> dataList) {
        String day = dayComboBox.getValue();
        if (day == null) {
            return;
        }

        ArrayList<Object> content = new ArrayList<>();
        content.add(day);
        content.add(slot.getOpen());
        content.add(slot.getClose());

        System.out.println("DEBUG: Sending DELETE_OPENING_TIME to server for " + day);
        if (client != null) {
            pendingDeleteSlot = slot;
            pendingDeleteList = dataList;
            client.handleMessageFromBoundary(TypeMessage.OPENING_TIME, 
                                                content, 
                                                Command.DELETE_OPENING_TIME);
        }
    }

    /**
     * Sends a DELETE_SPECIAL_OPENING_TIME command to the server for a specific date override.
     */
    private void deleteSpecialSlot(TimeSlot slot, ObservableList<TimeSlot> dataList) {
        LocalDate date = specialDatePicker.getValue();
        if (date == null) {
            return;
        }

        ArrayList<Object> content = new ArrayList<>();
        content.add(date);
        content.add(slot.getOpen());
        content.add(slot.getClose());

        System.out.println("DEBUG: Sending DELETE_SPECIAL_OPENING_TIME to server for " + date);
        if (client != null) {
            pendingDeleteSlot = slot;
            pendingDeleteList = dataList;
            client.handleMessageFromBoundary(TypeMessage.OPENING_TIME, 
                                                content, 
                                                Command.DELETE_SPECIAL_OPENING_TIME);
        }
    }

    /**
     * Displays a standard JavaFX alert box to the user.
     * 
     * @param type The type of alert (INFORMATION, WARNING, ERROR, etc.)
     * @param title The title of the dialog window
     * @param message The content text to display
     */
    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
