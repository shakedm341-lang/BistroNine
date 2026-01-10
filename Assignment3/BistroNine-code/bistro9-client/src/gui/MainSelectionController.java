package gui;

import controller.ClientController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainSelectionController {

    private ClientController client;

    @FXML
    private Button btnTerminalMode;

    @FXML
    private Button btnRemoteAccess;

    public void setClient(ClientController client) {
        this.client = client;
    }

    @FXML
    void enterTerminalMode(ActionEvent event) {
        try {
            System.out.println("Entering Terminal Mode Identification...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/TerminalIdentificationScreen.fxml"));
            Parent root = loader.load();

            TerminalIdentificationController controller = loader.getController();
            controller.setClient(this.client);

            switchScene(event, root, "BistroNine - Terminal Identification");

        } catch (Exception e) {
            System.out.println("Error loading Terminal Identification Screen:");
            e.printStackTrace();
        }
    }

    @FXML
    void enterRemoteAccess(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/LoginScreen.fxml"));
            Parent root = loader.load();
            
            LoginController loginController = loader.getController();
            loginController.setClient(client);
            loginController.setMode(LoginController.Mode.REMOTE);
            
            // Create a completely new Stage for the Remote Mode application
            Stage remoteStage = new Stage();
            remoteStage.setTitle("BistroNine Client - Login");

            Scene scene = new Scene(root);
            String cssPath = getClass().getResource("/gui/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            remoteStage.setScene(scene);
            remoteStage.setResizable(false);

            // Close the current (Main Selection) stage
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

            // Show and center the new remote stage
            System.out.println("Opening standalone Remote Login window...");
            remoteStage.show();
            remoteStage.centerOnScreen();
            
        } catch (Exception e) {
            System.out.println("Error launching Remote Mode window:");
            e.printStackTrace();
        }
    }

    @FXML
    void handleBackToConnection(ActionEvent event) {
        try {
            // If the client is connected, we might want to close the connection
            if (client != null) {
                // client.closeConnection(); // Assuming there's a close method if needed
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ConnectToServerGui.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("BistroNine Client - Connection to Server");
            Scene scene = new Scene(root);
            String cssPath = getClass().getResource("/gui/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            stage.setScene(scene);
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.show();
            stage.centerOnScreen();

        } catch (Exception e) {
            System.out.println("Error returning to Connection Screen:");
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

        // Terminal Identification should be maximized
        if (title.contains("Terminal Identification")) {
            stage.setFullScreen(false);
            stage.setMaximized(true);
        } else if (title.contains("Login Screen")) {
            stage.setFullScreen(false);
            stage.setMaximized(false);
            stage.setResizable(false);
        } else {
            stage.setFullScreen(false);
            stage.setMaximized(false);
            stage.setResizable(true);
        }

        stage.show();
        if (!stage.isFullScreen() && !stage.isMaximized()) {
            stage.centerOnScreen();
        }
    }
}
