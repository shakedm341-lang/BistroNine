package controller;

import ocsf.client.AbstractClient;
import java.io.*;
import java.util.ArrayList;

import data.*;
import gui.UpdateReservtionBoundry;

public class ClientController extends AbstractClient {
	
	// Define variables
	public static boolean awaitResponse = false;
	public static UpdateReservtionBoundry reservationBoundary;
	
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
				
			default:
				break;
			}
		} else {
			System.out.println("Client received non-byte[] message. Ignored.");
		}
	}
	
	// Helper method for handling the list of reservations
	private void handleGetAllReservations(Message message) {
		if (reservationBoundary != null) {
			@SuppressWarnings("unchecked")
			ArrayList<TableReservation> list = (ArrayList<TableReservation>) message.content;
			reservationBoundary.updateReservationTable(list); // Update table in boundary
		}
	}

	// Helper method for handling the update response
	private void handleUpdateReservationResponse(Message message) {
		if (reservationBoundary != null) {
			Boolean success = (Boolean) message.content;
			reservationBoundary.showUpdateMessage(success); // Update message in boundary
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