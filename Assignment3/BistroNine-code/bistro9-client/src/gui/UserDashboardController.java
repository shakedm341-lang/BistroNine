package gui;


import controller.ClientController;
import data.Subscriber;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class UserDashboardController {

    @FXML private Label lblWelcome;
    @FXML private StackPane contentArea;

    
    @FXML private Button btnManageTables;
    @FXML private Button btnRegisterClient;
    @FXML private Button btnViewReports;

    private ClientController client;
    private Subscriber currentUser;

    public void setClient(ClientController client) {
        this.client = client;
    }

    public void loadUserDetails(Subscriber user) {
        this.currentUser = user;
        lblWelcome.setText("Hello, " + user.getUsername());

        String type = user.getType(); 
        
        
        setButtonVisible(btnManageTables, false);
        setButtonVisible(btnRegisterClient, false);
        setButtonVisible(btnViewReports, false);
        
        
        if (type.equals("restaurant manager")) {
            setButtonVisible(btnManageTables, true);
            setButtonVisible(btnRegisterClient, true);
            setButtonVisible(btnViewReports, true);
        } 
        else if (type.equals("restaurant representative")) { 
            setButtonVisible(btnManageTables, true);
            setButtonVisible(btnRegisterClient, true);
        }
    }

    private void setButtonVisible(Button btn, boolean isVisible) {
        if (btn != null) {
            btn.setVisible(isVisible);
            btn.setManaged(isVisible);
        }
    }

    // --- Action Methods (Empty Stubs for now) ---

    @FXML
    void goToHome(ActionEvent event) {
        System.out.println("DEBUG: Go to Home / Profile");
        // TODO: Load Profile screen
    }

    @FXML
    void goToMyReservations(ActionEvent event) {
        System.out.println("DEBUG: Go to My Reservations");
        // TODO: Load My Reservations screen
    }

    @FXML
    void openNewReservation(ActionEvent event) {
       
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/NewReservation.fxml"));
            Parent root = loader.load();
            
            ReservationBoundry resController = loader.getController();
            resController.setClient(this.client);
            resController.initData(currentUser);
            
            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goToManageTables(ActionEvent event) {
        System.out.println("DEBUG: Go to Manage Tables");
        // TODO: Load Table Management screen
    }

    @FXML
    void goToRegisterClient(ActionEvent event) {
        System.out.println("DEBUG: Go to Register Client");
        // TODO: Load Register Client screen
    }

    @FXML
    void goToViewReports(ActionEvent event) {
        System.out.println("DEBUG: Go to View Reports");
        // TODO: Load Reports screen
    }

    @FXML
    void doLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MainSelection.fxml"));
            Parent root = loader.load();
            MainSelectionController controller = loader.getController();
            controller.setClient(client);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("BistroNine - Select Mode");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}