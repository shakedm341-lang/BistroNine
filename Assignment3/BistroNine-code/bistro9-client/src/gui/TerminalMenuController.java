package gui;

import controller.ClientController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Controller for the main terminal menu.
 * This screen provides the primary navigation options for users at the restaurant terminal,
 * including checking in for a table, joining the waitlist, and paying bills.
 */
public class TerminalMenuController {

    /** The client controller used for communication with the server */
    private ClientController client;

    @FXML
    private Button btnGetTable;

    @FXML
    private Button btnJoinWaitlist;

    @FXML
    private Button btnPayBill;

    @FXML
    private Button btnExit;

    @FXML
    private Label lblWelcome;

    /**
     * Initializes the controller with the client instance and updates the welcome message.
     * 
     * @param client The ClientController instance to use
     */
    public void setClient(ClientController client) {
        this.client = client;
        updateModeLabel();
    }

    /**
     * Updates the welcome label based on whether the user is a registered subscriber 
     * or a walk-in guest.
     */
    private void updateModeLabel() {
        if (lblWelcome != null) {
            // Check if user is identified as a subscriber in the BaseTerminalController
            if (BaseTerminalController.currentUserType == BaseTerminalController.UserType.SUBSCRIBER) {
                String name = BaseTerminalController.currentSubscriberName;
                lblWelcome.setText("Welcome, " + (name != null ? name : "Subscriber"));
            } else {
                lblWelcome.setText("Welcome, Guest");
            }
        }
    }

    /**
     * Navigates to the table check-in screen.
     * 
     * @param event The ActionEvent from the button click
     */
    @FXML
    void handleGetTable(ActionEvent event) {
        switchScene(event, "/gui/GetTableScreen.fxml", "Terminal - Check-In");
    }

    /**
     * Navigates to the screen for joining the waiting list.
     * 
     * @param event The ActionEvent from the button click
     */
    @FXML
    void handleJoinWaitlist(ActionEvent event) {
        switchScene(event, "/gui/JoinWaitlistScreen.fxml", "Terminal - Walk-In");
    }

    /**
     * Navigates to the screen for leaving the waiting list.
     * 
     * @param event The ActionEvent from the button click
     */
    @FXML
    void handleLeaveWaitlist(ActionEvent event) {
        switchScene(event, "/gui/LeaveWaitlistScreen.fxml", "Terminal - Leave Waiting List");
    }

    /**
     * Navigates to the bill payment screen.
     * 
     * @param event The ActionEvent from the button click
     */
    @FXML
    void handlePayBill(ActionEvent event) {
        switchScene(event, "/gui/PayBillScreen.fxml", "Terminal - Pay Bill");
    }

    /**
     * Resets the terminal session and returns to the initial identification screen.
     * 
     * @param event The ActionEvent from the button click
     */
    @FXML
    void handleExit(ActionEvent event) {
        try {
            // Reset user type to Guest when logging out from the terminal
            BaseTerminalController.setUserType(BaseTerminalController.UserType.GUEST);

            // Load the initial identification screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/TerminalIdentificationScreen.fxml"));
            Parent root = loader.load();

            // Pass the client controller to the next screen
            TerminalIdentificationController controller = loader.getController();
            controller.setClient(this.client);

            switchScene(event, root, "BistroNine - Terminal Mode");
        } catch (Exception e) {
            System.err.println("Error returning to Terminal Identification:");
            e.printStackTrace();
        }
    }

    /**
     * Helper method to switch scenes using an FXML path.
     * 
     * @param event The ActionEvent triggering the switch
     * @param fxmlPath Path to the FXML file
     * @param title Title for the new stage
     */
    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // If the controller inherits from BaseTerminalController, inject the client
            Object controller = loader.getController();
            if (controller instanceof BaseTerminalController) {
                ((BaseTerminalController) controller).setClient(this.client);
            }

            switchScene(event, root, title);
        } catch (Exception e) {
            System.err.println("Error loading screen: " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Helper method to switch scenes using a loaded Parent root.
     * Handles stage setup, titles, stylesheets, and display.
     * 
     * @param event The ActionEvent triggering the switch
     * @param root The loaded Parent container for the new scene
     * @param title Title for the new stage
     */
    private void switchScene(ActionEvent event, Parent root, String title) {
        // Retrieve the current stage
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle(title);
        
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }

        // Ensure the common application stylesheet is applied
        String cssPath = getClass().getResource("/gui/styles.css").toExternalForm();
        if (!scene.getStylesheets().contains(cssPath)) {
            scene.getStylesheets().add(cssPath);
        }

        // Maximize the window for a terminal-like experience
        stage.setMaximized(true);
        stage.show();
    }
}
