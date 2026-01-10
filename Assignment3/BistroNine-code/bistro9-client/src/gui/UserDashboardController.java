package gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import controller.ClientController;
import data.Command;
import data.Subscriber;
import data.TypeMessage;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class UserDashboardController {

    @FXML
    private Label lblWelcome;
    @FXML
    private StackPane contentArea;

    @FXML
    private Button btnHome;
    @FXML
    private Button btnMyReservations;
    @FXML
    private Button btnNewReservation;
    @FXML
    private Button btnLeaveWaitlist;
    @FXML
    private Button btnPayBill;
    @FXML
    private Button btnRestaurantOps; 
    @FXML
    private Button btnViewReports;   
    @FXML
    private Button btnLiveDashboard;

    private Button currentActiveBtn;

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
        ClientController.userDashboardController = this;
        lblWelcome.setText("Hello, " + user.getFirstName() + " " + user.getLastName());

        String type = user.getType(); // Assuming format like "restaurant manager"

        // 1. Reset all admin/staff buttons to hidden by default
        setButtonVisible(btnRestaurantOps, false);
        setButtonVisible(btnViewReports, false);
        setButtonVisible(btnLiveDashboard, false);
        setButtonVisible(btnPayBill, false);

        // 2. Turn on buttons based on specific roles
        if (type.equals("restaurant manager")) {
            // Manager sees ALL operational buttons
            setButtonVisible(btnRestaurantOps, true);
            setButtonVisible(btnViewReports, true);
            setButtonVisible(btnLiveDashboard, true); // Visible for Manager
            setButtonVisible(btnPayBill, true);

        } else if (type.equals("restaurant representative")) {
            // Representative sees Operations AND Live Dashboard
            setButtonVisible(btnRestaurantOps, true);
            setButtonVisible(btnLiveDashboard, true); // Visible for Representative
            setButtonVisible(btnPayBill, true);
            
            // Note: btnViewReports remains hidden for Representative (default behavior)
        } else if (type.equals("subscriber")) {
            setButtonVisible(btnPayBill, true);
        }

        // Set default active button (e.g., Home)
        setActiveButton(btnHome);
    }

    private void setButtonVisible(Button btn, boolean isVisible) {
        if (btn != null) {
            btn.setVisible(isVisible);
            btn.setManaged(isVisible); // Collapses the space if hidden
        }
    }

    /**
     * Updates the visual state of the sidebar buttons to show which one is active.
     */
    private void setActiveButton(Button clickedBtn) {
        // Remove active class from previous button
        if (currentActiveBtn != null) {
            currentActiveBtn.getStyleClass().remove("nav-btn-active");
        }

        // Add active class to new button
        if (clickedBtn != null) {
            clickedBtn.getStyleClass().add("nav-btn-active");
            currentActiveBtn = clickedBtn;
        }
    }

    // --- Action Methods ---

    @FXML
    void goToHome(ActionEvent event) {
    	 try {
             setActiveButton((Button) event.getSource());
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
            setActiveButton((Button) event.getSource());
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
            setActiveButton((Button) event.getSource());
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
    void handleExitWaitlist(ActionEvent event) {
        setActiveButton((Button) event.getSource());
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Leave Waitlist");
        alert.setHeaderText("Confirmation");
        alert.setContentText("Are you sure you want to leave the waitlist?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            ArrayList<Object> content = new ArrayList<>();
            content.add("subscriber"); 
            content.add(currentUser.getCustomerId());
            content.add(null);

            if (client != null) {
                client.handleMessageFromBoundary(TypeMessage.WAITLIST, content, Command.DELETE_FROM_WAIT_LIST);
            } else {
                TerminalUtils.showError("Error", "Client is not connected.");
            }
        }
    }

    public void onWaitlistDeleteResponse(Object response) {
        Platform.runLater(() -> {
            if (Boolean.TRUE.equals(response)) {
                TerminalUtils.showSuccess("Success", "You have been removed from the waitlist successfully.");
            } else {
                TerminalUtils.showError("Waitlist Info", "We couldn't find your entry on the waitlist or an error occurred.");
            }
        });
    }

    @FXML
    void goToLiveDashboard(ActionEvent event) {
        setActiveButton((Button) event.getSource());
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
        setActiveButton((Button) event.getSource());
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
        setActiveButton((Button) event.getSource());
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ReportsScreen.fxml"));
            Parent root = loader.load();

            ReportsController controller = loader.getController();
            controller.setDependencies(this.client);
            ClientController.reportsController = controller;

            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading Reports screen: " + e.getMessage());
        }
    }

    @FXML
    void goToPayBill(ActionEvent event) {
        setActiveButton((Button) event.getSource());
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/PayBillScreen.fxml"));
            Parent root = loader.load();

            PayBillController controller = loader.getController();
            controller.setDependencies(this.client, this.currentUser, this);

            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
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
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setMaximized(false);
            stage.show();
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}