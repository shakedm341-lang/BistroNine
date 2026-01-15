package gui;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.ResourceBundle;

import controller.ClientController;
import data.Command;
import data.SubscriberReport;
import data.TimeReport;
import data.TypeMessage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DateCell;
import javafx.util.Callback;

/**
 * Controller class for the Reports screen.
 * This class handles the generation and visualization of restaurant reports,
 * specifically Subscriber Reports and Time Reports. It allows users to select
 * a date range and visualizes the returned data using a LineChart.
 */
public class ReportsController implements Initializable {

    // --- FXML UI Components ---
    @FXML
    private ComboBox<String> comboReportType;

    @FXML
    private DatePicker dpStart;

    @FXML
    private DatePicker dpEnd;

    @FXML
    private Button btnFetch;

    @FXML
    private LineChart<String, Number> reportChart;

    @FXML
    private NumberAxis yAxis;

    /** The client controller used for server communication */
    private ClientController client;

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     * Sets up the report type options, default date ranges, and date restrictions.
     * 
     * @param location The location used to resolve relative paths for the root object.
     * @param resources The resources used to localize the root object.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Populate report type dropdown
        comboReportType.setItems(FXCollections.observableArrayList("Subscriber Report", "Time Report"));
        
        // Default dates to the previous month
        LocalDate today = LocalDate.now();
        LocalDate lastMonth = today.minusMonths(1);
        dpStart.setValue(lastMonth.withDayOfMonth(1));
        dpEnd.setValue(lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()));

        // Factory to restrict DatePicker to select only dates from completed months
        Callback<DatePicker, DateCell> dayCellFactory = dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                LocalDate lastAllowedDate = getLastDayOfPreviousMonth();
                if (item.isAfter(lastAllowedDate)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #eeeeee;");
                }
            }
        };

        dpStart.setDayCellFactory(dayCellFactory);
        dpEnd.setDayCellFactory(dayCellFactory);
    }

    /**
     * Helper method to get the last day of the previous month.
     * Reports are only available for completed months.
     * 
     * @return The last day of the month preceding the current one.
     */
    private LocalDate getLastDayOfPreviousMonth() {
        return LocalDate.now().withDayOfMonth(1).minusDays(1);
    }

    /**
     * Sets the ClientController dependency for server communication.
     * 
     * @param client The ClientController instance.
     */
    public void setDependencies(ClientController client) {
        this.client = client;
    }

    /**
     * Event handler for the "Fetch Report" button.
     * Validates the selected report type and date range before sending a request to the server.
     * 
     * @param event The action event triggered by the button click.
     */
    @FXML
    void handleFetchReport(ActionEvent event) {
        String reportType = comboReportType.getValue();
        LocalDate start = dpStart.getValue();
        LocalDate end = dpEnd.getValue();

        // --- Validation Section ---
        
        if (reportType == null || start == null || end == null) {
            TerminalUtils.showError("Missing Selection", "Please select a report type and date range.");
            return;
        }

        LocalDate lastAllowedDate = getLastDayOfPreviousMonth();
        if (start.isAfter(lastAllowedDate) || end.isAfter(lastAllowedDate)) {
            TerminalUtils.showError("Invalid Date", "Reports are only available for completed months. Please select dates up to " + lastAllowedDate + ".");
            return;
        }

        if (start.isAfter(end)) {
            TerminalUtils.showError("Invalid Range", "Start date must be before or equal to end date.");
            return;
        }

        // --- Request Preparation Section ---
        
        ArrayList<Object> content = new ArrayList<>();
        content.add(start);
        content.add(end);

        // Send request based on selected report type
        if (reportType.equals("Subscriber Report")) {
            client.handleMessageFromBoundary(TypeMessage.SUBSCRIBER_REPORT, content, Command.GET_SUBSCRIBER_REPORT_BY_RANGE_DATE);
        } else {
            client.handleMessageFromBoundary(TypeMessage.TIME_REPORT, content, Command.GET_TIME_REPORT_BY_RANGE_DATE);
        }
    }

    /**
     * Updates the UI to display a Subscriber Report.
     * Populates the chart with "Total Reservations" and "Total Waiting" data.
     * 
     * @param report The SubscriberReport data received from the server.
     */
    public void displaySubscriberReport(SubscriberReport report) {
        // Ensure UI updates happen on the JavaFX Application Thread
        Platform.runLater(() -> {
            reportChart.getData().clear();
            reportChart.setTitle("Subscriber Report: " + report.getStartDay() + " to " + report.getEndDay());
            yAxis.setLabel("Count");

            // Create series for Reservations
            XYChart.Series<String, Number> reservationsSeries = new XYChart.Series<>();
            reservationsSeries.setName("Total Reservations");
            
            // Create series for Waiting/Seated counts
            XYChart.Series<String, Number> waitingSeries = new XYChart.Series<>();
            waitingSeries.setName("Total Waiting (Seated)");

            // Populate series with data from report rows
            for (SubscriberReport.Row row : report.getRows()) {
                String dateStr = row.getReportDate().toString();
                reservationsSeries.getData().add(new XYChart.Data<>(dateStr, row.getTotalReservations()));
                waitingSeries.getData().add(new XYChart.Data<>(dateStr, row.getTotalWaiting()));
            }

            // Add series to the chart
            reportChart.getData().add(reservationsSeries);
            reportChart.getData().add(waitingSeries);
        });
    }

    /**
     * Updates the UI to display a Time Report.
     * Populates the chart with "Avg Arrival Time" and "Avg Leaving Time" data.
     * 
     * @param report The TimeReport data received from the server.
     */
    public void displayTimeReport(TimeReport report) {
        // Ensure UI updates happen on the JavaFX Application Thread
        Platform.runLater(() -> {
            reportChart.getData().clear();
            reportChart.setTitle("Time Report: " + report.getStartDay() + " to " + report.getEndDay());
            yAxis.setLabel("Minutes");

            // Create series for Average Arrival Time
            XYChart.Series<String, Number> arrivalSeries = new XYChart.Series<>();
            arrivalSeries.setName("Avg Arrival Time");
            
            // Create series for Average Leaving Time
            XYChart.Series<String, Number> leavingSeries = new XYChart.Series<>();
            leavingSeries.setName("Avg Leaving Time");

            // Populate series with data from report rows
            for (TimeReport.Row row : report.getRows()) {
                String dateStr = row.getReportDate().toString();
                arrivalSeries.getData().add(new XYChart.Data<>(dateStr, row.getAvgArrival()));
                leavingSeries.getData().add(new XYChart.Data<>(dateStr, row.getAvgLeaving()));
            }

            // Add series to the chart
            reportChart.getData().add(arrivalSeries);
            reportChart.getData().add(leavingSeries);
        });
    }
}