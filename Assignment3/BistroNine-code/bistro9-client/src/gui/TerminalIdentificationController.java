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

public class TerminalIdentificationController {

    private ClientController client;

    public void setClient(ClientController client) {
        this.client = client;
    }

    @FXML
    void handleSubscriber(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/LoginScreen.fxml"));
            Parent root = loader.load();

            LoginController loginController = loader.getController();
            loginController.setClient(this.client);
            loginController.setMode(LoginController.Mode.TERMINAL);

            // Create a new Stage for the Login Screen popup
            Stage popupStage = new Stage();
            popupStage.setTitle("BistroNine - Subscriber Login");

            // Set modality to block interaction with the owner window
            popupStage.initModality(Modality.WINDOW_MODAL);

            // Set the owner of the popup to be the current stage
            Stage owner = (Stage) ((Node) event.getSource()).getScene().getWindow();
            popupStage.initOwner(owner);

            // Create scene and load styling
            Scene scene = new Scene(root);
            String cssPath = getClass().getResource("/gui/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            popupStage.setScene(scene);

            // Show the popup and wait for it to close
            popupStage.showAndWait();

            // After the popup closes, check if the user is now logged in as a subscriber
            if (BaseTerminalController.currentUserType == BaseTerminalController.UserType.SUBSCRIBER) {
                // If logged in, proceed to switch the original parent window to the Terminal Menu
                goToTerminalMenu(event);
            }
        } catch (Exception e) {
            System.out.println("Error loading Login Screen for Terminal Mode:");
            e.printStackTrace();
        }
    }

    @FXML
    void handleGuest(ActionEvent event) {
        BaseTerminalController.setUserType(BaseTerminalController.UserType.GUEST);
        goToTerminalMenu(event);
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MainSelection.fxml"));
            Parent root = loader.load();

            MainSelectionController controller = loader.getController();
            controller.setClient(this.client);

            switchScene(event, root, "BistroNine - Select Mode");
        } catch (Exception e) {
            System.out.println("Error returning to Main Selection:");
            e.printStackTrace();
        }
    }

    private void goToTerminalMenu(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/TerminalMenu.fxml"));
            Parent root = loader.load();

            TerminalMenuController controller = loader.getController();
            controller.setClient(this.client);

            switchScene(event, root, "BistroNine - Terminal Mode");
        } catch (Exception e) {
            System.out.println("Error loading Terminal Menu:");
            e.printStackTrace();
        }
    }

    private void switchScene(ActionEvent event, Parent root, String title) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle(title);
        
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }

        // Ensure stylesheet is present
        String cssPath = getClass().getResource("/gui/styles.css").toExternalForm();
        if (!scene.getStylesheets().contains(cssPath)) {
            scene.getStylesheets().add(cssPath);
        }

        // In Terminal Mode, all screens except Login should be maximized.
        if (title.contains("Login")) {
            stage.setFullScreen(false);
            stage.setMaximized(false);
            stage.setResizable(true);
        } else if (title.contains("Main Selection") || title.contains("Select Mode")) {
            stage.setFullScreen(false);
            stage.setMaximized(false);
            stage.setResizable(true);
        } else {
            // This covers Terminal Menu and Terminal Identification
            stage.setFullScreen(false);
            stage.setMaximized(true);
        }

        stage.show();
        if (!stage.isFullScreen() && !stage.isMaximized()) {
            stage.centerOnScreen();
        }
    }
}

