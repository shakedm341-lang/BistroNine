package data;

import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Entity class representing the specific opening hours and available time slots for a particular date.
 */
public class OpeningHoursPerDay 
{
	private LocalDate day;
	private ArrayList<TimeSlot> slots;
	
	/**
	 * Constructor for OpeningHoursPerDay.
	 * @param day The specific LocalDate for which these opening hours apply.
	 */
	public OpeningHoursPerDay(LocalDate day) 
	{
		this.setDay(day);
	}

	/**
	 * Gets the list of available time slots for this specific day.
	 * @return An ArrayList of TimeSlot objects.
	 */
	public ArrayList<TimeSlot> getSlots() {
		return slots;
	}

	/**
	 * Sets the list of time slots for this specific day.
	 * @param slots An ArrayList of TimeSlot objects to set.
	 */
	public void setSlots(ArrayList<TimeSlot> slots) {
		this.slots = slots;
	}

	/**
	 * Gets the specific date associated with these opening hours.
	 * @return The LocalDate object.
	 */
	public LocalDate getDay() {
		return day;
	}

	/**
	 * Sets the specific date for these opening hours.
	 * @param day The LocalDate to set.
	 */
	public void setDay(LocalDate day) {
		this.day = day;
	}

}