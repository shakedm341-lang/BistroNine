package controller;

import java.util.ArrayList;

import data.Message;
import data.Subscriber;
import data.TableReservation;

public class CustomerController 
{
	private DataBaseController DBC=DataBaseController.getInstance();//Interfacing with the DB Controller
	
	/**
	 * Default constructor
	 */
	public CustomerController() 
	{
		
	}

	/**
	 * Handles messages received from the server and performs corresponding actions.
	 *
	 * @param msg The message received from the server.
	 * @return The result of the action performed, which can vary based on the
	 *         command.
	 */
	public Object handleMessageFromServer(Message msg) 
	{
		
		switch (msg.command) //Checking the type of message sent from the server (what action should be performed in the DB Controller)
		{
	    case CHECK_LOGIN_DETAILS:
	    	return checkLoginDetails(msg);
	    	
	    default:
	        System.out.println("Unknown task received.");
	        return null;
		}
	}

	/**
	 * Checks the login details of a subscriber.
	 *
	 * @param msg The message containing the username and password.
	 * @return The Subscriber object if login is successful, null otherwise.
	 */
	private Subscriber checkLoginDetails(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get username and password from the message
		
		ArrayList<Object> res = new ArrayList<>();//To store the result from the DB Controller(null or list of subscriber details)
		Subscriber sub = new Subscriber();
		
		
		//Set username  in the Subscriber object
		if (list.get(0) instanceof String) {
			sub.setUsername((String) list.get(0));
    	} else {
    	    System.out.println("Error: Index 0 is not a String!");
    	    return null;
    	}
		//Set  password in the Subscriber object
		if (list.get(1) instanceof String) {
			sub.setPassword((String) list.get(1));
    	} else {
    	    System.out.println("Error: Index 1 is not a String!");
    	    return null;
    	}
    	
    	return DBC.checkLoginDetails(sub);//Check login details in the DB and get subscriber details if valid, return null if invalid or list of object
    	
    	//Return to server the subscriber details as Subscriber object
    	}
    	
	}

