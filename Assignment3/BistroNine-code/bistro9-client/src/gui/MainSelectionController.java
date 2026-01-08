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
            
            System.out.println("Entering Remote Access (Login)...");
            switchScene(event, root, "BistroNine Client - Login Screen");
            
        } catch (Exception e) {
            System.out.println("Error loading Login Screen (Ensure the FXML exists!)");
            e.printStackTrace();
        }
    }
    
    private void switchScene(ActionEvent event, Parent root, String title) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle(title);

        // By default, allow resizing unless we explicitly want a fixed window.
        // The login screen should NOT be resizable.
        if (title.contains("Login Screen")) {
            stage.setResizable(false);
        } else {
            stage.setResizable(true);
        }

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
        stage.centerOnScreen();
    }
}
