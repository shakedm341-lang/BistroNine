package gui;

import controller.ClientController;
import data.Subscriber;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * Main controller for the Restaurant Management Dashboard.
 * This dashboard serves as a central hub for restaurant staff and managers,
 * allowing them to access various operational functionalities through a tabbed interface.
 * 
 * It manages the lifecycle and dependency injection for several sub-modules:
 * - Reservation creation and management
 * - Client registration and subscriber viewing
 * - Table management and restaurant settings
 */
public class RestaurantManagementController {

    // =================================================================================
    // FXML UI Components
    // =================================================================================

    /** The main container for all operational tabs */
    @FXML
    private TabPane opsTabPane;

    /** Tab for creating a new reservation manually */
    @FXML
    private Tab createReservationTab;
    
    /** Tab for viewing and managing existing reservations */
    @FXML
    private Tab reservationManagementTab;

    /** Tab for registering a new client into the system */
    @FXML
    private Tab registerClientTab;
    
    /** Tab for managing restaurant table layout and availability */
    @FXML
    private Tab manageTablesTab;

    /** Tab for restaurant-wide settings such as opening hours */
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
    
    /** Controller for the "Register Client" sub-view */
    @FXML
    private RegisterClientController registerClientViewController;
    
    /** Controller for the "Table Management" sub-view */
    @FXML
    private TableManagementController tableManagementViewController;

    /** Controller for the "Restaurant Settings" sub-view */
    @FXML
    private SettingsController settingsViewController;

    // =================================================================================
    // Data Fields
    // =================================================================================

    /** The network client used for server communication across all tabs */
    private ClientController client;
    
    /** The currently authenticated user (staff or manager) */
    private Subscriber currentUser;

    // =================================================================================
    // Initialization & Logic
    // =================================================================================

    /**
     * Called by JavaFX after all FXML fields are injected.
     * Sets up the tab selection listener to support lazy loading of data.
     */
    @FXML
    public void initialize() {
        // Add a listener to detect when the user switches tabs
        // This allows us to only fetch data from the server when a tab is actually viewed
        opsTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                loadDataForTab(newTab);
            }
        });
    }

    /**
     * Logic to determine which tab was selected and trigger the specific data refresh.
     * This implements lazy loading for the restaurant management dashboard to optimize network usage.
     * 
     * @param tab The tab that was recently selected by the user.
     */
    private void loadDataForTab(Tab tab) {
        // Prevent data loading if the client connection hasn't been established yet
        if (this.client == null) {
            System.out.println("DEBUG: Client not yet set, skipping lazy load for tab: " + tab.getText());
            return;
        }

        // Identify the tab and trigger the corresponding data fetch in its controller
        if (tab == manageTablesTab) {
            System.out.println("DEBUG: Lazy loading Table Management data");
            if (tableManagementViewController != null) {
                // TableManagementController.fetchTables() is package-private, 
                // allowing us to call it directly from here as we are in the 'gui' package.
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
     * Sets external dependencies and propagates them to all nested child controllers.
     * This method is typically called by the login screen or main app navigator after 
     * this controller's view is loaded.
     * 
     * @param client      The network client for server communication.
     * @param currentUser The currently logged-in user.
     */
    public void setDependencies(ClientController client, Subscriber currentUser) {
        this.client = client;
        this.currentUser = currentUser;

        System.out.println("DEBUG: RestaurantManagementController initialized for user: " 
                           + currentUser.getUsername());

        // Propagate the network client and user context to all sub-controllers
        
        // 1. Create Reservation Module
        if (createReservationViewController != null) {
            createReservationViewController.setClient(client);
            // Passing true for 'isStaff' mode in the reservation boundary
            createReservationViewController.initData(currentUser, true);
        }

        // 2. Reservation Management Module
        if (reservationViewController != null) {
            reservationViewController.setClient(client);
        }
        
        // 3. Client Registration Module
        if (registerClientViewController != null) {
            registerClientViewController.setClientController(client);
        }
        
        // 5. Table Management Module
        if (tableManagementViewController != null) {
            tableManagementViewController.setClient(client);
        }
        
        // 6. Restaurant Settings Module
        if (settingsViewController != null) {
            settingsViewController.setDependencies(client, currentUser);
        }

        // Trigger lazy loading for the initially selected tab now that dependencies are set
        Tab selectedTab = opsTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            loadDataForTab(selectedTab);
        }
    }

    /**
     * Programmatically switches the active tab to "Create Reservation".
     * Useful for navigation from other parts of the application UI.
     */
    public void navigateToCreateReservation() {
        if (opsTabPane != null && createReservationTab != null) {
            opsTabPane.getSelectionModel().select(createReservationTab);
        }
    }
}
