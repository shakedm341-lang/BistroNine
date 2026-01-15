package gui;

import controller.ClientController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Abstract base class for all terminal-related GUI controllers in the BistroNine application.
 * Provides common functionality for scene switching, client management, and user session state.
 */
public abstract class BaseTerminalController {

    /**
     * Enumeration representing the identification status of the user at the terminal.
     */
    public enum UserType {
        /** Unidentified user. */
        GUEST,
        /** Identified subscriber. */
        SUBSCRIBER
    }

    /** The type of user currently interacting with the terminal. */
    protected static UserType currentUserType = UserType.GUEST;
    /** The ID of the current subscriber, if logged in. */
    protected static String currentSubscriberId = null;
    /** The name of the current subscriber, if logged in. */
    protected static String currentSubscriberName = null;

    /** The shared client controller instance for server communication. */
    protected ClientController client;

    /**
     * Sets the client controller for this instance.
     * 
     * @param client The {@link ClientController} to use for server requests.
     */
    public void setClient(ClientController client) {
        this.client = client;
    }

    /**
     * Sets the current user type and clears subscriber details if transitioning to GUEST.
     * 
     * @param type The {@link UserType} to set for the current session.
     */
    public static void setUserType(UserType type) {
        currentUserType = type;
        // Reset subscriber details if switching types or starting fresh
        if (type == UserType.GUEST) {
            currentSubscriberId = null;
            currentSubscriberName = null;
        }
    }

    /**
     * Utility method to switch between different FXML scenes while maintaining the client connection.
     * 
     * @param event The ActionEvent that triggered the switch (used to find the current window).
     * @param fxmlPath The resource path to the target FXML file.
     * @param title The title to display on the window stage.
     */
    protected void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Handle dependency injection for the next controller
            Object controller = loader.getController();
            if (controller instanceof BaseTerminalController) {
                ((BaseTerminalController) controller).setClient(this.client);
            } else if (controller instanceof TerminalMenuController) {
                ((TerminalMenuController) controller).setClient(this.client);
            } else if (controller instanceof TerminalIdentificationController) {
                ((TerminalIdentificationController) controller).setClient(this.client);
            }

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

            // Ensure the global stylesheet is consistently applied
            String cssPath = getClass().getResource("/gui/styles.css").toExternalForm();
            if (!scene.getStylesheets().contains(cssPath)) {
                scene.getStylesheets().add(cssPath);
            }

            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            System.err.println("Error switching to scene: " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Common event handler for "Back" buttons to return to the main terminal menu.
     * 
     * @param event The ActionEvent from the back button.
     */
    @FXML
    protected void handleBack(ActionEvent event) {
        switchScene(event, "/gui/TerminalMenu.fxml", "BistroNine - Terminal Mode");
    }
}

