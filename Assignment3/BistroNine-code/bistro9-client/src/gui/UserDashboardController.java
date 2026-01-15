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

/**
 * Controller for the User Dashboard.
 * This is the main interface for logged-in subscribers, providing access to:
 * - Profile management (Home)
 * - Reservation creation and history
 * - Active reservation tracking
 * - Waitlist status
 * - Bill payment
 * - Managerial and operational tools (Role-based)
 */
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
    private Button btnVisitHistory;
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

    /** Tracks the currently selected navigation button for styling */
    private Button currentActiveBtn;

    private ClientController client;
    private Subscriber currentUser;

    /**
     * Injects the ClientController dependency.
     * @param client The active client controller for server communication.
     */
    public void setClient(ClientController client) {
        this.client = client;
    }

    /**
     * Sets the currently logged-in user.
     * @param user The subscriber object for the current session.
     */
    public void setCurrentUser(Subscriber user) {
		this.currentUser = user;
	}

    /**
     * Sets up the dashboard based on the logged-in user's role.
     * Configures button visibility and initializes the welcome message.
     * 
     * UPDATED: Both Manager and Representative can see the "Live Dashboard".
     * @param user The subscriber whose details are being loaded.
     */
    public void loadUserDetails(Subscriber user) {
        this.currentUser = user;
        // Register this controller instance globally for callbacks
        ClientController.userDashboardController = this;
        lblWelcome.setText("Hello, " + user.getFirstName() + " " + user.getLastName());

        String type = user.getType(); // Expected roles: "restaurant manager", "restaurant representative", "subscriber"

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
            setButtonVisible(btnLiveDashboard, true);
            setButtonVisible(btnPayBill, true);

        } else if (type.equals("restaurant representative")) {
            // Representative sees Operations AND Live Dashboard
            setButtonVisible(btnRestaurantOps, true);
            setButtonVisible(btnLiveDashboard, true);
            setButtonVisible(btnPayBill, true);
            
            // Note: btnViewReports remains hidden for Representative
        } else if (type.equals("subscriber")) {
            // Standard subscribers only see bill payment if they have an active session
            setButtonVisible(btnPayBill, true);
        }

        // Set default active button to Home (Profile View)
        setActiveButton(btnHome);
    }

    /**
     * Helper to set visibility and layout management of a button.
     * Managed property is set to false when invisible to collapse the space in the sidebar.
     * @param btn The button to toggle.
     * @param isVisible Whether the button should be visible and managed.
     */
    private void setButtonVisible(Button btn, boolean isVisible) {
        if (btn != null) {
            btn.setVisible(isVisible);
            btn.setManaged(isVisible); 
        }
    }

    /**
     * Updates the visual state of the sidebar buttons to show which one is active.
     * Adds the 'nav-btn-active' CSS class to the clicked button and removes it from the previous one.
     * @param clickedBtn The button that was just clicked.
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

    /**
     * Helper to clear the main content area and perform any necessary cleanups
     * before switching to a new view.
     */
    private void prepareContentArea() {
        contentArea.getChildren().clear();
        // Unregister reservation boundary to prevent logical memory leaks/alerts on other screens
        ClientController.unsubscribeReservationBoundry(null);
    }

    // --- Action Methods ---

    /**
     * Switches the content area to the Profile View.
     * @param event The action event from the Home button.
     */
    @FXML
    void goToHome(ActionEvent event) {
    	 try {
             setActiveButton((Button) event.getSource());
             FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ProfileView.fxml"));
             Parent root = loader.load();

             ProfileController controller = loader.getController();
             controller.setDependencies(this.client, this.currentUser,this);

             prepareContentArea();
             contentArea.getChildren().add(root);

         } catch (IOException e) {
             e.printStackTrace();
         }
    }

    /**
     * Switches the content area to the My Reservations view.
     * @param event The action event from the My Reservations button.
     */
    @FXML
    void goToMyReservations(ActionEvent event) {
        try {
            setActiveButton((Button) event.getSource());
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MyReservations.fxml"));
            Parent root = loader.load();

            MyReservationsController controller = loader.getController();
            controller.setDependencies(this.currentUser, this.client);

            prepareContentArea();
            contentArea.getChildren().add(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Switches the content area to the Visit History view.
     * @param event The action event from the Visit History button.
     */
    @FXML
    void goToVisitHistory(ActionEvent event) {
        try {
            setActiveButton((Button) event.getSource());
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/VisitHistory.fxml"));
            Parent root = loader.load();

            VisitHistoryController controller = loader.getController();
            controller.setDependencies(this.currentUser, this.client);

            prepareContentArea();
            contentArea.getChildren().add(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Switches the content area to the New Reservation creation screen.
     * @param event The action event from the New Reservation button.
     */
    @FXML
    void openNewReservation(ActionEvent event) {
        try {
            setActiveButton((Button) event.getSource());
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/NewReservation.fxml"));
            Parent root = loader.load();

            ReservationBoundry resController = loader.getController();
            resController.setClient(this.client);
            resController.initData(currentUser,false);

            prepareContentArea();
            contentArea.getChildren().add(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the request to leave the waitlist after user confirmation.
     * @param event The action event from the Leave Waitlist button.
     */
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

            // Send request to server to remove user from waitlist
            if (client != null) {
                client.handleMessageFromBoundary(TypeMessage.WAITLIST, content, Command.DELETE_FROM_WAIT_LIST);
            } else {
                TerminalUtils.showError("Error", "Client is not connected.");
            }
        }
    }

    /**
     * Callback method triggered when the server responds to a waitlist deletion request.
     * @param response Boolean indicating success or failure.
     */
    public void onWaitlistDeleteResponse(Object response) {
        // Run on UI thread to update status
        Platform.runLater(() -> {
            if (Boolean.TRUE.equals(response)) {
                TerminalUtils.showSuccess("Success", "You have been removed from the waitlist successfully.");
            } else {
                TerminalUtils.showError("Waitlist Info", "We couldn't find your entry on the waitlist or an error occurred.");
            }
        });
    }

    /**
     * Switches the content area to the Live Dashboard (Manager/Representative only).
     * @param event The action event from the Live Dashboard button.
     */
    @FXML
    void goToLiveDashboard(ActionEvent event) {
        setActiveButton((Button) event.getSource());
        System.out.println("DEBUG: Go to Live Dashboard");
         
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/LiveDashboard.fxml"));
            Parent root = loader.load();
            
             LiveDashboardController controller = loader.getController();
             controller.setDependencies(this.client);

            prepareContentArea();
            contentArea.getChildren().add(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the unified Restaurant Operations view (TabPane).
     * Accessible by Managers and Representatives to manage tables, waitlists, etc.
     * @param event The action event from the Restaurant Operations button.
     */
    @FXML
    void goToRestaurantOps(ActionEvent event) {
        setActiveButton((Button) event.getSource());
        try {
            // Load the FXML that contains the Tabs (Table Management, Waitlist, etc.)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/RestaurantManagement.fxml"));
            Parent root = loader.load();

            RestaurantManagementController opsController = loader.getController();
            
            // Pass the dependencies (client and user) to the new controller
            opsController.setDependencies(this.client, this.currentUser); 

            // Show in the center area
            prepareContentArea();
            contentArea.getChildren().add(root);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading Restaurant Management screen: " + e.getMessage());
        }
    }

    /**
     * Switches the content area to the Reports view (Manager only).
     * @param event The action event from the View Reports button.
     */
    @FXML
    void goToViewReports(ActionEvent event) {
        setActiveButton((Button) event.getSource());
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ReportsScreen.fxml"));
            Parent root = loader.load();

            ReportsController controller = loader.getController();
            controller.setDependencies(this.client);
            // Register reports controller for server callbacks
            ClientController.reportsController = controller;

            prepareContentArea();
            contentArea.getChildren().add(root);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading Reports screen: " + e.getMessage());
        }
    }

    /**
     * Switches the content area to the Bill Payment screen.
     * @param event The action event from the Pay Bill button.
     */
    @FXML
    void goToPayBill(ActionEvent event) {
        setActiveButton((Button) event.getSource());
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/PayBillScreen.fxml"));
            Parent root = loader.load();

            PayBillController controller = loader.getController();
            controller.setDependencies(this.client, this.currentUser, this);

            prepareContentArea();
            contentArea.getChildren().add(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Logs the user out and returns to the Login Screen.
     * @param event The action event from the Logout button.
     */
    @FXML
    void doLogout(ActionEvent event) {
        try {
            // Unregister current active boundary before logout
            ClientController.unsubscribeReservationBoundry(null);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/LoginScreen.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            controller.setClient(client);
            controller.setMode(LoginController.Mode.REMOTE);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("BistroNine Client - Login");
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
