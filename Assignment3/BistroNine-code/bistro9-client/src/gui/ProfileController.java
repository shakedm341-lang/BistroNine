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

/**
 * Controller class for the User Profile screen.
 * This class handles displaying and updating user information such as email and phone number.
 * It coordinates with the ClientController to communicate changes to the server.
 */
public class ProfileController {

	@FXML
	private TextField usernameField;
	@FXML
	private TextField emailField;
	@FXML
	private TextField phoneField;
	@FXML
	private TextField roleField;

	/** Original email value to track changes and for reversion if update fails */
	private String originalEmail;
	/** Original phone value to track changes and for reversion if update fails */
	private String originalPhone;

	private Subscriber currentUser;
	private ClientController client;
	private UserDashboardController mainDashboardController;

	/**
	 * Sets the necessary dependencies for the controller and initializes the view.
	 * 
	 * @param client The ClientController used for server communication.
	 * @param currentUser The currently logged-in Subscriber whose profile is being viewed.
	 * @param dashboard The main dashboard controller to allow state updates across the UI.
	 */
	public void setDependencies(ClientController client, Subscriber currentUser,UserDashboardController dashboard) {
		this.client = client;
		this.currentUser = currentUser;
		this.mainDashboardController = dashboard; // Save the reference
		ClientController.profileController = this;
		populateFields();
	}
	
	/**
	 * Populates the UI text fields with the current user's data.
	 */
	public void populateFields() {
		
		if(currentUser != null) {
			System.out.println("Populating fields for user: " + currentUser.getUsername() + "," + currentUser.getType() + "," + currentUser.getEmail() + "," + currentUser.getPhoneNumber());
			String currentUsername = currentUser.getUsername();
			String currentRole = currentUser.getType(); // e.g., "restaurant manager"
			originalEmail = currentUser.getEmail();
			originalPhone = currentUser.getPhoneNumber();

			// Set initial values in text fields
			usernameField.setText(currentUsername);
			roleField.setText(currentRole);
			emailField.setText(originalEmail);
			phoneField.setText(originalPhone);
			
		}
		else {
			System.out.println("Current user is null. Cannot populate fields.");
		}
		
	}

	/**
	 * Event handler for the "Update Profile" button.
	 * Performs validation on input fields and prompts for confirmation if changes are detected.
	 * 
	 * @param event The action event triggered by the button click.
	 */
	@FXML
	void handleUpdateProfile(ActionEvent event) {
		String newEmail = emailField.getText().trim();
		String newPhone = phoneField.getText().trim();

		// --- Validation Section ---
		
		// Validate that email is not empty
		if (newEmail.isEmpty()) {
			showAlert("Validation Error", "Email field cannot be empty. Please enter a valid email address.", AlertType.ERROR);
			return;
		}

		// Validate that phone is not empty
		if (newPhone.isEmpty()) {
			showAlert("Validation Error", "Phone field cannot be empty. Please enter a valid phone number.", AlertType.ERROR);
			return;
		}

		// Validate email format
		if (!isValidEmail(newEmail)) {
			showAlert("Invalid Email", "Email must contain '@' and a domain with a dot (e.g., .com, .org, .net).", AlertType.ERROR);
			return;
		}

		// Validate phone format
		if (!isValidPhone(newPhone)) {
			showAlert("Invalid Phone", "Phone number must be exactly 10 digits.", AlertType.ERROR);
			return;
		}

		// Check if any changes were actually made
		boolean emailChanged = !newEmail.equals(originalEmail);
		boolean phoneChanged = !newPhone.equals(originalPhone);

		if (!emailChanged && !phoneChanged) {
			showAlert("No Changes Detected", "You haven't changed any information.", AlertType.INFORMATION);
			return;
		}

		// --- Confirmation Section ---
		
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
			// Proceed to send updates to server
			sendDataToServer(newEmail, newPhone);
		} else {
			// Revert fields if user cancelled
			 emailField.setText(originalEmail);
			 phoneField.setText(originalPhone);
		}
	}
	
	/**
	 * Callback method called by ClientController after a profile update attempt.
	 * Updates local state and UI based on server response.
	 * 
	 * @param isSuccess True if the server successfully updated the record, false otherwise.
	 */
	public void updateProfileSuccess(boolean isSuccess) {
		// Ensure UI updates happen on the JavaFX Application Thread
        javafx.application.Platform.runLater(() -> {
            if (isSuccess) {
                // 1. Update the local Subscriber object so changes are reflected across the app
                currentUser.setEmail(emailField.getText());
                currentUser.setPhoneNumber(phoneField.getText());

                // 2. Update the "original" tracking variables to the new values
                originalEmail = emailField.getText();
                originalPhone = phoneField.getText();
                
                // 3. Notify the main dashboard of the user data change
                if (mainDashboardController != null) {
                    mainDashboardController.setCurrentUser(currentUser);
                }

                // 4. Show success message to the user
                showAlert("Success", "Profile updated successfully!", AlertType.INFORMATION);
            } else {
                // Update failed: Show error and revert fields to original values
                showAlert("Update Failed", "Could not update profile. Please try again.", AlertType.ERROR);
                
                emailField.setText(originalEmail);
                phoneField.setText(originalPhone);
            }
        });
    }

	/**
	 * Utility to show a confirmation dialog with OK/Cancel options.
	 * 
	 * @param title The title of the dialog window.
	 * @param content The message to display.
	 * @return true if OK was pressed, false otherwise.
	 */
	private boolean showConfirmationDialog(String title, String content) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(title);
		alert.setHeaderText("Update Confirmation");
		alert.setContentText(content);

		Optional<ButtonType> result = alert.showAndWait();
		return result.isPresent() && result.get() == ButtonType.OK;
	}

	/**
	 * Utility to show an alert dialog.
	 * 
	 * @param title The title of the alert window.
	 * @param content The message to display.
	 * @param type The AlertType (ERROR, INFORMATION, etc.)
	 */
	private void showAlert(String title, String content, AlertType type) {
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}

	/**
	 * Packages the profile data and sends it to the server via ClientController.
	 * 
	 * @param email The new email to save.
	 * @param phone The new phone number to save.
	 */
	private void sendDataToServer(String email, String phone) {
		ArrayList<Object> updateDetails = new ArrayList<>();

		// Prepare payload: [CustomerID, Phone, Email]
		updateDetails.add(currentUser.getCustomerId()); // Index 0: Integer (Customer ID)
		updateDetails.add(phone); // Index 1: String (Phone)
		updateDetails.add(email); // Index 2: String (Email)

		if (client != null) {
			// Send the message through the client boundary
			client.handleMessageFromBoundary(TypeMessage.CUSTOMER, // The broad category
												updateDetails, // The data of the subscriber
												Command.UPDATE_SUBSCRIBER_DETAILS // The specific command
			);
		} else {
			System.out.println("ClientController is not set. Cannot send data to server.");
		}

		System.out.println("Sending payload to server: " + updateDetails);

	}

	/**
	 * Validates email format using a simple regex: must contain "@" and a dot followed by domain.
	 * 
	 * @param email The email string to validate.
	 * @return true if email is valid, false otherwise.
	 */
	private boolean isValidEmail(String email) {
		if (email == null || email.isEmpty()) {
			return false;
		}
		// Check if email contains "@" and has a dot followed by at least one character
		return email.contains("@") && email.matches(".*@.*\\.[^.]+");
	}

	/**
	 * Validates phone format: must be exactly 10 numeric digits.
	 * 
	 * @param phone The phone string to validate.
	 * @return true if phone is valid (exactly 10 digits), false otherwise.
	 */
	private boolean isValidPhone(String phone) {
		if (phone == null || phone.isEmpty()) {
			return false;
		}
		// Remove any non-numeric characters (spaces, dashes, etc.)
		String digitsOnly = phone.replaceAll("[^0-9]", "");
		// Check if it's exactly 10 digits
		return digitsOnly.length() == 10;
	}

}