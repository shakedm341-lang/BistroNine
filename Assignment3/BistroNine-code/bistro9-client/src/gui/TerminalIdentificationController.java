package gui;

import controller.ClientController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controller for the Terminal Identification screen.
 * This screen is the entry point for the restaurant terminal, allowing users to 
 * identify themselves as subscribers or proceed as guests.
 */
public class TerminalIdentificationController {

    /**
     * The client controller instance for communication with the server.
     */
    private ClientController client;

    /**
     * Sets the client controller for this view.
     * 
     * @param client The ClientController instance.
     */
    public void setClient(ClientController client) {
        this.client = client;
    }

    /**
     * Event handler for the "Subscriber" button.
     * Opens the login screen as a modal popup to allow the user to authenticate.
     * If login is successful, transitions to the terminal menu.
     * 
     * @param event The action event triggered by the button.
     */
    @FXML
    void handleSubscriber(ActionEvent event) {
        try {
            // Initialize the Login Screen loader
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/LoginScreen.fxml"));
            Parent root = loader.load();

            // Configure the Login Controller for terminal identification mode
            LoginController loginController = loader.getController();
            loginController.setClient(this.client);
            loginController.setMode(LoginController.Mode.TERMINAL);

            // Setup a new stage for the modal popup
            Stage popupStage = new Stage();
            popupStage.setTitle("BistroNine - Subscriber Login");

            // Prevent interaction with the main window while login is open
            popupStage.initModality(Modality.WINDOW_MODAL);

            // Associate the popup with the current window as its owner
            Stage owner = (Stage) ((Node) event.getSource()).getScene().getWindow();
            popupStage.initOwner(owner);

            // Apply styles and scene
            Scene scene = new Scene(root);
            String cssPath = getClass().getResource("/gui/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            popupStage.setScene(scene);

            // Block execution until the login popup is closed
            popupStage.showAndWait();

            // After the popup closes, check if the user is now logged in as a subscriber
            if (BaseTerminalController.currentUserType == BaseTerminalController.UserType.SUBSCRIBER) {
                // If logged in, proceed to switch the original parent window to the Terminal Menu
                goToTerminalMenu(event);
            }
        } catch (Exception e) {
            System.err.println("Error loading Login Screen for Terminal Mode:");
            e.printStackTrace();
        }
    }

    /**
     * Event handler for the "Guest" button.
     * Sets the session user type to GUEST and proceeds to the terminal menu.
     * 
     * @param event The action event triggered by the button.
     */
    @FXML
    void handleGuest(ActionEvent event) {
        // Explicitly set the static user type in the base controller
        BaseTerminalController.setUserType(BaseTerminalController.UserType.GUEST);
        goToTerminalMenu(event);
    }

    /**
     * Event handler for the "Back" button.
     * Returns the user to the initial Main Selection screen.
     * 
     * @param event The action event triggered by the button.
     */
    @FXML
    void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MainSelection.fxml"));
            Parent root = loader.load();

            MainSelectionController controller = loader.getController();
            controller.setClient(this.client);

            switchScene(event, root, "BistroNine - Select Mode");
        } catch (Exception e) {
            System.err.println("Error returning to Main Selection:");
            e.printStackTrace();
        }
    }

    /**
     * Transitions the application view to the Terminal Menu.
     * 
     * @param event The action event that triggered the transition.
     */
    private void goToTerminalMenu(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/TerminalMenu.fxml"));
            Parent root = loader.load();

            TerminalMenuController controller = loader.getController();
            controller.setClient(this.client);

            switchScene(event, root, "BistroNine - Terminal Mode");
        } catch (Exception e) {
            System.err.println("Error loading Terminal Menu:");
            e.printStackTrace();
        }
    }

    /**
     * Helper method to switch scenes within the current stage.
     * Handles window state (maximized/resizable) based on the target scene.
     * 
     * @param event The triggering event to extract the current stage.
     * @param root  The root layout of the new scene.
     * @param title The title for the stage.
     */
    private void switchScene(ActionEvent event, Parent root, String title) {
        // Identify the current stage from the event source
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle(title);
        
        // Update the root of the existing scene or create a new one if necessary
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }

        // Maintain global styling across scenes
        String cssPath = getClass().getResource("/gui/styles.css").toExternalForm();
        if (!scene.getStylesheets().contains(cssPath)) {
            scene.getStylesheets().add(cssPath);
        }

        // Logic for window sizing based on context:
        // Main selection and Login are windowed; actual terminal functions are maximized.
        if (title.contains("Login") || title.contains("Main Selection") || title.contains("Select Mode")) {
            stage.setFullScreen(false);
            stage.setMaximized(false);
            stage.setResizable(true);
        } else {
            // This covers Terminal Menu and Terminal Identification
            stage.setFullScreen(false);
            stage.setMaximized(true);
        }

        stage.show();
        
        // Ensure non-maximized windows appear centered
        if (!stage.isFullScreen() && !stage.isMaximized()) {
            stage.centerOnScreen();
        }
    }
}
