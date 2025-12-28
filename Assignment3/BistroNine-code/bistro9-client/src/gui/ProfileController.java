package gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.Optional;

import controller.ClientController;
import data.Command;
import data.Message;
import data.Subscriber;
import data.TypeMessage;

public class ProfileController {

	@FXML
	private TextField usernameField;
	@FXML
	private TextField emailField;
	@FXML
	private TextField phoneField;
	@FXML
	private TextField roleField;

	private String originalEmail;
	private String originalPhone;

	private Subscriber currentUser;
	private ClientController client;
	private UserDashboardController mainDashboardController;

	public void setDependencies(ClientController client, Subscriber currentUser,UserDashboardController dashboard) {
		this.client = client;
		this.currentUser = currentUser;
		this.mainDashboardController = dashboard; // Save the reference
		populateFields();
	}
	
	public void populateFields() {
		
		if(currentUser != null) {
			System.out.println("Populating fields for user: " + currentUser.getUsername() + "," + currentUser.getType() + "," + currentUser.getEmail() + "," + currentUser.getPhoneNumber());
			String currentUsername = currentUser.getUsername();
			String currentRole = currentUser.getType(); // לדוגמה: "restaurant manager"
			originalEmail = currentUser.getEmail();
			originalPhone = currentUser.getPhoneNumber();

			usernameField.setText(currentUsername);
			roleField.setText(currentRole);
			emailField.setText(originalEmail);
			phoneField.setText(originalPhone);
			
		}
		else {
			System.out.println("Current user is null. Cannot populate fields.");
		}
		
	}

	@FXML
	void handleUpdateProfile(ActionEvent event) {
		String newEmail = emailField.getText();
		String newPhone = phoneField.getText();

		boolean emailChanged = !newEmail.equals(originalEmail);
		boolean phoneChanged = !newPhone.equals(originalPhone);

		if (!emailChanged && !phoneChanged) {
			showAlert("No Changes Detected", "You haven't changed any information.", AlertType.INFORMATION);
			return;
		}

		StringBuilder changesSummary = new StringBuilder();
		changesSummary.append("Are you sure you want to update the following details?\n\n");

		if (emailChanged) {
			changesSummary.append("Email: ").append(originalEmail).append(" -> ").append(newEmail).append("\n");
		}
		if (phoneChanged) {
			changesSummary.append("Phone: ").append(originalPhone).append(" -> ").append(newPhone).append("\n");
		}

		boolean confirmed = showConfirmationDialog("Confirm Update", changesSummary.toString());

		if (confirmed) {
			sendDataToServer(newEmail, newPhone);

			
		} else {
			 emailField.setText(originalEmail);
			 phoneField.setText(originalPhone);
		}
	}
	
	public void updateProfileSuccess(boolean isSuccess) {
        javafx.application.Platform.runLater(() -> {
            if (isSuccess) {
                // 1. Update the local Subscriber object so changes are reflected across the app
                currentUser.setEmail(emailField.getText());
                currentUser.setPhoneNumber(phoneField.getText());

                // 2. Update the "original" tracking variables to the new values
                originalEmail = emailField.getText();
                originalPhone = phoneField.getText();
                
                if (mainDashboardController != null) {
                    mainDashboardController.setCurrentUser(currentUser);
                }

                // 3. Show success message to the user
                showAlert("Success", "Profile updated successfully!", AlertType.INFORMATION);
            } else {
                // Update failed: Show error and revert fields to original values
                showAlert("Update Failed", "Could not update profile. Please try again.", AlertType.ERROR);
                
                emailField.setText(originalEmail);
                phoneField.setText(originalPhone);
            }
        });
    }

	
	private boolean showConfirmationDialog(String title, String content) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(title);
		alert.setHeaderText("Update Confirmation");
		alert.setContentText(content);

		Optional<ButtonType> result = alert.showAndWait();
		return result.isPresent() && result.get() == ButtonType.OK;
	}

	
	private void showAlert(String title, String content, AlertType type) {
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}

	private void sendDataToServer(String email, String phone) {
		ArrayList<Object> updateDetails = new ArrayList<>();

		updateDetails.add(currentUser.getCustomerId()); // Index 0: Integer (Customer ID)
		updateDetails.add(phone); // Index 1: String (Phone)
		updateDetails.add(email); // Index 2: String (Email)

		if (client != null) {
			client.handleMessageFromBoundary(TypeMessage.CUSTOMER, // The broad category
												updateDetails, // The data of the subscriber
												Command.UPDATE_SUBSCRIBER_DETAILS // The specific command
			);
		} else {
			System.out.println("ClientController is not set. Cannot send data to server.");
		}

		System.out.println("Sending payload to server: " + updateDetails);

	}

	

}