package controller;

import java.sql.Timestamp;
import java.util.ArrayList;


import data.Customer;
import data.ManWaiting;
import data.Message;
import data.Subscriber;
import data.Table;
import data.TableReservation;
import data.WaitList;

public class WaitListController 
{


	private static DataBaseController DBC=DataBaseController.getInstance();//Interfacing with the DB Controller

	/**
	 * Default constructor
	 */
	public WaitListController() 
	{

	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////managing messages //////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Handles messages from the server related to table operations.
	 * @param msg The message received from the server.
	 */
	public Object handleMessageFromServer(Message msg) 
	{

		switch (msg.command) //Checking the type of message sent from the server (what action should be performed in the DB Controller)
		{
		case GET_IN_TO_WAIT_LIST:
			return getInToWaitList(msg);

		case GET_WAIT_LIST:
			return getWaitList();

		case DELETE_FROM_WAIT_LIST:
			return deleteFromWaitList(msg);
		default:
			System.out.println("Unknown task received.");
			return null;
		}
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////Helper methods//////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Seats a waiter by updating their status in wait list and reservation details.
	 *check_in will get tableId updated and status to arrived
	 * walk_in will get reservation date to now and status to active without tableId because 
	 * he need to do check-in to get his table id
	 *
	 * @param waiter    The WaitList object representing the waiter to be seated.
	 * @param freeTable The Table object representing the free table.
	 * @param res       The TableReservation object associated with the waiter.
	 * @return true if the waiter was successfully seated; false otherwise.
	 */
	public synchronized static boolean seatWaiter(WaitList waiter, Table freeTable,TableReservation res)
	{
		String originalStatus = waiter.getStatus();
		Timestamp originalExitTime = waiter.getExitTimeFromList();
		//update waitlist status to seated and set exitTimeFromList to now
		waiter.setStatus("seated");
		waiter.setExitTimeFromList(new Timestamp(System.currentTimeMillis())); 
		if (!DBC.updateStatusAndExitTimeInWaitingListQuery(waiter)) 
		{
			return false;
		}

		//update reservation details in each case check-in / walk-in
		if (waiter.getType().equals("check_in")) 
		{
			res.setTableId(freeTable.getTableId());//update tableId because now we have a table for him and he in the restaurant
			res.setStatus("arrived");//update status to arrived

			TableController.updateTable(freeTable.getTableId(), "status", "occupied"); 
			if (!DBC.updateReservation(res)) 
			{
				System.out.println("Error linking table to reservation. Performing Rollback.");
				
				// ROLLBACK 
				TableController.updateTable(freeTable.getTableId(), "status", "available"); 
				waiter.setStatus(originalStatus); // waiting
				waiter.setExitTimeFromList(originalExitTime); // null or previous time
				DBC.updateStatusAndExitTimeInWaitingListQuery(waiter);
				// --- ROLLBACK END ---

				
				return false; // Error updating reservation

			}

			BillController.createNewBill( res);
			System.out.println("createNewBill in seatWaiter after rollback for check-in");
			return true; //Successfully seated the waiter

		} 
		else if (waiter.getType().equals("walk_in"))
		{//walk-in customer can come after a 15 minutes he got a table 
			//if he come he need to do receiveTableIdByConfCode to get his table id

			//update waitlist status to seated and set exitTimeFromList to now
			waiter.setStatus("notified");
			waiter.setExitTimeFromList(new Timestamp(System.currentTimeMillis())); 
			if (!DBC.updateStatusAndExitTimeInWaitingListQuery(waiter)) 
			{
				return false;
			}

			res.setReservationDate(new Timestamp(System.currentTimeMillis()));//update reservation date to now because now the reservation begins
			res.setStatus("active");//update status to active because now the reservation begins

			if (!DBC.updateReservation(res)) 
			{
				System.out.println("Error linking table to reservation. Performing Rollback.");

				// ROLLBACK

				waiter.setStatus("waiting"); 
				waiter.setExitTimeFromList(null); 
				DBC.updateStatusAndExitTimeInWaitingListQuery(waiter);


				return false; // Error updating reservation
			}

			return true; 
		}
		return false;


	}

	/**
	 * Converts a list of waitlist data from the database into a list of WaitList objects.
	 *
	 * @param allallWaits An ArrayList of ArrayLists, where each inner list contains the attributes of a waitlist entry.
	 * @return An ArrayList of WaitList objects representing the waitlist entries.
	 */
	public static ArrayList<WaitList> getAllWaitingAsWaitList(ArrayList<ArrayList<Object>> allallWaits)
	{

		ArrayList<WaitList> WaitsListAsWaitList = new ArrayList<>();

		for (ArrayList<Object> WaitAsList : allallWaits)
		{

			WaitList waitAsWaitList = new WaitList();

			//Set waiting_id in the WaitList object
			if (WaitAsList.get(0) instanceof Integer) 
			{
				waitAsWaitList.setWaitingId((Integer) WaitAsList.get(0));
			} else {
				System.out.println("Error: Index 0 is not a Integer!");
				return null; 
			}

			//Set reservationId in the WaitList object
			if (WaitAsList.get(1) instanceof Integer) 
			{
				waitAsWaitList.setReservationId((Integer) WaitAsList.get(1));
			} else {
				System.out.println("Error: Index 1 is not a Integer!");
				return null; 
			}

			//Set numberOfDiners in the WaitList object
			if (WaitAsList.get(2) instanceof Integer) 
			{
				waitAsWaitList.setNumberOfDiners((Integer) WaitAsList.get(2));
			} else {
				System.out.println("Error: Index 2 is not a Integer!");
				return null; 
			}

			//Set  entry Time To List in the WaitList object
			if (WaitAsList.get(3) instanceof Timestamp) 
			{
				waitAsWaitList.setEntryTimeToList((Timestamp) WaitAsList.get(3));
			} else {
				System.out.println("Error: Index 3 is not a Timestamp!");
				return null; 
			}

			//Set  exit Time From List in the WaitList object
			if (WaitAsList.get(4) instanceof Timestamp) 
			{
				waitAsWaitList.setExitTimeFromList((Timestamp) WaitAsList.get(4));
			} 
			else if (WaitAsList.get(4) == null)
			{
				waitAsWaitList.setExitTimeFromList(null);
			}
			else 
			{
				System.out.println("Error: Index 4 is not a Timestamp!");
				return null; 
			}

			//Set status in the WaitList object
			if (WaitAsList.get(5) instanceof  String ) 
			{
				waitAsWaitList.setStatus(( String ) WaitAsList.get(5));
			} else {
				System.out.println("Error: Index 5 is not a  String !");
				return null; 
			}

			//Set type in the WaitList object
			if (WaitAsList.get(6) instanceof  String ) 
			{
				waitAsWaitList.setType(( String ) WaitAsList.get(6));
			} else {
				System.out.println("Error: Index 6 is not a  String !");
				return null; 
			}


			WaitsListAsWaitList.add(waitAsWaitList);
		}
		return WaitsListAsWaitList;
	}


	/**
	 * * Finds a matching waiter in the waiting list for a given free table and
	 * seats the waiter if a match is found.
	 *updates the waitlist and reservation in the DB
	 *
	 * @param freeTable The Table object representing the free table.
	 * @return true if a matching waiter was found and seated; false otherwise.
	 */
	public synchronized static WaitList findMatchInWaitingList(Table freeTable)
	{

		ArrayList<ArrayList<Object>> allWaits = new ArrayList<>();

		allWaits = DBC.getWaitingListQuery(); //return all Waiting List as ArrayList<WaitList> ,the first one on the list is the one that waited the longest. 

		if (allWaits == null || allWaits.isEmpty()) 
		{
			return null; 
		}

		//list of all people Waiting/seated/canceled as list of WaitList objects
		ArrayList<WaitList> queue = getAllWaitingAsWaitList(allWaits);



		for (WaitList waiter : queue) 
		{
			if (waiter.getStatus().equals("waiting") && waiter.getType().equals("check_in"))
			{
				//Check if the table can accommodate the number of diners for this waiter check-in
				if (freeTable.getSeatsNumber() >= waiter.getNumberOfDiners()) 
				{

					TableReservation res = new TableReservation();
					res.setReservationId(waiter.getReservationId());
					if(!DBC.getReservationByReservationId(res))
					{
						System.out.println("Error fetching reservation for waiter ID: " + waiter.getWaitingId());
						continue; // failed to get reservation details
					}

					if (seatWaiter(waiter, freeTable,res)) 
					{
						return waiter; // Successfully seated the waiter
					}
				}
			}
		}

		//if no check-in found , try to find walk-in
		for (WaitList waiter : queue) 
		{
			//Check if the table can accommodate the number of diners for this waiter walk_in
			if (waiter.getStatus().equals("waiting") &&   waiter.getType().equals("walk_in"))
			{
				TableReservation res = new TableReservation();
				res.setReservationId(waiter.getReservationId());
				if(!DBC.getReservationByReservationId(res))
				{
					System.out.println("Error fetching reservation for waiter ID: " + waiter.getWaitingId());
					continue; // failed to get reservation details
				}

				if (seatWaiter(waiter, freeTable,res )) 
				{
					return waiter; 
				}
			}
		}

		return null;//not found suitable waiter for this table 
	}






	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////Logic methods//////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Deletes a customer from the wait list based on the provided message content.
	 *
	 * @param msg The message containing the confirmation code of the reservation to
	 *            be deleted. The content of the message is expected to be an
	 *            ArrayList<Object> with the following order: [Location 0 : String
	 *            type of customer ("customer" or "subscriber"), Location 1 : String
	 *            phone number (if type is "customer") or Integer subscriber ID (if type is
	 *            "subscriber"), Location 2 : String email (if type is "customer")]
	 * @return true if the customer was successfully deleted from the wait list;
	 *         false otherwise.
	 */
	private synchronized static boolean deleteFromWaitList(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;

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
			// Setting subscriber ID from the list we got from the message content
			if (list.get(1) instanceof Integer) {
				customerId = (int) list.get(1);
			} else {
				System.out.println("Error: Index 1 is not a Integer!");
				return false;
			}
		}


		ArrayList<WaitList> allWaiter = getAllWaitingAsWaitList(DBC.getWaitingListQuery());//Getting all wait list from the DB as list of WaitList objects
		if (allWaiter.isEmpty()) 
		{
			System.out.println("No waiters in the wait list.");
			return false; // No waiters to delete
		}

		for (WaitList waiter : allWaiter) 
		{
			TableReservation res = new TableReservation();
			res.setReservationId(waiter.getReservationId());

			if (!DBC.getReservationByReservationId(res)) 
			{
				System.out.println("Error retrieving reservation for wait list.");
				continue; // Error retrieving reservation
			}

			if (  waiter.getStatus().equals("waiting")) 
			{
				if (res.getCustomerId() == customerId)// Found the matching waiter to delete
				{

					if (DBC.deleteFromWaitList(waiter)) //Deleting the waiter from the wait list in the DB by Changeing his status to canceled
					{
						if (!DBC.deleteReservationByConfCode(res.getConfirmationCode())) //Deleting the reservation associated with the deleted waiter
						{
System.out.println("Error cancelling reservation for deleted waiter. Performing Rollback.");
							
							// ROLLBACK 
							
							waiter.setStatus("waiting");
							DBC.updateStatusAndExitTimeInWaitingListQuery(waiter); 
	
							
							return false; // Error cancelling reservation
						}
						return true; // Successfully deleted the waiter from the wait list
					} else {
						System.out.println("Error deleting waiter from wait list.");
						return false; // Error deleting waiter
					}
				}
			}
		}
		return false;
	}
	/**
	 * Retrieves the current wait list of customers who are waiting for a table.
	 *
	 *
	 * @return An ArrayList of ManWaiting objects representing the current wait
	 *         list.
	 */
	private ArrayList<ManWaiting> getWaitList()
	{
		ArrayList<ManWaiting> waitList = new ArrayList<>();

		ArrayList<WaitList> allWaiter = getAllWaitingAsWaitList(DBC.getWaitingListQuery());//Getting all wait list from the DB as list of WaitList objects

		for (WaitList waiter : allWaiter) 
		{
			if (waiter.getStatus().equals("waiting")) 
			{

				TableReservation res = new TableReservation();
				res.setReservationId(waiter.getReservationId());

				if (!DBC.getReservationByReservationId(res))
				{
					System.out.println("Error retrieving reservation for wait list.");
					return null; // Error retrieving reservation
				}

				Subscriber sub= new Subscriber();
				sub.setCustomerId(res.getCustomerId());
				if (!DBC.getCustomerByCustomerId(sub))
				{
					System.out.println("Error retrieving customer for wait list.");
					return null; // Error retrieving customer
				}

				ManWaiting manWait = new ManWaiting();
				manWait.setFirstName(sub.getFirstName());
				manWait.setLastName(sub.getLastName());
				manWait.setPhoneNumber(sub.getPhoneNumber());
				manWait.setEmail(sub.getEmail());
				manWait.setEntryTimeToList(waiter.getEntryTimeToList());
				waitList.add(manWait);

			}

		}
		return waitList;//Returning the list of waiters currently waiting

	}



	/**
	 * Handles the process of adding a walk-in customer to the wait list. It checks
	 * for immediate table availability(now and for 2 hours) . 
	 * If a table is available, it creates a reservation(with table and conf code) and bill and returns the reservation
	 *  If no table is available, it adds the customer to the wait list and
	 * creates a reservation (without table and with conf code) and returns the reservation
	 * 
	 * !!!!!walk-in customer that added to wait list need to do receiveTableIdByConfCode when he
	 * arrives to get his table ID
	 *
	 * @param msg The message containing the confirmation code of the reservation to
	 *            be deleted. The content of the message is expected to be an
	 *            ArrayList<Object> with the following order: [Location 0 : String 
	 *            type of customer ("customer" or "subscriber"), Location 1 : String
	 *            phone number (if type is "customer") or Integer Customer ID (if type is "subscriber"),
	 *             Location 2 : String email (if type is "customer"),
	 *            
	 * @return A TableReservation object representing the newly created reservation,
	 *           or null if the operation failed.         
	 */
	private synchronized static TableReservation getInToWaitList(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;

		WaitList newWait = new WaitList();//Creating a new reservation object


		String typeCustomer= new String();
		int customerId=-1;


		//Setting customer type from the list we got from the message content
		if (list.get(0) instanceof String) 
		{
			typeCustomer = (String) list.get(0);
		} else 
		{
			System.out.println("Error: Index 0 is not a String!");
			return null;
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
					return null;
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
					return null;
				}
			}
			else 
			{
				System.out.println("Error: Index 1 is not a String!");
				return null;
			}

			//return customer ID from the DB . if customer not exists he created in the DB and return his ID else return his ID
			customerId=DBC.getCustomerId(customer);//Getting customer ID from the DB based on the phone number or email provided

		}

		else if (typeCustomer.equals("subscriber")) 
		{
			// Setting Customer ID from the list we got from the message content
			if (list.get(1) instanceof Integer) {
				customerId = (int) list.get(1);
			} else {
				System.out.println("Error: Index 1 is not a Integer!");
				return null;
			}
		}

		//waitingId  AUTO_INCREMENT in DB

		// reservationId first null

		// Setting numberOfDiners from the list we got from the message content
		if (list.get(3) instanceof Integer) {
			newWait.setNumberOfDiners((int) list.get(3));
		} else {
			System.out.println("Error: Index 3 is not a Integer!");
			return null;
		}


		//entryTimeToList DEFAULT CURRENT_TIMESTAMP in DB

		//status DEFAULT 'waiting', in DB


		newWait.setType("walk_in");//Setting type to walk_in since this method is for walk-in customers only


		//check for available table right now for the number of diners 
		Table bestTable = TableController.findBestTableForNow(newWait.getNumberOfDiners());


		boolean canSitImmediately = false;

		//if there is a table available right now , check if there is someone that waiting for this table
		if (bestTable != null) 
		{

			boolean neededByQueue = DBC.isTableNeededQueue(bestTable.getSeatsNumber());//return true if there is someone in the wait list that his number of diners <= table size and in status waiting

			if (!neededByQueue) 
			{

				canSitImmediately = true;//There is a table available right now and no one is waiting for it, so the customer can sit immediately
			}
		}

		// if found a table available right now , create reservation and return table number with minus sign
		if (bestTable != null && canSitImmediately) 
		{
			//Create reservation without wait for walk-in customer
			TableReservation result=ReservationControler.createReservationWithoutWait(bestTable.getTableId(),newWait.getNumberOfDiners(),customerId);

			if (result!=null)
			{
				System.out.println("Reservation created successfully with confirmation code: " +result.getConfirmationCode());
				System.out.println("table ID: " + result.getTableId());
				return result; //Return to server the reservation with table and confirmation code 
			}
			else
			{
				return null;
			}
		}

		else// no available table right now , enter to wait list
		{

			TableReservation newRes =ReservationControler.createReservationWithWait(newWait.getNumberOfDiners(), customerId);
			if (newRes == null) 
			{

				return null;// Return null if the reservation was not created successfully in the DB
			}
			newWait.setReservationId(newRes.getReservationId());

			if (DBC.addToWaitList(newWait))//Return that the add to Wait List was created successfully in the DB 
			{

				System.out.println("Reservation created successfully with confirmation code: " + newRes.getConfirmationCode());
				System.out.println("table ID: " + newRes.getTableId());//table ID will be null because no table assigned yet
				return newRes;//Return to server reservation without table and with confirmation code
			}

			return null;//Return null if the reservation was not created successfully in the DB
		}
	}


}
