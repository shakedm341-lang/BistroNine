package gui;

import controller.ClientController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

/**
 * Controller class for the Main Selection Screen.
 * This screen allows the user to choose between Terminal Mode and Remote Access mode.
 */
public class MainSelectionController {

    /** The client controller used to interact with the server. */
    private ClientController client;

    /** Button to enter Terminal Mode. */
    @FXML
    private Button btnTerminalMode;

    /** Button to enter Remote Access mode. */
    @FXML
    private Button btnRemoteAccess;

    /**
     * Sets the client controller for this screen.
     * 
     * @param client The client controller to be used.
     */
    public void setClient(ClientController client) {
        this.client = client;
    }

    /**
     * Handles the ActionEvent when the Terminal Mode button is clicked.
     * Switches the scene to the Terminal Identification Screen.
     * 
     * @param event The ActionEvent triggered by clicking the button.
     */
    @FXML
    void enterTerminalMode(ActionEvent event) {
        try {
            System.out.println("Entering Terminal Mode Identification...");

            // Load the Terminal Identification FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/TerminalIdentificationScreen.fxml"));
            Parent root = loader.load();

            // Set the client controller in the new screen's controller
            TerminalIdentificationController controller = loader.getController();
            controller.setClient(this.client);

            // Switch to the identification scene
            switchScene(event, root, "BistroNine - Terminal Identification");

        } catch (Exception e) {
            System.out.println("Error loading Terminal Identification Screen:");
            e.printStackTrace();
        }
    }

    /**
     * Handles the ActionEvent when the Remote Access button is clicked.
     * Opens a new standalone window for the Login Screen in Remote mode.
     * 
     * @param event The ActionEvent triggered by clicking the button.
     */
    @FXML
    void enterRemoteAccess(ActionEvent event) {
        try {
            // Load the Login Screen FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/LoginScreen.fxml"));
            Parent root = loader.load();
            
            // Set up the Login controller with the client and Remote mode
            LoginController loginController = loader.getController();
            loginController.setClient(client);
            loginController.setMode(LoginController.Mode.REMOTE);
            
            // Create a completely new Stage for the Remote mode application
            Stage remoteStage = new Stage();
            remoteStage.setTitle("BistroNine Client - Login");

            Scene scene = new Scene(root);
            String cssPath = getClass().getResource("/gui/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            remoteStage.setScene(scene);
            remoteStage.setResizable(false);

            // Close the current (Main Selection) stage to transition to Remote mode
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

            // Show and center the new Remote mode stage
            System.out.println("Opening standalone Remote mode login window...");
            remoteStage.show();
            remoteStage.centerOnScreen();
            
        } catch (Exception e) {
            System.out.println("Error launching Remote mode window:");
            e.printStackTrace();
        }
    }

    /**
     * Handles the ActionEvent when the Back to Connection button is clicked.
     * Returns the user to the Server Connection Screen.
     * 
     * @param event The ActionEvent triggered by clicking the button.
     */
    @FXML
    void handleBackToConnection(ActionEvent event) {
        try {
            // If the client is connected, we might want to close the connection
            if (client != null) {
                // client.closeConnection(); // Assuming there's a close method if needed
            }

            // Load the Connection Screen FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ConnectToServerGui.fxml"));
            Parent root = loader.load();

            // Create a new Stage for the Connection screen
            Stage connectionStage = new Stage();
            connectionStage.setTitle("BistroNine Client - Connection to Server");
            Scene scene = new Scene(root);
            String cssPath = getClass().getResource("/gui/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            connectionStage.setScene(scene);
            connectionStage.setResizable(false);
            
            // Close the current (Main Selection) stage
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

            // Show and center the new stage
            connectionStage.show();
            connectionStage.centerOnScreen();

        } catch (Exception e) {
            System.out.println("Error returning to Connection Screen:");
            e.printStackTrace();
        }
    }
    
    /**
     * Utility method to switch the root of the current stage or create a new scene.
     * Handles layout adjustments like full screen/maximized states based on the target screen.
     * 
     * @param event The ActionEvent used to identify the current window.
     * @param root The new root Parent for the scene.
     * @param title The new title for the stage.
     */
    private void switchScene(ActionEvent event, Parent root, String title) {
        // Get the current stage from the event source
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle(title);

        Scene scene = stage.getScene();
        if (scene == null) {
            // If no scene exists, create one
            scene = new Scene(root);
            stage.setScene(scene);
        } else {
            // Otherwise, just change the root to avoid creating multiple scenes
            scene.setRoot(root);
        }

        // Ensure the common stylesheet is applied
        String cssPath = getClass().getResource("/gui/styles.css").toExternalForm();
        if (!scene.getStylesheets().contains(cssPath)) {
            scene.getStylesheets().add(cssPath);
        }

        // Apply specific layout settings based on the target screen
        if (title.contains("Terminal Identification")) {
            // Terminal Identification should be maximized
            stage.setFullScreen(false);
            stage.setMaximized(true);
        } else if (title.contains("Login Screen")) {
            // Login Screen is fixed size
            stage.setFullScreen(false);
            stage.setMaximized(false);
            stage.setResizable(false);
        } else {
            // Default settings for other screens
            stage.setFullScreen(false);
            stage.setMaximized(false);
            stage.setResizable(true);
        }

        stage.show();
        
        // Center the window if it's not maximized or full screen
        if (!stage.isFullScreen() && !stage.isMaximized()) {
            stage.centerOnScreen();
        }
    }
}
