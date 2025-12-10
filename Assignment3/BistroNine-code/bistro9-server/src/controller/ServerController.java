package controller;

import java.io.*;
import data.*; // Imported KryoUtil via 'data.*'
import ocsf.server.*;

public class ServerController extends AbstractServer {
	
	final public static int DEFAULT_PORT = 5555;

	private ReservationControler reservationsController;

	public ServerController(int port, String dbPassword) {
		super(port);
		
		// 1. Initialize the Database Connection first
		DataBaseController.initiateDBC(dbPassword);
		
		// 2. Initialize the Logic Controller
		this.reservationsController = new ReservationControler();
	}

	@Override
	public void handleMessageFromClient(Object msg, ConnectionToClient client) {
		// Strict check: We only support byte[] (Kryo)
		if (msg instanceof byte[]) {
			// Deserialize
			Message message = (Message) KryoUtil.deserialize((byte[]) msg);

			switch (message.type) {
			case RESERVATION:
				handleReservationRequest(message, client);
				break;

			default:
				System.out.println("Unknown command received: " + message.type);
				break;
			}
		} else {
			System.out.println("Server received non-byte[] message. Ignored.");
		}
	}

	// Helper method to handle reservation logic
	private void handleReservationRequest(Message message, ConnectionToClient client) {
		// Delegate logic to the specific controller
		Object response = reservationsController.handleMessageFromServer(message);
		
		// Prepare response
		message.content = response;
		
		// Send back to client
		try {
			// STRICT: Serialize to byte[] before sending back
			byte[] data = KryoUtil.serialize(message);
			client.sendToClient(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void serverStarted() {
		System.out.println("Server listening for connections on port " + getPort());
	}

	protected void serverStopped() {
		System.out.println("Server has stopped listening for connections.");
	}

	@Override
	protected void clientConnected(ConnectionToClient client) {
		super.clientConnected(client);
		System.out.println("----------------------------------------");
		System.out.println("Client connection successful!");
		String clientIp = client.getInetAddress().getHostAddress();
		System.out.println("Client IP: " + clientIp);
		System.out.println("----------------------------------------");
	}
}