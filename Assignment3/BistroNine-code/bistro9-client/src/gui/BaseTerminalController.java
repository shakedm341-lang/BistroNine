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
    protected static String currentSubscriberName = null;

    protected ClientController client;

    public void setClient(ClientController client) {
        this.client = client;
    }

    public static void setUserType(UserType type) {
        currentUserType = type;
        // Reset subscriber details if switching types or starting fresh
        if (type == UserType.GUEST) {
            currentSubscriberId = null;
            currentSubscriberName = null;
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

            stage.setMaximized(true);
            stage.show();
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

