package controller;

import java.io.*;
import data.Message;
import ocsf.server.*;

public class ServerController extends AbstractServer {
	
	final public static int DEFAULT_PORT = 5555;

	private ReservationControler reservationsController;

	public ServerController(int port, String dbPassword) {
		super(port);
		
		// 1. Initialize the Database Connection first using the new name
		DataBaseController.initiateDBC(dbPassword);
		
		// 2. Initialize the Logic Controller (now safe to access DB)
		this.reservationsController = new ReservationControler();
	}

	public void handleMessageFromClient(Object msg, ConnectionToClient client) {
		if (msg instanceof Message) {
			Message message = (Message) msg;

			switch (message.type) {
			case RESERVATION:
				Object respond = (Object) reservationsController.handleMessageFromServer(message);
				message.content = respond;
				try {
					client.sendToClient(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;

			default:
				System.out.println("Unknown command received.");
				break;
			}
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