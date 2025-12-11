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
	 * @param msg The message containing the login details (username and password).
	 * @return The Subscriber object if the login details are valid; null otherwise.
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
    	
    	res = DBC.checkLoginDetails(sub);//Check login details in the DB and get subscriber details if valid, return null if invalid or list of object
    	
    	if(res==null)
    	{
    		return null;//Return to server  null if login details are invalid
    	}
    	else
    	{
    		//Set subscriber details in the Subscriber object
    		if (res.get(0) instanceof Integer) 
    		{
    			sub.setId((Integer) res.get(0));
    			
            } else {
                System.out.println("Error: Index 0  is not an Integer! " );
                return null; 
            }
    		//Set subscriber details in the Subscriber object
			if (res.get(1) instanceof String) {
				sub.setfirstName((String) res.get(1));
			} else {
				System.out.println("Error: Index 1  is not a String!");
				return null;
			}
			//Set subscriber details in the Subscriber object
			if (res.get(2) instanceof String) {
				sub.setlastName((String) res.get(2));
			} else {
				System.out.println("Error: Index 2  is not a String!");
				return null;
			}
			//Set subscriber details in the Subscriber object
			if (res.get(3) instanceof String) {
				sub.setphoneNumber((String) res.get(3));
			} else {
				System.out.println("Error: Index 3  is not a String!");
				return null;
			}
			//Set subscriber details in the Subscriber object
			if (res.get(4) instanceof String) {
				sub.setEmail((String) res.get(4));
			} else {
				System.out.println("Error: Index 4  is not a String!");
				return null;
			}
			//Set subscriber details in the Subscriber object
			if (res.get(5) instanceof String) {
				sub.setType((String) res.get(5));
			} else {
				System.out.println("Error: Index 5  is not a String!");
				return null;
			}
			//Set subscriber details in the Subscriber object
			if (res.get(6) instanceof Integer) {
				sub.setsubscriberId((Integer) res.get(6));

			} else {
				System.out.println("Error: Index 6  is not an Integer! ");
				return null;
			}
			//Set subscriber details in the Subscriber object
			if (res.get(7) instanceof String) {
				sub.setPersonalInfo((String) res.get(7));
			} else {
				System.out.println("Error: Index 7  is not a String!");
				return null;
			}
			//Set subscriber details in the Subscriber object
			if (res.get(8) instanceof String) {
				sub.setUsername((String) res.get(8));
			} else {
				System.out.println("Error: Index 8  is not a String!");
				return null;
			}
			//Set subscriber details in the Subscriber object
			if (res.get(9) instanceof String) {
				sub.setPassword((String) res.get(9));
			} else {
				System.out.println("Error: Index 9  is not a String!");
				return null;
			}
			
			return sub;//Return to server the subscriber details as Subscriber object
    	}
    	
	}
}
