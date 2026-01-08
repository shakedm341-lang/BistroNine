package gui;

import controller.ClientController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public abstract class BaseTerminalController {

    public enum UserType {
        GUEST,
        SUBSCRIBER
    }

    protected static UserType currentUserType = UserType.GUEST;
    protected static String currentSubscriberId = null;

    protected ClientController client;

    public void setClient(ClientController client) {
        this.client = client;
    }

    public static void setUserType(UserType type) {
        currentUserType = type;
        // Reset subscriber ID if switching types or starting fresh
        if (type == UserType.GUEST) {
            currentSubscriberId = null;
        }
    }

    protected void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof BaseTerminalController) {
                ((BaseTerminalController) controller).setClient(this.client);
            } else if (controller instanceof TerminalMenuController) {
                ((TerminalMenuController) controller).setClient(this.client);
            } else if (controller instanceof TerminalIdentificationController) {
                ((TerminalIdentificationController) controller).setClient(this.client);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
            stage.centerOnScreen();
        } catch (Exception e) {
            System.err.println("Error switching to scene: " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    protected void handleBack(ActionEvent event) {
        switchScene(event, "/gui/TerminalMenu.fxml", "BistroNine - Terminal Mode");
    }
}

