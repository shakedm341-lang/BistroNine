package gui;

import controller.ClientController;
import data.Subscriber;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * Main controller for the Restaurant Management Dashboard.
 * Serves as a container for various operational tabs.
 */
public class RestaurantManagementController {

    @FXML
    private TabPane opsTabPane;

    @FXML
    private Tab createReservationTab;
    
    @FXML
    private Tab registerClientTab;
    
    @FXML
    private Tab subscribersTab;

    // =================================================================================
    // Nested Controllers Injection
    // JavaFX automatically injects the controllers of included FXML files.
    // Naming Convention: [fx:id of the include] + "Controller"
    // =================================================================================

    /**
     * Controller for the "Create New Reservation" tab.
     * In FXML: <fx:include fx:id="createReservationView" ... />
     */
    @FXML
    private ReservationBoundry createReservationViewController;

    /**
     * Controller for the "Reservation Management" tab.
     * In FXML: <fx:include fx:id="reservationView" ... />
     */
    @FXML
    private ReservationManagementController reservationViewController;
    
    @FXML
    private RegisterClientController registerClientViewController;
    
    @FXML
    private SubscribersViewController subscribersViewController;

    // =================================================================================
    // Data Fields
    // =================================================================================

    private ClientController client;
    private Subscriber currentUser;

    // =================================================================================
    // Initialization & Logic
    // =================================================================================

    /**
     * Sets external dependencies and propagates them to child controllers.
     * This method is typically called by the login screen or main app navigator.
     * * @param client      The network client for server communication.
     * @param currentUser The currently logged-in user.
     */
    public void setDependencies(ClientController client, Subscriber currentUser) {
        this.client = client;
        this.currentUser = currentUser;

        System.out.println("DEBUG: RestaurantManagementController initialized for user: " 
                           + currentUser.getUsername());

        // Propagate dependencies to the nested "Create Reservation" controller
        if (createReservationViewController != null) {
            createReservationViewController.setClient(client);
            createReservationViewController.initData(currentUser,true);
            // If the child controller needs the user info:
            // createReservationViewController.setCurrentUser(currentUser); 
        }

        // Propagate dependencies to the nested "Reservation Management" controller
        if (reservationViewController != null) {
            reservationViewController.setClient(client);
        }
        
        if (registerClientViewController != null) {
            registerClientViewController.setClientController(client);
        }
        if (subscribersViewController != null) {
			subscribersViewController.setClientController(client);
		}
    }

    /**
     * Programmatically switches the active tab to "Create Reservation".
     * Useful for navigation from other parts of the UI.
     */
    public void navigateToCreateReservation() {
        if (opsTabPane != null && createReservationTab != null) {
            opsTabPane.getSelectionModel().select(createReservationTab);
        }
    }
}