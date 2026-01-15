package gui;

import controller.Init_All;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;

public class ServerInitController {

    @FXML
    private PasswordField txtDbPassword;

    @FXML
    private Button btnInitialize;

    @FXML
    private Button btnSkip;

    @FXML
    private void initialize() {
        // Optional: setup logic if needed
    }

    @FXML
    private void onInitializeClicked() {
        String password = txtDbPassword.getText();

        if (password == null || password.trim().isEmpty()) {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText(null);
            alert.setContentText("There is nothing written in the password field.");
            alert.showAndWait();
            return;
        }

        // Call the static method in Init_All
        boolean success = Init_All.runInitialization(password);
        
        if (success) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Database initialized successfully.");
            alert.showAndWait();
            // Move to next screen
            ServerMain.showServerPortScreen();
        } else {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Initialization Failed");
            alert.setContentText("Could not connect to DB with provided password. Check console.");
            alert.showAndWait();
        }
    }

    @FXML
    private void onSkipClicked() {
        // Just move to the next screen without doing anything
        ServerMain.showServerPortScreen();
    }
}