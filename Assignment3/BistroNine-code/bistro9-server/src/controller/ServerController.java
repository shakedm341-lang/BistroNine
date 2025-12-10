package controller;

import java.io.*;
import data.*; 
import ocsf.server.*;
import gui.ServerDashboardController;
import javafx.application.Platform;

public class ServerController extends AbstractServer {
	
	final public static int DEFAULT_PORT = 5555;
	private ReservationControler reservationsController;
	
	private ServerDashboardController serverUI;

	public ServerController(int port, String dbPassword, ServerDashboardController serverUI) {
		super(port);
		this.serverUI = serverUI;
		
		DataBaseController.initiateDBC(dbPassword);
		this.reservationsController = new ReservationControler();
	}

	@Override
	public void handleMessageFromClient(Object msg, ConnectionToClient client) {
		if (msg instanceof byte[]) {
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

	private void handleReservationRequest(Message message, ConnectionToClient client) {
		Object response = reservationsController.handleMessageFromServer(message);
		message.content = response;
		try {
			byte[] data = KryoUtil.serialize(message);
			client.sendToClient(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void serverStarted() {
		System.out.println("Server listening for connections on port " + getPort());
	}

	@Override
	protected void serverStopped() {
		System.out.println("Server has stopped listening for connections.");
	}

	@Override
	protected void clientConnected(ConnectionToClient client) {
		super.clientConnected(client);
		
		// 1. Get info safely while connected
		if (client.getInetAddress() != null) {
			String clientIp = client.getInetAddress().getHostAddress();
			String hostName = client.getInetAddress().getHostName();
			
			// 2. SAVE the info into the client object so we can read it later if it disconnects
			client.setInfo("IP", clientIp);
			client.setInfo("Host", hostName);
			
			System.out.println("Client Connected: " + clientIp);
	
			// 3. Update GUI
			if (serverUI != null) {
				Platform.runLater(() -> {
					serverUI.addClient(clientIp, hostName);
				});
			}
		}
	}

	@Override
	synchronized protected void clientDisconnected(ConnectionToClient client) {
		// 1. Retrieve the saved IP instead of asking the closed socket
		String clientIp = (String) client.getInfo("IP");
		
		System.out.println("Client Disconnected: " + clientIp);
		
		if (serverUI != null && clientIp != null) {
			Platform.runLater(() -> {
				serverUI.updateClientStatus(clientIp, "Disconnected");
			});
		}
	}
	
	@Override
	synchronized protected void clientException(ConnectionToClient client, Throwable exception) {
		// 1. Retrieve the saved IP
		String clientIp = (String) client.getInfo("IP");
		
		// If the connection failed before we even saved the IP, we can't update the GUI
		if (clientIp != null && serverUI != null) {
			Platform.runLater(() -> {
				serverUI.updateClientStatus(clientIp, "Aborted");
			});
		}
	}
}