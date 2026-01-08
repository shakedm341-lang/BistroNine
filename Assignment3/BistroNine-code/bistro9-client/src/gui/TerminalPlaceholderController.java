package gui;

import controller.ClientController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TerminalPlaceholderController {

    private ClientController client;

    public void setClient(ClientController client) {
        this.client = client;
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/TerminalMenu.fxml"));
            Parent root = loader.load();

            TerminalMenuController controller = loader.getController();
            controller.setClient(this.client);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("BistroNine - Terminal Mode");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
            stage.centerOnScreen();
        } catch (Exception e) {
            System.out.println("Error returning to Terminal Menu:");
            e.printStackTrace();
        }
    }
}

