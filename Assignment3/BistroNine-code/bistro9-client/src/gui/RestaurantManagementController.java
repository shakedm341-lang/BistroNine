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
    private Tab reservationManagementTab;

    @FXML
    private Tab registerClientTab;
    
    @FXML
    private Tab subscribersTab;

    @FXML
    private Tab manageTablesTab;

    @FXML
    private Tab settingsTab;

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

    @FXML
    private TableManagementController tableManagementViewController;

    @FXML
    private SettingsController settingsViewController;

    // =================================================================================
    // Data Fields
    // =================================================================================

    private ClientController client;
    private Subscriber currentUser;

    // =================================================================================
    // Initialization & Logic
    // =================================================================================

    @FXML
    public void initialize() {
        // Add a listener to detect when the user switches tabs
        opsTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                loadDataForTab(newTab);
            }
        });
    }

    /**
     * Logic to determine which tab was selected and trigger the specific data refresh.
     * This implements lazy loading for the restaurant management dashboard.
     */
    private void loadDataForTab(Tab tab) {
        if (this.client == null) {
            System.out.println("DEBUG: Client not yet set, skipping lazy load for tab: " + tab.getText());
            return;
        }

        if (tab == subscribersTab) {
            System.out.println("DEBUG: Lazy loading Subscribers data");
            if (subscribersViewController != null) {
                subscribersViewController.sendRequestToServer();
            }
        } else if (tab == manageTablesTab) {
            System.out.println("DEBUG: Lazy loading Table Management data");
            if (tableManagementViewController != null) {
                // Using the specific method in TableManagementController to fetch data
                // In TableManagementController, fetchTables is private, but it is called by refreshTableData
                // Wait, looking at TableManagementController.java:
                // void refreshTableData(ActionEvent event) { fetchTables(); }
                // and fetchTables() is private.
                // I should probably make fetchTables public or just call refreshTableData(null).
                // Actually, let's check if I can call fetchTables. No, it's private.
                // I'll call refreshTableData(null) or I'll go back and make fetchTables public.
                // Looking at the code for TableManagementController, refreshTableData is @FXML and package-private (default).
                // RestaurantManagementController and TableManagementController are in the same package 'gui'.
                // So I can call fetchTables().
                tableManagementViewController.fetchTables();
            }
        } else if (tab == settingsTab) {
            System.out.println("DEBUG: Lazy loading Settings data");
            if (settingsViewController != null) {
                settingsViewController.requestOpeningHoursFromServer();
            }
        }
    }

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
        if (tableManagementViewController != null) {
            tableManagementViewController.setClient(client);
        }
        if (settingsViewController != null) {
            settingsViewController.setDependencies(client, currentUser);
        }

        // Trigger lazy loading for the initially selected tab
        Tab selectedTab = opsTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            loadDataForTab(selectedTab);
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