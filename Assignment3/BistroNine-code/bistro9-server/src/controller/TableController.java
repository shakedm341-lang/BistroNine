package controller;

import java.sql.Timestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;


import data.Message;
import data.Subscriber;
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
	 * Identifies reservations that conflict with a proposed update to a table's seat count.
	 * Simulates the restaurant state with the updated seat count to verify if all existing reservations 
	 * can still be accommodated using the "Time Snapshot" logic.
	 *
	 * @param tableId  The ID of the table to be updated.
	 * @param newSeats The proposed new number of seats for the table.
	 * @return ArrayList<TableReservation> A list of reservations that would cause overbooking if the update is applied.
	 */
	
	private ArrayList<TableReservation> getConflictingReservationsForUpdate(int tableId, int newSeats) 
	{
		// ---------------------------------------------------------
		// Retrieve current snapshot
		// ---------------------------------------------------------
		// Fetch all currently active reservations and the current  tables 
		ArrayList<TableReservation> allReservations = ReservationControler.getAllReservationsActive();
		ArrayList<Table> allTables = TableController.getTableInRestaurant();
		
		ArrayList<TableReservation> conflicts = new ArrayList<>();
		ArrayList<Integer> addedReservationIds = new ArrayList<>(); // Helper to avoid duplicates

		//if data is null, return empty list.
		if (allReservations == null || allTables == null) return new ArrayList<>();

		// ---------------------------------------------------------
		// Check for immediate conflicts 
		// ---------------------------------------------------------
		// Check if the table is currently occupied ("arrived").
		// Physical changes (like changing seats) cannot be made while customers are seated.
		for (TableReservation res : allReservations) 
		{
			if (res.getStatus().equalsIgnoreCase("arrived") && res.getTableId() == tableId) 
			{
				conflicts.add(res);
				addedReservationIds.add(res.getReservationId());
			}
		}

		// ---------------------------------------------------------
		// Build Simulated Table List ("Virtual Restaurant")
		// ---------------------------------------------------------
		// Create a copy of the restaurant tables, but apply the NEW seat count 
		// specifically to the table being updated.
		ArrayList<Table> simulatedTables = new ArrayList<>();
		for (Table t : allTables) 
		{
			if (!t.getStatus().equalsIgnoreCase("cancelled")) 
			{
				Table copy = new Table();
				copy.setTableId(t.getTableId());
				
				// Critical Step: Apply the proposed change to the simulation
				if (t.getTableId() == tableId) {
					copy.setSeatsNumber(newSeats); // Use the new requested size
				} else {
					copy.setSeatsNumber(t.getSeatsNumber()); // Keep original size for others
				}
				
				simulatedTables.add(copy);
			}
		}
		
		// Sort the remaining tables by seat count (Ascending).
		// This is crucial for the "Best Fit" algorithm to try assigning small groups to small tables first.
		sortTablesBySeatsAscending(simulatedTables);

		// ---------------------------------------------------------
		// Filter only active future reservations
		// ---------------------------------------------------------
		// We filter out cancelled or completed reservations, focusing only on future "active" ones.
		ArrayList<TableReservation> futureRes = new ArrayList<>();
		for (TableReservation res : allReservations) 
		{
			if (res.getStatus().equalsIgnoreCase("active")) futureRes.add(res);
		}

		// ---------------------------------------------------------
		//  Apply "Time Snapshot" Logic 
		// ---------------------------------------------------------
		// Collect all unique time points (start times) to check for overlaps.
		ArrayList<LocalDateTime> timePointsToCheck = new ArrayList<>();
		
		for (TableReservation res : futureRes) 
		{
			LocalDateTime time = res.getReservationDate().toLocalDateTime();
			// Manual check for duplicates to mimic Set behavior
			if (!timePointsToCheck.contains(time)) 
			{
				timePointsToCheck.add(time);
			}
		}

		// ---------------------------------------------------------
		// Execute Time Snapshot Analysis
		// ---------------------------------------------------------
		// Iterate through every critical time point to check if the NEW table configuration works.
		for (LocalDateTime timePoint : timePointsToCheck) 
		{
			// Get all reservations that are happening at this exact moment
			ArrayList<TableReservation> activeAtMoment = new ArrayList<>();
			for (TableReservation res : futureRes) 
			{
				LocalDateTime start = res.getReservationDate().toLocalDateTime();
				LocalDateTime end = start.plusHours(2); // Assuming 2 hours duration
				
				if ((start.isBefore(timePoint) || start.equals(timePoint)) && end.isAfter(timePoint)) 
				{
					activeAtMoment.add(res);
				}
			}

			// Check capacity: Does the updated table layout (simulatedTables) support the load?
			if (!canFitOptimally(activeAtMoment, simulatedTables)) 
			{
				// If not, flag all reservations in this time slot as conflicts
				for (TableReservation problematicRes : activeAtMoment)
				{
					if (!addedReservationIds.contains(problematicRes.getReservationId()))
					{
						conflicts.add(problematicRes);
						addedReservationIds.add(problematicRes.getReservationId());
					}
				}
			}
		}

		return conflicts;
	}
	
	
	/**
	 * Identifies reservations that conflict with the proposed deletion of a specific table.
	 * Simulates the restaurant state without the specified table to verify if remaining tables 
	 * can support existing reservations using the "Time Snapshot" logic.
	 *
	 * @param tableId The ID of the table to be deleted.
	 * @return ArrayList<TableReservation> A list of reservations that would be displaced/unassigned if the table is deleted.
	 */
	private ArrayList<TableReservation> getConflictingReservationsForDeletion(int tableId) 
	{
		// ---------------------------------------------------------
		// Retrieve Data Snapshot
		// ---------------------------------------------------------
		// Fetch all currently active reservations and the current  tables.
		ArrayList<TableReservation> allReservations = ReservationControler.getAllReservationsActive();
		ArrayList<Table> allTables = TableController.getTableInRestaurant();
		
		ArrayList<TableReservation> conflicts = new ArrayList<>();
		ArrayList<Integer> addedReservationIds = new ArrayList<>(); // Helper set to prevent duplicate entries in the conflict list

		// Safety check: if data is unavailable, assume no conflicts (or handle as error upstream)
		if (allReservations == null || allTables == null) return new ArrayList<>();

		// ---------------------------------------------------------
		// Immediate Conflict Check 
		// ---------------------------------------------------------
		// Check if the table is currently occupied by a customer ("arrived").
		// We cannot physically remove a table while people are sitting at it.
		for (TableReservation res : allReservations) 
		{
			if (res.getStatus().equalsIgnoreCase("arrived") && res.getTableId() == tableId) 
			{
				conflicts.add(res);
				addedReservationIds.add(res.getReservationId());
			}
		}

		// ---------------------------------------------------------
		//  Build Simulated Environment ("Virtual Restaurant")
		// ---------------------------------------------------------
		// Create a new list of tables that represents the restaurant state AFTER the proposed deletion.
		// We copy every table EXCEPT the one being deleted.
		ArrayList<Table> simulatedTables = new ArrayList<>();
		for (Table t : allTables) 
		{
			// Filter out cancelled tables and the specific tableId we want to delete
			if (!t.getStatus().equalsIgnoreCase("cancelled") && t.getTableId() != tableId) 
			{
				Table copy = new Table();
				copy.setTableId(t.getTableId());
				copy.setSeatsNumber(t.getSeatsNumber());
				simulatedTables.add(copy);
			}
		}
		
		// Sort the remaining tables by seat count (Ascending).
		// This is crucial for the "Best Fit" algorithm to try assigning small groups to small tables first.
		sortTablesBySeatsAscending(simulatedTables);

		// ---------------------------------------------------------
		// Filter Relevant Future Reservations
		// ---------------------------------------------------------
		// We only care about "active" reservations (future bookings). 
		// "Arrived" ones were handled , and "Cancelled/Completed" are irrelevant.
		ArrayList<TableReservation> futureRes = new ArrayList<>();
		for (TableReservation res : allReservations) 
		{
			if (res.getStatus().equalsIgnoreCase("active")) futureRes.add(res);
		}

		// ---------------------------------------------------------
		// Time Snapshot Logic Preparation
		// ---------------------------------------------------------
		// we collect critical "Time Points" (reservation start times).
		// If an overbooking happens, it will start at one of these moments.
		ArrayList<LocalDateTime> timePointsToCheck = new ArrayList<>();
		
		for (TableReservation res : futureRes) 
		{
			LocalDateTime time = res.getReservationDate().toLocalDateTime();
			// Manual check to ensure we only process each unique time slot once
			if (!timePointsToCheck.contains(time)) 
			{
				timePointsToCheck.add(time);
			}
		}

		// ---------------------------------------------------------
		// Execute Time Snapshot Analysis
		// ---------------------------------------------------------
		// Iterate through every critical time point to check restaurant capacity.
		for (LocalDateTime timePoint : timePointsToCheck) 
		{
			// Find all reservations that overlap with this specific time point.
			// (A reservation is active if: StartTime <= TimePoint < EndTime)
			ArrayList<TableReservation> activeAtMoment = new ArrayList<>();
			for (TableReservation res : futureRes) 
			{
				LocalDateTime start = res.getReservationDate().toLocalDateTime();
				LocalDateTime end = start.plusHours(2); // Assuming fixed duration of 2 hours per booking
				
				if ((start.isBefore(timePoint) || start.equals(timePoint)) && end.isAfter(timePoint)) 
				{
					activeAtMoment.add(res);
				}
			}

			// ---------------------------------------------------------
			// Verify Capacity 
			// ---------------------------------------------------------
			// Check if the REDUCED list of tables (simulatedTables) can handle the load (activeAtMoment).
			if (!canFitOptimally(activeAtMoment, simulatedTables)) 
			{
				// If they don't fit, mark ALL reservations active at this moment as potential conflicts.
				// This indicates that at this specific time, the restaurant is overbooked without the deleted table.
				for (TableReservation problematicRes : activeAtMoment)
				{
					if (!addedReservationIds.contains(problematicRes.getReservationId()))
					{
						conflicts.add(problematicRes);
						addedReservationIds.add(problematicRes.getReservationId());
					}
				}
			}
		}

		return conflicts;
	}
	
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

		sortTablesBySeatsAscending(tempTables);
		
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
		if (tables == null) {
	        return new ArrayList<Table>(); 
	    }
		
		ArrayList<Table> activeTables = new ArrayList<>();

	    for (Table table : tables) 
	    {
	        
	        if (!table.getStatus().equalsIgnoreCase("cancelled")) 
	        {
	            activeTables.add(table);
	        }
	    }
	    
	    return activeTables; //return the list of available/occupied tables to server
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
	 * Deletes a table. Returns a list of subscribers associated with conflicting reservations if conflicts exist.
	 * 
	 * * @param msg The message containing the table details. The content of the
	 *            message is expected to be an ArrayList<Object> with the following
	 *            order: [Location 0 : tableId (Integer)].
	 * @return 
	 * - Empty ArrayList<Subscriber>: Table deleted 
	 * - ArrayList<Subscriber> : can't deleted Table (List contains the subscribers involved in conflicting reservations).
	 * - null: Error or Invalid Input.
	 */
	private ArrayList<Subscriber> deleteTable(Message msg)
	{
		@SuppressWarnings("unchecked")
		ArrayList<Object> list = (ArrayList<Object>) msg.content;

		int tableId = 0;
		if (list.get(0) instanceof Integer) 
		{
			tableId = (int) list.get(0);
		} 
		else 
		{
			System.out.println("Error: Index 0 is not a Integer!");
			return null;
		}

		// Check for conflicts
		ArrayList<TableReservation> conflicts = getConflictingReservationsForDeletion(tableId);

		// If the list is NOT empty, we have conflicts. 
		if (!conflicts.isEmpty()) 
		{
			
			System.out.println("\n====== Delete Table Failed: Conflicts Found ===");
			System.out.println("Cannot delete Table ID: " + tableId + " because of the following reservations:");
			
			for (TableReservation res : conflicts) 
			{
				System.out.println(" -> Reservation ID: " + res.getReservationId() + 
								   " | Date: " + res.getReservationDate() + 
								   " | Diners: " + res.getNumberOfDiners() + 
								   " | Customer ID: " + res.getCustomerId());
			}
			System.out.println("====================================================\n");


			ArrayList<Subscriber> conflictingSubscribers = CustomerController.getSubscribersFromReservations(conflicts);
			
			return conflictingSubscribers; 
		}

		// else list is empty , Safe to delete.
		if (DBC.deleteTableQuery(tableId)) 
		{
			System.out.println("Table ID: " + tableId + " deleted successfully.");
			return new ArrayList<Subscriber>();
		}
		
		//Database Error
		System.out.println("Failed to delete table ID: " + tableId + " (DB Error).");
		return null; 
	}
	
	/**
	 * Updates the number of seats for a table. 
	 * Returns a list of subscribers associated with conflicting reservations if the update is not possible.
	 *
	 * @param msg The message containing the table update details. The content of
	 * 		the message is expected to be an ArrayList<Object> with the
	 * 		following order: [Location 0 : tableId (Integer), Location 1 :seatsNumber (Integer)]
	 * 
	 * @return 
	 * - Empty ArrayList<Subscriber>: Table updated
	 * - ArrayList<Subscriber>: can't updated Table (List contains the subscribers involved in conflicting reservations).
	 * - null: Error or Invalid Input.
	 */
	private ArrayList<Subscriber> updateTableSeatsNumber(Message msg)
	{
		@SuppressWarnings("unchecked")
		ArrayList<Object> list = (ArrayList<Object>) msg.content;
		
		int tableId = 0;
		int seatsNumber = 0;
		
		// Parse Input
		if (list.get(0) instanceof Integer) {
			tableId = (int) list.get(0);
		} else {
			return null;
		}
		
		if (list.get(1) instanceof Integer) {
			seatsNumber = (int) list.get(1);
		} else {
			return null;
		}

		// 1. Check for conflicts using the snapshot logic (Update Version)
		ArrayList<TableReservation> conflicts = getConflictingReservationsForUpdate(tableId, seatsNumber);

		// 2. If conflicts exist, print and return them
		if (!conflicts.isEmpty()) 
		{
			System.out.println("\n===  Update Table Failed: Conflicts Found ===");
			
			
			System.out.println("Cannot update Table ID: " + tableId + " to " + seatsNumber + " seats because of:");
			for (TableReservation res : conflicts) 
			{
				System.out.println(" -> Reservation ID: " + res.getReservationId() + 
								   " | Diners: " + res.getNumberOfDiners() + 
								   " | Date: " + res.getReservationDate());
			}
			System.out.println("====================================================\n");
			
			ArrayList<data.Subscriber> conflictingSubscribers = CustomerController.getSubscribersFromReservations(conflicts);
			
			return conflictingSubscribers;
		}

		// 3. No conflicts -> Perform Update
		if (updateTable(tableId, "seatsNumber", seatsNumber)) 
		{
			System.out.println(" Table ID: " + tableId + " updated to " + seatsNumber + " seats successfully.");
			return new ArrayList<Subscriber>(); // Success
		}
		
		return null; // DB Error
	}
}




