package client;

import ocsf.client.AbstractClient;
import common.*;
import java.io.*;
import java.util.ArrayList;


/**
 * This class serves as the Client Controller.
 * It extends the OCSF AbstractClient class and is responsible for managing
 * the communication between the user interface (Boundaries) and the Server.
 * It handles sending requests to the server and processing responses.
 */

public class ClientController extends AbstractClient { 
	
	/**
     * Singleton instance of the controller.
     * This static variable allows any Boundary in the system to access 
     * the ClientController instance and send messages to the server easily.
     */
	public static ClientController instance;
	
	/**
     * Constructs an instance of the ClientConsole.
     * Initializes the connection to the server and saves the current instance.
     *
     * @param host The host IP to connect.
     * @param port The port number on which the server is listening for connections.
     * @throws IOException If an I/O error occurs when opening the connection.
     */
	
	public ClientController(String host, int port) throws IOException {
        super(host, port); 
        instance = this;   
        openConnection(); 
    }
	
	/**
     * This method handles any message received from the server.
     * It is called automatically by OCSF when data arrives.
     *
     * @param msg The message received from the server.
     */
	
    protected void handleMessageFromServer(Object msg) {
        System.out.println("I get message: " + msg.toString());
    }
	
    /**
     * This method accepts data from the user interface boundary, packages it into a 
     * Message object, and sends it to the server.
     *
     * @param type    The message category as defined in the TypeMessage enum.
     * @param command The specific string identifier for the requested operation.
     * @param content The list of data objects containing the payload to be sent.
     */
    
	public void handleMessageFromBoundary(TypeMessage type, String command, ArrayList content) {        
        Message msg = new Message(type, command, content);
        
        try
        {
            sendToServer(msg);
        } 
        catch (IOException e)
        {
            System.out.println("Error sending message to server.");
            e.printStackTrace();
        }
    }
	
	
	
//	public static void main(String[] args) {
//	    try {
//	        // 1. ננסה ליצור את הקונטרולר (זה ינסה להתחבר לשרת)
//	        System.out.println("בודק: מנסה ליצור ClientController...");
//	        ClientController client = new ClientController("localhost", 5555);
//	        
//	        // 2. ננסה לשלוח הודעת ניסיון
//	        System.out.println("בודק: מנסה לשלוח הודעה...");
//	        java.util.ArrayList<Object> data = new java.util.ArrayList<>();
//	        data.add("בדיקה");
//	        
//	        // שימי לב: השתמשי ב-TypeMessage שיצרת אצלך
//	        client.handleMessageFromBoundary(common.TypeMessage.reservation, "CHECK_TEST", data);
//	        
//	        System.out.println("בודק: הפקודה עברה ללא קריסה מיידית!");
//	        
//	    } catch (java.io.IOException e) {
//	        System.out.println("הצלחה חלקית: הקוד רץ, אבל אין שרת מחובר (וזה הגיוני).");
//	        System.out.println("השגיאה שקיבלנו: " + e.getMessage());
//	    } catch (Exception e) {
//	        System.out.println("נכשל: יש שגיאה בקוד: " + e.getMessage());
//	        e.printStackTrace();
//	    }
//	}
}
