package gui;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

import controller.ClientController;
import data.Command;
import data.Subscriber;
import data.TimeSlot;
import data.TypeMessage;
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
import javafx.scene.layout.VBox;
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

    @FXML
    public void initialize() {
        // Initialize Day ComboBox
        dayComboBox.setItems(FXCollections.observableArrayList(
            "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"
        ));

        // Setup Standard Table Columns
        setupTableColumns(standardHoursTable, standardOpenCol, standardCloseCol, standardActionCol, standardSlots);

        // Setup Special Table Columns
        setupTableColumns(specialHoursTable, specialOpenCol, specialCloseCol, specialActionCol, specialSlots);

        // Setup Special Dates Highlighting
        setupDatePickerHighlighting();

        // MOCK: Pre-fill some dates that have special hours for demonstration
        loadMockDatesWithSpecialHours();
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

    private void loadMockDatesWithSpecialHours() {
        // Mock data: Highlight tomorrow and a few other dates
        datesWithSpecialHours.add(LocalDate.now().plusDays(1));
        datesWithSpecialHours.add(LocalDate.now().plusDays(3));
        datesWithSpecialHours.add(LocalDate.now().plusDays(7));
        datesWithSpecialHours.add(LocalDate.now().plusMonths(1)); // Some date next month
        
        System.out.println("MOCK: Highlighted " + datesWithSpecialHours.size() + " dates in the calendar.");
    }

    private void setupTableColumns(TableView<TimeSlot> table, 
                                   TableColumn<TimeSlot, String> openCol, 
                                   TableColumn<TimeSlot, String> closeCol, 
                                   TableColumn<TimeSlot, Void> actionCol, 
                                   ObservableList<TimeSlot> dataList) {
        
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
                            dataList.remove(slot);
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
    }

    @FXML
    void onDaySelected(ActionEvent event) {
        String selectedDay = dayComboBox.getValue();
        if (selectedDay != null) {
            // MOCK: In a real scenario, we would request this from the server
            loadMockStandardHours(selectedDay);
        }
    }

    @FXML
    void onDateSelected(ActionEvent event) {
        LocalDate selectedDate = specialDatePicker.getValue();
        if (selectedDate != null) {
            // MOCK: In a real scenario, we would request this from the server
            loadMockSpecialHours(selectedDate);
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

    private void loadMockStandardHours(String day) {
        standardSlots.clear();
        standardSlots.add(new TimeSlot(LocalTime.of(8, 0), LocalTime.of(14, 0)));
        standardSlots.add(new TimeSlot(LocalTime.of(17, 0), LocalTime.of(23, 0)));
    }

    private void loadMockSpecialHours(LocalDate date) {
        specialSlots.clear();
        // Just mock some data for now
        specialSlots.add(new TimeSlot(LocalTime.of(10, 0), LocalTime.of(20, 0)));
    }

    @FXML
    void addStandardSlot(ActionEvent event) {
        addSlotToList(standardOpenTxt, standardCloseTxt, standardSlots);
    }

    @FXML
    void addSpecialSlot(ActionEvent event) {
        addSlotToList(specialOpenTxt, specialCloseTxt, specialSlots);
    }

    private void addSlotToList(TextField openTxt, TextField closeTxt, ObservableList<TimeSlot> list) {
        try {
            LocalTime open = LocalTime.parse(openTxt.getText());
            LocalTime close = LocalTime.parse(closeTxt.getText());
            
            if (close.isBefore(open)) {
                showAlert(AlertType.ERROR, "Invalid Time", "Closing time must be after opening time.");
                return;
            }

            list.add(new TimeSlot(open, close));
            openTxt.clear();
            closeTxt.clear();

        } catch (DateTimeParseException e) {
            showAlert(AlertType.ERROR, "Invalid Format", "Please use HH:mm format (e.g., 08:00).");
        }
    }

    @FXML
    void saveStandardHours(ActionEvent event) {
        String day = dayComboBox.getValue();
        if (day == null) {
            showAlert(AlertType.WARNING, "Missing Data", "Please select a day first.");
            return;
        }

        // Prepare content for Command.UPDATE_OPENING_TIME
        // Format: [String day, ArrayList<LocalTime> times]
        ArrayList<Object> content = new ArrayList<>();
        content.add(day);
        
        ArrayList<LocalTime> times = new ArrayList<>();
        for (TimeSlot slot : standardSlots) {
            times.add(slot.getOpen());
            times.add(slot.getClose());
        }
        content.add(times);

        System.out.println("DEBUG: Sending UPDATE_OPENING_TIME to server for " + day);
        if (client != null) {
            client.handleMessageFromBoundary(TypeMessage.OPENING_TIME, 
                                                content, 
                                                Command.UPDATE_OPENING_TIME);
        }
        
        
        showAlert(AlertType.INFORMATION, "Save Successful", "Weekly hours for " + day + " have been saved locally (Mock).");
    }

    @FXML
    void saveSpecialDate(ActionEvent event) {
        LocalDate date = specialDatePicker.getValue();
        if (date == null) {
            showAlert(AlertType.WARNING, "Missing Data", "Please select a date first.");
            return;
        }

        // Prepare content for Command.ADD_NEW_SPECIAL_OPENING_TIME
        // Format: [LocalDate date, ArrayList<LocalTime> times]
        ArrayList<Object> content = new ArrayList<>();
        content.add(date);
        
        ArrayList<LocalTime> times = new ArrayList<>();
        for (TimeSlot slot : specialSlots) {
            times.add(slot.getOpen());
            times.add(slot.getClose());
        }
        content.add(times);

        System.out.println("DEBUG: Sending ADD_NEW_SPECIAL_OPENING_TIME to server for " + date);
        if (client != null) {
            client.handleMessageFromBoundary(TypeMessage.OPENING_TIME, 
                                                content, 
                                                Command.ADD_NEW_SPECIAL_OPENING_TIME);
        }
        

        showAlert(AlertType.INFORMATION, "Save Successful", "Special hours for " + date + " have been saved locally (Mock).");
    }
    

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

