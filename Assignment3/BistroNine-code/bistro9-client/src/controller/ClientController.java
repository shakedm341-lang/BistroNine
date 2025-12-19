package controller;

import ocsf.client.AbstractClient;
import java.io.*;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.ArrayList;

import data.*;
import gui.LoginController;
import gui.ReservationBoundry;
import gui.UpdateReservtionBoundry;

public class ClientController extends AbstractClient {

	// Define variables
	public static boolean awaitResponse = false;
	public static UpdateReservtionBoundry updatereservationBoundary;
	public static LoginController loginController;
	public static ReservationBoundry reservationBoundry;

	// Constructor
	public ClientController(String host, int port) throws IOException {
		super(host, port);
		openConnection();
	}

	@Override
	// Handle message from server
	protected void handleMessageFromServer(Object msg) {
		// Strict check: We only support byte[] (Kryo)
		if (msg instanceof byte[]) {
			// Deserialize the byte array back to a Message object
			Message message = (Message) KryoUtil.deserialize((byte[]) msg);

			// Define commands
			switch (message.command) {
			case GET_ALL_RESERVATIONS:
				handleGetAllReservations(message);
				break;

			case UPDATE_RESERVATION_DETAILS:
				handleUpdateReservationResponse(message);
				break;
			case CHECK_LOGIN_DETAILS:
				handleLoginResponse(message);
				break;
			case CHECK_TABLE_AVAILABILITY:
				handleTableAvailabilityResponse(message);
				break;
			case CREATE_NEW_RESERVATION:
				handleCreateReservationResponse(message);
			default:
				break;
			}

		} else

		{
			System.out.println("Client received non-byte[] message. Ignored.");
		}
	}

	private void handleTableAvailabilityResponse(Message message) {
		if (reservationBoundry != null) {
			@SuppressWarnings("unchecked")
			
	        ArrayList<LocalTime> availableTimes = (ArrayList<LocalTime>) message.content;
	        
	        reservationBoundry.updateAvailableHours(availableTimes); // Update available times in boundary
		}
	}
	private void handleCreateReservationResponse(Message message) {
		if (reservationBoundry != null) {
			Boolean success = (Boolean) message.content;
			reservationBoundry.onReservationCreationResponse(success); // Show creation message in boundary
		}
	}

	private void handleLoginResponse(Message message) {
		if (loginController != null) {
			Subscriber subscriber = (Subscriber) message.content;
			loginController.handleServerLoginResponse(subscriber); // Process login response in boundary
		}
	}

	// Helper method for handling the list of reservations
	private void handleGetAllReservations(Message message) {
		if (updatereservationBoundary != null) {
			@SuppressWarnings("unchecked")
			ArrayList<TableReservation> list = (ArrayList<TableReservation>) message.content;
			updatereservationBoundary.updateReservationTable(list); // Update table in boundary
		}
	}

	// Helper method for handling the update response
	private void handleUpdateReservationResponse(Message message) {
		if (updatereservationBoundary != null) {
			Boolean success = (Boolean) message.content;
			updatereservationBoundary.showUpdateMessage(success); // Update message in boundary
		}
	}

	// Handle message from boundary
	public void handleMessageFromBoundary(TypeMessage type, Object content, Command command) {
		Message msg = new Message();
		msg.type = type;
		msg.content = content;
		msg.command = command;

		try {
			// Ensure connection is open before sending
			if (!isConnected()) {
				openConnection();
			}

			// STRICT: Serialize to byte[] before sending
			byte[] data = KryoUtil.serialize(msg);
			sendToServer(data);

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Could not send message to server: Terminating client." + e);
			quit(); // Quit client
		}
	}

	public void quit() {
		try {
			closeConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}