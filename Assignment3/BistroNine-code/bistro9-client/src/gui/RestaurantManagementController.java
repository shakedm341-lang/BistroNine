package gui;

import controller.ClientController;
import data.Subscriber;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;

public class RestaurantManagementController {

    @FXML
    private TabPane opsTabPane;

    @FXML
    private Tab registerClientTab;

    @FXML
    private Tab manageTablesTab;

    @FXML
    private Tab reservationManagementTab;

    @FXML
    private Tab waitingListTab;

    @FXML
    private Tab settingsTab;

    private ClientController client;
    private Subscriber currentUser;

    private boolean reservationTabLoaded = false;

    public void setDependencies(ClientController client, Subscriber currentUser) {
        this.client = client;
        this.currentUser = currentUser;

        System.out.println(
            "DEBUG: RestaurantManagementController initialized for user: "
            + currentUser.getUsername()
        );

        initTabListeners();
    }

    /**
     * Attach listeners to all tabs
     */
    private void initTabListeners() {

        reservationManagementTab.setOnSelectionChanged(
            new EventHandler<Event>() {

                @Override
                public void handle(Event event) {

                    if (reservationManagementTab.isSelected()
                            && !reservationTabLoaded) {

                        loadReservationManagementTab();
                        reservationTabLoaded = true;
                    }
                }
            }
        );

        // דוגמה עתידית:
        // manageTablesTab.setOnSelectionChanged(...)
        // waitingListTab.setOnSelectionChanged(...)
    }

    private void loadReservationManagementTab() {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/gui/ReservationManagement.fxml")
            );

            AnchorPane pane = loader.load();

            ReservationManagementController controller =
                    loader.getController();
            controller.setClient(client);

            reservationManagementTab.setContent(pane);

            System.out.println(
                "DEBUG: Reservation Management tab loaded"
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
