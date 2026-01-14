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

public class ReportsController implements Initializable {

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

    private ClientController client;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        comboReportType.setItems(FXCollections.observableArrayList("Subscriber Report", "Time Report"));
        
        // Default dates to last month
        LocalDate today = LocalDate.now();
        LocalDate lastMonth = today.minusMonths(1);
        dpStart.setValue(lastMonth.withDayOfMonth(1));
        dpEnd.setValue(lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()));

        // Restrict dates to today or past
        Callback<DatePicker, DateCell> dayCellFactory = dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item.isAfter(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #eeeeee;");
                }
            }
        };

        dpStart.setDayCellFactory(dayCellFactory);
        dpEnd.setDayCellFactory(dayCellFactory);
    }

    public void setDependencies(ClientController client) {
        this.client = client;
    }

    @FXML
    void handleFetchReport(ActionEvent event) {
        String reportType = comboReportType.getValue();
        LocalDate start = dpStart.getValue();
        LocalDate end = dpEnd.getValue();

        if (reportType == null || start == null || end == null) {
            TerminalUtils.showError("Missing Selection", "Please select a report type and date range.");
            return;
        }

        if (start.isAfter(LocalDate.now()) || end.isAfter(LocalDate.now())) {
            TerminalUtils.showError("Invalid Date", "Reports cannot be generated for future dates.");
            return;
        }

        if (start.isAfter(end)) {
            TerminalUtils.showError("Invalid Range", "Start date must be before or equal to end date.");
            return;
        }

        ArrayList<Object> content = new ArrayList<>();
        content.add(start);
        content.add(end);

        if (reportType.equals("Subscriber Report")) {
            client.handleMessageFromBoundary(TypeMessage.SUBSCRIBER_REPORT, content, Command.GET_SUBSCRIBER_REPORT_BY_RANGE_DATE);
        } else {
            client.handleMessageFromBoundary(TypeMessage.TIME_REPORT, content, Command.GET_TIME_REPORT_BY_RANGE_DATE);
        }
    }

    public void displaySubscriberReport(SubscriberReport report) {
        Platform.runLater(() -> {
            reportChart.getData().clear();
            reportChart.setTitle("Subscriber Report: " + report.getStartDay() + " to " + report.getEndDay());
            yAxis.setLabel("Count");

            XYChart.Series<String, Number> reservationsSeries = new XYChart.Series<>();
            reservationsSeries.setName("Total Reservations");
            
            XYChart.Series<String, Number> waitingSeries = new XYChart.Series<>();
            waitingSeries.setName("Total Waiting (Seated)");

            for (SubscriberReport.Row row : report.getRows()) {
                String dateStr = row.getReportDate().toString();
                reservationsSeries.getData().add(new XYChart.Data<>(dateStr, row.getTotalReservations()));
                waitingSeries.getData().add(new XYChart.Data<>(dateStr, row.getTotalWaiting()));
            }

            reportChart.getData().add(reservationsSeries);
            reportChart.getData().add(waitingSeries);
        });
    }

    public void displayTimeReport(TimeReport report) {
        Platform.runLater(() -> {
            reportChart.getData().clear();
            reportChart.setTitle("Time Report: " + report.getStartDay() + " to " + report.getEndDay());
            yAxis.setLabel("Minutes");

            XYChart.Series<String, Number> arrivalSeries = new XYChart.Series<>();
            arrivalSeries.setName("Avg Arrival Time");
            
            XYChart.Series<String, Number> leavingSeries = new XYChart.Series<>();
            leavingSeries.setName("Avg Leaving Time");

            for (TimeReport.Row row : report.getRows()) {
                String dateStr = row.getReportDate().toString();
                arrivalSeries.getData().add(new XYChart.Data<>(dateStr, row.getAvgArrival()));
                leavingSeries.getData().add(new XYChart.Data<>(dateStr, row.getAvgLeaving()));
            }

            reportChart.getData().add(arrivalSeries);
            reportChart.getData().add(leavingSeries);
        });
    }
}

