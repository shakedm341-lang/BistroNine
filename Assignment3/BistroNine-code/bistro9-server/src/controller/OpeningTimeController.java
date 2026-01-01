package controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

import data.Message;
import data.OpeningHours;
import data.OpeningHoursPerDay;
import data.Table;
import data.TableReservation;
import data.TimeSlot;

public class OpeningTimeController 
{
	
	private static DataBaseController DBC=DataBaseController.getInstance();//Interfacing with the DB Controller

	public OpeningTimeController() 
	{
		
	}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////managing messages //////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	public Object handleMessageFromServer(Message msg) 
	{

		switch (msg.command) //Checking the type of message sent from the server (what action should be performed in the DB Controller)
		{
		case UPDATE_OPENING_TIME:
			return updateOpeningTime(msg);
			
		case ADD_NEW_SPECIAL_OPENING_TIME:
			return addNewSpecialOpeningTime(msg);
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
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////Logic methods//////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
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
	/**
     * Updates the opening time for a specific day of the week.deletes old opening time and insert the new one.
     *
     * @param msg The message containing the reservation details. The content of the
	 *            message is expected to be an ArrayList<Object> with the following
	 *            order:[Location 0 :  String ('SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'),
	 *             Location 1 : ArrayList<LocalTime> openingTimes(pairs of start and end times)]
     * @return true if the update was successful, false otherwise.
     */
	private boolean updateOpeningTime(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the customer id from the message content

		String day;
		ArrayList<LocalTime> openingTimeFromMsg = new ArrayList<>();


		// Setting day from the list we got from the message content
		if (list.get(0) instanceof String) {
			day = (String) list.get(0);
		} 
		else 
		{
			System.out.println("Error: Index 0 is not a String!");
			return false;
		}	

		// Setting opening time list from the list we got from the message content
		if (list.size() > 1 && list.get(1) instanceof ArrayList) 
		{
			ArrayList<?> tempList = (ArrayList<?>) list.get(1); 
	        
	        // Validate that the list contains LocalTime objects
	        if (!tempList.isEmpty() && !(tempList.get(0) instanceof LocalTime)) {
	             System.out.println("Error: List content is NOT LocalTime! (Maybe Strings?)");
	             return false;
	        }

	        @SuppressWarnings("unchecked")
	        ArrayList<LocalTime> specificList = (ArrayList<LocalTime>) list.get(1);
	        openingTimeFromMsg = specificList;
		}
		else 
		{
			System.out.println("Error: Index 1 is not a list !");
			return false;
		}
		//Validate that the list contains pairs of start and end times
		if (openingTimeFromMsg.size() % 2 != 0) {
			System.out.println("Error: Hours list must contain pairs (Start, End).");
			return false;
		}

		// Create TimeSlot objects from the opening times
		ArrayList<TimeSlot> openingTime = new ArrayList<>();
		for (int i = 0; i < openingTimeFromMsg.size(); i += 2) 
		{
			openingTime.add( new TimeSlot(openingTimeFromMsg.get(i),openingTimeFromMsg.get(i+1)));

		}

		//updateOpeningTimeQuery delete the old opening time and insert the new one(maybe have split opening hours so we need to delete all the old ones and insert the new ones)
		return DBC.updateOpeningTimeQuery(day, openingTime);//return to server true if the update was successful, false otherwise
	}
	
	/**
	 * Adds new special opening time for a specific date.delete old special opening time if exists in this date.
	 *
	 * @param msg The message containing the special opening time details. The
	 *            content of the message is expected to be an ArrayList<Object> with
	 *            the following order:[Location 0 : LocalDate (specific date),
	 *            Location 1 : ArrayList<LocalTime> openingTimes(pairs of start and
	 *            end times)]
	 * @return true if the addition was successful, false otherwise.
	 */
	private boolean addNewSpecialOpeningTime(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the customer id from the message content
		
		LocalDate day;
		ArrayList<LocalTime> openingTimeFromMsg = new ArrayList<>();


		// Setting day from the list we got from the message content
		if (list.get(0) instanceof LocalDate) {
			day = (LocalDate) list.get(0);
		} 
		else 
		{
			System.out.println("Error: Index 0 is not a String!");
			return false;
		}	

		// Setting opening time list from the list we got from the message content
		if (list.size() > 1 && list.get(1) instanceof ArrayList) 
		{
			ArrayList<?> tempList = (ArrayList<?>) list.get(1); 
	        
	        // Validate that the list contains LocalTime objects
	        if (!tempList.isEmpty() && !(tempList.get(0) instanceof LocalTime)) {
	             System.out.println("Error: List content is NOT LocalTime! (Maybe Strings?)");
	             return false;
	        }

	        @SuppressWarnings("unchecked")
	        ArrayList<LocalTime> specificList = (ArrayList<LocalTime>) list.get(1);
	        openingTimeFromMsg = specificList;
		}
		else 
		{
			System.out.println("Error: Index 1 is not a list !");
			return false;
		}
		//Validate that the list contains pairs of start and end times
		if (openingTimeFromMsg.size() % 2 != 0) {
			System.out.println("Error: Hours list must contain pairs (Start, End).");
			return false;
		}

		// Create TimeSlot objects from the opening times
		ArrayList<TimeSlot> openingTime = new ArrayList<>();
		for (int i = 0; i < openingTimeFromMsg.size(); i += 2) 
		{
			openingTime.add( new TimeSlot(openingTimeFromMsg.get(i),openingTimeFromMsg.get(i+1)));

		}
		// Create OpeningHoursPerDay object
		OpeningHoursPerDay openingHours = new OpeningHoursPerDay(day);
		openingHours.setSlots(openingTime);
		openingHours.setDay(day);
		
		
		//updateOpeningTimeQuery delete the old opening time and insert the new one(maybe have split opening hours so we need to delete all the old ones and insert the new ones)
		return DBC.addNewSpecialOpeningTimeQuery(openingHours);//return to server true if the update was successful, false otherwise
	}

}
