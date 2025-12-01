package controller;

import ocsf.client.AbstractClient;
import java.io.*;

import data.Command;
import data.Message;
import data.TypeMessage;

public class ClientController extends AbstractClient {

	public ClientController(String host, int port) throws IOException {
		super(host, port);
		openConnection();
	}

	protected void handleMessageFromServer(Object msg) {
		System.out.println("I get message: " + msg.toString());
	}

	public void handleMessageFromBoundary(TypeMessage type, Object content, Command command) {
		Message msg = new Message();
		msg.type = type;
		msg.content = content;
		msg.command = command;

		try {
			sendToServer(msg);
		} catch (IOException e) {
			System.out.println("Error sending message to server.");
			e.printStackTrace();
		}
	}
}