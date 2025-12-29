package gui;

import java.net.URL;
import java.util.ResourceBundle;

import controller.ClientController;
import data.Subscriber;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class LiveDashboardController implements Initializable {

    @FXML 
    private TabPane mainTabPane;

    // References to the Tab objects defined in FXML
    @FXML 
    private Tab tabActiveOrders;
    @FXML 
    private Tab tabWaitingList;
    @FXML 
    private Tab tabCurrentDiners;

    // =================================================================================
    // Nested Controllers Injection
    // JavaFX automatically injects these based on the <fx:include fx:id="..."> tag.
    // Naming rule: [fx:id] + "Controller"
    // =================================================================================
    
    @FXML 
    private TabActiveReservationController tabActiveReservationController;
    
    @FXML 
    private TabWaitingListController tabWaitingListController;
    
    @FXML 
    private TabCurrentDinersController tabCurrentDinersController;

    // Dependencies
    private ClientController client;
    

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Add a listener to detect when the user switches tabs
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                loadDataForTab(newTab);
            }
        });
    }

    /**
     * Initializes the dashboard dependencies and passes them to the sub-controllers.
     * This method is called by the main UserDashboardController.
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

        // 2. Manually trigger data load for the default tab (Active Orders)
        // because the listener might not fire for the initial default selection.
        if (tabActiveReservationController != null) {
            tabActiveReservationController.refreshData();
        }
    }

    /**
     * Logic to determine which tab was selected and trigger the specific data refresh.
     */
    private void loadDataForTab(Tab tab) {
        // Check which tab object is currently selected
        if (tab == tabActiveOrders) {
            System.out.println("Switched to Active Orders tab");
            if (tabActiveReservationController != null) {
            	tabActiveReservationController.refreshData();
            }
        } 
        else if (tab == tabWaitingList) {
            System.out.println("Switched to Waiting List tab");
            if (tabWaitingListController != null) {
                tabWaitingListController.refreshData();
            }
        } 
        else if (tab == tabCurrentDiners) {
            System.out.println("Switched to Current Diners tab");
            if (tabCurrentDinersController != null) {
                tabCurrentDinersController.refreshData();
            }
        }
    }
}