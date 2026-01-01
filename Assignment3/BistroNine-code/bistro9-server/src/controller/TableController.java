package controller;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import data.Bill;
import data.Message;
import data.Table;
import data.TableReservation;
import data.WaitList;

public class TableController 
{
	private static DataBaseController DBC=DataBaseController.getInstance();//Interfacing with the DB Controller
	
	/**
	 * constructor for the TableController class
	 */
	public  TableController()
	{
		
	}
	
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////managing messages //////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Handles messages from the server related to table operations.
	 */
	public Object handleMessageFromServer(Message msg) 
	{
		
		switch (msg.command) //Checking the type of message sent from the server (what action should be performed in the DB Controller)
		{
	    case RECEIVE_TABLE_ID:
	    	return receiveTableIdByConfCode(msg);
	    case ADD_TABLE: 
			return addTable(msg);
			
		case DELETE_TABLE: 
			return deleteTable(msg);
			
		case UPDATE_TABLE_SEATS: 
			return updateTableSeatsNumber(msg);
			
		case GET_ALL_AVAILABLE_TABLES:
			return getAllAvailableTables();
	    default:
	        System.out.println("Unknown task received.");
	        return null;
		}
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////Helper methods//////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * "Finds the smallest available table for immediate seating that does not conflict with 
	 * incoming reservations for the next 2 hours
	 *
	 * @param requiredSeats The number of seats required for the table.
	 * @return The best available Table object that meets the criteria, or null if
	 *         no suitable table is found.
	 */
	public static Table findBestTableForNow(int requiredSeats) 
	{
	    ArrayList<Table> allTables = TableController.getTableInRestaurant();//get all tables in the restaurant
	    ArrayList<TableReservation> futureReservations = ReservationControler.getAllReservationsActive();//get all active,arrived future reservations

	    Table bestTable = null;
	    LocalDateTime nowTime = LocalDateTime.now();
	    LocalDateTime safetyTimeLimit = nowTime.plusHours(2);//safety time limit is 2 hours from now

	    if (allTables == null) return null;

	    ///////////fins best table available now and for the next 2 hours for immediate seating/////////////////
	    
	    
	    for (Table t : allTables) 
	    {
	        // Check if the table is available and meets the required seats 
	        if (t.getStatus().equalsIgnoreCase("available") && t.getSeatsNumber() >= requiredSeats) 
	        {
	        		// loop that ran again on all tables
	            int availableTablesCount = 0;
	            for (Table otherT : allTables) 
	            {
		            	//Counting the tables that are both vacant and the same size (or larger) than the current table (t)
			        	//This gives us the total "stock" we have to offer for reservations
	                if (otherT.getStatus().equals("available") && otherT.getSeatsNumber() >= t.getSeatsNumber()) {
	                    availableTablesCount++;
	                }
	            }

	            // counting "threatening" reservations that may need this table (t)
	            int imminentReservationsCount = 0;//Variable for counting "threatening" orders
	            if (futureReservations != null) 
	            {
	                for (TableReservation res : futureReservations) 
	                {
	                    // check only active or arrived reservations for counting the reservations that may need this table (t)
	                    if (res.getStatus().equals("active") || res.getStatus().equals("arrived")) {
	                        
	                        // check if the reservation can fit on this table (t)
	                        if (res.getNumberOfDiners() <= t.getSeatsNumber()) {
	                            
	                          // check if the reservation time is within the next 2 hours and after now minus 15 minutes(laters)
	                            LocalDateTime resTime = res.getReservationDate().toLocalDateTime();
	                            if (resTime.isBefore(safetyTimeLimit) && resTime.isAfter(nowTime.minusMinutes(15))) 
	                            {
	                                imminentReservationsCount++;
	                            }
	                        }
	                    }
	                }
	            }

	          
	            //check if there are more available tables than imminent reservations that may need this table (t)
	            if (availableTablesCount > imminentReservationsCount) 
	            {
	                if (bestTable == null || t.getSeatsNumber() < bestTable.getSeatsNumber()) {
	                    bestTable = t;
	                }
	            }
	            //else, this table (t) is not suitable as it may be needed for imminent reservations
	        }
	    }
	    
	    return bestTable;//return the best table found or null if none found
	}
	
	/**
	 * Retrieves all tables in the restaurant from the database.
	 *
	 * @return An ArrayList of Table objects representing all tables in the
	 *         restaurant.
	 */
	public static ArrayList<Table> getTableInRestaurant()
	{
		ArrayList<ArrayList<Object>> allTables = new ArrayList<>();// List to hold all tables from the DB as a list of
																	// lists of objects
		ArrayList<Table> tablesListAsTable = new ArrayList<>();// List to hold all tables as Table objects
		//b3
		allTables = DBC.getAllTablesInRestaurant();//return all tables  from the DB as a list of lists of objects else return null
		for (ArrayList<Object> tableAsList : allTables) {// For each table in the list of tables
			Table table = new Table();// Create a new Table object

			// Set table ID in the Table object
			if (tableAsList.get(0) instanceof Integer) {
				table.setTableId((Integer) tableAsList.get(0));
			} else {
				System.out.println("Error: Index 0 is not a Integer!");
				return null;
			}

			// Set number of seats in the Table object
			if (tableAsList.get(1) instanceof Integer) {
				table.setSeatsNumber((Integer) tableAsList.get(1));
			} else {
				System.out.println("Error: Index 1 is not a Integer!");
				return null;
			}

			// Set location in the Table object
			if (tableAsList.get(2) instanceof String) {
				table.setLocation((String) tableAsList.get(2));
			} else {
				System.out.println("Error: Index 2 is not a String!");
				return null;
			}

			//set status in the Table object
			if (tableAsList.get(3) instanceof String) {
				table.setStatus((String) tableAsList.get(3));
			} else {
				System.out.println("Error: Index 3 is not a String!");
				return null;
			}
			
			tablesListAsTable.add(table);// Add the table to the list of tables as Table objects
		}

		return tablesListAsTable;
	}

	/**
	 * Updates a specific column of a table in the database.
	 *
	 * @param tableId    The ID of the table to be updated.
	 * @param columnName The name of the column to be updated.
	 * @param newValue   The new value to be set for the specified column.
	 * @return true if the update was successful, false otherwise.
	 */
	public static boolean updateTable(int tableId, String columnName, Object newValue) 
	{
		if (columnName.equals("status")) 
		{
			if (newValue instanceof String) 
			{
                return DBC.updateTableStatus(tableId, (String) newValue);
            } 
            else 
            {
                System.out.println("Error: newValue is not a String!");
                return false;
            }
		}
		
		if (columnName.equals("seatsNumber")) 
		{
			if (newValue instanceof Integer) 
            {
                return DBC.updateTableSeatsNumber(tableId, (Integer) newValue);
            } 
            else 
            {
                System.out.println("Error: newValue is not an Integer!");
                return false;
            }
		}
		return false;
		
		
    }
	
	/**
	 * Sorts a list of Table objects from the smallest to the largest number of on their Seats Number
	 * 
	 *
	 * @param tables The ArrayList of Table objects to be sorted.
	 */
	private void sortTablesBySeatsAscending(ArrayList<Table> tables) 
	{
	    if (tables == null || tables.size() <= 1) 
	    {
	        return; // No need to sort if empty or 1 element
	    }

	    int n = tables.size();
	    
	    // Bubble Sort Loop
	    for (int i = 0; i < n - 1; i++) 
	    {
	        for (int j = 0; j < n - i - 1; j++) 
	        {
	            // Compare current table with the next one based on seat number
	            Table t1 = tables.get(j);
	            Table t2 = tables.get(j + 1);

	            if (t1.getSeatsNumber() > t2.getSeatsNumber()) 
	            {
	                // Swap elements
	                tables.set(j, t2);
	                tables.set(j + 1, t1);
	            }
	        }
	    }
	}
	
	/**
	 * Checks if two reservations overlap in time.
	 *
	 * @param r1 The first TableReservation object.
	 * @param r2 The second TableReservation object.
	 * @return true if the reservations overlap, false otherwise.
	 */
	private boolean checkOverlap(TableReservation r1, TableReservation r2) 
	{
		// Assuming each reservation lasts for 2 hours
		LocalDateTime start1 = r1.getReservationDate().toLocalDateTime();
		LocalDateTime end1 = start1.plusHours(2); //assuming each reservation lasts for 2 hours

		
		LocalDateTime start2 = r2.getReservationDate().toLocalDateTime();
		LocalDateTime end2 = start2.plusHours(2);

		// Check for overlap
		return start1.isBefore(end2) && start2.isBefore(end1);
	}

	/**
	 * Checks if a list of reservations can fit optimally into a list of available
	 * tables.
	 *
	 * @param reservations    The ArrayList of TableReservation objects to be
	 *                        fitted.
	 * @param availableTables The ArrayList of available Table objects.
	 * @return true if all reservations can fit optimally, false otherwise.
	 */
	private boolean canFitOptimally(ArrayList<TableReservation> reservations, ArrayList<Table> availableTables) 
	{
		//create a temporary copy of the available tables to manipulate
		ArrayList<Table> tempTables = new ArrayList<>();
		for (Table t : availableTables) 
		{
			Table table=new Table();
			table.setTableId(t.getTableId());
			table.setSeatsNumber(t.getSeatsNumber());
			tempTables.add(table);
		}

		//sort reservations by number of diners from largest to smallest
		ArrayList<TableReservation> sortedRes = new ArrayList<>(reservations);
		ReservationControler.sortReservationsByDinersDescending(sortedRes);
		

		//find optimal fit for each reservation
		for (TableReservation res : sortedRes) 
		{
			boolean matched = false;

			for (int i = 0; i < tempTables.size(); i++) //loop through available tables
			{
				Table t = tempTables.get(i);
				
				if (t.getSeatsNumber() >= res.getNumberOfDiners()) //if the table can accommodate the reservation
				{
					//assign the table to the reservation and remove it from available tables
					tempTables.remove(i); 
					matched = true;
					break; 
				}
			}

			//if no suitable table found for this reservation
			if (!matched) 
			{
				return false; //cannot fit all reservations optimally
			}
		}

		//all reservations fitted successfully to available tables
		return true; 
	}
	
	
	/**
	 * Calculates the earliest safe date to update or delete a table without
	 * conflicting with existing reservations.
	 *
	 * @param tableId  The ID of the table to be updated or deleted.
	 * @param newSeats The new number of seats for the table (if updating), or 0 if
	 *                 deleting.
	 * @return A LocalDate indicating the earliest safe date for the operation, or null or today if safe to proceed immediately.
	 */
	private LocalDate calculateEarliestSafeDate(int tableId, int newSeats) 
	{
		//get all active reservations and all tables in the restaurant
		ArrayList<TableReservation> allReservations = ReservationControler.getAllReservationsActive();
		ArrayList<Table> allTables = TableController.getTableInRestaurant(); 

		if (allReservations == null || allTables == null) 
		{
			return null; // Fail safe handling
		}
		//a parameter to hold the last conflict date found
		LocalDate lastConflictDate = null;
		LocalDate today = LocalDate.now();

		//check if there is an arrived reservation for this table today
		for (TableReservation res : allReservations) 
		{
			if (res.getStatus().equalsIgnoreCase("arrived") && res.getTableId() == tableId) 
			{
				lastConflictDate = today;
			}
		}

		//create a simulated list of tables without the table we want to delete or with the table updated seats number
		ArrayList<Table> simulatedTables = new ArrayList<>();
		for (Table t : allTables) 
		{
			if (t.getTableId() == tableId)//the table we want to update or delete 
			{
				//if updating seats number
				if (newSeats > 0) 
				{
					//add the updated table to the simulated list
					Table updatedTable = new Table();
					updatedTable.setTableId(t.getTableId());
					updatedTable.setSeatsNumber(newSeats);
					simulatedTables.add(updatedTable);
				}
				//if deleting the table do not add it to the simulated list
			} 
			else //other tables
			{

				Table copy = new Table();
				copy.setTableId(t.getTableId());
				copy.setSeatsNumber(t.getSeatsNumber());
				simulatedTables.add(copy); 
			}
		}

		//sorting the simulated tables by seats number from smallest to largest
		sortTablesBySeatsAscending(simulatedTables);

		// creating a list of future reservations (active only)
		ArrayList<TableReservation> futureRes = new ArrayList<>();
		
		for (TableReservation res : allReservations) 
		{
			if (res.getStatus().equalsIgnoreCase("active")) 
			{
				futureRes.add(res);
			}
		}

	
		ArrayList<Integer> checkedRes = new ArrayList<>(); 

		for (TableReservation currentRes : futureRes) 
		{
			if (checkedRes.contains(currentRes.getReservationId())) continue;//if already checked, skip
			
			//creating a group of conflicting reservations (same time overlap)
			ArrayList<TableReservation> conflictingGroup = new ArrayList<>();
			conflictingGroup.add(currentRes);//add the current reservation to the conflicting group
			checkedRes.add(currentRes.getReservationId());//mark current reservation as checked

			for (TableReservation otherRes : futureRes)//check other reservations if overlap with current reservation 
			{
				//skip itself and already checked reservations
				if (currentRes.getReservationId() != otherRes.getReservationId() && !checkedRes.contains(otherRes.getReservationId())) 
				{
					//if there is an overlap, add to the conflicting group and mark as checked
					if (checkOverlap(currentRes, otherRes)) 
					{
						conflictingGroup.add(otherRes);
						checkedRes.add(otherRes.getReservationId());
					}
				}
			}
			//end of creating the conflicting group for the current reservation
			
			//check if the conflicting group for the current reservation can fit optimally in the simulated tables
			if (!canFitOptimally(conflictingGroup, simulatedTables)) 
			{
				LocalDate conflictDate = currentRes.getReservationDate().toLocalDateTime().toLocalDate();
				if (lastConflictDate == null || conflictDate.isAfter(lastConflictDate)) 
				{
					lastConflictDate = conflictDate;
				}
			}
		}

		//after checking all reservations, see if there was any conflict
		if (lastConflictDate != null) 
		{
			return lastConflictDate.plusDays(1); //return the next available date after the last conflict date
		}
		
		return today; //no conflicts found, safe to update/delete immediately
	}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////Logic methods//////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Retrieves all available/occupied tables in the restaurant from the database.
	 *
	 * @return An ArrayList of Table objects representing all available tables in
	 *         the restaurant.
	 */
	private ArrayList<Table> getAllAvailableTables()
	{
		ArrayList<Table> tables = getTableInRestaurant();//get all tables in the restaurant today available/occupied/deleted
		for (Table table : tables) 
		{
			if (table.getStatus().equals("deleted")) 
			{
				tables.remove(table);//remove deleted tables from the list
			}
		}
		return tables;//return the list of available/occupied tables to server
	}
	/**
	 * Receives a table ID based on the provided conference code in the message."check in" process and create a bill for the reservation.
	 *
	 * @param msg The message containing the reservation details. The content of the
	 *            message is expected to be an ArrayList<Object> with the following
	 *            order:[Location 0 :  conferenceCode (Integer)]
	 * @return A TableReservation object with updated details, or null if not found
	 *         or an error occurs.check the status of the reservation and update the customer if getting a table or get in to waitlisted.
	 */
	private TableReservation receiveTableIdByConfCode(Message msg)
	{
	
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the reservation details from the message content
		int conferenceCode=0;
		
		// Setting conference Code from the list we got from the message content
		if (list.get(0) instanceof Integer) 
		{
			conferenceCode = (int) list.get(0);
		} 
		else 
		{
			System.out.println("Error: Index 0 is not a Integer!");
			return null;
		}
		
		TableReservation checkInRes =new TableReservation();
		checkInRes.setConfirmationCode(conferenceCode);//set the conference code in the reservation object
		
		//return the reservation details from the DB in the table reservation object or null if not found
		if (!DBC.getReservationsByConferenceCodeQuery(checkInRes))
		{
			System.out.println("Reservation not found.");
			return null;//reservation not found
		}
		
		
		//else , check if the customer is late
		if (checkInRes.getStatus().equals("cancelled"))// if the customer is late by more than 15 minutes 
		{
			    System.out.println("Customer is late by " + (System.currentTimeMillis() - checkInRes.getReservationDate().getTime()) / 60000 );
			    return null; //reservation is canceled
		}

			
		//else, update the reservation as arrived and get table id
		checkInRes.setArrivalTime(new Timestamp(System.currentTimeMillis()));//set the arrival time to now
		checkInRes.setStatus("arrived");//set the status to arrived
			

		ArrayList<Table> tables=getTableInRestaurant();//get all tables in the restaurant today
		if (tables == null) {
		    System.out.println("Error fetching tables from DB.");
		    return null;
		}
		Table bestTable = null;
		
		//find the best suitable table for the reservation that available
		for (Table table : tables) 
		{     
		    // if the table can accommodate the number of diners in the check In reservation
		    if (table.getSeatsNumber() >= checkInRes.getNumberOfDiners()) 
		    {
		         //if the table is available
		        if (table.getStatus().equals("available"))
		        {
		        		//if no best table found yet or the current table has less seats than the best table found so far
		        		if (bestTable == null || table.getSeatsNumber() < bestTable.getSeatsNumber()) 
		        		{
	                    bestTable = table;
		        		}
		        	}
		    }
		 }
		
		 
		 if (bestTable != null)//if a suitable table is found 
		 {

			
			//update the table data in the DB returning true if successful or false if failed
			
			if (!updateTable(bestTable.getTableId(), "status", "occupied")) 
			{
					System.out.println("Failed to update table status.");
					return null; // failed to update table status
			}
			
			//update the reservation data in the DB return true if successful or false if failed
			checkInRes.setTableId(bestTable.getTableId());
			
			
			if (!DBC.updateReservation(checkInRes)) 
			{
				System.out.println("Failed to update reservation.");
				TableController.updateTable(bestTable.getTableId(), "status", "available");
				return null; // failed to update reservation
			}
			
			//create a new bill for the reservation 
			if (!BillController.createNewBill(checkInRes))
		    {
		         System.out.println("Warning: Reservation completed but Bill creation failed.");
		         return null; // Bill creation failed
		    }
			
			ArrayList<WaitList> allWaiter = WaitListController.getAllWaitingAsWaitList(DBC.getWaitingListQuery());
			
			if (allWaiter != null) 
			{
				for (WaitList waiter : allWaiter) 
				{
					if (waiter.getReservationId() == checkInRes.getReservationId()) 
					{
						waiter.setStatus("seated");
						waiter.setExitTimeFromList(new Timestamp(System.currentTimeMillis()));
						// update the waitlist status to seated in the DB 
						if (!DBC.updateStatusAndExitTimeInWaitingListQuery(waiter)) 
						{
							System.out.println("Failed to remove from waitlist.");
							return null; // failed to remove from waitlist
						}
						break;
					}
				}
			}
			
		 } 
		 else //no suitable table found get in to the waitlist
		 {
				System.out.println("No suitable table available for immediate seating. Adding to waitlist.");

				WaitList waitListEntry = new WaitList();
				
				waitListEntry.setReservationId(checkInRes.getReservationId());
				waitListEntry.setEntryTimeToList(new Timestamp(System.currentTimeMillis()));
				waitListEntry.setStatus("waiting");
				waitListEntry.setType("check_in");
				
				
				if (!DBC.addToWaitList(waitListEntry)) 
				{
					System.out.println("Failed to add to waitlist.");
					return null; // failed to add to waitlist
				}

				// update the reservation status to waiting in the DB return true if successful
				// or false if failed
				checkInRes.setStatus("waiting");
				if (!DBC.updateReservation(checkInRes)) 
				{
					System.out.println("Failed to update reservation to waiting.");
					return null; // failed to update reservation
				}
				
				
				
		 }
		
		 return checkInRes;
	}

	/**
	 * Adds a new table to the database based on the details provided in the
	 * message.
	 *
	 * @param msg The message containing the table details. The content of the
	 *            message is expected to be an ArrayList<Object> with the following
	 *            order: [Location 0 : seatsNumber (Integer), Location 1 : location
	 *            (String)]
	 * @return The Table object that was added to the database, or null if an error
	 */
	private Table addTable(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the reservation details from the message content
		
		Table table =new Table();
		
		//tableId  give by DB auto increment
		
		// Setting seats Number from the list we got from the message content
		if (list.get(0) instanceof Integer) 
		{
			table.setSeatsNumber((int) list.get(0)); 
		} 
		else 
		{
			System.out.println("Error: Index 0 is not a Integer!");
			return null;
		}
		
		// Setting location from the list we got from the message content
		if (list.get(1) instanceof String) 
		{
			table.setLocation((String) list.get(1)); 
		} 
		else 
		{
			System.out.println("Error: Index 1 is not a String!");
			return null;
		}
		
		table.setStatus("available");
		
		
		return DBC.addTableQuery(table);//return the table added to the DB (with tableId given by the DB) or null if failed

	}

	/**
	 * Deletes a table from the database based on the table ID provided in the
	 * message.checks for conflicts with existing reservations before deletion.
	 *
	 * @param msg The message containing the table ID. The content of the message is
	 *            expected to be an ArrayList<Object> with the following order:
	 *            [Location 0 : tableId (Integer)]
	 * @return A String indicating the result of the deletion:
	 *         - "true" if the table was deleted successfully.
	 *         - A date string (YYYY-MM-DD) if there are conflicts, indicating the next
	 *         available date to delete.
	 *         - null if an error occurs.
	 */
	private String deleteTable(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the reservation details from the message content

		int tableId=0;

		// Setting tableId from the list we got from the message content
		if (list.get(0) instanceof Integer) 
		{
			tableId =(int) list.get(0); 
		} 
		else 
		{
			System.out.println("Error: Index 0 is not a Integer!");
			return null;
		}

		LocalDate safeDateToDelete = calculateEarliestSafeDate(tableId, -1);


		//if the calculation failed
		if (safeDateToDelete == null) 
		{
			return null;
		}

		//if there are conflicts, return the next available date to delete
		else if (safeDateToDelete.isAfter(LocalDate.now()))
		{
			return safeDateToDelete.toString();
		}
		
		else if (safeDateToDelete.isEqual(LocalDate.now()))
        {
			// delete the table from the DB
			if (DBC.deleteTableQuery(tableId)) 
			{
				return "true"; //table deleted successfully;
			}
			return null; // DB Error
        }
		return null;
	}
	
	/**
	 * Updates the number of seats for a table in the database based on the details
	 * provided in the message. Checks for conflicts with existing reservations
	 * before updating.
	 *
	 * @param msg The message containing the table update details. The content of
	 *            the message is expected to be an ArrayList<Object> with the
	 *            following order: [Location 0 : tableId (Integer), Location 1 :
	 *            seatsNumber (Integer)]
	 * @return A String indicating the result of the update: - "true" if the table
	 *         was updated successfully. - A date string (YYYY-MM-DD) if there are
	 *         conflicts, indicating the next available date to update. - null if an
	 *         error occurs.
	 */
	private String updateTableSeatsNumber(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the reservation details from the message content
		
		int tableId=0;
		int seatsNumber=0;
		
		// Setting tableId from the list we got from the message content
		if (list.get(0) instanceof Integer) 
		{
			tableId =(int) list.get(0); 
		} 
		else 
		{
			System.out.println("Error: Index 0 is not a Integer!");
			return null;
		}
		
		// Setting seats Number from the list we got from the message content
		if (list.get(1) instanceof Integer) 
		{
			seatsNumber=(int) list.get(1); 
		} 
		else 
		{
			System.out.println("Error: Index 1 is not a Integer!");
			return null;
		}
		//calculate the earliest safe date to update the table seats number
		LocalDate safeDateToUpdate = calculateEarliestSafeDate(tableId, seatsNumber);

		//if the calculation failed
		if (safeDateToUpdate == null) 
		{
			return null;
		}

		//if there are conflicts, return the next available date to update
		else if (safeDateToUpdate.isAfter(LocalDate.now()))
		{
			return safeDateToUpdate.toString();
		}

		//else if safe to update today
		else if (safeDateToUpdate.isEqual(LocalDate.now()))
        {
			
			if (updateTable(tableId, "seatsNumber", seatsNumber)) 
			{
				return "true"; //table updated successfully;
			}
			return null; // DB Error
        }
		return null;
	}
}


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////Automated tasks//////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////





