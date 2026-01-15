package gui;

import java.net.URL;
import java.util.ResourceBundle;

import controller.ClientController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * Controller for the Live Dashboard view in the BistroNine client application.
 * This class manages a TabPane containing three main live-monitoring views:
 * Active Orders, Waiting List, and Current Diners.
 * It handles the initialization of sub-controllers and triggers data refreshes
 * when users switch between tabs (lazy loading).
 */
public class LiveDashboardController implements Initializable {

    /** The main container for the different dashboard sections. */
    @FXML 
    private TabPane mainTabPane;

    // References to the Tab objects defined in FXML
    /** Tab representing the list of active reservations/orders. */
    @FXML 
    private Tab tabActiveOrders;
    /** Tab representing the restaurant's waiting list. */
    @FXML 
    private Tab tabWaitingListTab;
    /** Tab representing guests currently dining at the restaurant. */
    @FXML 
    private Tab tabCurrentDinersTab;
    /** Tab representing the list of subscribers. */
    @FXML 
    private Tab subscribersTab;
    /** Tab representing subscriber visit logs search. */
    @FXML
    private Tab subscriberHistoryTab;

    // =================================================================================
    // Nested Controllers Injection
    // JavaFX automatically injects these based on the <fx:include fx:id="..."> tag.
    // Naming rule: [fx:id] + "Controller"
    // =================================================================================
    
    /** Controller for the 'Active Reservations' sub-view. */
    @FXML 
    private TabActiveReservationController tabActiveReservationController;
    
    /** Controller for the 'Waiting List' sub-view. */
    @FXML 
    private TabWaitingListController tabWaitingListController;
    
    /** Controller for the 'Current Diners' sub-view. */
    @FXML 
    private TabCurrentDinersController tabCurrentDinersController;

    /** Controller for the 'Subscribers List' sub-view. */
    @FXML
    private SubscribersViewController subscribersViewController;

    /** Controller for the 'Visit Logs' sub-view. */
    @FXML
    private SubscriberVisitHistoryController subscriberVisitHistoryController;

    /** The main client controller used for server communication. */
    private ClientController client;
    

    /**
     * Initializes the controller. Sets up a listener on the TabPane to 
     * automatically refresh data whenever the active tab changes.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Add a listener to detect when the user switches tabs
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                // Trigger lazy-loading/refresh of the selected tab's data
                loadDataForTab(newTab);
            }
        });
    }

    /**
     * Sets the necessary dependencies for this controller and its sub-controllers.
     * This ensures all child views have access to the server communication client.
     *
     * @param client The ClientController instance for API requests.
     */
    public void setDependencies(ClientController client) {
        this.client = client;
        

        // 1. Pass dependencies to all sub-controllers immediately.
        // This ensures they have the client/user objects ready, 
        // but does NOT trigger the server request yet (lazy loading).
        if (tabActiveReservationController != null) {
            tabActiveReservationController.initData(client);
        }
        if (tabWaitingListController != null) {
            tabWaitingListController.initData(client);
        }
        if (tabCurrentDinersController != null) {
            tabCurrentDinersController.initData(client);
        }
        if (subscribersViewController != null) {
            subscribersViewController.setClientController(client);
        }
        if (subscriberVisitHistoryController != null) {
            subscriberVisitHistoryController.setClientController(client);
        }

        // 2. Initial load for the currently visible tab once dependencies are set.
        Tab selectedTab = mainTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            loadDataForTab(selectedTab);
        }
    }

    /**
     * Determines which tab was selected and triggers the specific data refresh
     * for that tab's sub-controller.
     *
     * @param tab The Tab that was just selected by the user.
     */
    private void loadDataForTab(Tab tab) {
    	
    	if (this.client == null) {
            System.out.println("DEBUG: Client not yet set, skipping data load for tab: " + tab.getText());
            return;
        }
    	
    	
        // Match the selected Tab object with the injected Tab fields
        if (tab == tabActiveOrders) {
            System.out.println("Switched to Active Orders tab");
            if (tabActiveReservationController != null) {
            	tabActiveReservationController.refreshData();
            }
        } 
        else if (tab == tabWaitingListTab) {
            System.out.println("Switched to Waiting List tab");
            if (tabWaitingListController != null) {
                tabWaitingListController.refreshData();
            }
        } 
        else if (tab == tabCurrentDinersTab) {
            System.out.println("Switched to Current Diners tab");
            if (tabCurrentDinersController != null) {
                tabCurrentDinersController.refreshData();
            }
        }
        else if (tab == subscribersTab) {
            System.out.println("Switched to Subscribers tab");
            if (subscribersViewController != null) {
                subscribersViewController.sendRequestToServer();
            }
        }
        else if (tab == subscriberHistoryTab) {
            System.out.println("Switched to Visit Logs tab");
        }
    }
}