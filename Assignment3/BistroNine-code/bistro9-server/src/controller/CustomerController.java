package controller;

import java.sql.Timestamp;
import java.time.LocalTime;
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
	    	
	    case ADD_NEW_SUBSCRIBER:
	    	return addNewSubscriber(msg);
	    	
	    	case UPDATE_SUBSCRIBER_DETAILS:
	    		return updateSubscriberDetails(msg);
	    		
		case GET_ALL_SUBSCRIBERS:
			return getAllSubscribers(msg);
			
	    default:
	        System.out.println("Unknown task received.");
	        return null;
		}
	}
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////Logic methods//////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Checks the login details of a subscriber.
	 * 
	 * @param msg The message containing the login details. The content of the
	 *            message is expected to be an ArrayList<Object> with the following
	 *            order: [Location 0 :username (String),Location 1:password
	 *            (String)]
	 * @return Subscriber object if login details are valid, null otherwise.
	 */
	private Subscriber checkLoginDetails(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get username and password from the message
		
		
		Subscriber sub = new Subscriber();
		
		
		//Set user name  in the Subscriber object
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
    	
    	return DBC.checkLoginDetails(sub);//Check login details in the DB and get subscriber details if valid, return null if invalid 
    	
    	//Return to server the subscriber details as Subscriber object
    	}
    	
	/**
	 * Adds a new subscriber to the database.
	 * @param msg The message containing the subscriber details. 
	 * 		The content of the message is expected to be an ArrayList<Object> with the following order:
	 * 		[Location 0 :firstName (String),Location 1:lastName (String),Location 2:type (String),
	 * 		Location 3:personalInfo (String),Location 4:username (String),Location 5:password (String),
	 * 		Location 6:phoneNumber (String),Location 7:email (String)]
	 * @return Subscriber object for the subscriber id assigned by the database.
	 */
	private Subscriber addNewSubscriber(Message msg)
	{
	
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the reservation details from the message content
    	
		Subscriber sub = new Subscriber();
		
		//subscriberId give by DB auto increment
		
		// Setting first Name from the list we got from the message content
    		if (list.get(0) instanceof String) {
    					 sub.setFirstName((String) list.get(0)) ;
    		} else {
    					System.out.println("Error: Index 0 is not a String!");
    					return null;
    		}
    		
    		// Setting last Name from the list we got from the message content
			if (list.get(1) instanceof String) {
				sub.setLastName((String) list.get(1));
			} else {
				System.out.println("Error: Index 1 is not a String!");
				return null;
			}
    		
    		 // Setting type from the list we got from the message content
			if (list.get(2) instanceof String) {
				sub.setType((String) list.get(2));
            } else {
                System.out.println("Error: Index 2 is not a String!");
                return null;
            }
    		
    		 // Setting personal Info from the list we got from the message content
			if (list.get(3) instanceof String) {
					sub.setPersonalInfo((String) list.get(3));
			} else {
					System.out.println("Error: Index 3 is not a String!");
					return null;
			}

		 // Setting user name from the list we got from the message content	
			if (list.get(4) instanceof String) {
					sub.setUsername((String) list.get(4));
			} else {
					System.out.println("Error: Index 4 is not a String!");
					return null;
			}
    		
    		// Setting password from the list we got from the message content
			if (list.get(5) instanceof String) {
				sub.setPassword((String) list.get(5));
			} else {
				System.out.println("Error: Index 5 is not a String!");
				return null;
			}
			
		//setting phone Number from the list we got from the message content
		if (list.get(6) instanceof String) {
			sub.setPhoneNumber((String) list.get(6));
		} else {
			System.out.println("Error: Index 6 is not a String!");
			return null;
		}
		
		//setting email from the list we got from the message content
		if (list.get(7) instanceof String) {
			sub.setEmail((String) list.get(7));
		} else {
			System.out.println("Error: Index 7 is not a String!");
			return null;
		}
		
		sub.setCustomerId(DBC.getCustomerId(sub));//return get customer id if exists in customer table else set new customer id
		//Adding the new subscriber to the DB and getting the subscriberId assigned by the DB
		sub.setSubscriberId(DBC.addNewSubscriber(sub));
		
		return sub;
	}
	
	/**
	 * Updates the subscriber details in the database.
	 * 
	 * @param msg The message containing the subscriber details to be updated. The
	 *            content of the message is expected to be an ArrayList<Object> with
	 *            the following order: [Location 0 :customerId (Integer),Location
	 *            1:phoneNumber (String) or null,Location 2:email (String) or null]
	 * @return true if the update was successful, false otherwise.
	 */
	private boolean updateSubscriberDetails(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the reservation details from the message content
    	
		Subscriber sub = new Subscriber();
		
		//setting customer Id from the list we got from the message content
		if (list.get(0) instanceof Integer) {
			sub.setCustomerId((int) list.get(0));
		} else {
			System.out.println("Error: Index 0 is not an Integer!");
			return false;
		}

		//setting phone Number from the list we got from the message content
		if (list.get(1) instanceof String) {
			sub.setPhoneNumber((String) list.get(1));
		} 
		else if (list.get(1) == null) 
		{
			sub.setPhoneNumber(null);
		}
		else {
			System.out.println("Error: Index 1 is not a String!");
			return false;
		}
		
		//setting email from the list we got from the message content
		if (list.get(2) instanceof String) {
			sub.setEmail((String) list.get(2));
		}
		else if (list.get(2) == null) {
				sub.setEmail(null);
		}
		else {
			System.out.println("Error: Index 2 is not a String!");
			return false;
		}
		
		//Updating the subscriber phone number or email or both in the DB(for exemple if  phone number is null, only email will be updated)
		return DBC.updateSubscriberDetails(sub);//return to server true if update successful else false
		
	}

	/**
	 * Retrieves all subscribers from the database.
	 * 
	 * @param msg The message requesting all subscribers.
	 * @return An ArrayList of Subscriber objects representing all subscribers in
	 *         the database.
	 */
	private ArrayList<Subscriber> getAllSubscribers(Message msg)
	{
		
		ArrayList<Subscriber> subListAsSubscriber = new ArrayList<>();
				
		ArrayList<ArrayList<Object>> allSubscriber = new ArrayList<>();
		allSubscriber = DBC.getAllSubscribersQuery();

	
		for (ArrayList<Object> subAsList : allSubscriber)
		{
    		
			Subscriber subAsSubscriber = new Subscriber();
	    	    
	    	    //Set subscriberId  in the Subscriber object
			if (subAsList.get(0) instanceof Integer) {
					subAsSubscriber.setSubscriberId((int) subAsList.get(0));
			} else {
					System.out.println("Error: Index 0 is not an Integer!");
					return null;
			}
			
			//Set customerId  in the Subscriber object
			if (subAsList.get(1) instanceof Integer) {
				subAsSubscriber.setCustomerId((int) subAsList.get(1));
			} else {
				System.out.println("Error: Index 1 is not an Integer!");
				return null;
			}

    			//Set first Name in the Subscriber object
			if (subAsList.get(2) instanceof String) {
				subAsSubscriber.setFirstName((String) subAsList.get(2));
			} else {
				System.out.println("Error: Index 2 is not a String!");
				return null;
			}
            
            //Set last Name in the Subscriber object
			if (subAsList.get(3) instanceof String) {
				subAsSubscriber.setLastName((String) subAsList.get(3));
            } else {
                System.out.println("Error: Index 3 is not a String!");
                return null;
            }
            
             //Set type in the Subscriber object
			if (subAsList.get(4) instanceof String) {
				subAsSubscriber.setType((String) subAsList.get(4));
            } else {
                System.out.println("Error: Index 4 is not a String!");
                return null;
            }
            
             //Set personal Info in the Subscriber object
			if (subAsList.get(5) instanceof String) {
				subAsSubscriber.setPersonalInfo((String) subAsList.get(5));
            }
			else if (subAsList.get(5) == null) 
			{
				subAsSubscriber.setPersonalInfo("");
			}
			else {
                System.out.println("Error: Index 5 is not a String!");
                return null;
            }

             //Set user name in the Subscriber object
			if (subAsList.get(6) instanceof String) {
				subAsSubscriber.setUsername((String) subAsList.get(6));
            } else {
                System.out.println("Error: Index 6 is not a String!");
                return null;
            }
            
            //Set password in the Subscriber object
			if (subAsList.get(7) instanceof String) {
				subAsSubscriber.setPassword((String) subAsList.get(7));
			} else {
				System.out.println("Error: Index 7 is not a String!");
				return null;
			}
			//Set phone Number in the Subscriber object
			if (subAsList.get(8) instanceof String) {
				subAsSubscriber.setPhoneNumber((String) subAsList.get(8));
			} else {
				System.out.println("Error: Index 8 is not a String!");
				return null;
			}
			//Set email in the Subscriber object
			if (subAsList.get(9) instanceof String) {
				subAsSubscriber.setEmail((String) subAsList.get(9));
			} else {
				System.out.println("Error: Index 9 is not a String!");
				return null;
			}
			
	    		subListAsSubscriber.add(subAsSubscriber);
    		}
    	
		return subListAsSubscriber;
	}
	
}

