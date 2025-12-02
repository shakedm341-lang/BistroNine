package serverSide2;

import java.io.*;
import java.util.ArrayList;

import ocsf.server.*;

/**
 * This class overrides some of the methods in the abstract superclass in order
 * to give more functionality to the server.
 */
public class ServerController extends AbstractServer {
	// Class variables

	/**
	 * The default port to listen on.
	 */
	final public static int DEFAULT_PORT = 5555;

	// Changed from mysqlConnection to ReservationsController as per architecture
	private ReservationControler reservationsController;

	/**
	 * Constructs an instance of the echo server.
	 *
	 * @param port The port number to connect on.
	 */
	public ServerController(int port) {
		super(port);
		// Initialize the logic controller
		this.reservationsController = new ReservationControler();
	}

	/**
	 * This method handles any messages received from the client.
	 *
	 * @param msg    The message received from the client.
	 * @param client The connection from which the message originated.
	 */
	public void handleMessageFromClient(Object msg, ConnectionToClient client) {
		if (msg instanceof Message) {
			Message message = (Message) msg;

			switch (message.type) {
			case RESERVATION:
				ArrayList<String> resList = (ArrayList<String>) reservationsController.handleMessageFromServer(message);
				message.content = resList;
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

	/**
	 * This method overrides the one in the superclass. Called when the server
	 * starts listening for connections.
	 */
	protected void serverStarted() {
		System.out.println("Server listening for connections on port " + getPort());

		// Ensure the DB connection is alive by calling getInstance
		DataBaseController.getInstance();
	}

	/**
	 * This method overrides the one in the superclass. Called when the server stops
	 * listening for connections.
	 */
	protected void serverStopped() {
		System.out.println("Server has stopped listening for connections.");
	}

	// ---------------------------------------

	// the main method left here commented on purpose
	// for debugging on console without the GUI !!!

	// ---------------------------------------

//	public static void main(String[] args) {
//		int port = 0; // Port to listen on
//
//		try {
//			port = Integer.parseInt(args[0]); // Get port from command line
//		} catch (Throwable t) {
//			port = DEFAULT_PORT; // Set port to 5555
//		}
//
//		ServerController sv = new ServerController(port);
//
//		try {
//			sv.listen(); // Start listening for connections
//		} catch (Exception ex) {
//			System.out.println("ERROR - Could not listen for clients!");
//		}
//	}
}