package gui;

import java.net.URL;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import controller.ClientController;
import data.Command;
import data.Subscriber;
import data.TableReservation;
import data.TypeMessage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class VisitHistoryController implements Initializable, IReservationViewer {

    @FXML
    private TableView<TableReservation> historyTable;

    @FXML
    private TableColumn<TableReservation, Timestamp> colDate;

    @FXML
    private TableColumn<TableReservation, Timestamp> colArrival;

    @FXML
    private TableColumn<TableReservation, Timestamp> colLeaving;

    @FXML
    private TableColumn<TableReservation, Integer> colGuests;

    private ObservableList<TableReservation> historyList = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    private ClientController clientController;
    private Subscriber currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initColumns();
    }

    private void initColumns() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("reservationDate"));
        colDate.setCellFactory(column -> new TableCell<TableReservation, Timestamp>() {
            @Override
            protected void updateItem(Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toLocalDateTime().format(dateFormatter));
                }
            }
        });

        colArrival.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        colArrival.setCellFactory(column -> new TableCell<TableReservation, Timestamp>() {
            @Override
            protected void updateItem(Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("-");
                } else {
                    setText(item.toLocalDateTime().format(timeFormatter));
                }
            }
        });

        colLeaving.setCellValueFactory(new PropertyValueFactory<>("leavingTime"));
        colLeaving.setCellFactory(column -> new TableCell<TableReservation, Timestamp>() {
            @Override
            protected void updateItem(Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("-");
                } else {
                    setText(item.toLocalDateTime().format(timeFormatter));
                }
            }
        });

        colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfDiners"));

        historyTable.setItems(historyList);
    }

    public void setDependencies(Subscriber user, ClientController controller) {
        this.currentUser = user;
        this.clientController = controller;
        loadHistoryData();
    }

    private void loadHistoryData() {
        if (currentUser == null || clientController == null) return;

        ArrayList<Object> params = new ArrayList<>();
        params.add("subscriber");
        params.add(currentUser.getCustomerId());

        clientController.setReservationViewer(this);
        clientController.handleMessageFromBoundary(
                TypeMessage.RESERVATION,
                params,
                Command.GET_ALL_RESERVATIONS_BY_CUSTOMER
        );
    }

    @Override
    public void setReservationsList(ArrayList<TableReservation> reservations) {
        javafx.application.Platform.runLater(() -> {
            if (reservations != null) {
                // Filter for completed visits (either status is 'completed' or has arrival/leaving times)
                ArrayList<TableReservation> completedVisits = reservations.stream()
                        .filter(res -> "completed".equalsIgnoreCase(res.getStatus()) || 
                                      (res.getArrivalTime() != null && res.getLeavingTime() != null))
                        .collect(Collectors.toCollection(ArrayList::new));
                
                historyList.setAll(completedVisits);
            }
        });
    }
}

