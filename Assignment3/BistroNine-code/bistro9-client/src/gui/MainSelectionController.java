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
        
            
            //System.out.println("Entering Terminal Mode...");
            //for test, we load the updateReservationBoundry directly
            
            try {
                System.out.println("Entering Terminal Mode (Test: Update Reservation)...");

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ReservationGui.fxml"));
                Parent root = loader.load();

                UpdateReservtionBoundry controller = loader.getController();

                controller.setClient(this.client);

                switchScene(event, root, "BistroNine Client - Update Reservation");

            } catch (Exception e) {
                System.out.println("Error loading Update Reservation Screen:");
                e.printStackTrace();
            }
			
            // TODO: Load TerminalMenu.fxml
            /* FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/TerminalMenu.fxml"));
            Parent root = loader.load();
            TerminalMenuController controller = loader.getController();
            controller.setClient(client);
            switchScene(event, root);
            */
            
        
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
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}