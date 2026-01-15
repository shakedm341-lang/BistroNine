package data;


import java.util.ArrayList;

/**
 * Entity class representing the opening hours for a specific day.
 * Holds the day of the week and a list of time slots during which the restaurant is active.
 */
public class OpeningHours 
{
	private String day;//'SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'
	private ArrayList<TimeSlot> slots;
	
	/**
	 * Constructor for OpeningHours.
	 * @param day the day of the week (e.g., 'SUNDAY', 'MONDAY').
	 */
	public OpeningHours(String day) 
	{
		this.setDay(day);
	}

	/**
	 * Gets the day of the week.
	 * @return The day as a String.
	 */
	public String getDay() {
		return day;
	}

	/**
	 * Sets the day of the week.
	 * @param day The day string to set.
	 */
	public void setDay(String day) {
		this.day = day;
	}

	/**
	 * Gets the list of available time slots for this day.
	 * @return An ArrayList of TimeSlot objects.
	 */
	public ArrayList<TimeSlot> getSlots() {
		return slots;
	}

	/**
	 * Sets the list of time slots for this day.
	 * @param slots An ArrayList of TimeSlot objects to set.
	 */
	public void setSlots(ArrayList<TimeSlot> slots) {
		this.slots = slots;
	}

}