package controller;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import data.Message;
import data.OpeningHours;
import data.OpeningHoursPerDay;
import data.Subscriber;

import data.TableReservation;
import data.TimeSlot;

public class OpeningTimeController 
{

	private static DataBaseController DBC=DataBaseController.getInstance();//Interfacing with the DB Controller
	/**
	 * Default constructor
	 */
	public OpeningTimeController() 
	{

	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////managing messages //////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Handles messages received from the server and performs corresponding actions
	 * related to opening times.
	 *
	 * @param msg The message received from the server containing the command and
	 *            content.
	 * @return The result of the action performed, which can vary based on the
	 *         command.
	 */
	public Object handleMessageFromServer(Message msg) 
	{

		switch (msg.command) //Checking the type of message sent from the server (what action should be performed in the DB Controller)
		{
		case CLOSE_RESTAURANT_ON_SPECIAL_DAY:
			return closeRestaurantOnSpecialDay(msg);
		case UPDATE_OPENING_TIME:
			return updateOpeningTime(msg);
		case UPDATE_SPECIAL_OPENING_TIME:
			return updateSpecialOpeningTime(msg);
		case ADD_NEW_OPENING_TIME:
			return addNewOpeningTime(msg);

		case ADD_NEW_SPECIAL_OPENING_TIME:
			return addNewSpecialOpeningTime(msg);

		case DELETE_OPENING_TIME:
			return deleteOpeningTime(msg);

		case DELETE_SPECIAL_OPENING_TIME:
			return deleteSpecialOpeningTime(msg);

		case GET_WEEKLY_OPENING_TIME:
			return getWeeklyOpeningTime(msg);

		case GET_SPECIAL_OPENING_TIME:
			return getSpecialOpeningTime(msg);
		default:
			System.out.println("Unknown task received.");
			return null;
		}
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////Helper methods//////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////	


	/**
	 * Retrieves the opening time slots for a specific date from the database.Special Hours or Weekly Hours
	 *
	 * @param date The date for which to retrieve opening time slots.
	 * @return An ArrayList of TimeSlot objects representing the opening time slots
	 *         for the specified date.
	 */
	public static ArrayList<TimeSlot> getOpeningTime(LocalDate day)
	{
		OpeningHoursPerDay openingHours = new OpeningHoursPerDay(day);

		DBC.getOpeningHoursByDate(openingHours);//update opening hours for the specific date from the DB in the OpeningHoursPerDay object else put null in the slots list

		return openingHours.getSlots();
	}


	/**
	 * Retrieves a list of table reservations that conflict with the specified special opening hours for a specific date.
	 *
	 * @param openingHours The OpeningHoursPerDay object representing the specific date and time slots.
	 * @return An ArrayList of TableReservation objects that conflict.
	 */
	private static ArrayList<TableReservation> getConflictingReservationsForSpecialOpeningTime(OpeningHoursPerDay openingHours)
	{
		// Get all active reservations
		ArrayList<TableReservation> reservations = ReservationControler.getAllReservationsActive();

		final long RESERVATION_DURATION_MINUTES = 120; // Assuming 2 hours duration

		if (reservations == null || reservations.isEmpty()) 
		{
			return new ArrayList<TableReservation>(); 
		}

		ArrayList<TableReservation> conflictedReservations = new ArrayList<>();

		for (TableReservation res : reservations) 
		{
			Timestamp dbTimestamp = res.getReservationDate();

			if (dbTimestamp == null) {
				continue;
			}

			// Convert Timestamp to LocalDateTime
			LocalDateTime resDateTime = dbTimestamp.toLocalDateTime();

			// Extract the specific LocalDate from the reservation
			LocalDate resDate = resDateTime.toLocalDate();

			LocalTime resStartTime = resDateTime.toLocalTime();
			LocalTime resEndTime = resStartTime.plusMinutes(RESERVATION_DURATION_MINUTES);
			boolean endsNextDay = resEndTime.isBefore(resStartTime); // check if the reservation ends the next day

			// Check if the reservation date matches the special opening hours date
			if (resDate.equals(openingHours.getDay())) 
			{
				// Check if the reservation time falls within any of the opening time slots
				if (openingHours.getSlots() != null && !openingHours.getSlots().isEmpty()) 
				{
					TimeSlot slot = openingHours.getSlots().get(0); 
					
					// Determine if this is an overnight shift (e.g., 12:00 to 08:00)
					boolean isOvernight = slot.getOpen().isAfter(slot.getClose());
					boolean conflict = false;

					if (isOvernight) 
					{
						// Overnight Logic: Conflict if reservation touches morning part OR evening part
						boolean touchesMorningPart = resStartTime.isBefore(slot.getClose());
						boolean touchesEveningPart = endsNextDay || resEndTime.isAfter(slot.getOpen());
						
						conflict = touchesMorningPart || touchesEveningPart;
					} 
					else 
					{
						// Standard Logic: Conflict if reservation overlaps the single interval
						boolean startsBeforeSlotCloses = resStartTime.isBefore(slot.getClose());
						boolean endsAfterSlotOpens = endsNextDay || resEndTime.isAfter(slot.getOpen());
						
						conflict = startsBeforeSlotCloses && endsAfterSlotOpens;
					}

					if (conflict)
					{
						conflictedReservations.add(res);
					}
				}
			}
		}

		return conflictedReservations;
	}

	/**
	 * Retrieves a list of table reservations that conflict with the specified opening  hours that is being deleted.
	 * conflicting means that the reservation time overlaps with the opening hours time slots.
	 *
	 * @param openingHours The OpeningHours object representing the day and time slots of
	 * opening hours to  check against.
	 * @return An ArrayList of TableReservation objects that conflict with the
	 *         specified opening hours.
	 */
	private static ArrayList<TableReservation> getConflictingReservationsForOpeningTime(OpeningHours openingHours)
	{
		//get all active reservations from the DB
		ArrayList<TableReservation> reservations = ReservationControler.getAllReservationsActive();

		final long RESERVATION_DURATION_MINUTES = 120; //assuming each reservation lasts for 2 hours

		if (reservations == null || reservations.isEmpty()) 
		{
			return new ArrayList<TableReservation>(); 
		}

		ArrayList<TableReservation> conflictedReservations = new ArrayList<>();


		for (TableReservation res : reservations) 
		{

			//get the reservation date from the DB
			Timestamp dbTimestamp = res.getReservationDate();


			if (dbTimestamp == null) {
				continue;
			}

			//convert Timestamp to LocalDateTime
			LocalDateTime resDateTime = dbTimestamp.toLocalDateTime();

			//get the day name of the reservation date (e.g., MONDAY, TUESDAY)
			String resDayName = resDateTime.getDayOfWeek().name();

			//get the time start of the reservation
			LocalTime resStartTime = resDateTime.toLocalTime();

			//Calculate the end time of the reservation
			LocalTime resEndTime = resStartTime.plusMinutes(RESERVATION_DURATION_MINUTES);

			// check if the reservation ends the next day
			boolean endsNextDay = resEndTime.isBefore(resStartTime);

			//check if the reservation day matches the opening hours day that we want to check
			if (resDayName.equalsIgnoreCase(openingHours.getDay())) 
			{

				//check if the reservation time falls within any of the opening time slots
				if (openingHours.getSlots() != null && !openingHours.getSlots().isEmpty()) 
				{
					TimeSlot slot = openingHours.getSlots().get(0); //assuming only one time slot per day for simplicity;
					
					// Determine if this is an overnight shift
					boolean isOvernight = slot.getOpen().isAfter(slot.getClose());
					boolean conflict = false;

					if (isOvernight) 
					{
						// Overnight Logic
						boolean touchesMorningPart = resStartTime.isBefore(slot.getClose());
						boolean touchesEveningPart = endsNextDay || resEndTime.isAfter(slot.getOpen());
						
						conflict = touchesMorningPart || touchesEveningPart;
					} 
					else 
					{
						// Standard Logic
						boolean startsBeforeSlotCloses = resStartTime.isBefore(slot.getClose());
						boolean endsAfterSlotOpens = endsNextDay || resEndTime.isAfter(slot.getOpen());
						
						conflict = startsBeforeSlotCloses && endsAfterSlotOpens;
					}

					if (conflict) 
					{
						conflictedReservations.add(res);
					}
				}
			}
		}

		return conflictedReservations; //return the list of conflicting reservations 
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////Logic methods//////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Closes the restaurant on a specific date.
	 * If active reservations exist on that date, it returns the list of subscribers with
	 * conflicting reservations instead of closing the restaurant 
	 * If no conflicts: deletes any existing special opening hours for that date and
	 * creates a new special opening hours 00:00-00:00 (Closed)
	 *
	 * @param msg Message containing: [Location 0: LocalDate (the date to close)]
	 * @return 
	 * - Empty ArrayList: Successfully closed.
	 * - ArrayList with Subscribers: Failed, list of conflicting customers.
	 * - null: Error.
	 */
	private synchronized static ArrayList<Subscriber> closeRestaurantOnSpecialDay(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;


		if (list == null || list.isEmpty()) {
			System.out.println("Error: Invalid message content.");
			return null;
		}

		LocalDate dateToClose;

		// Setting dateToClose from the list
		if (list.get(0) instanceof LocalDate) {
			dateToClose = (LocalDate) list.get(0);
		} 
		else 
		{
			System.out.println("Error: Index 0 is not a LocalDate!");
			return null;
		}   


		ArrayList<TableReservation> allReservations = ReservationControler.getAllReservationsActive();
		ArrayList<TableReservation> conflicts = new ArrayList<>();

		if (allReservations != null) 
		{
			for (TableReservation res : allReservations) 
			{
				if (res.getReservationDate() != null) 
				{
					// Check if the reservation falls on the requested date
					LocalDate resDate = res.getReservationDate().toLocalDateTime().toLocalDate();

					if (resDate.equals(dateToClose)) 
					{
						conflicts.add(res);
					}
				}
			}
		}

		//  If conflicts found, return Subscribers of the list of conflicting reservations
		if (!conflicts.isEmpty()) 
		{
			ArrayList<Subscriber> conflictingSubscribers = CustomerController.getSubscribersFromReservations(conflicts);
			System.out.println("\n====== Close Restaurant Failed: Conflicts Found on " + dateToClose + " ===");
			if (conflictingSubscribers != null) {
				for (Subscriber sub : conflictingSubscribers) {
					String name = (sub.getSubscriberId() > 0) ? sub.getFirstName() + " " + sub.getLastName() : "Guest";
					System.out.println(" -> " + name + " | Email: " + sub.getEmail() + " | Phone: " + sub.getPhoneNumber());
				}
			}
			return conflictingSubscribers;
		}

		//  No conflicts - Set hours to 00:00 - 00:00 (Closed) 
		OpeningHoursPerDay closedHours = new OpeningHoursPerDay(dateToClose);
		ArrayList<TimeSlot> closedSlotList = new ArrayList<>();


		closedSlotList.add(new TimeSlot(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT));  // 00:00 to 00:00 implies "Closed"
		closedHours.setSlots(closedSlotList);


		// If a record exists for this date , delete it and insert the closed hours.
		// If no record exists , Insert a new one.
		// return true if successful , else false
		if (DBC.closeRestaurantOnSpecialDayQuery(closedHours)) 
		{
			System.out.println("Restaurant closed successfully on " + dateToClose);
			return new ArrayList<Subscriber>(); // Success
		}

		System.out.println("Error: Database operation failed.");
		return null;// Database Error
	}

	/**
	 * Updates the special opening time for a specific date.changes old opening time
	 * to the new one. if the new opening time is later than the old opening time or
	 * the new closing time is earlier than the old closing time deletes the lost
	 * hours and checks for conflicting reservations. If there are conflicting
	 * reservations, the update fails and returns the list of subscribers with
	 * conflicting reservations.
	 * 
	 * 
	 *
	 * @param msg The message containing the reservation details. The content of the
	 *            message is expected to be an ArrayList<Object> with the following
	 *            order:[Location 0 : LocalDate (specific date), Location 1 :
	 *            LocalTime (old opening time), Location 2 : LocalTime (old closing
	 *            time), Location 3 : LocalTime (new opening time), Location 4 :
	 *            LocalTime (new closing time)]
	 * @return - Empty ArrayList<Subscriber>: update successful, no conflicts.
	 *         -ArrayList<Subscriber> : update failed due to conflicts, the list
	 *         contains the subscribers that have conflicting reservations. 
	 *         - null:error occurred during update.
	 */
	private synchronized static ArrayList<Subscriber> updateSpecialOpeningTime(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;


		if (list == null || list.size() < 5) {
			System.out.println("Error: Invalid message content size.");
			return null;
		}

		LocalDate day; 
		LocalTime oldOpen ;
		LocalTime oldClose ;
		LocalTime newOpen ;
		LocalTime newClose ;


		// Setting day from the list 
		if (list.get(0) instanceof LocalDate) {
			day = (LocalDate) list.get(0);
		} 
		else 
		{
			System.out.println("Error: Index 0 is not a LocalDate!");
			return null;
		}	

		// Setting Old opening time 
		if (list.get(1) instanceof LocalTime) {
			oldOpen=(LocalTime) list.get(1) ;
		} 
		else 
		{
			System.out.println("Error: Index 1 is not a LocalTime!");
			return null;
		}	

		// Setting Old closing time 
		if (list.get(2) instanceof LocalTime) {
			oldClose = (LocalTime) list.get(2);
		} else {
			System.out.println("Error: Index 2 is not a LocalTime!");
			return null;
		}

		// Setting New opening time 
		if (list.get(3) instanceof LocalTime) {
			newOpen=(LocalTime) list.get(3) ;
		} 
		else 
		{
			System.out.println("Error: Index 3 is not a LocalTime!");
			return null;
		}	

		// Setting New closing time 
		if (list.get(4) instanceof LocalTime) {
			newClose = (LocalTime) list.get(4);
		} else {
			System.out.println("Error: Index 4 is not a LocalTime!");
			return null;
		}


		ArrayList<TimeSlot> slotsToCheck = new ArrayList<>();



		// Check if we are opening later (cutting morning hours)
		if (newOpen.isAfter(oldOpen)) {
			slotsToCheck.add(new TimeSlot(oldOpen, newOpen));
		}

		// Check if we are closing earlier (cutting evening hours)
		if (newClose.isBefore(oldClose)) {
			slotsToCheck.add(new TimeSlot(newClose, oldClose));
		}


		// Check for conflicts in the lost hours 
		if (!slotsToCheck.isEmpty()) 
		{

			OpeningHoursPerDay lostHoursToCheck = new OpeningHoursPerDay(day);
			lostHoursToCheck.setSlots(slotsToCheck);


			ArrayList<TableReservation> conflicts = getConflictingReservationsForSpecialOpeningTime(lostHoursToCheck);

			if (!conflicts.isEmpty()) 
			{
				ArrayList<Subscriber> conflictingSubscribers = CustomerController.getSubscribersFromReservations(conflicts);
				System.out.println("\n====== Update Special Opening Time Failed: Conflicts Found ===");
				if (conflictingSubscribers != null) {
					for (Subscriber sub : conflictingSubscribers) {
						String name = (sub.getSubscriberId() > 0) ? sub.getFirstName() + " " + sub.getLastName() : "Guest";
						System.out.println(" -> Conflict: " + name + " | Email: " + sub.getEmail() + " | Phone: " + sub.getPhoneNumber());
					}
				}
				// Return the list of subscribers who are disturbed
				return conflictingSubscribers;
			}
		}

		// No conflicts or hours were extended - proceed with the update


		OpeningHoursPerDay newHours = new OpeningHoursPerDay(day);
		ArrayList<TimeSlot> newSlots = new ArrayList<>();
		newSlots.add(new TimeSlot(newOpen, newClose));
		newHours.setSlots(newSlots);

		OpeningHoursPerDay oldHours = new OpeningHoursPerDay(day);
		ArrayList<TimeSlot> oldSlots = new ArrayList<>();
		oldSlots.add(new TimeSlot(oldOpen, oldClose));
		oldHours.setSlots(oldSlots);

		// get old hours and change to new hours in the DB return true if successful 	else false
		if (DBC.updateSpecialOpeningTimeQuery(oldHours, newHours)) 
		{
			System.out.println("Special Opening Time for " + day + " updated successfully.");
			return new ArrayList<Subscriber>(); // Success 
		}

		System.out.println("Error: Database update failed.");
		return null;// Database Error
	}


	/**
	 * Updates the opening time for a  day of the week.changes old opening time to the new one.
	 * if the new opening time is later than the old opening time or the new closing time is earlier than the old closing time
	 * deletes the lost hours and checks for conflicting reservations.
	 * If there are conflicting reservations, the update fails and returns the list of subscribers with conflicting reservations.
	 * 
	 * 
	 *
	 * @param msg The message containing the reservation details. The content of the
	 *            message is expected to be an ArrayList<Object> with the following
	 *            order:[Location 0 : String ('SUNDAY', 'MONDAY', 'TUESDAY',
	 *            'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'), Location 1 :
	 *            LocalTime (old opening time), Location 2 : LocalTime (old closing
	 *            time), Location 3 : LocalTime (new opening time), Location 4 :
	 *            LocalTime (new closing time)]
	 * @return - Empty ArrayList<Subscriber>: update successful, no conflicts.
	 *  -ArrayList<Subscriber> : update failed due to conflicts, the list
	 *         contains the subscribers that have conflicting reservations. 
	 *         - null: error occurred during update.
	 */
	private synchronized static ArrayList<Subscriber> updateOpeningTime(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the customer id from the message content


		if (list == null || list.size() < 5) {
			System.out.println("Error: Invalid message content size.");
			return null;
		}

		String day;
		LocalTime oldOpen ;
		LocalTime oldClose ;
		LocalTime newOpen ;
		LocalTime newClose ;


		// Setting day from the list we got from the message content
		if (list.get(0) instanceof String) {
			day = (String) list.get(0);
		} 
		else 
		{
			System.out.println("Error: Index 0 is not a String!");
			return null;
		}	

		// Setting opening time from the list we got from the message content
		if (list.get(1) instanceof LocalTime) {
			oldOpen=(LocalTime) list.get(1) ;
		} 
		else 
		{
			System.out.println("Error: Index 1 is not a LocalTime!");
			return null;
		}	

		// Setting closing time from the list we got from the message content
		if (list.get(2) instanceof LocalTime) {
			oldClose = (LocalTime) list.get(2);
		} else {
			System.out.println("Error: Index 2 is not a LocalTime!");
			return null;
		}

		// Setting opening time from the list we got from the message content
		if (list.get(3) instanceof LocalTime) {
			newOpen=(LocalTime) list.get(3) ;
		} 
		else 
		{
			System.out.println("Error: Index 3 is not a LocalTime!");
			return null;
		}	

		// Setting closing time from the list we got from the message content
		if (list.get(4) instanceof LocalTime) {
			newClose = (LocalTime) list.get(4);
		} else {
			System.out.println("Error: Index 4 is not a LocalTime!");
			return null;
		}



		ArrayList<TimeSlot> slotsToCheck = new ArrayList<>();

		// Check if we are opening later (cutting morning hours)
		if (newOpen.isAfter(oldOpen)) {
			slotsToCheck.add(new TimeSlot(oldOpen, newOpen));
		}

		// Check if we are closing earlier (cutting evening hours)
		if (newClose.isBefore(oldClose)) {
			slotsToCheck.add(new TimeSlot(newClose, oldClose));
		}


		//Check for conflicts reservtions in the lost hours 
		if (!slotsToCheck.isEmpty()) 
		{
			// Create a temporary object just for the check
			OpeningHours lostHoursToCheck = new OpeningHours(day);
			lostHoursToCheck.setSlots(slotsToCheck);

			ArrayList<TableReservation> conflicts = getConflictingReservationsForOpeningTime(lostHoursToCheck);

			if (!conflicts.isEmpty()) 
			{
				ArrayList<Subscriber> conflictingSubscribers = CustomerController.getSubscribersFromReservations(conflicts);
				System.out.println("\n====== Update Opening Time Failed: Conflicts Found ===");
				if (conflictingSubscribers != null) {
					for (Subscriber sub : conflictingSubscribers) {
						String name = (sub.getSubscriberId() > 0) ? sub.getFirstName() + " " + sub.getLastName() : "Guest";
						System.out.println(" -> Conflict: " + name + " | Email: " + sub.getEmail() + " | Phone: " + sub.getPhoneNumber());
					}
				}
				// Return the list of subscribers who are disturbed
				return conflictingSubscribers;
			}
		}

		//  No conflicts or hours were extended - proceed with the update
		OpeningHours newHours = new OpeningHours(day);
		ArrayList<TimeSlot> newSlots = new ArrayList<>();
		newSlots.add(new TimeSlot(newOpen, newClose));
		newHours.setSlots(newSlots);

		OpeningHours oldHours = new OpeningHours(day);
		ArrayList<TimeSlot> oldSlots = new ArrayList<>();
		oldSlots.add(new TimeSlot(oldOpen, oldClose));
		oldHours.setSlots(oldSlots);

		// get old hours and change to new hours in the DB return true if successful 	else false
		if (DBC.updateOpeningTimeQuery(oldHours,newHours)) 
		{
			System.out.println("Opening Time for " + day + " updated successfully.");
			return new ArrayList<Subscriber>(); // Success 
		}

		System.out.println("Error: Database update failed.");
		return null;// Database Error

	}


	/**
	 * Updates the opening time for a specific day of the week.deletes old opening time and insert the new one.
	 *
	 * @param msg The message containing the reservation details. The content of the
	 *            message is expected to be an ArrayList<Object> with the following
	 *            order:[Location 0 :  String ('SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'),
	 *             Location 1 : LocalTime (opening time), 
	 *             Location 2 : LocalTime (closing time)]
	 * @return true if the update was successful, false otherwise.
	 */
	private synchronized static boolean addNewOpeningTime(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the customer id from the message content

		String day;
		LocalTime openingTimeFromMsg ;
		LocalTime closingTimeFromMsg ;


		ArrayList<TimeSlot> timeSlotList = new ArrayList<>();

		// Setting day from the list we got from the message content
		if (list.get(0) instanceof String) {
			day = (String) list.get(0);
		} 
		else 
		{
			System.out.println("Error: Index 0 is not a String!");
			return false;
		}	
		OpeningHours openingHours = new OpeningHours(day);//create OpeningHours object for the specific date

		// Setting opening time from the list we got from the message content
		if (list.get(1) instanceof LocalTime) {
			openingTimeFromMsg=(LocalTime) list.get(1) ;
		} 
		else 
		{
			System.out.println("Error: Index 1 is not a LocalTime!");
			return false;
		}	

		// Setting closing time from the list we got from the message content
		if (list.get(2) instanceof LocalTime) {
			closingTimeFromMsg = (LocalTime) list.get(2);
		} else {
			System.out.println("Error: Index 2 is not a LocalTime!");
			return false;
		}
		TimeSlot timeSlot=new TimeSlot(openingTimeFromMsg,closingTimeFromMsg);

		timeSlotList.add(timeSlot);//add the time slot to the list

		openingHours.setSlots(timeSlotList);//set the time slot list to the opening hours object



		//updateOpeningTimeQuery insert the new one
		return DBC.updateOpeningTimeQuery(openingHours);//return to server true if the update was successful, false otherwise
	}

	/**
	 * Adds new special opening time for a specific date.delete old special opening time if exists in this date.
	 *
	 * @param msg The message containing the special opening time details. The
	 *            content of the message is expected to be an ArrayList<Object> with
	 *            the following order:[Location 0 : LocalDate (specific date),
	 *            Location 1 : LocalTime (opening time),
	 *            Location 2 : LocalTime (closing time)]
	 * @return true if the addition was successful, false otherwise.
	 */
	private synchronized static boolean addNewSpecialOpeningTime(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the customer id from the message content

		LocalDate day;
		LocalTime openingTimeFromMsg ;
		LocalTime closingTimeFromMsg ;



		ArrayList<TimeSlot> timeSlotList = new ArrayList<>();

		// Setting day from the list we got from the message content
		if (list.get(0) instanceof LocalDate) {
			day = (LocalDate) list.get(0);
		} 
		else 
		{
			System.out.println("Error: Index 0 is not a LocalDate!");
			return false;
		}	
		OpeningHoursPerDay openingHours = new OpeningHoursPerDay(day);//create OpeningHoursPerDay object for the specific date
		// Setting opening time from the list we got from the message content
		if (list.get(1) instanceof LocalTime) {
			openingTimeFromMsg=(LocalTime) list.get(1) ;
		} 
		else 
		{
			System.out.println("Error: Index 1 is not a LocalTime!");
			return false;
		}	

		// Setting closing time from the list we got from the message content
		if (list.get(2) instanceof LocalTime) {
			closingTimeFromMsg = (LocalTime) list.get(2);
		} else {
			System.out.println("Error: Index 2 is not a LocalTime!");
			return false;
		}
		TimeSlot timeSlot=new TimeSlot(openingTimeFromMsg,closingTimeFromMsg);

		timeSlotList.add(timeSlot);//add the time slot to the list

		openingHours.setSlots(timeSlotList);//set the time slot list to the opening hours object


		//updateOpeningTimeQuery insert the new one
		return DBC.addNewSpecialOpeningTimeQuery(openingHours);//return to server true if the update was successful, false otherwise
	}

	/**
	 * Deletes the special opening time for a specific date.deletes only if there are no conflicting reservations.
	 *
	 * @param msg The message containing the reservation details. The content of the
	 *            message is expected to be an ArrayList<Object> with the following
	 *            order:[Location 0 : LocalDate (specific date),
	 *            Location 1 : LocalTime (opening time),
	 *            Location 2 : LocalTime (closing time)]
	 * 
	 * @return - Empty ArrayList<Subscriber>: deletion successful, no conflicts.
	 * - ArrayList<Subscriber> : deletion failed due to conflicts, the list contains the subscribers that have conflicting reservations.
	 * - null: error occurred during deletion.
	 */
	private synchronized static ArrayList<Subscriber> deleteSpecialOpeningTime(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the customer id from the message content

		LocalDate day;
		LocalTime openingTimeFromMsg ;
		LocalTime closingTimeFromMsg ;



		ArrayList<TimeSlot> timeSlotList = new ArrayList<>();

		// Setting day from the list we got from the message content
		if (list.get(0) instanceof LocalDate) {
			day = (LocalDate) list.get(0);
		} 
		else 
		{
			System.out.println("Error: Index 0 is not a LocalDate!");
			return null;
		}	
		OpeningHoursPerDay openingHours = new OpeningHoursPerDay(day);//create OpeningHoursPerDay object for the specific date
		// Setting opening time from the list we got from the message content
		if (list.get(1) instanceof LocalTime) {
			openingTimeFromMsg=(LocalTime) list.get(1) ;
		} 
		else 
		{
			System.out.println("Error: Index 1 is not a LocalTime!");
			return null;
		}	

		// Setting closing time from the list we got from the message content
		if (list.get(2) instanceof LocalTime) {
			closingTimeFromMsg = (LocalTime) list.get(2);
		} else {
			System.out.println("Error: Index 2 is not a LocalTime!");
			return null;
		}
		TimeSlot timeSlot=new TimeSlot(openingTimeFromMsg,closingTimeFromMsg);

		timeSlotList.add(timeSlot);//add the time slot to the list

		openingHours.setSlots(timeSlotList);//set the time slot list to the opening hours object



		ArrayList<TableReservation> conflicts = getConflictingReservationsForSpecialOpeningTime(openingHours);
		// If the list is NOT empty, we have conflicts. 
		if (!conflicts.isEmpty()) 
		{
			ArrayList<Subscriber> conflictingSubscribers = CustomerController.getSubscribersFromReservations(conflicts);
			System.out.println("\n====== Delete Special Opening Time Failed: Conflicts Found ===");
			if (conflictingSubscribers != null) {
				for (Subscriber sub : conflictingSubscribers) {
					String name = (sub.getSubscriberId() > 0) ? sub.getFirstName() + " " + sub.getLastName() : "Guest";
					System.out.println(" -> " + name + " | Email: " + sub.getEmail() + " | Phone: " + sub.getPhoneNumber());
				}
			}
			System.out.println("====================================================\n");

			return conflictingSubscribers; 
		}

		// else list is empty , Safe to delete.
		if (DBC.deleteSpecialOpeningTimeQuery(openingHours)) 
		{
			System.out.println("Special Opening Time for date " + day.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " deleted successfully.");
			return new ArrayList<Subscriber>();
		}

		//Database Error
		System.out.println("Error: Could not delete Special Opening Time for date " + day.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".");
		return null; 
	}

	/**
	 * Deletes the opening time for a specific day of the week.deletes only if there are no conflicting reservations.
	 *
	 * @param msg The message containing the reservation details. The content of the
	 *            message is expected to be an ArrayList<Object> with the following
	 *            order:[Location 0 : String ('SUNDAY', 'MONDAY', 'TUESDAY',
	 *            'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'), Location 1 :
	 *            LocalTime (opening time), Location 2 : LocalTime (closing time)]
	 *            
	 * @return - Empty ArrayList<Subscriber>: deletion successful, no conflicts.
	 * - ArrayList<Subscriber> : deletion failed due to conflicts, the list contains the subscribers that have conflicting reservations.
	 * - null: error occurred during deletion.
	 */
	private synchronized static ArrayList<Subscriber> deleteOpeningTime(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the customer id from the message content

		String day;
		LocalTime openingTimeFromMsg ;
		LocalTime closingTimeFromMsg ;


		ArrayList<TimeSlot> timeSlotList = new ArrayList<>();

		// Setting day from the list we got from the message content
		if (list.get(0) instanceof String) {
			day = (String) list.get(0);
		} 
		else 
		{
			System.out.println("Error: Index 0 is not a String!");
			return null;
		}	
		OpeningHours openingHours = new OpeningHours(day);//create OpeningHours object for the specific date

		// Setting opening time from the list we got from the message content
		if (list.get(1) instanceof LocalTime) {
			openingTimeFromMsg=(LocalTime) list.get(1) ;
		} 
		else 
		{
			System.out.println("Error: Index 1 is not a LocalTime!");
			return null;
		}	

		// Setting closing time from the list we got from the message content
		if (list.get(2) instanceof LocalTime) {
			closingTimeFromMsg = (LocalTime) list.get(2);
		} else {
			System.out.println("Error: Index 2 is not a LocalTime!");
			return null;
		}
		TimeSlot timeSlot=new TimeSlot(openingTimeFromMsg,closingTimeFromMsg);

		timeSlotList.add(timeSlot);//add the time slot to the list

		openingHours.setSlots(timeSlotList);//set the time slot list to the opening hours object



		ArrayList<TableReservation> conflicts = getConflictingReservationsForOpeningTime(openingHours);
		// If the list is NOT empty, we have conflicts. 
		if (!conflicts.isEmpty()) 
		{
			ArrayList<Subscriber> conflictingSubscribers = CustomerController.getSubscribersFromReservations(conflicts);
			System.out.println("\n====== Delete  Opening Time Failed: Conflicts Found ===");
			if (conflictingSubscribers != null) {
				for (Subscriber sub : conflictingSubscribers) {
					String name = (sub.getSubscriberId() > 0) ? sub.getFirstName() + " " + sub.getLastName() : "Guest";
					System.out.println(" -> " + name + " | Email: " + sub.getEmail() + " | Phone: " + sub.getPhoneNumber());
				}
			}
			System.out.println("====================================================\n");

			return conflictingSubscribers; 
		}

		// else list is empty , Safe to delete.
		if (DBC.deleteOpeningTimeQuery(openingHours)) 
		{
			System.out.println("Opening Time for day " + day + " deleted successfully.");
			return new ArrayList<Subscriber>();
		}

		//Database Error
		System.out.println("Error: Could not delete Opening Time for day " + day + ".");
		return null; 




	}

	/**
	 * Retrieves the special opening time for all special days from the database.
	 *
	 * @param msg The message containing the request details.not used in this method
	 * @return An ArrayList of OpeningHoursPerDay objects representing the special
	 *         opening time for each special day, or null if no special opening
	 *         hours are found in the database.
	 */
	private ArrayList<OpeningHoursPerDay> getSpecialOpeningTime(Message msg)
	{
		ArrayList<LocalDate> datesList = new  ArrayList<>();

		ArrayList<OpeningHoursPerDay> specialOpeningHoursAsList = new ArrayList<>();

		if (!DBC.getAllSpecialDaysQuery(datesList))// update datesList with all the special opening days without hours from the DB, return false not successful
		{
			return null;//
		}
		if (datesList.isEmpty()) 
		{
			return null;// return null to server that there are no special opening days in the DB
		}
		// For each date in datesList, get the opening hours and add to specialOpeningHoursAsList
		for (LocalDate day : datesList) 
		{
			OpeningHoursPerDay openingHoursForSpecificDate = new OpeningHoursPerDay(day);

			openingHoursForSpecificDate.setSlots(getOpeningTime(day));

			specialOpeningHoursAsList.add(openingHoursForSpecificDate);

		}
		return specialOpeningHoursAsList;
	}

	/**
	 * Retrieves the weekly opening time for all days of the week from the database.
	 *
	 * @param msg The message containing the request details.not used in this method
	 * @return An ArrayList of OpeningHours objects representing the weekly opening
	 *         time for each day of the week, or null if no opening hours are found
	 *         in the database.if a day is closed all day the slots list will be empty in the OpeningHours object
	 */
	private ArrayList<OpeningHours> getWeeklyOpeningTime(Message msg) 
	{
		ArrayList<OpeningHours> allHoursOfTheWeekAsList= new ArrayList<>();

		OpeningHours openingHoursForSunday = new OpeningHours("SUNDAY");

		if (!DBC.getWeeklyOpeningTimeForSpecificDayQuery(openingHoursForSunday))//update opening hours for Sunday from the DB in the OpeningHours object(if day closes all day put empty slots list) return true if successful, false otherwise
		{
			return null;//return to server that there are no opening hours in the DB
		}
		allHoursOfTheWeekAsList.add(openingHoursForSunday);

		OpeningHours openingHoursForMonday = new OpeningHours("MONDAY");
		if (!DBC.getWeeklyOpeningTimeForSpecificDayQuery(openingHoursForMonday))
		{
			return null;// return to server that there are no opening hours in the DB
		}
		allHoursOfTheWeekAsList.add(openingHoursForMonday);

		OpeningHours openingHoursForTuesday = new OpeningHours("TUESDAY");
		if (!DBC.getWeeklyOpeningTimeForSpecificDayQuery(openingHoursForTuesday)) {
			return null;// return to server that there are no opening hours in the DB
		}
		allHoursOfTheWeekAsList.add(openingHoursForTuesday);

		OpeningHours openingHoursForWednesday = new OpeningHours("WEDNESDAY");
		if (!DBC.getWeeklyOpeningTimeForSpecificDayQuery(openingHoursForWednesday)) {
			return null;// return to server that there are no opening hours in the DB
		}
		allHoursOfTheWeekAsList.add(openingHoursForWednesday);

		OpeningHours openingHoursForThursday = new OpeningHours("THURSDAY");
		if (!DBC.getWeeklyOpeningTimeForSpecificDayQuery(openingHoursForThursday)) {
			return null;// return to server that there are no opening hours in the DB
		}
		allHoursOfTheWeekAsList.add(openingHoursForThursday);

		OpeningHours openingHoursForFriday = new OpeningHours("FRIDAY");
		if (!DBC.getWeeklyOpeningTimeForSpecificDayQuery(openingHoursForFriday)) {
			return null;// return to server that there are no opening hours in the DB
		}
		allHoursOfTheWeekAsList.add(openingHoursForFriday);

		OpeningHours openingHoursForSaturday = new OpeningHours("SATURDAY");
		if (!DBC.getWeeklyOpeningTimeForSpecificDayQuery(openingHoursForSaturday)) {
			return null;// return to server that there are no opening hours in the DB
		}
		allHoursOfTheWeekAsList.add(openingHoursForSaturday);

		return allHoursOfTheWeekAsList;	

	}


}
