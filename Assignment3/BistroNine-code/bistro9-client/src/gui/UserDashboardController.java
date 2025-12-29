package gui;

import java.io.IOException;

import controller.ClientController;
import data.Subscriber;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class UserDashboardController {

    @FXML
    private Label lblWelcome;
    @FXML
    private StackPane contentArea;

 
    @FXML
    private Button btnRestaurantOps; 
    @FXML
    private Button btnViewReports;   
    
    @FXML
    private Button btnLiveDashboard;

    private ClientController client;
    private Subscriber currentUser;

    public void setClient(ClientController client) {
        this.client = client;
    }
    public void setCurrentUser(Subscriber user) {
		this.currentUser = user;
	}

    /**
     * Sets up the dashboard based on the logged-in user's role.
     * UPDATED: Both Manager and Representative can see the "Live Dashboard".
     */
    public void loadUserDetails(Subscriber user) {
        this.currentUser = user;
        lblWelcome.setText("Hello, " + user.getUsername());

        String type = user.getType(); // Assuming format like "restaurant manager"

        // 1. Reset all admin/staff buttons to hidden by default
        setButtonVisible(btnRestaurantOps, false);
        setButtonVisible(btnViewReports, false);
        setButtonVisible(btnLiveDashboard, false);

        // 2. Turn on buttons based on specific roles
        if (type.equals("restaurant manager")) {
            // Manager sees ALL operational buttons
            setButtonVisible(btnRestaurantOps, true);
            setButtonVisible(btnViewReports, true);
            setButtonVisible(btnLiveDashboard, true); // Visible for Manager

        } else if (type.equals("restaurant representative")) {
            // Representative sees Operations AND Live Dashboard
            setButtonVisible(btnRestaurantOps, true);
            setButtonVisible(btnLiveDashboard, true); // Visible for Representative
            
            // Note: btnViewReports remains hidden for Representative (default behavior)
        }
    }

    private void setButtonVisible(Button btn, boolean isVisible) {
        if (btn != null) {
            btn.setVisible(isVisible);
            btn.setManaged(isVisible); // Collapses the space if hidden
        }
    }

    // --- Action Methods ---

    @FXML
    void goToHome(ActionEvent event) {
    	 try {
             FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ProfileView.fxml"));
             Parent root = loader.load();

             ProfileController controller = loader.getController();
             controller.setDependencies(this.client, this.currentUser,this);

             contentArea.getChildren().clear();
             contentArea.getChildren().add(root);

         } catch (IOException e) {
             e.printStackTrace();
         }
    }

    @FXML
    void goToMyReservations(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MyReservations.fxml"));
            Parent root = loader.load();

            MyReservationsController controller = loader.getController();
            controller.setDependencies(this.currentUser, this.client);

            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void openNewReservation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/NewReservation.fxml"));
            Parent root = loader.load();

            ReservationBoundry resController = loader.getController();
            resController.setClient(this.client);
            resController.initData(currentUser,false);

            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    void goToLiveDashboard(ActionEvent event) {
        System.out.println("DEBUG: Go to Live Dashboard");
         
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/LiveDashboard.fxml"));
            Parent root = loader.load();
            
            // Assume the controller needs client/user dependencies
             LiveDashboardController controller = loader.getController();
             controller.setDependencies(this.client);

            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    /**
     * Loads the unified Restaurant Operations view (TabPane).
     * This replaces goToManageTables and goToRegisterClient.
     */
    @FXML
    void goToRestaurantOps(ActionEvent event) {
        try {
            // Load the FXML that contains the Tabs
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/RestaurantManagement.fxml"));
            Parent root = loader.load();

            // Get the controller with the NEW name
            RestaurantManagementController opsController = loader.getController();
            
            // Pass the dependencies (client and user) to the new controller
            opsController.setDependencies(this.client, this.currentUser); 

            // Show in the center area
            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading Restaurant Management screen: " + e.getMessage());
        }
    }

    @FXML
    void goToViewReports(ActionEvent event) {
        System.out.println("DEBUG: Go to View Reports");
        // TODO: Load Reports screen (Analytical reports for Manager)
    }

    @FXML
    void doLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MainSelection.fxml"));
            Parent root = loader.load();
            MainSelectionController controller = loader.getController();
            controller.setClient(client);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("BistroNine - Select Mode");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}