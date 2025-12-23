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

    private ClientController client;
    private Subscriber currentUser;

    public void setClient(ClientController client) {
        this.client = client;
    }

    /**
     * Sets up the dashboard based on the logged-in user's role.
     * Logic updated to support the new unified "Restaurant Operations" button.
     */
    public void loadUserDetails(Subscriber user) {
        this.currentUser = user;
        lblWelcome.setText("Hello, " + user.getUsername());

        String type = user.getType(); // Assuming format like "restaurant manager"

        // Default: Hide operations buttons
        setButtonVisible(btnRestaurantOps, false);
        setButtonVisible(btnViewReports, false);

        // Role-based visibility
        if (type.equals("restaurant manager")) {
        	
            // Manager sees Operations (Tables/Clients) AND Reports 
            setButtonVisible(btnRestaurantOps, true);
            setButtonVisible(btnViewReports, true);
        } else if (type.equals("restaurant representative")) {
            // Representative sees Operations 
            setButtonVisible(btnRestaurantOps, true);
            // Usually, specific analytical reports are for managers, 
            // but Reps see lists/orders inside Operations tabs
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
        System.out.println("DEBUG: Go to Home / Profile");
        // TODO: Load Profile screen
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
            resController.initData(currentUser);

            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);

        } catch (Exception e) {
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