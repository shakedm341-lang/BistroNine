package controller;


import java.time.LocalDate;

import java.util.ArrayList;


import data.Customer;
import data.Message;
import data.Subscriber;

import data.TableReservation;

public class CustomerController 
{
	private static DataBaseController DBC=DataBaseController.getInstance();//Interfacing with the DB Controller

	/**
	 * Default constructor
	 */
	public CustomerController() 
	{

	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////managing messages //////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

		case CHECK_LOGIN_DETAILSֹֹ_BY_TAG_READER:
			return checkLoginDetailsByTagReader(msg);

		case ADD_NEW_SUBSCRIBER:
			return addNewSubscriber(msg);

		case UPDATE_SUBSCRIBER_DETAILS:
			return updateSubscriberDetails(msg);

		case GET_ALL_SUBSCRIBERS:
			return getAllSubscribers();

		case LOST_CONF_CODE:
			return LostConfCode(msg);

		case GET_ALL_CONF_CODE_BY_CUSTOMER_ID:
			return getConfCodeByCustomerId(msg);

		default:
			System.out.println("Unknown task received.");
			return null;
		}
	}
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////Helper methods//////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Converts a list of reservations into a list of unique Subscribers.
	 * Iterates through the provided reservations, identifies unique customer IDs, 
	 * and fetches their full details from the database to create Subscriber objects.
	 * 
	 * * @param reservations The list of TableReservation objects containing customer IDs.
	 * @return ArrayList<Subscriber> A list of unique Subscriber objects. Returns an empty list if input is null.
	 */
	public static ArrayList<Subscriber> getSubscribersFromReservations(ArrayList<TableReservation> reservations)
	{
		// Safety check: if input is null, return null 
		if (reservations == null) return null;

		ArrayList<Subscriber> subscribersList = new ArrayList<>();
		ArrayList<Integer> addedCustomerIds = new ArrayList<>(); // Used to filter out duplicate customers

		// Iterate through all reservations to identify unique customers
		for (TableReservation res : reservations) 
		{
			int customerId = res.getCustomerId();

			// Process only if we haven't already fetched this customer's details
			if (!addedCustomerIds.contains(customerId)) 
			{
				Subscriber sub = new Subscriber();
				sub.setCustomerId(customerId);

				// Attempt to fetch full customer details from the Database based on ID
				if (DBC.getCustomerByCustomerId(sub)) 
				{
					// Success: Add  subscriber to the result list
					subscribersList.add(sub);
					addedCustomerIds.add(customerId); 
				}
				else
				{
					// Error: The reservation points to a Customer ID that doesn't exist 

					System.out.println(" Critical Data Integrity Issue: Reservation " + 
							res.getReservationId() + " belongs to Customer ID " + 
							customerId + ", but this customer does not exist in DB!");
					return null; 
				}
			}
		}

		return subscribersList;
	}

	/**
	 * Retrieves the customer type based on the provided customer ID.
	 *
	 * @param customerId The ID of the customer.
	 * @return The type of the customer (e.g., Customer, Subscriber, Restaurant
	 *         Representative, Restaurant Manager).
	 */
	public static String getCustomerType(int customerId)
	{
		return DBC.getCustomerType(customerId);//return the customer type from DB /Customer/subscriber/restaurant representative/restaurant manager
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////Logic methods//////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Checks the login subscriber Id of a subscriber.
	 * 
	 * @param msg The message containing the login details. The content of the
	 *            message is expected to be an ArrayList<Object> with the following
	 *            order: [Location 0 :subscriberId (int)]
	 * @return Subscriber object if login details are valid, null otherwise.
	 */
	private Subscriber checkLoginDetailsByTagReader(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get username and password from the message

		int subId = -1;

		//Set subscriber Id  in the Subscriber object
		if (list.get(0) instanceof Integer) {
			subId = (Integer) list.get(0);
		} else {
			System.out.println("Error: Index 0 is not a Integer!");
			return null;
		}

		ArrayList<Subscriber> allSub = getAllSubscribers();

		for (Subscriber sub : allSub)
		{
			if (sub.getSubscriberId()==subId)
			{
				return sub;
			}
		}

		return null;
	}

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
	private synchronized Subscriber addNewSubscriber(Message msg)
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
		System.out.println("added sucss new subscriber with subscriber id: "+sub.getCustomerId());
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
	private synchronized boolean updateSubscriberDetails(Message msg)
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
	private ArrayList<Subscriber> getAllSubscribers()
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

	/**
	 * Retrieves lost confirmation codes for a customer or subscriber for today's reservations.
	 * 
	 * @param msg The message containing the details to retrieve lost confirmation
	 *            codes. The content of the message is expected to be an
	 *            ArrayList<Object> with the following order: [Location 0
	 *            :typeCustomer (String: "customer" or "subscriber"), Location 1:
	 *            phoneNumber (String) or subscriberId (Integer), Location 2: email
	 *            (String) - only if typeCustomer is "customer"]
	 * @return true if email and SMS are send to customer else false
	 */
	private boolean LostConfCode(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the details from the message content



		String typeCustomer= new String();
		int customerId=-1;


		//Setting customer type from the list we got from the message content
		if (list.get(0) instanceof String) 
		{
			typeCustomer = (String) list.get(0);
		} else 
		{
			System.out.println("Error: Index 0 is not a String!");
			return false;
		}


		if (typeCustomer.equals("customer")) 
		{
			Customer customer = new Customer();

			if (list.get(1) instanceof String) 
			{
				customer.setPhoneNumber((String) list.get(1))  ;// Setting phone Number from the list we got from the message content
				if (list.get(2) instanceof String) 
				{
					customer.setEmail((String) list.get(2));// Setting email from the list we got from the message content
				}
				else if (list.get(2)==null)//if the email is null
				{
					customer.setEmail(null);
				}
				else 
				{
					System.out.println("Error: Index 2 is not a String!");
					return false;
				}
			}
			else if (list.get(1)==null)//if the phone number is null
			{
				customer.setPhoneNumber(null);
				if (list.get(2) instanceof String) 
				{
					customer.setEmail((String) list.get(2))  ;//Setting email from the list we got from the message content
				}
				else 
				{
					System.out.println("Error: Index 2 is not a String!");
					return false;
				}
			}
			else 
			{
				System.out.println("Error: Index 1 is not a String!");
				return false;
			}

			//return customer ID from the DB . if customer not exists he created in the DB and return his ID else return his ID
			customerId=DBC.getCustomerId(customer);//Getting customer ID from the DB based on the phone number or email provided
		}

		else if (typeCustomer.equals("subscriber")) 
		{
			// Setting customer ID from the list we got from the message content
			if (list.get(1) instanceof Integer) {
				customerId = (int) list.get(1);
			} else {
				System.out.println("Error: Index 1 is not a Integer!");
				return false;
			}
		}

		//Getting all reservations of the customer from the DB
		ArrayList<TableReservation> allRes = ReservationControler.getAllReservationsAsTableReservation(DBC.getAllReservationsQueryByCustomerId(customerId));
		if (allRes == null) 
		{
			System.out.println("error return all reservayion by customer id" + customerId);
			return false;

		}
		ArrayList<Integer> confCodes = new ArrayList<>();

		for (TableReservation res : allRes) 
		{
			if (res.getStatus().equals("active"))//Checking only active reservations
			{
				LocalDate resDate = res.getReservationDate().toLocalDateTime().toLocalDate();
				if (resDate.equals(LocalDate.now()))//Checking if the reservation date is today
				{
					confCodes.add(res.getConfirmationCode());//Adding the existing confirmation code to the list to return to the server

				}

			}
		}
		Subscriber sub = new Subscriber();
		sub.setCustomerId( customerId);

		if (!DBC.getCustomerByCustomerId(sub))
		{
			System.out.println("Error: could not find customer  ");
			return false;
		}

		if (!confCodes.isEmpty()) 
		{
			StringBuilder codesAsString = new StringBuilder();


			for (Integer code : confCodes) 
			{
				codesAsString.append("confirmation code: ").append(code).append("\n"); 
			}


			EmailSendController.sendEmail(sub.getEmail(), "Confirmation Code Recovery","Don't worry bistro9 is here for you😊, everything is saved with us!\n"+"Here are all the confirmation codes for your orders for today:\n"+codesAsString);// send email to the customer with all his confirmation codes for today

			SmsSendController.sendSms(sub.getPhoneNumber(), "Confirmation Code Recovery","Don't worry bistro9 is here for you😊, everything is saved with us!\n"+"Here are all the confirmation codes for your orders for today:\n"+codesAsString);

		}
		else {
			EmailSendController.sendEmail(sub.getEmail(), "Oops, we couldn't find any confirmation codes for today🤭","We searched the system, but we didn't find any active confirmation codes for today.\n"+" Could it be that the reservation is for a different date?");// send email to the customer that no confirmation codes found for today

			SmsSendController.sendSms(sub.getPhoneNumber(), "Oops, we couldn't find any confirmation codes for today🤭","We searched the system, but we didn't find any active confirmation codes for today.\n"+" Could it be that the reservation is for a different date?");

		}


		return true;
	}

	/**
	 * Retrieves lost confirmation codes for a customer or subscriber for today's reservations.
	 * 
	 * @param msg The message containing the details to retrieve lost confirmation
	 *            codes. The content of the message is expected to be an
	 *            ArrayList<Object> with the following order: [Location 0 : customerId (Integer)]
	 * @return An ArrayList of Integer representing the  confirmation codes of the customer
	 */
	private ArrayList<Integer> getConfCodeByCustomerId(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the details from the message content

		int customerId=-1;

		// Setting customer Id from the list we got from the message content
		if (list.get(0) instanceof Integer) 
		{
			customerId = (int) list.get(0);
		} else {
			System.out.println("Error: Index 0 is not a Integer!");
			return null;
		}


		//Getting all reservations of the customer from the DB
		ArrayList<TableReservation> allRes = ReservationControler.getAllReservationsAsTableReservation(DBC.getAllReservationsQueryByCustomerId(customerId));
		if (allRes == null) 
		{
			System.out.println("error return all reservayion by customer id" + customerId);
			return null;

		}
		ArrayList<Integer> confCodes = new ArrayList<>();

		for (TableReservation res : allRes) 
		{
			if (res.getStatus().equals("active"))//Checking only active reservations
			{
				LocalDate resDate = res.getReservationDate().toLocalDateTime().toLocalDate();
				if (resDate.equals(LocalDate.now()))//Checking if the reservation date is today
				{
					confCodes.add(res.getConfirmationCode());//Adding the existing confirmation code to the list to return to the server

				}

			}
		}
		return confCodes;
	}
}

